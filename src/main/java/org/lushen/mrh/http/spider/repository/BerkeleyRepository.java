package org.lushen.mrh.http.spider.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.lushen.mrh.http.spider.HttpRepository;
import org.lushen.mrh.http.spider.HttpUrl;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

public class BerkeleyRepository implements HttpRepository {

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock(false); // 读写锁

	private final String rootPath; // 存储目录

	private final String storeName; // 仓库名称

	private final boolean autoCleanup; //是否清理上次遗留数据

	private Environment environment;

	private EntityStore allStore;

	private PrimaryIndex<Long, PersistObject> allIndex;

	private EntityStore waitingStore;

	private PrimaryIndex<Long, PersistObject> waitingIndex;

	private EntityStore runningStore;

	private PrimaryIndex<Long, PersistObject> runningIndex;

	public BerkeleyRepository(String rootPath, String storeName, boolean autoCleanup) {
		super();
		this.rootPath = rootPath;
		this.storeName = storeName;
		this.autoCleanup = autoCleanup;
		initialize();
	}

	private void initialize() {

		// 初始化db目录
		File dbDirectory = new File(new File(this.rootPath), this.storeName);
		if ( ! dbDirectory.exists()) {
			dbDirectory.mkdirs();
		}
		if(this.autoCleanup) {
			try {
				FileUtils.cleanDirectory(dbDirectory);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		// 初始化db环境上下文
		EnvironmentConfig environmentConfig = new EnvironmentConfig();
		environmentConfig.setAllowCreate(true);
		environmentConfig.setTransactional(false);
		environmentConfig.setReadOnly(false);
		this.environment = new Environment(dbDirectory, environmentConfig);

		// 初始化总存储
		StoreConfig allConfig = new StoreConfig();
		allConfig.setAllowCreate(true);
		allConfig.setReadOnly(false);
		allConfig.setTransactional(false);
		this.allStore = new EntityStore(this.environment, this.storeName, allConfig);
		this.allIndex = this.allStore.getPrimaryIndex(Long.class, PersistObject.class);

		// 初始化等待中任务存储
		StoreConfig waitingConfig = new StoreConfig();
		waitingConfig.setAllowCreate(true);
		waitingConfig.setReadOnly(false);
		waitingConfig.setTransactional(false);
		this.waitingStore = new EntityStore(this.environment, StringUtils.join(this.storeName, ".wait"), waitingConfig);
		this.waitingIndex = this.waitingStore.getPrimaryIndex(Long.class, PersistObject.class);

		// 初始化运行中任务存储
		StoreConfig runningConfig = new StoreConfig();
		runningConfig.setAllowCreate(true);
		runningConfig.setReadOnly(false);
		runningConfig.setTransactional(false);
		this.runningStore = new EntityStore(this.environment, StringUtils.join(this.storeName, ".run"), runningConfig);
		this.runningIndex = this.runningStore.getPrimaryIndex(Long.class, PersistObject.class);

		// 运行中有任务，全部移动到等待中
		EntityCursor<PersistObject> cursor = runningIndex.entities();
		try {
			Iterator<PersistObject> iterator = cursor.iterator();
			while(iterator.hasNext()) {
				PersistObject entity = iterator.next();
				this.waitingIndex.putNoOverwrite(entity);
				iterator.remove();
			}
		} finally {
			cursor.close();
		}

	}

	@Override
	public void close() throws IOException {
		Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if(this.runningStore != null) {
				this.runningStore.close();
			}
			if(this.waitingStore != null) {
				this.waitingStore.close();
			}
			if(this.allStore != null) {
				this.allStore.close();
			}
			if(this.environment != null) {
				this.environment.close();
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean hasMore() {
		Lock lock = rwLock.readLock();
		lock.lock();
		try {
			return runningIndex.count() > 0 || waitingIndex.count() > 0;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public HttpAcknowledge pull() {
		List<HttpAcknowledge> acknowledges = pull(1);
		return acknowledges.isEmpty() ? null : acknowledges.get(0);
	}

	@Override
	public List<HttpAcknowledge> pull(int size) {
		Lock lock = rwLock.writeLock();
		lock.lock();
		try {
			List<HttpAcknowledge> entities = new ArrayList<HttpAcknowledge>();
			EntityCursor<PersistObject> cursor = waitingIndex.entities();
			try {
				Iterator<PersistObject> iterator = cursor.iterator();
				for(int i=0; i<size; i++) {
					if(iterator.hasNext()) {
						PersistObject entity = iterator.next();
						runningIndex.putNoOverwrite(entity);
						iterator.remove();
						entities.add(new BerkeleyAcknowledge(entity.toHttpUrl()));
					} else {
						break;
					}
				}
				return entities;
			} finally {
				cursor.close();
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void save(HttpUrl httpUrl) {
		saveBatch(Arrays.asList(httpUrl));
	}

	@Override
	public void saveBatch(List<HttpUrl> httpUrls) {
		Lock lock = rwLock.writeLock();
		lock.lock();
		try {
			for(HttpUrl httpUrl : httpUrls) {
				if(allIndex.get(httpUrl.getId()) == null) {
					PersistObject entity = new PersistObject();
					entity.setId(httpUrl.getId());
					entity.setParentId(httpUrl.getParentId());
					entity.setUrl(httpUrl.getUrl());
					entity.setTimestamp(httpUrl.getTimestamp());
					entity.setDepth(httpUrl.getDepth());
					entity.setAttempts(httpUrl.getAttempts());
					allIndex.putNoOverwrite(entity);
					waitingIndex.putNoOverwrite(entity);
				}
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void trancate() {
		Lock lock = rwLock.writeLock();
		lock.lock();
		try {
			allStore.truncateClass(PersistObject.class);
			waitingStore.truncateClass(PersistObject.class);
			runningStore.truncateClass(PersistObject.class);
		} finally {
			lock.unlock();
		}
	}

	private class BerkeleyAcknowledge implements HttpAcknowledge {

		private HttpUrl httpUrl;

		private boolean ack;

		public BerkeleyAcknowledge(HttpUrl httpUrl) {
			super();
			this.httpUrl = httpUrl;
			this.ack = false;
		}

		@Override
		public HttpUrl get() {
			return this.httpUrl;
		}

		@Override
		public void ack(boolean success) {
			Lock lock = rwLock.writeLock();
			lock.lock();
			try {
				if(this.ack) {
					throw new RuntimeException("Duplicate ack is not allowed !");
				} else {
					this.ack = true;
				}
				if(success) {
					runningIndex.delete(httpUrl.getId());
				} else {
					PersistObject entity = runningIndex.get(httpUrl.getId());
					runningIndex.delete(entity.getId());
					waitingIndex.putNoOverwrite(entity);
				}
				{
					PersistObject entity = allIndex.get(httpUrl.getId());
					entity.setAttempts(entity.getAttempts()+1);
					allIndex.put(entity);
				}
			} finally {
				lock.unlock();
			}
		}

	}

	@Entity
	public static class PersistObject {

		@PrimaryKey
		private long id;

		private long parentId;

		private String url;

		private long timestamp;

		private int depth;

		private int attempts;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public long getParentId() {
			return parentId;
		}

		public void setParentId(long parentId) {
			this.parentId = parentId;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		public int getDepth() {
			return depth;
		}

		public void setDepth(int depth) {
			this.depth = depth;
		}

		public int getAttempts() {
			return attempts;
		}

		public void setAttempts(int attempts) {
			this.attempts = attempts;
		}

		public HttpUrl toHttpUrl() {
			return new HttpUrl(id, parentId, url, timestamp, depth, attempts);
		}

	}

}
