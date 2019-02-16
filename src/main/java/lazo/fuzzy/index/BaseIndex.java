package lazo.fuzzy.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lazo.fuzzy.sketch.NGramSignature;
import lazo.index.LazoIndex;
import lazo.index.LazoIndex.LazoCandidate;
import lazo.sketch.LazoSketch;

public class BaseIndex {

    private int k;
    private int n;
    private final int ORIGINAL_STRING = 0;
    private final float D = 0.05f;
    private Map<Integer, LazoIndex> indexes;

    public BaseIndex(int k, int n) {
	this.k = k;
	this.n = n;
	indexes = new HashMap<>();
	for (int ngramSize = 2; ngramSize < n + 1; ngramSize++) {
	    indexes.put(ngramSize, new LazoIndex(k, D));
	}
	indexes.put(ORIGINAL_STRING, new LazoIndex(k, D));
    }

    public boolean insert(Object key, NGramSignature signature) {
	assert (signature.getN() == this.n);

	for (int ngramSize = 2; ngramSize < n + 1; ngramSize++) {
	    LazoIndex index = indexes.get(ngramSize);
	    LazoSketch sketch = signature.getSketch(ngramSize);
	    index.insert(key, sketch);
	}
	indexes.get(ORIGINAL_STRING).insert(key, signature.getSketch(ORIGINAL_STRING));

	return true;
    }

    public Set<LazoCandidate> queryNgram(NGramSignature signature, int n, float js, float jc) {
	LazoSketch sketch = signature.getSketch(n);
	LazoIndex index = this.indexes.get(n);
	Set<LazoCandidate> results = index.query(sketch, js, jc);
	return results;
    }

    private float calculateTheta(int card, float aggrJC, float localJC) {
	double beta = Math.log((card / aggrJC));
	double theta = (localJC * beta) / Math.log(card);
	return (float) theta;
    }

    public class FuzzyLazoCandidate {
	public final Object key;
	public final float m;

	public FuzzyLazoCandidate(Object key, float m) {
	    this.key = key;
	    this.m = m;
	}
    }

    public Set<FuzzyLazoCandidate> query(NGramSignature signature, float sim) {
	Set<FuzzyLazoCandidate> results = new HashSet<>();
	Map<Object, List<Float>> metrics = new HashMap<>();
	for (int ngramSize = 2; ngramSize < n + 1; ngramSize++) {
	    Set<LazoCandidate> ngramResults = this.queryNgram(signature, ngramSize, 0f, 0.1f); // FIXME:
	    int totalResults = ngramResults.size();
	    float totalAggregatedJC = 0;
	    for (LazoCandidate lc : ngramResults) {
		totalAggregatedJC += lc.jcx;
	    }
	    for (LazoCandidate lc : ngramResults) {
		float metric = calculateTheta(totalResults, totalAggregatedJC, lc.jcx);
		if (!metrics.containsKey(lc.key)) {
		    metrics.put(lc.key, new ArrayList<>());
		}
		metrics.get(lc.key).add(metric);
	    }
	}
	// Aggregate with avg results and return in LazoCandidate
	for (Entry<Object, List<Float>> entry : metrics.entrySet()) {
	    List<Float> resultMetrics = entry.getValue();
	    float total = 0;
	    for (Float f : resultMetrics) {
		total += f;
	    }
	    float avgAggr = total / resultMetrics.size();
	    FuzzyLazoCandidate nlc = new FuzzyLazoCandidate(entry.getKey(), avgAggr);
	    results.add(nlc);
	}
	return results;
    }

    public float[][] calculateBeta(List<Object> keys, List<NGramSignature> signatures, int ngramSize) {
	assert (keys.size() == signatures.size());

	// assign an index in the matrix to each key
	Map<Object, Integer> mapKeyToIndex = new HashMap<>();
	for (int i = 0; i < keys.size(); i++) {
	    mapKeyToIndex.put(keys.get(i), i);
	}

	// matrix where to store the ix
	float[][] intersectionMatrix = new float[signatures.size()][signatures.size()];

	// fill in the matrix
	for (int i = 0; i < signatures.size(); i++) {
	    NGramSignature signature = signatures.get(i);
	    Object key = keys.get(i);
	    int index = mapKeyToIndex.get(key);
	    Set<LazoCandidate> results = this.queryNgram(signature, ngramSize, 0, 0);
	    // long cardinality = signature.getCardinality(ngramSize);
	    for (LazoCandidate result : results) {
		Object resultKey = result.key;
		float jc = result.jcx;
		// int ix = (int) (jc * cardinality); // jc = ix/c so ix = jc*c
		int resultIndex = mapKeyToIndex.get(resultKey);
		intersectionMatrix[index][resultIndex] = jc;
	    }
	}
	return intersectionMatrix;
    }

}
