package indexer.offline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Tokenizer {
	private final static Logger logger = Logger.getLogger(Tokenizer.class);
	private static final int MAX_TOKEN_SIZE = 20;
	private static StanfordCoreNLP instance = null;
	
	private Annotation atext;	
	private Set<String>wordsToRemove;
	
	public static enum TokenType {
		UNIGRAM, BIGRAM, TRIGRAM
	}

	public Tokenizer(String text, TokenType type) {
		atext = new Annotation(text);
		getPipeline().annotate(atext);
		
		wordsToRemove = new HashSet<>();
		if (!TokenType.UNIGRAM.equals(type)) {
			wordsToRemove.addAll(Arrays.asList("a","an","the")); 
		}
	}

	public List<String> getTokens() {
		List<String> tokens = new ArrayList<String>();
		for (CoreLabel token : atext.get(CoreAnnotations.TokensAnnotation.class)) {
			String tokenStr = token.toString().toLowerCase();
			if (tokenStr.matches("[a-zA-Z]+") 
				&& tokenStr.length() <= MAX_TOKEN_SIZE
				&& !wordsToRemove.contains(tokenStr))
				
				tokens.add(tokenStr);
		}
		return tokens;
	}
	
	public static TokenType getTokenTypeFromNGramSize(int nGramSize) {
		if (nGramSize == 1) 
			return TokenType.UNIGRAM;
		if (nGramSize == 2)
			return TokenType.BIGRAM;
		else
			return TokenType.TRIGRAM;
	}

	private static StanfordCoreNLP getPipeline() {
		if (instance == null)
			instance = createPipeline();
		return instance;
	}

	private static StanfordCoreNLP createPipeline() {
		Properties props = new Properties();
		props.put("annotators", "tokenize");
		props.put("tokenize.whitespace", "false");
		props.put("tokenize.options",
				"normalizeParentheses=false, normalizeOtherBrackets=false");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		return pipeline;
	}

	public static void main(String[] args) throws IOException {
		try {
			Document doc = Jsoup.parse(new File(args[0]), "utf-8");
			Properties props = new Properties();
			props.put("annotators", "tokenize");
			props.put("tokenize.whitespace", "false");
			props.put("tokenize.options",
					"normalizeParentheses=false, normalizeOtherBrackets=false");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			List<Annotation> annotations = new LinkedList<Annotation>();
			annotations.add(new Annotation(doc.select("title,body").text()));
			annotations.add(new Annotation(doc.select("a[href]").attr("href")));
			annotations.add(new Annotation(doc.select(
					"meta[name~=(keywords|description)[content]").attr(
					"content")));

			pipeline.annotate(annotations);

			for (Annotation ann : annotations) {
				for (CoreLabel token : ann
						.get(CoreAnnotations.TokensAnnotation.class)) {
					System.out.println(String.format("token = %s", token));
				}
			}
		} catch (ArrayIndexOutOfBoundsException | FileNotFoundException e) {
			System.out.println("usage: Tokenizer <input-file>");
			System.exit(1);
		}

	}
}
