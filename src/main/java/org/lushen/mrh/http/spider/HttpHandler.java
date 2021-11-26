package org.lushen.mrh.http.spider;

/**
 * 爬取内容处理器
 * 
 * @author hlm
 */
public interface HttpHandler {

	/**
	 * 处理当前 URL 爬取内容
	 * 
	 * @param httpUrl
	 * @param response
	 * @throws Exception
	 */
	public void handle(HttpUrl httpUrl, HttpClientResponse response) throws Exception;

}
