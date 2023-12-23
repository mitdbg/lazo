package lazo.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lazo.sketch.LazoSketch;

public class LazoIndex {

    private final boolean ECH = true;

    // metrics
    private long ech_time;
    public int corrections;
    public int js_impactful_corrections;
    public int jcx_impactful_corrections;
    public float magnitude_correction;

    private int k;
    private float d;
    private float fp_rate;
    private float fn_rate;
    private int numThresholds;

    private int gcdSliceSize;
    private int gcdBands;
    private List<Map<Long, Set<Object>>> hashTables;
    private List<Map<Object, Set<Long>>> segmentIds;
    private int[] hashRanges;
    private Map<Object, Long> keyCardinality;

    // threshold - (b,r)
    private Map<Integer, Integer[]> thresholdToBandsRows = new HashMap<>();

    // integration precision
    private float IP = 0.001f;

    public LazoIndex() {
	this.k = 64;
	this.d = 0.05f; // default for 20 indexes
	this.fp_rate = 0.5f;
	this.fn_rate = 0.5f;

	this.initIndex(this.k, this.d, this.fp_rate, this.fn_rate);
    }

    public LazoIndex(int k) {
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.d = 0.05f; // default for 20 indexes
	this.fp_rate = 0.5f;
	this.fn_rate = 0.5f;

	this.initIndex(this.k, this.d, this.fp_rate, this.fn_rate);
    }

    public LazoIndex(int k, float d) {
	if (d < 0 || d > 0.5) {
	    throw new IllegalArgumentException(
		    "Threshold for d must be in the range [0,0.5], recommended:" + "0.05 or 0.1");
	}
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.d = d;
	this.fp_rate = 0.5f;
	this.fn_rate = 0.5f;

	this.initIndex(this.k, this.d, this.fp_rate, this.fn_rate);
    }

    private void initIndex(int k, float d, float fp_rate, float fn_rate) {
	this.keyCardinality = new HashMap<>();

	this.numThresholds = (int) (1 / d);

	Set<Integer> rowCombinations = new HashSet<>();

	// associate threshold to b,r combination and keep rows to compute
	// bin-options
	for (int i = 0; i < this.numThresholds; i++) {
	    float threshold = d * i;
	    Integer[] bandsAndRows = this.computeOptimalParameters(threshold, k, fp_rate, fn_rate);
	    this.thresholdToBandsRows.put(i, bandsAndRows);
	    int rows = bandsAndRows[1];
	    rowCombinations.add(rows);
	}
	// compute bin-options
	int gcdSliceSize = findGCDOf(rowCombinations.toArray(new Integer[rowCombinations.size()]));
	int gcdBands = this.k / gcdSliceSize;
	this.gcdSliceSize = gcdSliceSize;
	this.gcdBands = gcdBands;
	this.hashTables = new ArrayList<>();
	this.segmentIds = new ArrayList<>();
	this.hashRanges = new int[gcdBands];
	// hash tables
	for (int i = 0; i < gcdBands; i++) {
	    Map<Long, Set<Object>> mp = new HashMap<>();
	    hashTables.add(mp);
	    Map<Object, Set<Long>> l = new HashMap<>();
	    segmentIds.add(l);
	}
	// hash ranges
	for (int i = 0; i < hashRanges.length; i++) {
	    hashRanges[i] = i * gcdSliceSize;
	}

    }

    public int __getNumHashTables() {
	return this.hashTables.size();
    }

    private int gcd(int x, int y) {
	// Euclid's algo
	return (y == 0) ? x : gcd(y, x % y);
    }

    public int findGCDOf(Integer... numbers) {
	return Arrays.stream(numbers).reduce(0, (x, y) -> gcd(x, y));
    }

    private float computeFalsePositiveProbability(float threshold, int bands, int rows) {
	float start = 0.0f;
	float end = threshold;
	float area = 0.0f;
	float x = start;
	while (x < end) {
	    area += 1 - Math.pow((1 - Math.pow(x + 0.5 * IP, rows)), bands) * IP;
	    x = x + IP;
	}
	return (float) area;

    }

    private float computeFalseNegativeProbability(float threshold, int bands, int rows) {
	float start = threshold;
	float end = 1.0f;
	float area = 0.0f;
	float x = start;
	while (x < end) {
	    area += 1 - (1 - Math.pow((1 - Math.pow(x + 0.5 * IP, rows)), bands) * IP);
	    x = x + IP;
	}
	return (float) area;
    }

    private Integer[] computeOptimalParameters(float threshold, int k, float fp_rate, float fn_rate) {
	float minError = Float.MAX_VALUE;
	int optimalBands = 0;
	int optimalRows = 0;

	int maximumRows = 0;
	for (int band = 1; band < k + 1; band++) {
	    maximumRows = (int) (k / band);
	    for (int rows = 1; rows < maximumRows + 1; rows++) {
		float falsePositives = this.computeFalsePositiveProbability(threshold, band, rows);
		float falseNegatives = this.computeFalseNegativeProbability(threshold, band, rows);
		float error = fp_rate * falsePositives + fn_rate * falseNegatives;
		if (error < minError) {
		    minError = error;
		    optimalBands = band;
		    optimalRows = rows;
		}
	    }
	}
	return new Integer[] { optimalBands, optimalRows };
    }

    public long get_ech_time() {
	return this.ech_time;
    }

    private long segmentHash(long[] segment) {
	return Arrays.hashCode(segment);
    }

    public boolean insert(Object key, LazoSketch sketch) {
	// Store cardinality of key
	keyCardinality.put(key, sketch.getCardinality());
	// Obtain segments of this sketch
	List<long[]> segments = new ArrayList<>();
	for (int start : this.hashRanges) {
	    int end = start + this.gcdSliceSize;
	    long[] segment = Arrays.copyOfRange(sketch.getHashValues(), start, end);
	    segments.add(segment);
	}
	// Insert key in the hashmap handling each band
	for (int i = 0; i < this.gcdBands; i++) {
	    long[] sg = segments.get(i);
	    Map<Long, Set<Object>> hashTable = hashTables.get(i);
	    long segId = segmentHash(sg);
	    if (hashTable.get(segId) == null) {
		Set<Object> l = new HashSet<>();
		hashTable.put(segId, l);
	    }
	    hashTable.get(segId).add(key);

	    // Storing segment information
	    Map<Object, Set<Long>> segmentIdInfo = segmentIds.get(i);
	    if (segmentIdInfo.get(key) == null) {
	        Set<Long> l = new HashSet<>();
	        segmentIdInfo.put(key, l);
	    }
	    segmentIdInfo.get(key).add(segId);
	}
	return true;
    }
    
    // To remove existing data from the index, the segments are saved
    //   when inserting data. There is a tradeoff between the default
    //   storage needed to keep the segments vs. keeping just the hash
    //   values (reduce data structure burden) and do more work on removal.
    //   Depending on workloads one or the other would be better.
    public boolean remove(Object key) {
        if (keyCardinality.get(key) == null) {
            return false;
        }
        // Remove cardinality of key
        keyCardinality.remove(key);
        // Remove key from hashmaps
        for (int i = 0; i < this.gcdBands; i++) {
            Map<Object, Set<Long>> segmentIdInfo = segmentIds.get(i);
            Map<Long, Set<Object>> hashTable = hashTables.get(i);
            if (segmentIdInfo.get(key) == null) continue;
            for (long segId : segmentIdInfo.get(key)) {
                hashTable.get(segId).remove(key);
                if (hashTable.get(segId).isEmpty()) {
                    hashTable.remove(segId);
                }
            }
            segmentIdInfo.remove(key);
        }
        return true;
    }
    
    public boolean update(Object key, LazoSketch sketch) {
        this.remove(key);
        return this.insert(key, sketch);
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
	return this.query(sketch, js_threshold, 0f);
    }

    public Set<LazoCandidate> queryContainment(LazoSketch sketch, float jcx_threshold) {
	return this.query(sketch, 0f, jcx_threshold);
    }

    private Set<Object> setIX(Set<Object> a, Set<Object> b) {
	Set<Object> ix = new HashSet<>();
	Set<Object> smaller = null;
	Set<Object> larger = null;
	if (a.size() >= b.size()) {
	    smaller = b;
	    larger = a;
	} else {
	    smaller = a;
	    larger = b;
	}
	for (Object s : smaller) {
	    if (larger.contains(s)) {
		ix.add(s);
	    }
	}
	return ix;
    }

    public Set<Object> querySlice(LazoSketch sketch, int bands, int rows) {
	Set<Object> candidates = new HashSet<>();
	// ix on rows, un on bands
	for (int b = 0; b < bands; b++) {
	    int gcdFactorsPerRows = rows / this.gcdSliceSize;
	    Set<Object> bandCandidates = new HashSet<>();
	    for (int i = 0; i < gcdFactorsPerRows; i++) {
		int start = this.hashRanges[(b * this.gcdSliceSize) + i];
		int end = (this.hashRanges[(b * this.gcdSliceSize) + i] + 1) * this.gcdSliceSize;
		long[] segment = Arrays.copyOfRange(sketch.getHashValues(), start, end);
		long segId = segmentHash(segment);

		Map<Long, Set<Object>> hashTable = this.hashTables.get(b);
		if (hashTable.containsKey(segId)) {
		    Set<Object> queryResult = hashTable.get(segId);
		    if (bandCandidates.size() == 0) {
			bandCandidates.addAll(queryResult);
		    } else {
			bandCandidates = setIX(bandCandidates, queryResult);
			if (bandCandidates.size() == 0) {
			    break; // no candidates in this band
			}
		    }
		} else {
		    break; // key does not even exist
		}
	    }
	    // We now union bandCandidates with candidates
	    candidates.addAll(bandCandidates);
	}
	return candidates;
    }

    public Set<LazoCandidate> query(LazoSketch sketch, float js_threshold, float jcx_threshold) {

	// Get all candidates
	Map<Object, Float> partialCandidates = new HashMap<>();
	Set<Object> seenCandidates = new HashSet<>();

	for (int i = 0; i < this.numThresholds; i++) {
	    int key_threshold = this.numThresholds - i - 1;
	    float queryingThreshold = key_threshold * this.d;
	    Integer[] bandsAndRows = thresholdToBandsRows.get(key_threshold);
	    int bands = bandsAndRows[0];
	    int rows = bandsAndRows[1];
	    Set<Object> thresholdCandidates = this.querySlice(sketch, bands, rows);
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
	    boolean corrected = false;
	    float originalJSUpper = -1;
	    float originalJCXUpper = -1;
	    float originalJSLower = -1;
	    float originalJCXLower = -1;
	    if (estJCXUpper > jcxMaxBound && jcxMaxBound > 0) {
		corrected = true;
		long correctedAlpha = this.correctEstimate(minCardinality, queryCardinality, jcxMaxBound);
		originalJSUpper = estJSUpper;
		estJSUpper = (float) (minCardinality - correctedAlpha) / (float) (maxCardinality + correctedAlpha);
		estJCYUpper = candidateCardinality > 0
			? (float) (minCardinality - correctedAlpha) / (float) candidateCardinality : 0F;
		originalJCXUpper = estJCXUpper;
		estJCXUpper = jcxMaxBound;
	    } else if (estJCYUpper > jcyMaxBound && jcyMaxBound > 0) {
		corrected = true;
		long correctedAlpha = this.correctEstimate(minCardinality, candidateCardinality, jcyMaxBound);
		originalJSUpper = estJSUpper;
		estJSUpper = (float) (minCardinality - correctedAlpha) / (float) (maxCardinality + correctedAlpha);
		float magnitudeChange = Math.abs(estJSUpper - originalJSUpper);
		this.magnitude_correction += magnitudeChange;
		originalJCXUpper = estJCXUpper;
		estJCXUpper = queryCardinality > 0
			? (float) (minCardinality - correctedAlpha) / (float) queryCardinality : 0F;
		estJCYUpper = jcyMaxBound;
	    }

	    if (estJCXLower > jcxMaxBound && jcxMaxBound > 0) {
		corrected = true;
		long correctedAlpha = this.correctEstimate(minCardinality, queryCardinality, jcxMaxBound);
		originalJSLower = estJSLower;
		estJSLower = (float) (minCardinality - correctedAlpha) / (float) (maxCardinality + correctedAlpha);
		float magnitudeChange = Math.abs(estJSLower - originalJSLower);
		this.magnitude_correction += magnitudeChange;
		estJCYLower = candidateCardinality > 0
			? (float) (minCardinality - correctedAlpha) / (float) candidateCardinality : 0F;
		originalJCXLower = estJCXLower;
		estJCXLower = jcxMaxBound;

	    } else if (estJCYLower > jcyMaxBound && jcyMaxBound > 0) {
		corrected = true;
		long correctedAlpha = this.correctEstimate(minCardinality, candidateCardinality, jcyMaxBound);
		originalJSLower = estJSLower;
		estJSLower = (float) (minCardinality - correctedAlpha) / (float) (maxCardinality + correctedAlpha);
		float magnitudeChange = Math.abs(estJSLower - originalJSLower);
		this.magnitude_correction += magnitudeChange;
		originalJCXLower = estJCXLower;
		estJCXLower = queryCardinality > 0
			? (float) (minCardinality - correctedAlpha) / (float) queryCardinality : 0F;
		estJCYLower = jcyMaxBound;
	    }
	    if (corrected) {
		corrections++;
		if (originalJSUpper <= js_threshold && estJSUpper > js_threshold) {
		    this.js_impactful_corrections++;
		}
		if (originalJSUpper > js_threshold && estJSUpper <= js_threshold) {
		    this.js_impactful_corrections++;
		}
		if (originalJCXUpper <= jcx_threshold && estJCXUpper > jcx_threshold) {
		    this.jcx_impactful_corrections++;
		}
		if (originalJCXUpper > jcx_threshold && estJCXUpper <= jcx_threshold) {
		    this.jcx_impactful_corrections++;
		}
		if (originalJCXLower <= jcx_threshold && estJCXLower > jcx_threshold) {
		    this.jcx_impactful_corrections++;
		}
		if (originalJCXLower > jcx_threshold && estJCXLower <= jcx_threshold) {
		    this.jcx_impactful_corrections++;
		}
		float magnitudeChange = Math.abs(estJSUpper - originalJSUpper);
		this.magnitude_correction += magnitudeChange;
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
	long newAlpha = (long) (minCardinality - (jcBound * sketchCardinality));
	return newAlpha;
    }
}
