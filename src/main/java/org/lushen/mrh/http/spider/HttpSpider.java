package org.lushen.mrh.http.spider;

/**
 * spider 接口
 * 
 * @author hlm
 */
public interface HttpSpider {

	/**
	 * 启动调度
	 */
	public void startup();

	/**
	 * 停止调度
	 */
	public void shutdown();

	/**
	 * 阻塞等待完成
	 */
	public void await();

}
