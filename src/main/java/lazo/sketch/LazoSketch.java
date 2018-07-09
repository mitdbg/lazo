package lazo.sketch;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;
import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.google.common.hash.HashFunction;

public class LazoSketch implements Sketch {

    private int k;
    private int seed;
    private HashFunction hf;

    private long cardinality = -1;
    private Sketch underlyingSketch;
    private ICardinality ic;

    public LazoSketch(int k, SketchType sketchType, HashFunctionType hft) {
	this.k = k;
	this.seed = 666;
	this.hf = SketchUtils.initializeHashFunction(hft, this.k);
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

    public LazoSketch(int k, SketchType sketchType) {
	this.k = k;
	this.seed = 666;
	this.hf = SketchUtils.initializeHashFunction(HashFunctionType.MURMUR3, this.k);
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

    @Override
    public void update(String value) {
	ic.offer(value);
	this.underlyingSketch.update(value);
    }

    public Sketch getSketch() {
	return underlyingSketch;
    }

    public long getCardinality() {
	if (this.cardinality == -1) {
	    this.cardinality = ic.cardinality();
	}
	return this.cardinality;
    }

    @Override
    public long[] getHashValues() {
	return underlyingSketch.getHashValues();
    }

}
