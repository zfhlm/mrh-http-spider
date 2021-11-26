package org.lushen.mrh.http.spider.filter;

import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlFilter;
import org.lushen.mrh.http.spider.HttpUrlFilterChain;

/**
 * 权重适配 filter
 * 
 * @author hlm
 */
public class OrderedFilter implements HttpUrlFilter {

	private HttpUrlFilter filter;

	private int order;

	public OrderedFilter(HttpUrlFilter filter, int order) {
		super();
		this.filter = filter;
		this.order = order;
	}

	@Override
	public void doFilter(HttpUrl httpUrl, HttpUrlFilterChain chain) throws Exception {
		this.filter.doFilter(httpUrl, chain);
	}

	@Override
	public String getName() {
		return this.filter.getName();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
