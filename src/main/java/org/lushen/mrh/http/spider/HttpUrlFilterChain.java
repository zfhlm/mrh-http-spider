package org.lushen.mrh.http.spider;

/**
 * http url 过滤链
 * 
 * @author hlm
 */
public interface HttpUrlFilterChain {

	/**
	 * 执行下一个过滤逻辑
	 * 
	 * @param httpUrl
	 * @throws Exception
	 */
	public void invoke(HttpUrl httpUrl) throws Exception;

}
