package org.lushen.mrh.http.spider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.lushen.mrh.http.spider.client.HttpComponentsClient;
import org.lushen.mrh.http.spider.collector.RegexCollector;
import org.lushen.mrh.http.spider.impl.ConcurrentSpider;
import org.lushen.mrh.http.spider.repository.MemoryRepository;

public class TestConcurrentSpider {

	public static void main(String[] args) throws Exception {

		// http 客户端
		HttpClient httpClient = new HttpComponentsClient();
		httpClient.init();

		// url 抽取器
		AtomicLong idGenerator = new AtomicLong(1);
		HttpUrlCollector collector = new RegexCollector(() -> idGenerator.incrementAndGet());

		// url 存储仓库
		HttpRepository repository = new MemoryRepository();

		// url 过滤器
		List<HttpUrlFilter> filters = Arrays.asList(new HttpUrlFilter() {
			@Override
			public void doFilter(HttpUrl httpUrl, HttpUrlFilterChain chain) throws Exception {
				// 域名过滤
				if(httpUrl.getUrl().contains("www.jianshu.com")) {
					chain.invoke(httpUrl);
				}
			}
		}, new HttpUrlFilter() {
			@Override
			public void doFilter(HttpUrl httpUrl, HttpUrlFilterChain chain) throws Exception {
				// 层数过滤
				if(httpUrl.getDepth() <= 3) {
					chain.invoke(httpUrl);
				}
			}
		});

		// 爬取内容处理器
		HttpHandler handler = new HttpHandler() {
			@Override
			public void handle(HttpUrl httpUrl, HttpClientResponse response) throws Exception {
				System.err.println("success : " + httpUrl);
				/*if(response.getContentType() != null && MediaType.TEXT_HTML.includes(MediaType.parseMediaType(response.getContentType()))) {
					System.err.println(new String(response.getBody()));
				}*/
			}
		};

		// 存储 root url 到仓库
		HttpUrl root = new HttpUrl();
		root.setId(1);
		root.setParentId(-1);
		root.setUrl("https://www.jianshu.com/");
		root.setDepth(1);
		root.setTimestamp(System.currentTimeMillis());
		repository.save(root);

		// 开始爬取
		HttpSpider spider = new ConcurrentSpider(repository, httpClient, collector, filters, handler);
		spider.startup();

		// 阻塞等待完成
		spider.await();

		// 关闭释放资源
		repository.close();
		httpClient.close();

	}

}
