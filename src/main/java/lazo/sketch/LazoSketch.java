package lazo.sketch;

import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;
import com.clearspring.analytics.stream.cardinality.ICardinality;

public class LazoSketch implements Sketch {

    private int k;
    private SketchType sketchType;

    private long cardinality = -1;
    private Sketch underlyingSketch;
    private ICardinality ic;

    public LazoSketch() {
	this.k = 64;
	this.sketchType = SketchType.MINHASH; // default
	this.underlyingSketch = new MinHash(k);
	// Cardinality estimator here
	ic = new HyperLogLogPlus(18, 25);
    }

    public LazoSketch(int k) {
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.sketchType = SketchType.MINHASH; // default
	this.underlyingSketch = new MinHash(k);
	// Cardinality estimator here
	ic = new HyperLogLogPlus(18, 25);
    }

    public LazoSketch(int k, SketchType sketchType) {
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.sketchType = sketchType;
	switch (sketchType) {
	case MINHASH:
	    this.underlyingSketch = new MinHash(k);
	    break;
	case MINHASH_OPTIMAL:
	    this.underlyingSketch = new MinHashOptimal(k);
	    break;
	default:
	    System.out.println("Sketch type unrecognized");
	}
	// Cardinality estimator here
	ic = new HyperLogLogPlus(18, 25);
    }

    public LazoSketch(int k, SketchType sketchType, ICardinality ic) {
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.sketchType = sketchType;
	switch (sketchType) {
	case MINHASH:
	    this.underlyingSketch = new MinHash(k);
	    break;
	case MINHASH_OPTIMAL:
	    this.underlyingSketch = new MinHashOptimal(k);
	    break;
	default:
	    System.out.println("Sketch type unrecognized");
	}
	// Cardinality estimator here
	this.ic = ic;
    }

    public ICardinality getCardinalityEstimator() {
	return this.ic;
    }

    @Override
    public void update(String value) {
	if (value == null) {
	    throw new IllegalArgumentException("Value cannot be null");
	}
	// cardinality materialization is not up to date
	cardinality = -1;
	ic.offer(value);
	this.underlyingSketch.update(value);
    }

    public Sketch getSketch() {
	return underlyingSketch;
    }

    public long getCardinality() {
	if (cardinality != -1) {
	    // has been precomputed and it is up to date
	    return cardinality;
	}
	// it has not been precomputed or it is not up to date
	return ic.cardinality();
    }

    @Override
    public long[] getHashValues() {
	return underlyingSketch.getHashValues();
    }

    @Override
    public void setHashValues(long[] hashValues) {
	if (hashValues.length != this.k) {
	    throw new IllegalArgumentException("Input array size incompatible with this number of permutations (k)");
	}
	this.underlyingSketch.setHashValues(hashValues);
    }

    public LazoSketch merge(LazoSketch b) {
	long[] aHV = this.getHashValues();
	long[] bHV = b.getHashValues();
	if (aHV.length != bHV.length) {
	    throw new IllegalArgumentException("Cannot merge differently-sized sketches");
	}
	long[] mergedHashValues = new long[aHV.length];
	for (int i = 0; i < aHV.length; i++) {
	    if (aHV[i] <= bHV[i]) {
		mergedHashValues[i] = aHV[i];
	    } else {
		mergedHashValues[i] = bHV[i];
	    }
	}
	// merge cardinality estimator as well
	ICardinality mergedCardinality = null;
	try {
	    mergedCardinality = this.getCardinalityEstimator().merge(b.getCardinalityEstimator());
	} catch (CardinalityMergeException e) {
	    e.printStackTrace();
	}
	LazoSketch merged = new LazoSketch(this.k, this.sketchType, mergedCardinality);
	merged.setHashValues(mergedHashValues);
	return merged;
    }

    public void setCardinality(long cardinality) {
        this.cardinality = cardinality;
    }

}
