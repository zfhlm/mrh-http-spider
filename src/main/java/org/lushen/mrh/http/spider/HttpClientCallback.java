package org.lushen.mrh.http.spider;

/**
 * http 客户端响应回调
 * 
 * @author hlm
 */
public interface HttpClientCallback {

	/**
	 * 请求成功回调
	 * 
	 * @param httpUrl
	 * @param response
	 */
	public void onSuccess(HttpUrl httpUrl, HttpClientResponse response);

	/**
	 * 请求失败回调
	 * 
	 * @param httpUrl
	 * @param cause
	 */
	public void onFailure(HttpUrl httpUrl, Throwable cause);

	/**
	 * 请求取消回调
	 * 
	 * @param httpUrl
	 */
	public void onCancel(HttpUrl httpUrl);

}
