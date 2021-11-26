package org.lushen.mrh.http.spider.collector;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.lushen.mrh.http.spider.HttpClientResponse;
import org.lushen.mrh.http.spider.HttpUrl;
import org.springframework.http.MediaType;

/**
 * http url 正则提取
 * 
 * @author hlm
 */
public class RegexCollector extends LegalUrlCollector {

	private static final String JAVASCRIPT = "javascript";

	private static final Pattern HREF_PATTERN = Pattern.compile("(?i)href\\s*=\\s*[\"']{1}([^\"']*)[\"']{1}");

	private static final Pattern SRC_PATTERN = Pattern.compile("(?i)src\\s*=\\s*[\"']{1}([^\"']*)[\"']{1}");

	private final LongSupplier idGenerator;

	public RegexCollector(LongSupplier idGenerator) {
		this(Arrays.asList(MediaType.parseMediaType("text/*")), idGenerator);
	}

	public RegexCollector(List<MediaType> contentTypes, LongSupplier idGenerator) {
		super(contentTypes);
		this.idGenerator = idGenerator;
	}

	@Override
	public List<HttpUrl> doCollect(HttpUrl httpUrl, HttpClientResponse response) throws Exception {

		// 正则提取
		String text = new String(response.getBody(), Charset.defaultCharset());
		Set<String> relatives = new HashSet<String>();
		for(Pattern pattern : Arrays.asList(HREF_PATTERN, SRC_PATTERN)) {
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				relatives.add(matcher.group(1).trim());
			}
		}

		if(relatives.isEmpty()) {
			return Collections.emptyList();
		}

		// 路径转换
		URI parent = URI.create(httpUrl.getUrl());
		Set<String> absolutes = new HashSet<String>();
		for(String relative : relatives) {
			try {
				if(StringUtils.startsWithIgnoreCase(relative, JAVASCRIPT)) {
					continue;
				}
				else if(StringUtils.startsWithIgnoreCase(relative, HTTP)) {
					absolutes.add(relative);
				}
				else if(StringUtils.startsWith(relative, "//")) {
					absolutes.add(StringUtils.join(parent.getScheme(), ":", relative));
				}
				else {
					absolutes.add(parent.resolve(relative).toString());
				}
			} catch (Exception cause) {
				log.warn(String.format("Illegal relative uri : [%s]", relative));
			}
		}

		// 对象转换
		List<HttpUrl> list = new ArrayList<HttpUrl>();
		for(String absolute : absolutes) {
			HttpUrl element = new HttpUrl();
			element.setId(idGenerator.getAsLong());
			element.setParentId(httpUrl.getId());
			element.setUrl(absolute);
			element.setDepth(httpUrl.getDepth()+1);
			element.setTimestamp(System.currentTimeMillis());
			list.add(element);
		}

		return list;
	}

}
