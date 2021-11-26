package org.lushen.mrh.http.spider;

/**
 * http url 定义
 * 
 * @author hlm
 */
public class HttpUrl {

	private long id;			// 唯一ID

	private long parentId;		// 上级ID

	private String url;			// 链接字符串

	private long timestamp;		// 抓取时间

	private int depth;			// 抓取深度

	private int attempts;		// 尝试次数

	public HttpUrl() {
		super();
	}

	public HttpUrl(long id, long parentId, String url, long timestamp, int depth) {
		this(id, parentId, url, timestamp, depth, 0);
	}

	public HttpUrl(long id, long parentId, String url, long timestamp, int depth, int attempts) {
		super();
		this.id = id;
		this.parentId = parentId;
		this.url = url;
		this.timestamp = timestamp;
		this.depth = depth;
		this.attempts = attempts;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getParentId() {
		return parentId;
	}

	public void setParentId(long parentId) {
		this.parentId = parentId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[id=");
		builder.append(id);
		builder.append(", parentId=");
		builder.append(parentId);
		builder.append(", url=");
		builder.append(url);
		builder.append(", timestamp=");
		builder.append(timestamp);
		builder.append(", depth=");
		builder.append(depth);
		builder.append(", attempts=");
		builder.append(attempts);
		builder.append("]");
		return builder.toString();
	}

}
