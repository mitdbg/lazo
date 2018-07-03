package lazo.benchmark;

import java.util.Set;

public class Utils {

    public static float computeJS(Set<String> a, Set<String> b) {
	// more efficient union and ix
	float js = 0;
	Set<String> smaller = null;
	Set<String> larger = null;
	if (a.size() >= b.size()) {
	    smaller = b;
	    larger = a;
	} else {
	    smaller = a;
	    larger = b;
	}
	int hits = 0;
	for (String s : smaller) {
	    if (larger.contains(s)) {
		hits += 1;
	    }
	}
	int ix = hits;
	int union = (smaller.size() + larger.size()) - ix;
	js = ix / union;
	return js;
    }

}
