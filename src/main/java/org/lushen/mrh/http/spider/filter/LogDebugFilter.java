package org.lushen.mrh.http.spider.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlFilter;
import org.lushen.mrh.http.spider.HttpUrlFilterChain;

/**
 * http url debug级别日志过滤器
 * 
 * @author hlm
 */
public class LogDebugFilter implements HttpUrlFilter {

	private final Log log = LogFactory.getLog(getClass().getSimpleName());

	@Override
	public void doFilter(HttpUrl httpUrl, HttpUrlFilterChain chain) throws Exception {
		if(log.isDebugEnabled()) {
			log.debug("Begin do filter with http url : " + httpUrl);
		}
		chain.invoke(httpUrl);
	}

}
