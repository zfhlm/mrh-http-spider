package org.lushen.mrh.http.spider.client;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.lushen.mrh.http.spider.HttpClientResponse;

/**
 * http 响应对象默认实现，缓存响应信息，可多次读取
 * 
 * @author hlm
 */
public class DefaultClientResponse implements HttpClientResponse {

	private static final String CONTENT_LENGTH = "Content-Length";

	private static final String CONTENT_TYPE = "Content-Type";

	private String protocol;

	private int status;

	private String message;

	private Map<String, String[]> headers;

	private byte[] body;

	public DefaultClientResponse(String protocol, int status, String message, Map<String, String[]> headers, byte[] body) {
		super();
		this.protocol = protocol;
		this.status = status;
		this.message = message;
		this.headers = headers;
		this.body = body;
	}

	@Override
	public String getProtocol() {
		return this.protocol;
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public String getMessage() {
		return this.message;
	}

	@Override
	public String getContentType() {
		return getHeaderValue(CONTENT_TYPE);
	}

	@Override
	public long getContentLength() {
		return NumberUtils.toLong(getHeaderValue(CONTENT_LENGTH), -1);
	}

	@Override
	public String getHeaderValue(String name) {
		return Arrays.stream(getHeaderValues(name)).findFirst().orElse(null);
	}

	@Override
	public String[] getHeaderValues(String name) {
		String[] values = this.headers.get(name);
		if(values == null) {
			values = this.headers.entrySet().stream().filter(e -> StringUtils.equalsIgnoreCase(name, e.getKey())).map(e -> e.getValue()).findFirst().orElse(null);
		}
		return Optional.ofNullable(values).orElse(new String[0]);
	}

	@Override
	public Map<String, String[]> getHeaders() {
		return this.headers;
	}

	@Override
	public byte[] getBody() {
		return this.body;
	}

}
