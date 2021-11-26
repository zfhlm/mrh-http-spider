package org.lushen.mrh.http.spider.client;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lushen.mrh.http.spider.HttpClient;
import org.lushen.mrh.http.spider.HttpClientCallback;
import org.lushen.mrh.http.spider.HttpClientResponse;
import org.lushen.mrh.http.spider.HttpUrl;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * okHttp3 客户端
 * 
 * @author hlm
 */
public class HttpOkHttp3Client implements HttpClient {

	private final Log log = LogFactory.getLog(getClass().getSimpleName());

	private ConnectionPool connectionPool;

	private Dispatcher dispatcher;

	private OkHttpClient okHttpClient;

	@Override
	public void init() throws IOException {

		//https证书
		X509TrustManager trustManager = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
			public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
		};
		SSLSocketFactory sslSocketFactory = null;
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, new X509TrustManager[]{trustManager}, new java.security.SecureRandom());
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}

		//http 连接池
		this.connectionPool = new ConnectionPool();

		//http 请求处理线程池
		this.dispatcher = new Dispatcher();

		//创建okHttp客户端
		OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
		clientBuilder.sslSocketFactory(sslSocketFactory, trustManager);
		clientBuilder.dispatcher(this.dispatcher);
		clientBuilder.connectionPool(this.connectionPool);
		this.okHttpClient = clientBuilder.build();

	}

	@Override
	public void close() throws IOException {
		if(this.connectionPool != null) {
			this.connectionPool.evictAll();
		}
		if(this.dispatcher != null) {
			this.dispatcher.cancelAll();
		}
		if(this.dispatcher.executorService() != null) {
			this.dispatcher.executorService().shutdown();
		}
	}

	@Override
	public Future<HttpClientResponse> doRequest(HttpUrl httpUrl, HttpClientCallback callback) {

		try {

			//创建请求对象
			Request.Builder okRequestBuilder = new Request.Builder();
			okRequestBuilder.url(httpUrl.getUrl());
			Request okRequest = okRequestBuilder.build();
			Call okCall = okHttpClient.newCall(okRequest);

			// 响应对象转换，只转换一次
			AtomicReference<HttpClientResponse> reference = new AtomicReference<HttpClientResponse>();
			Function<Response, HttpClientResponse> function = (response -> {
				synchronized (reference) {
					if(reference.get() == null) {
						reference.set(parse(response));
					}
					return reference.get();
				}
			});

			//发起请求
			AtomicBoolean isDone = new AtomicBoolean();
			okCall.enqueue(new Callback() {
				@Override
				public void onResponse(Call call, Response response) throws IOException {
					try {
						callback.onSuccess(httpUrl, function.apply(response));
					} finally {
						isDone.set(true);
					}
				}
				@Override
				public void onFailure(Call call, IOException cause) {
					try {
						if(okCall.isCanceled()) {
							callback.onCancel(httpUrl);
						} else {
							callback.onFailure(httpUrl, cause);
						}
					} finally {
						isDone.set(true);
					}
				}
			});

			return new Future<HttpClientResponse>() {
				@Override
				public boolean isDone() {
					return okCall.isExecuted() && isDone.get();
				}
				@Override
				public boolean isCancelled() {
					return okCall.isCanceled();
				}
				@Override
				public HttpClientResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
					throw new ExecutionException("Not support method !", null);
				}
				@Override
				public HttpClientResponse get() throws InterruptedException, ExecutionException {
					while(true) {
						if(isDone()) {
							if(reference.get() != null) {
								return reference.get();
							} else {
								throw new ExecutionException("Execute error !", null);
							}
						}
						Thread.sleep(50L);
					}
				}
				@Override
				public boolean cancel(boolean mayInterruptIfRunning) {
					try {
						okCall.cancel();
						return true;
					} catch (Exception e) {
						log.warn(e.getMessage(), e);
						return false;
					}
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

	private HttpClientResponse parse(Response response) {

		String protocol = response.protocol().toString();
		int status = response.code();
		String message = response.message();

		Map<String, String[]> headers = new HashMap<String, String[]>();
		response.headers().toMultimap().forEach((name, values) -> {
			headers.put(name, values.stream().toArray(len -> new String[len]));
		});

		ResponseBody responseBody = response.body();
		byte[] body = new byte[0];
		try {
			if(responseBody != null) {
				body = responseBody.bytes();
			}
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		} finally {
			if(responseBody != null) {
				responseBody.close();
			}
		}

		return new DefaultClientResponse(protocol, status, message, headers, body);
	}

}
