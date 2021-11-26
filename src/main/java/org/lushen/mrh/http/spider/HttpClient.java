package org.lushen.mrh.http.spider;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * http 客户端
 * 
 * @author hlm
 */
public interface HttpClient extends Closeable {

	/**
	 * 初始化方法
	 * 
	 * @throws Exception
	 */
	default void init() throws IOException {}

	/**
	 * 发起http请求
	 * 
	 * @param httpUrl
	 * @param callback
	 * @return
	 * @throws Exception
	 */
	public Future<HttpClientResponse> doRequest(HttpUrl httpUrl, HttpClientCallback callback);

}
