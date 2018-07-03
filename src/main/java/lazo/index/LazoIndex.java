package lazo.index;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lazo.sketch.LazoSketch;

public class LazoIndex {

    private final boolean ECH = true;

    // metrics
    private long ech_time;

    private int k;
    private float d;
    private int numIndexes;
    private Map<Integer, MinHashLSH> indexes;
    private Map<Object, Long> keyCardinality;

    public LazoIndex(int k, float d) {
	this.k = k;
	this.d = d;

	this.keyCardinality = new HashMap<>();

	this.numIndexes = (int) (1 / d);
	this.indexes = new HashMap<>();
	for (int i = 0; i < this.numIndexes; i++) {
	    float threshold = d * i;
	    this.indexes.put(i, new MinHashLSH(threshold, k));
	}
    }

    public LazoIndex(int k) {
	this.k = k;
	this.d = 0.05f; // default for 20 indexes

	this.keyCardinality = new HashMap<>();

	this.numIndexes = (int) (1 / d);
	this.indexes = new HashMap<>();
	for (int i = 0; i < this.numIndexes; i++) {
	    float threshold = d * i;
	    this.indexes.put(i, new MinHashLSH(threshold, k));
	}
    }

    public long get_ech_time() {
	return this.ech_time;
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
	    int key_threshold = this.numIndexes - i - 1;
	    float queryingThreshold = key_threshold * this.d;
	    Set<Object> thresholdCandidates = indexes.get(key_threshold).query(sketch.getSketch());
	    for (Object pCandidate : thresholdCandidates) {
		if (!seenCandidates.contains(pCandidate)) {
		    partialCandidates.put(pCandidate, queryingThreshold);
		    seenCandidates.add(pCandidate);
		}
	    }
	}

	Set<LazoCandidate> candidates = new HashSet<>();

	// compute estimates for each partialCandidate
	long s = System.currentTimeMillis();
	for (Object key : partialCandidates.keySet()) {
	    float th = partialCandidates.get(key);
	    float lowerThreshold = th;
	    float upperThreshold = th + this.d;
	    long queryCardinality = sketch.getCardinality();
	    long candidateCardinality = this.keyCardinality.get(key);
	    long minCardinality = Math.min(queryCardinality, candidateCardinality);
	    long maxCardinality = Math.max(queryCardinality, candidateCardinality);
	    long alphaLower = this.getAlpha(minCardinality, maxCardinality, lowerThreshold);
	    long alphaUpper = this.getAlpha(minCardinality, maxCardinality, upperThreshold);

	    long ixLower = this.estimateIntersection(minCardinality, alphaLower);
	    long ixUpper = this.estimateIntersection(minCardinality, alphaUpper);
	    long unLower = this.estimateUnion(maxCardinality, alphaLower);
	    long unUpper = this.estimateUnion(maxCardinality, alphaUpper);

	    float estJSLower = unLower > 0 ? (float) ixLower / (float) unLower : 0F;
	    float estJSUpper = unUpper > 0 ? (float) ixUpper / (float) unUpper : 0F;
	    float estJCXLower = queryCardinality > 0 ? (float) ixLower / (float) queryCardinality : 0F;
	    float estJCXUpper = queryCardinality > 0 ? (float) ixUpper / (float) queryCardinality : 0F;
	    float estJCYLower = candidateCardinality > 0 ? (float) ixLower / (float) candidateCardinality : 0F;
	    float estJCYUpper = candidateCardinality > 0 ? (float) ixUpper / (float) candidateCardinality : 0F;

	    float jcxMaxBound = queryCardinality > 0 ? (float) minCardinality / (float) queryCardinality : 0F;
	    if (jcxMaxBound > 1)
		jcxMaxBound = 1F;
	    float jcyMaxBound = candidateCardinality > 0 ? (float) minCardinality / (float) candidateCardinality : 0F;
	    if (jcyMaxBound > 1)
		jcyMaxBound = 1F;

	    if (!this.ECH) {
		float avgJs = (estJSLower + estJSUpper) / 2;
		float avgJcx = (estJCXLower + estJCXUpper) / 2;
		float avgJcy = (estJCYLower + estJCYUpper) / 2;
		if (avgJs >= js_threshold && avgJcx >= jcx_threshold) {
		    candidates.add(new LazoCandidate(key, avgJs, avgJcx, avgJcy));
		}
		continue;
	    }

	    if (estJCXUpper > jcxMaxBound && jcxMaxBound > 0) {
		long correctedAlpha = this.correctEstimate(minCardinality, queryCardinality, jcxMaxBound);
		estJSUpper = (float) (minCardinality - correctedAlpha) / (float) (maxCardinality + correctedAlpha);
		estJCYUpper = candidateCardinality > 0
			? (float) (minCardinality - correctedAlpha) / (float) candidateCardinality : 0F;
		estJCXUpper = jcxMaxBound;
	    } else if (estJCYUpper > jcyMaxBound && jcyMaxBound > 0) {
		long correctedAlpha = this.correctEstimate(minCardinality, candidateCardinality, jcyMaxBound);
		estJSUpper = (float) (minCardinality - correctedAlpha) / (float) (maxCardinality + correctedAlpha);
		estJCXUpper = queryCardinality > 0
			? (float) (minCardinality - correctedAlpha) / (float) queryCardinality : 0F;
		estJCYUpper = jcyMaxBound;
	    }

	    if (estJCXLower > jcxMaxBound && jcxMaxBound > 0) {
		long correctedAlpha = this.correctEstimate(minCardinality, queryCardinality, jcxMaxBound);
		estJSLower = (float) (minCardinality - correctedAlpha) / (float) (maxCardinality + correctedAlpha);
		estJCYLower = candidateCardinality > 0
			? (float) (minCardinality - correctedAlpha) / (float) candidateCardinality : 0F;
		estJCXLower = jcxMaxBound;

	    } else if (estJCYLower > jcyMaxBound && jcyMaxBound > 0) {
		long correctedAlpha = this.correctEstimate(minCardinality, candidateCardinality, jcyMaxBound);
		estJSLower = (float) (minCardinality - correctedAlpha) / (float) (maxCardinality + correctedAlpha);
		estJCXLower = queryCardinality > 0
			? (float) (minCardinality - correctedAlpha) / (float) queryCardinality : 0F;
		estJCYLower = jcyMaxBound;
	    }
	    float avgJs = (estJSLower + estJSUpper) / 2;
	    float avgJcx = (estJCXLower + estJCXUpper) / 2;
	    float avgJcy = (estJCYLower + estJCYUpper) / 2;

	    // Filter out results based on thresholds
	    if (avgJs >= js_threshold && avgJcx >= jcx_threshold) {
		candidates.add(new LazoCandidate(key, avgJs, avgJcx, avgJcy));
	    }
	}
	long e = System.currentTimeMillis();
	this.ech_time += (e - s);
	return candidates;
    }

    private long getAlpha(long minCardinality, long maxCardinality, float threshold) {
	// (min - max * js) / (1 + js)
	return (long) ((minCardinality - (threshold * maxCardinality)) / (1 + threshold));
    }

    private long estimateIntersection(long minCardinality, long alpha) {
	return minCardinality - alpha;
    }

    private long estimateUnion(long maxCardinality, long alpha) {
	return maxCardinality + alpha;
    }

    private long correctEstimate(long minCardinality, long sketchCardinality, float jcBound) {
	return (long) (minCardinality - (jcBound * sketchCardinality));
    }
}
