package lazo.fuzzy.sketch;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    public static String[] getTokens(String s) {
	String[] words = s.split(" ");
	return words;
    }

    public static List<String> extractNGram(String s, int ngramSize) {
	List<String> ngrams = new ArrayList<>();

	s = s.toLowerCase();

	// Split only by space for now
	String[] words = StringUtils.getTokens(s);
	for (String word : words) {
	    for (int i = 0; i < word.length() + 1 - ngramSize; i++) {
		String ngram = word.substring(i, i + ngramSize);
		ngrams.add(ngram);
	    }
	}

	return ngrams;
    }

}
