package org.lushen.mrh.http.spider.collector;

import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.lushen.mrh.http.spider.HttpClient;
import org.lushen.mrh.http.spider.HttpClientCallback;
import org.lushen.mrh.http.spider.HttpClientResponse;
import org.lushen.mrh.http.spider.HttpUrl;
import org.lushen.mrh.http.spider.HttpUrlCollector;
import org.lushen.mrh.http.spider.client.HttpComponentsClient;

public class TestRegexCollector {

	public static void main(String[] args) throws Exception {

		AtomicLong idGenerator = new AtomicLong(1);

		HttpUrl httpUrl = new HttpUrl();
		httpUrl.setId(1);
		httpUrl.setParentId(-1);
		httpUrl.setUrl("https://www.jianshu.com/");
		httpUrl.setDepth(1);
		httpUrl.setTimestamp(System.currentTimeMillis());

		HttpClient httpClient = new HttpComponentsClient();
		httpClient.init();

		HttpUrlCollector collector = new RegexCollector(() -> idGenerator.incrementAndGet());

		Future<HttpClientResponse> future = httpClient.doRequest(httpUrl, new HttpClientCallback() {
			@Override
			public void onSuccess(HttpUrl httpUrl, HttpClientResponse response) {

				// response
				System.out.println("onSuccess : " + httpUrl);
				System.out.println("line : " + response.getStatus() + " " + response.getProtocol() + " " + response.getMessage());
				System.out.println("ContentType : " + response.getContentType());
				System.out.println("ContentLength : " + response.getContentLength());
				response.getHeaders().forEach((name, values) -> {
					System.out.println("Headers : " + name + " - " + Arrays.toString(values));
				});
				System.out.println(new String(response.getBody()));

				// urls
				try {
					collector.collect(httpUrl, response).forEach(System.err::println);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			@Override
			public void onFailure(HttpUrl httpUrl, Throwable cause) {
				System.out.println("onFailure : " + httpUrl + ", cause = " + cause.getMessage());
			}
			@Override
			public void onCancel(HttpUrl httpUrl) {
				System.out.println("onCancel : " + httpUrl);
			}
		});

		while(true) {
			if(future.isDone()) {
				break;
			}
			Thread.sleep(3000L);
		}

		httpClient.close();

	}

}
