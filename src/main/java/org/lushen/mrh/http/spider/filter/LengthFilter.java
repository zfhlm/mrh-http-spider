package org.lushen.mrh.http.spider.filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlFilter;
import org.lushen.mrh.http.spider.HttpUrlFilterChain;

/**
 * http url 最大长度过滤器
 * 
 * @author hlm
 */
public class LengthFilter implements HttpUrlFilter {

	private final Log log = LogFactory.getLog(getClass().getSimpleName());

	private final int maxLength;

	public LengthFilter(int maxLength) {
		super();
		this.maxLength = maxLength;
	}

	@Override
	public void doFilter(HttpUrl httpUrl, HttpUrlFilterChain chain) throws Exception {
		if(StringUtils.length(httpUrl.getUrl()) <= this.maxLength) {
			chain.invoke(httpUrl);
		} else {
			if(log.isDebugEnabled()) {
				log.debug("Filter http url : " + httpUrl);
			}
		}
	}

}
