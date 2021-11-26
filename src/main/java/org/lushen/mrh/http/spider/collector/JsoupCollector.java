package org.lushen.mrh.http.spider.collector;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lushen.mrh.http.spider.HttpClientResponse;
import org.lushen.mrh.http.spider.HttpUrl;
import org.springframework.http.MediaType;

/**
 * jsoup 抽取器
 * 
 * @author hlm
 */
public class JsoupCollector extends LegalUrlCollector {

	private final LongSupplier idGenerator;

	public JsoupCollector(LongSupplier idGenerator) {
		this(Arrays.asList(MediaType.parseMediaType("text/*")), idGenerator);
	}

	public JsoupCollector(List<MediaType> contentTypes, LongSupplier idGenerator) {
		super(contentTypes);
		this.idGenerator = idGenerator;
	}

	@Override
	public List<HttpUrl> doCollect(HttpUrl httpUrl, HttpClientResponse response) throws Exception {

		Document document = Jsoup.parse(new String(response.getBody(), Charset.defaultCharset()), httpUrl.getUrl());
		Elements links = document.select("a[href]");
		Elements medias = document.select("[src]");
		Elements imports = document.select("link[href]");

		List<HttpUrl> list = new ArrayList<HttpUrl>();
		for(Element link : links) {
			HttpUrl element = new HttpUrl();
			element.setId(idGenerator.getAsLong());
			element.setParentId(httpUrl.getId());
			element.setUrl(link.absUrl("href"));
			element.setDepth(httpUrl.getDepth()+1);
			element.setTimestamp(System.currentTimeMillis());
			list.add(element);
		}
		for(Element media : medias) {
			HttpUrl element = new HttpUrl();
			element.setId(idGenerator.getAsLong());
			element.setParentId(httpUrl.getId());
			element.setUrl(media.absUrl("src"));
			element.setDepth(httpUrl.getDepth()+1);
			element.setTimestamp(System.currentTimeMillis());
			list.add(element);
		}
		for(Element im : imports) {
			HttpUrl element = new HttpUrl();
			element.setId(idGenerator.getAsLong());
			element.setParentId(httpUrl.getId());
			element.setUrl(im.absUrl("href"));
			element.setDepth(httpUrl.getDepth()+1);
			element.setTimestamp(System.currentTimeMillis());
			list.add(element);
		}

		return list;
	}

}
