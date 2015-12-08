package indexer;

import indexer.dao.DocumentFeatures;
import indexer.dao.InvertedIndexRow;

import java.util.HashMap;
import java.util.Map;

public class DocumentScore implements Comparable<DocumentScore> {
	private String url;
	private double rank;

	/*
	 * Feature counts of words occurring in this document.
	 */
	private Map<String, DocumentFeatures> wordFeatures;

	public DocumentScore(String word, DocumentFeatures features) {
		this.url = new String(features.getUrl());
		this.wordFeatures = new HashMap<String, DocumentFeatures>();
		addFeatures(word, features);
	}

	public void addFeatures(String word, DocumentFeatures features) {
		wordFeatures.put(word, features);
	}

	public String getUrl() {
		return url;
	}

	public void setRank(double r) {
		this.rank = r;
	}

	public double getRank() {
		return rank;
	}

	@Override
	public int compareTo(DocumentScore other) {
		return (-1) * Double.compare(this.rank, other.rank);
	}

	@Override
	public String toString() {
		StringBuffer fmt = new StringBuffer();
		fmt.append(String.format("URL %s ; rank=%f\n", this.url, this.rank));
		for(String word: wordFeatures.keySet())
			fmt.append(String.format("%s features=%s\n", word, wordFeatures.get(word)));
		
		return fmt.toString();
	}

}
