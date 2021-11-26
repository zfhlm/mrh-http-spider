package org.lushen.mrh.http.spider.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * 单线程 spider
 * 
 * @author hlm
 */
public class SingleThreadSpider implements HttpSpider, Runnable {

	private final Log log = LogFactory.getLog(getClass().getSimpleName());

	private final ExecutorService worker = Executors.newSingleThreadExecutor();

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	private final HttpRepository repository;

	private final HttpClient client;

	private final HttpUrlCollector collector;

	private final List<HttpUrlFilter> filters;

	private final HttpHandler handler;

	public SingleThreadSpider(HttpRepository repository, HttpClient client, HttpUrlCollector collector, List<HttpUrlFilter> filters, HttpHandler handler) {
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
			worker.execute(this);
		}
	}

	@Override
	public void shutdown() {
		if(isRunning.get()) {
			isRunning.set(false);
			worker.shutdownNow();
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
				// 同步阻塞执行网络IO
				this.client.doRequest(acknowledge.get(), new HttpClientCallback() {
					@Override
					public void onSuccess(HttpUrl httpUrl, HttpClientResponse response) {
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
					}
					@Override
					public void onFailure(HttpUrl httpUrl, Throwable cause) {
						log.warn("Failure ::: " + cause.getMessage(), cause);
						acknowledge.ack(false);
					}
					@Override
					public void onCancel(HttpUrl httpUrl) {
						log.info("cancel :: " + acknowledge.get());
						acknowledge.ack(false);
					}
				}).get();
			}
		} catch (Exception cause) {
			if(isRunning.get()) {
				log.warn("Retry scheduling, cause by :: " + cause.getMessage(), cause);
				worker.execute(this);
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
