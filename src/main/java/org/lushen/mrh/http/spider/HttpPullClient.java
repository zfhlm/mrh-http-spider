package org.lushen.mrh.http.spider;

import java.io.Closeable;
import java.util.List;

/**
 * 拉取未处理完成 URL 接口
 * 
 * @author hlm
 */
public interface HttpPullClient extends Closeable {

	/**
	 * 是否有更多未处理完成 URL
	 * 
	 * @return
	 */
	public boolean hasMore();
	
	/**
	 * 拉取未处理完成 URL
	 * 
	 * @return
	 */
	public HttpAcknowledge pull();

	/**
	 * 拉取指定数量未处理完成 URL
	 * 
	 * @return
	 */
	public List<HttpAcknowledge> pull(int size);

	/**
	 * ack 确认接口
	 * 
	 * @author hlm
	 */
	public static interface HttpAcknowledge {

		/**
		 * 获取当前 URL
		 * 
		 * @return
		 */
		public HttpUrl get();

		/**
		 * 确认当前 URL
		 * 
		 * @param success 是否成功
		 */
		public void ack(boolean success);

	}

}
