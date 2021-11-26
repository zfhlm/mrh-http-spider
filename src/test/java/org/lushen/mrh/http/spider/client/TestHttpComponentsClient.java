package org.lushen.mrh.http.spider.client;

import java.util.Arrays;
import java.util.concurrent.Future;

import org.lushen.mrh.http.spider.HttpClient;
import org.lushen.mrh.http.spider.HttpClientCallback;
import org.lushen.mrh.http.spider.HttpClientResponse;
import org.lushen.mrh.http.spider.HttpUrl;

public class TestHttpComponentsClient {

	public static void main(String[] args) throws Exception {

		HttpUrl httpUrl = new HttpUrl();
		httpUrl.setId(1);
		httpUrl.setParentId(-1);
		httpUrl.setUrl("https://www.jianshu.com/");
		httpUrl.setDepth(1);
		httpUrl.setTimestamp(System.currentTimeMillis());

		HttpClient httpClient = new HttpComponentsClient();
		httpClient.init();

		Future<HttpClientResponse> future = httpClient.doRequest(httpUrl, new HttpClientCallback() {
			@Override
			public void onSuccess(HttpUrl httpUrl, HttpClientResponse response) {
				System.out.println("onSuccess : " + httpUrl);
				System.out.println("line : " + response.getStatus() + " " + response.getProtocol() + " " + response.getMessage());
				System.out.println("ContentType : " + response.getContentType());
				System.out.println("ContentLength : " + response.getContentLength());
				response.getHeaders().forEach((name, values) -> {
					System.out.println("Headers : " + name + " - " + Arrays.toString(values));
				});
				System.out.println(new String(response.getBody()));
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
			Thread.sleep(2000L);
		}

		httpClient.close();

	}

}
