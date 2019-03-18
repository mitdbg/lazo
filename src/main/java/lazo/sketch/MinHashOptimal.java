package lazo.sketch;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.stream.LongStream;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

/**
 * This sketch is an immplementation of "Optimal Densification for fast and
 * accurate minwise hashing" Anshumali Shrivastava, ICML'17
 * 
 * @author Raul - raulcf@csail.mit.edu
 */

public class MinHashOptimal implements Sketch {

    private final long mersennePrime = (1 << 61) - 1;
    private final long empty = Long.MAX_VALUE;

    private final int random;
    private final int theHashValue;

    private int seed;
    private int k;
    private HashFunction hf;
    private int logPermutations;

    private long[] a;
    private long[] b;
    private long[] hashValues;

    private boolean densified = false;

    public MinHashOptimal(int k) {
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.seed = 666;
	this.hf = SketchUtils.initializeHashFunction(HashFunctionType.MURMUR3, this.seed);
	this.hashValues = SketchUtils.initializeHashValues(k, this.empty);
	this.initializePermutations();

	Random rnd = new Random(this.seed);
	this.random = rnd.nextInt(Integer.MAX_VALUE - 1) + 1;
	this.theHashValue = this.random % 2 == 0 ? this.random : this.random + 1;
	this.logPermutations = (int) (Math.log(this.k) / Math.log(2)) + 1;
    }

    public MinHashOptimal(int k, int seed, HashFunctionType hashFunctionType) {
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.seed = seed;
	this.hf = SketchUtils.initializeHashFunction(hashFunctionType, this.seed);
	this.hashValues = SketchUtils.initializeHashValues(k, this.empty);
	this.initializePermutations();

	Random rnd = new Random(this.seed);
	this.random = rnd.nextInt((int) Math.pow(2, 32)) + 1;
	this.theHashValue = this.random % 2 == 0 ? this.random : this.random + 1;

	this.logPermutations = (int) (Math.log(this.k) / Math.log(2));
    }

    public long getEmptyValue() {
	return this.empty;
    }

    private void initializePermutations() {
	// FIXME; factorize common to minhash
	Random gen = new Random(this.seed);
	LongStream as = gen.longs(k, 1, mersennePrime);
	LongStream bs = gen.longs(k, 0, mersennePrime);
	a = as.toArray();
	b = bs.toArray();
    }

    @Override
    public long[] getHashValues() {
	if (!densified) {
	    this.densify();
	}
	return this.hashValues;
    }

    @Override
    public void update(String value) {
	if (value == null) {
	    throw new IllegalArgumentException("Value cannot be null");
	}
	if (densified) {
	    throw new IllegalStateException("This MinHash has been previously densified; adding new"
		    + "values post-densification is not well defined. In particular, those values are not"
		    + "guaranteed to be reflected in the MinHash.");
	}
	HashCode hc = hf.hashString(value, Charset.defaultCharset());
	long hv = hc.asLong();

	int bucket = (int) hv % this.k;

	bucket = Math.abs(bucket);

	if (hv < this.hashValues[bucket]) {
	    this.hashValues[bucket] = hv;
	}
    }

    private int getRandomDoubleHash(int bucketId, int count) {
	int toh = ((bucketId + 1) << 10) + count;
	long newValue = ((int) (this.theHashValue * toh << 3) >> (32 - this.logPermutations));
	newValue = Math.abs(newValue);
	if (newValue == this.k)
	    newValue -= 1;
	return (int) newValue;
    }

    public void densify() {
	this.densified = true;
	for (int i = 0; i < this.hashValues.length; i++) {
	    if (this.hashValues[i] == this.empty) {
		int nonce = 0;
		while (this.hashValues[i] == this.empty) {
		    nonce++;
		    int index = this.getRandomDoubleHash(i, nonce);
		    this.hashValues[i] = this.hashValues[index];
		}
	    }
	}
    }

    public float jaccard(MinHashOptimal other) {
	return SketchUtils.jaccard(this.getHashValues(), other.getHashValues());
    }

    public void clear() {
	SketchUtils.initializeHashValues(this.k, this.empty);
    }

    @Override
    public void setHashValues(long[] hashValues) {
	if (hashValues.length != this.k) {
	    throw new IllegalArgumentException("Input array size incompatible with this number of permutations (k)");
	}
	this.hashValues = hashValues;
    }

}
