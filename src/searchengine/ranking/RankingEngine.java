package searchengine.ranking;

import indexer.DocumentScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import searchengine.query.QueryWord;
import searchengine.ranking.RankerInfo.RankerType;

public final class RankingEngine {
	
	private final List<DocumentScore> documentList;
	private final List<QueryWord> query;
	private final int corpusSize;
	private final Map<QueryWord, Integer> wordDfs;
	
	private Map<RankerType, Double> rankerSet;

	public RankingEngine(List<DocumentScore> docs, List<QueryWord> query,
		int corpusSize, Map<QueryWord, Integer> wordDfs) {
		
		this.documentList = docs;
		this.query = query;
		this.corpusSize = corpusSize;
		this.wordDfs = wordDfs;
		
		this.rankerSet = new HashMap<>();
	}
	
	public void addRanker(RankerType ranker, double weight) {
		rankerSet.put(ranker, weight);
	}

	public void setRankers(Map<RankerType, Double> rs) {
		rankerSet.putAll(rs);
	}

	/**
	 * Apply the set rankers to the documents
	 * @throws Exception
	 */
	public List<Ranker> applyRankers() throws Exception {

		List<Ranker> rankers = new ArrayList<>(rankerSet.size());

		if (rankerSet.containsKey(RankerType.RANKER_TFIDF)) {
			Ranker rankerTfIdf = new RankerTfIdf(documentList, query,
					corpusSize, wordDfs);
			rankerTfIdf.setWeight(rankerSet.get(RankerType.RANKER_TFIDF));
			rankers.add(rankerTfIdf);
		}

		if (rankerSet.containsKey(RankerType.RANKER_HEADER)) {
			Ranker rankerHeader = new RankerHeader(documentList);
			rankerHeader.setWeight(rankerSet.get(RankerType.RANKER_HEADER));
			rankers.add(rankerHeader);
		}

		if (rankerSet.containsKey(RankerType.RANKER_LINKS)) {
			Ranker rankerLinks = new RankerLinks(documentList);
			rankerLinks.setWeight(rankerSet.get(RankerType.RANKER_LINKS));
			rankers.add(rankerLinks);
		}

		if (rankerSet.containsKey(RankerType.RANKER_META)) {
			Ranker rankerMeta = new RankerMeta(documentList);
			rankerMeta.setWeight(rankerSet.get(RankerType.RANKER_META));
			rankers.add(rankerMeta);
		}

		if (rankerSet.containsKey(RankerType.RANKER_TOTALCOUNT)) {
			Ranker rankerTotalCnt = new RankerTotalCount(documentList);
			rankerTotalCnt.setWeight(rankerSet
					.get(RankerType.RANKER_TOTALCOUNT));
			rankers.add(rankerTotalCnt);
		}

		if (rankerSet.containsKey(RankerType.RANKER_QUERYMATCH)) {
			Ranker rankerQueryMatch = new RankerQueryMatch(documentList);
			rankerQueryMatch.setWeight(rankerSet
					.get(RankerType.RANKER_QUERYMATCH));
			rankers.add(rankerQueryMatch);
		}

		if (rankerSet.containsKey(RankerType.RANKER_POSITION)) {
			Ranker rankerPosition = new RankerPosition(documentList, query);
			rankerPosition.setWeight(rankerSet.get(RankerType.RANKER_POSITION));
			rankers.add(rankerPosition);
		}
		
		if (rankerSet.containsKey(RankerType.RANKER_URLCOUNT)) {
			Ranker rankerUrlCount = new RankerUrlCount(documentList, query);
			rankerUrlCount.setWeight(rankerSet.get(RankerType.RANKER_URLCOUNT));
			rankers.add(rankerUrlCount);
		}

		// Mulithreaded ranker execution
		ExecutorService es = Executors.newFixedThreadPool(rankers.size());
		for (Ranker ranker : rankers) {
			es.execute(ranker);
		}

		es.shutdown();
		try {
			es.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			throw new Exception(e);
		}

		for (Ranker ranker : rankers) {
			if (ranker.getRanks().size() > 0) {
				// logger.info("Name - " + ranker.getRankingName() +" -- "
				// +ranker.getRanks().get(0));
			}
		}

		return rankers;
	}
}
