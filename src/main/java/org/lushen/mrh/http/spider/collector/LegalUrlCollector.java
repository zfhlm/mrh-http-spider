package org.lushen.mrh.http.spider.collector;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lushen.mrh.http.spider.HttpClientResponse;
import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlCollector;
import org.springframework.http.MediaType;

public abstract class LegalUrlCollector implements HttpUrlCollector {

	protected final Log log = LogFactory.getLog(getClass().getSimpleName());

	protected static final String HTTP = "http";

	private List<MediaType> contentTypes;

	public LegalUrlCollector(List<MediaType> contentTypes) {
		super();
		this.contentTypes = contentTypes;
	}

	@Override
	public List<HttpUrl> collect(HttpUrl httpUrl, HttpClientResponse response) throws Exception {

		// 在指定 Content-Type 下才进行抽取
		if(response.getContentType() != null) {
			MediaType mediaType = MediaType.parseMediaType(response.getContentType());
			if(this.contentTypes.stream().filter(contentType -> contentType.includes(mediaType)).findFirst().isPresent()) {
				return doCollect(httpUrl, response).stream()
						.filter(element -> {
							// 过滤非 HTTP URL
							if(StringUtils.startsWithIgnoreCase(element.getUrl(), HTTP)) {
								return true;
							} else {
								if(log.isDebugEnabled()) {
									log.debug("Not http link :: " + element.getUrl());
								}
								return false;
							}
						})
						.map(element -> {
							// URL转义
							try {
								URL url = new URL(element.getUrl());
								URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), null);
								element.setUrl(uri.toString());
								return element;
							} catch (Exception e) {
								log.info("Escape failed :: " + e.getMessage() + ", link :: " + element.getUrl());
								return null;
							}
						})
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
			}
		}

		return Collections.emptyList();
	}

	/**
	 * 抽取方法
	 * 
	 * @param httpUrl
	 * @param response
	 * @return
	 * @throws Exception
	 */
	protected abstract List<HttpUrl> doCollect(HttpUrl httpUrl, HttpClientResponse response) throws Exception;

}
