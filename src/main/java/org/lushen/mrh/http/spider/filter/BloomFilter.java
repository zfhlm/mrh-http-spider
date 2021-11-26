package org.lushen.mrh.http.spider.filter;

import java.util.BitSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlFilter;
import org.lushen.mrh.http.spider.HttpUrlFilterChain;

/**
 * http url 布隆过滤器
 * 
 * @author hlm
 */
public final class BloomFilter implements HttpUrlFilter {

	private final Log log = LogFactory.getLog(getClass().getSimpleName());

	private BitSet bitSet = new BitSet((int) Math.pow(2, 16));

	@Override
	public void doFilter(HttpUrl httpUrl, HttpUrlFilterChain chain) throws Exception {
		if(setIfNoExist(httpUrl.getUrl().hashCode() & 0x7FFFFFFF)) {
			chain.invoke(httpUrl);
		} else {
			if(log.isDebugEnabled()) {
				log.debug("Filter http url : " + httpUrl);
			}
		}
	}

	private boolean setIfNoExist(int hash) {
		synchronized (this) {
			if( ! bitSet.get(hash)) {
				bitSet.set(hash);
				return true;
			}
			return false;
		}
	}

}
