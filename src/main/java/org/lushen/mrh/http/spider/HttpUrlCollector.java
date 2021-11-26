package org.lushen.mrh.http.spider;

import java.util.List;

/**
 * http URL 提取接口
 * 
 * @author hlm
 */
public interface HttpUrlCollector {

	/**
	 * 提取 http url
	 * 
	 * @param httpUrl
	 * @param response
	 * @return
	 * @throws Exception
	 */
	public List<HttpUrl> collect(HttpUrl httpUrl, HttpClientResponse response) throws Exception;

}
