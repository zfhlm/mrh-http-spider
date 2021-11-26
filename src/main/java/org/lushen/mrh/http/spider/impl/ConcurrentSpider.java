package org.lushen.mrh.http.spider.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lushen.mrh.http.spider.HttpClient;
import org.lushen.mrh.http.spider.HttpClientCallback;
import org.lushen.mrh.http.spider.HttpClientResponse;
import org.lushen.mrh.http.spider.HttpHandler;
import org.lushen.mrh.http.spider.HttpPullClient.HttpAcknowledge;
import org.lushen.mrh.http.spider.HttpRepository;
import org.lushen.mrh.http.spider.HttpSpider;
import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlCollector;
import org.lushen.mrh.http.spider.HttpUrlFilter;
import org.lushen.mrh.http.spider.HttpUrlFilterChain;
import org.lushen.mrh.http.spider.filter.DefaultFilterChain;

/**
 * 多线程并发 spider
 * 
 * @author hlm
 */
public class ConcurrentSpider implements HttpSpider, Runnable {

	private final Log log = LogFactory.getLog(getClass().getSimpleName());

	private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

	private final ExecutorService workers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);

	private final LinkedHashSet<Future<HttpClientResponse>> futures = new LinkedHashSet<Future<HttpClientResponse>>();

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	private final HttpRepository repository;

	private final HttpClient client;

	private final HttpUrlCollector collector;

	private final List<HttpUrlFilter> filters;

	private final HttpHandler handler;

	public ConcurrentSpider(HttpRepository repository, HttpClient client, HttpUrlCollector collector,
			List<HttpUrlFilter> filters, HttpHandler handler) {
		super();
		this.repository = repository;
		this.client = client;
		this.collector = collector;
		this.filters = filters;
		this.handler = handler;
	}

	@Override
	public void startup() {
		if( ! isRunning.get() ) {
			isRunning.set(true);
			scheduler.execute(this);
		}
	}

	@Override
	public void shutdown() {
		if(isRunning.get()) {
			isRunning.set(false);
			futures.forEach(future -> {
				if( ! future.isDone() && ! future.isCancelled() ) {
					future.cancel(true);
				}
			});
			scheduler.shutdownNow();
			workers.shutdownNow();
		}
	}

	@Override
	public void run() {
		try {
			while(true) {
				if( ! isRunning.get()) {
					break;
				}
				if( ! repository.hasMore() ) {
					shutdown();
					break;
				}
				// 获取任务
				HttpAcknowledge acknowledge = repository.pull();
				if(acknowledge == null) {
					Thread.sleep(100L);
					continue;
				}
				// 执行异步网络IO，网络IO回调异步处理
				Future<HttpClientResponse> future = this.client.doRequest(acknowledge.get(), new HttpClientCallback() {
					@Override
					public void onSuccess(HttpUrl httpUrl, HttpClientResponse response) {
						workers.execute(() -> {
							try {
								List<HttpUrl> children = new ArrayList<HttpUrl>();
								List<HttpUrlFilter> allFilters = new ArrayList<HttpUrlFilter>(filters.size() + 1);
								allFilters.addAll(filters);
								allFilters.add(new HttpUrlFilter() {
									@Override
									public void doFilter(HttpUrl child, HttpUrlFilterChain chain) throws Exception {
										children.add(child);
									}
								});
								for(HttpUrl child : collector.collect(acknowledge.get(), response)) {
									new DefaultFilterChain(allFilters).invoke(child);
								}
								repository.saveBatch(children);
								handler.handle(acknowledge.get(), response);
								acknowledge.ack(true);
							} catch (Exception cause) {
								log.warn(cause.getMessage(), cause);
								acknowledge.ack(false);
							}
						});
					}
					@Override
					public void onFailure(HttpUrl httpUrl, Throwable cause) {
						workers.execute(() -> {
							log.warn("Failure ::: " + cause.getMessage(), cause);
							acknowledge.ack(false);
						});
					}
					@Override
					public void onCancel(HttpUrl httpUrl) {
						workers.execute(() -> {
							log.info("cancel :: " + acknowledge.get());
							acknowledge.ack(false);
						});
					}
				});
				futures.add(future);
				// 移除已完成任务
				Iterator<Future<HttpClientResponse>> iterator = futures.iterator();
				while(iterator.hasNext()) {
					Future<HttpClientResponse> it = iterator.next();
					if(it.isDone() || it.isCancelled()) {
						iterator.remove();
					}
				}
			}
		} catch (Exception cause) {
			if(isRunning.get()) {
				log.warn("Retry scheduling, cause by :: " + cause.getMessage(), cause);
				scheduler.execute(this);
			} else {
				log.warn("Stop scheduling and do not retry.");
			}
		}
	}

	@Override
	public void await() {
		while (true) {
			if( ! isRunning.get() ) {
				break;
			} else {
				try {
					Thread.sleep(500L);
				} catch (InterruptedException e) {
					log.info(e.getMessage(), e);
				}
			}
		}
	}

}
