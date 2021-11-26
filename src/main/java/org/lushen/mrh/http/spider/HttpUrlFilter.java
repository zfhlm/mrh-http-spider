package org.lushen.mrh.http.spider;

/**
 * http url 过滤器
 * 
 * @author hlm
 */
public interface HttpUrlFilter {

	public static final int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

	public static final int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

	/**
	 * 过滤器名称
	 * 
	 * @return
	 */
	default String getName() {
		return getClass().getName();
	}

	/**
	 * 过滤器排序权重，数值越小排序越前
	 * 
	 * @return
	 */
	default int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	/**
	 * 执行过滤逻辑
	 * 
	 * @param httpUrl
	 * @param chain
	 * @throws Exception
	 */
	public void doFilter(HttpUrl httpUrl, HttpUrlFilterChain chain) throws Exception;

}
