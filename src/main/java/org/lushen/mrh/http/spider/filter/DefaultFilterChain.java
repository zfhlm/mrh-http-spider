package org.lushen.mrh.http.spider.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlFilter;
import org.lushen.mrh.http.spider.HttpUrlFilterChain;

/**
 * 默认实现
 * 
 * @author hlm
 */
public class DefaultFilterChain implements HttpUrlFilterChain {

	private final AtomicInteger offset = new AtomicInteger(0);

	private final List<HttpUrlFilter> filters = new ArrayList<HttpUrlFilter>();

	public DefaultFilterChain(List<HttpUrlFilter> filters) {
		super();
		this.filters.addAll(filters);
	}

	@Override
	public void invoke(HttpUrl httpUrl) throws Exception {
		if(offset.get() < this.filters.size()) {
			int index = this.offset.getAndIncrement();
			HttpUrlFilter filter = this.filters.get(index);
			try {
				filter.doFilter(httpUrl, this);
			} catch (Exception cause) {
				String message = String.format("Invoke http filter failure at index [%s] named [%s].", index, filter.getName());
				throw new Exception(message, cause);
			}
		}
	}

}
