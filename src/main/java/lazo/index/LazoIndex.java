package lazo.index;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lazo.sketch.LazoSketch;

public class LazoIndex {

    private final boolean ECH = true;

    private int k;
    private int d;
    private int numIndexes;
    private Map<Float, MinHashLSH> indexes;
    private Map<Object, Integer> keyCardinality;

    public LazoIndex(int k, int d) {
	this.k = k;
	this.d = d;

	this.keyCardinality = new HashMap<>();

	this.numIndexes = (int) (1 / d);
	this.indexes = new HashMap<>();
	for (int i = 0; i < this.numIndexes; i++) {
	    float threshold = d * i;
	    this.indexes.put(threshold, new MinHashLSH(threshold, k));
	}
    }

    public boolean insert(Object key, LazoSketch sketch) {
	for (MinHashLSH lshIndex : indexes.values()) {
	    lshIndex.insert(key, sketch);
	}
	keyCardinality.put(key, sketch.getCardinality());
	return false;
    }

    public class LazoCandidate {
	public final Object key;
	public final float js;
	public final float jcx;
	public final float jcy;

	public LazoCandidate(Object key, float js, float jcx, float jcy) {
	    this.key = key;
	    this.js = js;
	    this.jcx = jcx;
	    this.jcy = jcy;
	}
    }

    public Set<LazoCandidate> querySimilarity(LazoSketch sketch, float js_threshold) {
	return this.query(sketch, js_threshold, 0);
    }

    public Set<LazoCandidate> queryContainment(LazoSketch sketch, float jcx_threshold) {
	return this.query(sketch, 0, jcx_threshold);
    }

    public Set<LazoCandidate> query(LazoSketch sketch, float js_threshold, float jcx_threshold) {

	// Get all candidates
	// TODO: is it necessary to pre-materialize this?
	Map<Object, Float> partialCandidates = new HashMap<>();
	Set<Object> seenCandidates = new HashSet<>();
	for (int i = 0; i < this.numIndexes; i++) {
	    float queryingThreshold = (float) (this.d * i);
	    Set<Object> thresholdCandidates = indexes.get(queryingThreshold).query(sketch.getSketch());
	    for (Object pCandidate : thresholdCandidates) {
		if (!seenCandidates.contains(pCandidate)) {
		    partialCandidates.put(pCandidate, queryingThreshold);
		    seenCandidates.add(pCandidate);
		}
	    }
	}

	Set<LazoCandidate> candidates = new HashSet<>();

	// compute estimates for each partialCandidate
	for (Object key : partialCandidates.keySet()) {
	    float th = partialCandidates.get(key);
	    float lowerThreshold = th;
	    float upperThreshold = th + this.d;
	    int queryCardinality = sketch.getCardinality();
	    int candidateCardinality = this.keyCardinality.get(key);
	    int minCardinality = Math.min(queryCardinality, candidateCardinality);
	    int maxCardinality = Math.max(queryCardinality, candidateCardinality);
	    int alphaLower = this.getAlpha(minCardinality, maxCardinality, lowerThreshold);
	    int alphaUpper = this.getAlpha(minCardinality, maxCardinality, upperThreshold);

	    int ixLower = this.estimateIntersection(minCardinality, alphaLower);
	    int ixUpper = this.estimateIntersection(minCardinality, alphaUpper);
	    int unLower = this.estimateUnion(maxCardinality, alphaLower);
	    int unUpper = this.estimateUnion(maxCardinality, alphaUpper);

	    float estJSLower = unLower > 0 ? ixLower / unLower : 0;
	    float estJSUpper = unUpper > 0 ? ixUpper / unUpper : 0;
	    float estJCXLower = queryCardinality > 0 ? ixLower / queryCardinality : 0;
	    float estJCXUpper = queryCardinality > 0 ? ixUpper / queryCardinality : 0;
	    float estJCYLower = candidateCardinality > 0 ? ixLower / candidateCardinality : 0;
	    float estJCYUpper = candidateCardinality > 0 ? ixUpper / candidateCardinality : 0;

	    float jcxMaxBound = queryCardinality > 0 ? minCardinality / queryCardinality : 0;
	    if (jcxMaxBound > 1)
		jcxMaxBound = 1;
	    float jcyMaxBound = candidateCardinality > 0 ? minCardinality / candidateCardinality : 0;
	    if (jcyMaxBound > 1)
		jcyMaxBound = 1;

	    if (!this.ECH) {
		float avgJs = (estJSLower + estJSUpper) / 2;
		float avgJcx = (estJCXLower + estJCXUpper) / 2;
		float avgJcy = (estJCYLower + estJCYUpper) / 2;
		candidates.add(new LazoCandidate(key, avgJs, avgJcx, avgJcy));
	    }

	    if (estJCXUpper > jcxMaxBound && jcxMaxBound > 0) {
		int correctedAlpha = this.correctEstimate(minCardinality, queryCardinality, jcxMaxBound);
		estJSUpper = (minCardinality - correctedAlpha) / (maxCardinality + correctedAlpha);
		estJCYUpper = candidateCardinality > 0 ? (minCardinality - correctedAlpha) / candidateCardinality : 0;
		estJCXUpper = jcxMaxBound;
	    } else if (estJCYUpper > jcyMaxBound && jcyMaxBound > 0) {
		int correctedAlpha = this.correctEstimate(minCardinality, candidateCardinality, jcyMaxBound);
		estJSUpper = (minCardinality - correctedAlpha) / (maxCardinality + correctedAlpha);
		estJCXUpper = queryCardinality > 0 ? (minCardinality - correctedAlpha) / queryCardinality : 0;
		estJCYUpper = jcyMaxBound;
	    }

	    if (estJCXLower > jcxMaxBound && jcxMaxBound > 0) {
		int correctedAlpha = this.correctEstimate(minCardinality, queryCardinality, jcxMaxBound);
		estJSLower = (minCardinality - correctedAlpha) / (maxCardinality + correctedAlpha);
		estJCYLower = candidateCardinality > 0 ? (minCardinality - correctedAlpha) / candidateCardinality : 0;
		estJCXLower = jcxMaxBound;

	    } else if (estJCYLower > jcyMaxBound && jcyMaxBound > 0) {
		int correctedAlpha = this.correctEstimate(minCardinality, candidateCardinality, jcyMaxBound);
		estJSLower = (minCardinality - correctedAlpha) / (maxCardinality + correctedAlpha);
		estJCXLower = queryCardinality > 0 ? (minCardinality - correctedAlpha) / queryCardinality : 0;
		estJCYLower = jcyMaxBound;
	    }
	    float avgJs = (estJSLower + estJSUpper) / 2;
	    float avgJcx = (estJCXLower + estJCXUpper) / 2;
	    float avgJcy = (estJCYLower + estJCYUpper) / 2;

	    // Filter out results based on thresholds
	    if (avgJs > js_threshold && avgJcx > jcx_threshold) {
		candidates.add(new LazoCandidate(key, avgJs, avgJcx, avgJcy));
	    }
	}
	return candidates;
    }

    private int getAlpha(int minCardinality, int maxCardinality, float threshold) {
	// (min - max * js) / (1 + js)
	return (int) ((minCardinality - (threshold * maxCardinality)) / (1 + threshold));
    }

    private int estimateIntersection(int minCardinality, int alpha) {
	return minCardinality - alpha;
    }

    private int estimateUnion(int maxCardinality, int alpha) {
	return maxCardinality + alpha;
    }

    private int correctEstimate(int minCardinality, int sketchCardinality, float jcBound) {
	return (int) (minCardinality - (jcBound * sketchCardinality));
    }
}
