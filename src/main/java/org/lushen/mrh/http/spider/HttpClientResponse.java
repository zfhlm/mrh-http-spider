package org.lushen.mrh.http.spider;

import java.util.Map;

/**
 * http 响应对象
 * 
 * @author hlm
 */
public interface HttpClientResponse {

	/**
	 * 获取响应协议版本
	 * 
	 * @return
	 */
	public String getProtocol();

	/**
	 * 获取响应状态码
	 * 
	 * @return
	 */
	public int getStatus();

	/**
	 * 获取响应原因信息
	 * 
	 * @return
	 */
	public String getMessage();

	/**
	 * 获取 Content-Type
	 * 
	 * @return
	 */
	public String getContentType();

	/**
	 * 获取 Content-Length，不存在返回-1
	 * 
	 * @return
	 */
	public long getContentLength();

	/**
	 * 获取请求头对应 value
	 * 
	 * @param name
	 * @return
	 */
	public String getHeaderValue(String name);

	/**
	 * 获取请求头对应 value
	 * 
	 * @param name
	 * @return
	 */
	public String[] getHeaderValues(String name);

	/**
	 * 获取所有请求头
	 * 
	 * @return
	 */
	public Map<String, String[]> getHeaders();

	/**
	 * 获取响应IO流
	 * 
	 * @return
	 */
	public byte[] getBody();

}
