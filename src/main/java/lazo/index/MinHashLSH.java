package lazo.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lazo.sketch.Sketch;

public class MinHashLSH {

    private float threshold;
    private int k;
    private int bands;
    private int rows;
    private float false_positive_rate;
    private float false_negative_rate;

    private List<Map<long[], List<Object>>> hashTables;
    private int[] hashRanges;
    private Map<Object, List<long[]>> keys;

    // integration precision
    private float IP = 0.001f;

    public MinHashLSH(float threshold, int k) {
	this.threshold = threshold;
	this.k = k;
	// 0.5 are good default values for false positive and negative
	this.computeOptimalParameters(this.threshold, this.k, 0.5f, 0.5f);

	this.initializeHashTables();
	this.keys = new HashMap<>();
    }

    public MinHashLSH(float threshold, int k, float false_positive_rate, float false_negative_rate) {
	this.threshold = threshold;
	this.k = k;
	this.computeOptimalParameters(this.threshold, this.k, false_positive_rate, false_negative_rate);

	this.initializeHashTables();
	this.keys = new HashMap<>();
    }

    public MinHashLSH(float threshold, int k, int bands, int rows) {
	this.threshold = threshold;
	this.k = k;
	this.bands = bands;
	this.rows = rows;

	this.initializeHashTables();
	this.keys = new HashMap<>();
    }

    private void initializeHashTables() {
	this.hashTables = new ArrayList<>();
	this.hashRanges = new int[this.bands];
	// hash tables
	for (int i = 0; i < hashTables.size(); i++) {
	    Map<long[], List<Object>> mp = new HashMap<>();
	    hashTables.add(mp);
	}
	// hash ranges
	for (int i = 0; i < hashRanges.length; i++) {
	    hashRanges[i] = i * this.rows;
	}
    }

    private double p(double s, int rows, int bands) {
	return 1 - Math.pow((1 - Math.pow(s, rows)), bands);
    }

    private float integrate(float a, float b, int rows, int bands) {
	float area = 0;
	float x = a;
	while (x < b) {
	    area += this.p(x + 0.5 * IP, rows, bands) * IP;
	    x = x + IP;
	}
	return (float) area;
    }

    private float computeFalsePositiveProbability(float threshold, int bands, int rows) {
	return this.integrate(0.0f, threshold, rows, bands);
    }

    private float computeFalseNegativeProbability(float threshold, int bands, int rows) {
	return this.integrate(threshold, 0.0f, rows, bands);
    }

    private void computeOptimalParameters(float threshold, int k, float fp_rate, float fn_rate) {
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
	// FIXME; return this in a tuple or similar; set in constructor instead
	this.rows = optimalRows;
	this.bands = optimalBands;
    }

    public boolean insert(Object key, Sketch mh) {

	List<long[]> segments = new ArrayList<>();
	for (int start : this.hashRanges) {
	    int end = start + this.rows;
	    long[] segment = Arrays.copyOfRange(mh.getHashValues(), start, end);
	    segments.add(segment);
	}
	this.keys.put(key, segments);

	for (int i = 0; i < this.bands; i++) {
	    long[] sg = segments.get(i);
	    Map<long[], List<Object>> hashTable = hashTables.get(i);
	    if (hashTable.get(sg) == null) {
		List<Object> l = new ArrayList<>();
		hashTable.put(sg, l);
	    }
	    hashTable.get(sg).add(key);
	}

	return true;
    }

    public Set<Object> query(Sketch mh) {
	Set<Object> candidates = new HashSet<>();
	for (int i = 0; i < this.bands; i++) {
	    int start = this.hashRanges[i];
	    int end = this.hashRanges[i] * this.rows;
	    Map<long[], List<Object>> hashTable = hashTables.get(i);
	    long[] segment = Arrays.copyOfRange(mh.getHashValues(), start, end);
	    if (hashTable.containsKey(segment)) {
		candidates.addAll(hashTable.get(segment));
	    }
	}
	return candidates;
    }

}
