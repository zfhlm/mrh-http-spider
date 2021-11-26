package org.lushen.mrh.http.spider.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.ssl.SSLContexts;
import org.lushen.mrh.http.spider.HttpClient;
import org.lushen.mrh.http.spider.HttpClientCallback;
import org.lushen.mrh.http.spider.HttpClientResponse;
import org.lushen.mrh.http.spider.HttpUrl;

/**
 * apache http components 客户端
 * 
 * @author hlm
 */
public class HttpComponentsClient implements HttpClient {

	private final Log log = LogFactory.getLog(getClass().getSimpleName());

	private ConnectingIOReactor ioReactor;

	private PoolingNHttpClientConnectionManager connectionManager;

	private CloseableHttpAsyncClient httpAsyncClient;

	@Override
	public void init() throws IOException {

		//https证书
		RegistryBuilder<SchemeIOSessionStrategy> registryBuilder = RegistryBuilder.<SchemeIOSessionStrategy>create();
		registryBuilder.register("http", NoopIOSessionStrategy.INSTANCE);
		registryBuilder.register("https", new SSLIOSessionStrategy(SSLContexts.createDefault()));
		Registry<SchemeIOSessionStrategy> registry = registryBuilder.build();

		//io处理器
		IOReactorConfig.Builder iorConfigBuilder = IOReactorConfig.custom();
		this.ioReactor = new DefaultConnectingIOReactor(iorConfigBuilder.build());

		//http连接池
		this.connectionManager = new PoolingNHttpClientConnectionManager(this.ioReactor, registry);

		//创建并启动http客户端
		HttpAsyncClientBuilder asyncClientBuilder = HttpAsyncClientBuilder.create();
		asyncClientBuilder.setConnectionManager(this.connectionManager);
		asyncClientBuilder.disableCookieManagement();
		this.httpAsyncClient = asyncClientBuilder.build();
		this.httpAsyncClient.start();

	}

	@Override
	public void close() throws IOException {
		if(this.httpAsyncClient != null) {
			this.httpAsyncClient.close();
		}
		if(this.connectionManager != null) {
			this.connectionManager.shutdown();
		}
		if(this.ioReactor != null) {
			ioReactor.shutdown();
		}
	}

	@Override
	public Future<HttpClientResponse> doRequest(HttpUrl httpUrl, HttpClientCallback callback) {

		try {

			//创建请求对象
			HttpGet httpGet = new HttpGet(httpUrl.getUrl());
			httpGet.setConfig(RequestConfig.custom().build());

			// 响应对象转换，只转换一次
			AtomicReference<HttpClientResponse> reference = new AtomicReference<HttpClientResponse>();
			Function<HttpResponse, HttpClientResponse> function = (response -> {
				synchronized (reference) {
					if(reference.get() == null) {
						reference.set(parse(response));
					}
					return reference.get();
				}
			});

			//发起请求
			Future<HttpResponse> future = httpAsyncClient.execute(httpGet, new FutureCallback<HttpResponse>() {
				@Override
				public void completed(org.apache.http.HttpResponse response) {
					callback.onSuccess(httpUrl, function.apply(response));
				}
				@Override
				public void failed(Exception cause) {
					callback.onFailure(httpUrl, cause);
				}
				@Override
				public void cancelled() {
					callback.onCancel(httpUrl);
				}
			});

			// 返回future
			return new Future<HttpClientResponse>() {
				@Override
				public boolean isDone() {
					return future.isDone();
				}
				@Override
				public boolean isCancelled() {
					return future.isCancelled();
				}
				@Override
				public HttpClientResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
					return function.apply(future.get(timeout, unit));
				}
				@Override
				public HttpClientResponse get() throws InterruptedException, ExecutionException {
					return function.apply(future.get());
				}
				@Override
				public boolean cancel(boolean mayInterruptIfRunning) {
					return future.cancel(mayInterruptIfRunning);
				}
			};

		} catch (Exception cause) {

			callback.onFailure(httpUrl, cause);
			
			return new Future<HttpClientResponse>() {
				@Override
				public boolean isDone() {
					return true;
				}
				@Override
				public boolean isCancelled() {
					return true;
				}
				@Override
				public HttpClientResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
					return null;
				}
				@Override
				public HttpClientResponse get() throws InterruptedException, ExecutionException {
					return null;
				}
				@Override
				public boolean cancel(boolean mayInterruptIfRunning) {
					return true;
				}
			};

		}

	}

	private HttpClientResponse parse(HttpResponse response) {

		int status = response.getStatusLine().getStatusCode();
		String protocol = String.valueOf(response.getStatusLine().getProtocolVersion());
		String message = response.getStatusLine().getReasonPhrase();

		Map<String, String[]> headers = new HashMap<String, String[]>();
		Arrays.stream(response.getAllHeaders()).collect(Collectors.groupingBy(Header::getName)).forEach((name, values) -> {
			headers.put(name, values.stream().map(e -> e.getValue()).toArray(len -> new String[len]));
		});

		byte[] body = new byte[0];
		try {
			HttpEntity httpEntity = response.getEntity();
			if(httpEntity != null) {
				body = IOUtils.toByteArray(httpEntity.getContent());
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		return new DefaultClientResponse(protocol, status, message, headers, body);
	}

}
