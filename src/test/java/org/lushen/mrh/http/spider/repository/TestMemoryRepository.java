package org.lushen.mrh.http.spider.repository;

import java.io.IOException;

import org.lushen.mrh.http.spider.HttpPullClient.HttpAcknowledge;
import org.lushen.mrh.http.spider.HttpRepository;
import org.lushen.mrh.http.spider.HttpUrl;

public class TestMemoryRepository {

	public static void main(String[] args) throws IOException {

		HttpRepository repository = new MemoryRepository();

		repository.save(new HttpUrl(1, 0, "https://www.jianshu.com/", System.currentTimeMillis(), 1));

		System.out.println("----------------------------------");

		HttpAcknowledge acknowledge = repository.pull();
		System.out.println(acknowledge.get());
		System.out.println(repository.hasMore());
		acknowledge.ack(false);
		System.out.println(repository.hasMore());

		System.out.println("----------------------------------");

		acknowledge = repository.pull();
		System.out.println(acknowledge.get());
		System.out.println(repository.hasMore());
		acknowledge.ack(true);
		System.out.println(repository.hasMore());

		System.out.println("----------------------------------");

		acknowledge = repository.pull();
		System.out.println(acknowledge);
		System.out.println(repository.hasMore());

		repository.close();

	}

}
