package crawler.servlet.multinodal.producer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import crawler.clients.HttpClient;
import crawler.parsers.Parser;
import crawler.requests.Http10Request;
import crawler.responses.Http10Response;
import crawler.servlet.multinodal.status.WorkerStatus;
import crawler.threadpool.DiskBackedQueue;

public class UrlPoster extends Thread {
	private final Logger logger = Logger.getLogger(getClass());
	private DiskBackedQueue<String> urlQueue;
	private Map<String, WorkerStatus> workerStatusMap;

	public UrlPoster(DiskBackedQueue<String> urlQueue,
			Map<String, WorkerStatus> workerStatusMap) {
		this.urlQueue = urlQueue;
		this.workerStatusMap = workerStatusMap;
	}

	@Override
	public void run() {
		while (true) {
			String url = null;
			synchronized (urlQueue) {
				try {
					logger.debug("UrlPoster waiting for jobQueue");
					if (urlQueue.getSize() <= 0)
						urlQueue.wait();
				} catch (InterruptedException e) {
					logger.error("UrlPoster got interrupt. Shutting down");
					e.printStackTrace();
					break;
				}
				logger.debug("UrlPoster received notify. Waking up!");
				url = urlQueue.dequeue();
			}

			if (workerStatusMap.size() > 0) {
				// we have workers! put them to work
				String ipPort = getNextWorker(workerStatusMap);
				Http10Request request = new Http10Request();
				request.setHeader("Content-Type", Parser.formEncoding);
				request.setBody("url=" + url);
				request.setMethod("POST");
				try {
					logger.debug("Url Poster sending url:" + url + " to worker");
					Http10Response response = HttpClient.post("http://"
							+ ipPort + "/worker", request);
					logger.debug("Url Poster got response "
							+ response.getResponse() + "for url:" + url
							+ " body:" + new String(response.getBody()));
				} catch (IOException | ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				logger.debug("Dequeued url " + url + " and sent to " + ipPort);
			} else {
				// no workers. just put the url back into the queue

				urlQueue.enqueue(url);

			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String getNextWorker(Map<String, WorkerStatus> workerStatusMap) {
		Set<String> set = workerStatusMap.keySet();
		List<String> list = new ArrayList<String>();
		list.addAll(set);
		int size = list.size();
		Random random = new Random();
		int idx = random.nextInt(size);
		return list.get(idx);
	}

}
