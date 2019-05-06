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
	float hits = 0;
	for (String s : smaller) {
	    if (larger.contains(s)) {
		hits += 1;
	    }
	}
	float ix = hits;
	float union = (smaller.size() + larger.size()) - ix;
	js = (float) (ix / union);
	return js;
    }

    public static float computeJC(Set<String> a, Set<String> b) {
	float ix = 0;
	Set<String> smaller = null;
	Set<String> larger = null;
	if (a.size() >= b.size()) {
	    smaller = b;
	    larger = a;
	} else {
	    smaller = a;
	    larger = b;
	}
	for (String s : smaller) {
	    if (larger.contains(s)) {
		ix += 1;
	    }
	}

	return (float) (ix / (float) a.size());
    }

    public static float[] computeJSAndJC(Set<String> a, Set<String> b) {
	float js_jcx_jcy[] = new float[3];

	float ix = 0;
	Set<String> smaller = null;
	Set<String> larger = null;
	if (a.size() >= b.size()) {
	    smaller = b;
	    larger = a;
	} else {
	    smaller = a;
	    larger = b;
	}
	for (String s : smaller) {
	    if (larger.contains(s)) {
		ix += 1;
	    }
	}

	float union = (smaller.size() + larger.size()) - ix;
	float js = (float) (ix / union);
	float jcx = (float) (ix / (float) a.size());
	float jcy = (float) (ix / (float) b.size());
	js_jcx_jcy[0] = js;
	js_jcx_jcy[1] = jcx;
	js_jcx_jcy[2] = jcy;

	return js_jcx_jcy;
    }

}
