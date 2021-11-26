package org.lushen.mrh.http.spider;

import java.io.Closeable;
import java.util.List;

/**
 * 存储接口
 * 
 * @author hlm
 */
public interface HttpRepository extends HttpPullClient, Closeable {

	/**
	 * 保存 URL
	 * 
	 * @param httpUrl
	 */
	public void save(HttpUrl httpUrl);

	/**
	 * 保存批量 URL
	 * 
	 * @param httpUrls
	 */
	public void saveBatch(List<HttpUrl> httpUrls);

	/**
	 * 清除所有 URL
	 */
	public void trancate();

}
