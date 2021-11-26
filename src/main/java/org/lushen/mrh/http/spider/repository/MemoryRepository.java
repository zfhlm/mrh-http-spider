package org.lushen.mrh.http.spider.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.map.LinkedMap;
import org.lushen.mrh.http.spider.HttpRepository;
import org.lushen.mrh.http.spider.HttpUrl;

/**
 * 内存存储实现
 * 
 * @author hlm
 */
public class MemoryRepository implements HttpRepository {

	private final Map<String, HttpUrl> repository = new HashMap<String, HttpUrl>();

	private final LinkedMap<String, HttpUrl> waiting = new LinkedMap<String, HttpUrl>();

	private final LinkedMap<String, HttpUrl> running = new LinkedMap<String, HttpUrl>();

	@Override
	public boolean hasMore() {
		synchronized (this) {
			return ! this.waiting.isEmpty() || ! this.running.isEmpty();
		}
	}

	@Override
	public HttpAcknowledge pull() {
		synchronized (this) {
			if(this.waiting.isEmpty()) {
				return null;
			} else {
				HttpUrl httpUrl = this.waiting.remove(0);
				this.running.put(httpUrl.getUrl(), httpUrl);
				return new MemoryAcknowledge(httpUrl);
			}
		}
	}

	@Override
	public List<HttpAcknowledge> pull(int size) {
		synchronized (this) {
			if(this.waiting.isEmpty()) {
				return Collections.emptyList();
			} else {
				List<HttpAcknowledge> acknowledges = new ArrayList<HttpAcknowledge>(size);
				for(int i=0; i<size; i++) {
					HttpUrl httpUrl = this.waiting.remove(0);
					if(httpUrl != null) {
						this.running.put(httpUrl.getUrl(), httpUrl);
						acknowledges.add(new MemoryAcknowledge(httpUrl));
					} else {
						break;
					}
				}
				return acknowledges;
			}
		}
	}

	@Override
	public void save(HttpUrl httpUrl) {
		synchronized (this) {
			if( ! this.repository.containsKey(httpUrl.getUrl()) ) {
				this.repository.put(httpUrl.getUrl(), httpUrl);
				this.waiting.put(httpUrl.getUrl(), httpUrl);
			}
		}
	}

	@Override
	public void saveBatch(List<HttpUrl> httpUrls) {
		synchronized (this) {
			for(HttpUrl httpUrl : httpUrls) {
				if( ! this.repository.containsKey(httpUrl.getUrl()) ) {
					this.repository.put(httpUrl.getUrl(), httpUrl);
					this.waiting.put(httpUrl.getUrl(), httpUrl);
				}
			}
		}
	}

	@Override
	public void trancate() {
		synchronized (this) {
			this.running.clear();
			this.waiting.clear();
			this.repository.clear();
		}
	}

	@Override
	public void close() {
		trancate();
	}

	private class MemoryAcknowledge implements HttpAcknowledge {

		private HttpUrl httpUrl;

		private boolean ack;

		public MemoryAcknowledge(HttpUrl httpUrl) {
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
			synchronized (MemoryRepository.this) {
				if(this.ack) {
					throw new RuntimeException("Duplicate ack is not allowed !");
				} else {
					this.ack = true;
				}
				if(success) {
					MemoryRepository.this.running.remove(this.httpUrl.getUrl());
				} else {
					MemoryRepository.this.running.remove(this.httpUrl.getUrl());
					MemoryRepository.this.waiting.put(this.httpUrl.getUrl(), this.httpUrl);
				}
				Optional.ofNullable(MemoryRepository.this.repository.get(this.httpUrl.getUrl())).ifPresent(e -> {
					e.setAttempts(e.getAttempts()+1);
				});
			}
		}

	}

}
