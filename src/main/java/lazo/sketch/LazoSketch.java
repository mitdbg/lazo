package lazo.sketch;

import com.google.common.hash.HashFunction;

public class LazoSketch implements Sketch {

    private final int maxHash = (1 << 32) - 1;

    private int k;
    private int seed;
    private HashFunction hf;
    private long[] hashValues;

    private int cardinality;
    private Sketch underlyingSketch;

    public LazoSketch(int k, SketchType sketchType) {
	this.k = k;
	this.seed = 666;
	this.hf = SketchUtils.initializeHashFunction(HashFunctionType.MURMUR3, this.k);
	switch (sketchType) {
	case MINHASH:
	    this.underlyingSketch = new MinHash(k);
	case MINHASH_OPTIMAL:
	    this.underlyingSketch = new MinHashOptimal(k);
	default:
	    // FIXME
	}
	// Cardinality estimator here
	// TODO:
    }

    @Override
    public void update(String value) {
	// TODO: update cardinality estimation
	this.underlyingSketch.update(value);
    }

    public Sketch getSketch() {
	return underlyingSketch;
    }

    public int getCardinality() {
	return this.cardinality;
    }

    @Override
    public long[] getHashValues() {
	return underlyingSketch.getHashValues();
    }

}
