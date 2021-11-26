package org.lushen.mrh.http.spider.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlFilter;
import org.lushen.mrh.http.spider.HttpUrlFilterChain;

/**
 * http url 最大深度过滤器
 * 
 * @author hlm
 */
public class DepthFilter implements HttpUrlFilter {

	private final Log log = LogFactory.getLog(getClass().getSimpleName());

	private final int maxDepth;

	public DepthFilter(int maxDepth) {
		super();
		this.maxDepth = maxDepth;
	}

	@Override
	public void doFilter(HttpUrl httpUrl, HttpUrlFilterChain chain) throws Exception {
		if(httpUrl.getDepth() <= this.maxDepth) {
			chain.invoke(httpUrl);
		} else {
			if(log.isDebugEnabled()) {
				log.debug("Filter http url : " + httpUrl);
			}
		}
	}

}
