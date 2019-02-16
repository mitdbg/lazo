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
	this.k = k;
	this.seed = 666;
	this.hf = SketchUtils.initializeHashFunction(HashFunctionType.MURMUR3, this.k);
	this.hashValues = SketchUtils.initializeHashValues(k, this.empty);
	this.initializePermutations();

	Random rnd = new Random(this.seed);
	this.random = rnd.nextInt(Integer.MAX_VALUE - 1) + 1;
	this.theHashValue = this.random % 2 == 0 ? this.random : this.random + 1;

	this.logPermutations = (int) (Math.log(this.k) / Math.log(2)) + 1;
    }

    public MinHashOptimal(int k, int seed, HashFunctionType hashFunctionType) {
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

    private void initializePermutations() {
	// FIXME; factorize common to minhash
	Random gen = new Random(this.seed);
	a = new long[k];
	b = new long[k];
	LongStream as = gen.longs(k, 1, mersennePrime);
	LongStream bs = gen.longs(k, 0, mersennePrime);
	a = as.toArray();
	b = bs.toArray();
    }

    boolean used = false;

    @Override
    public void update(String value) {
	used = true;
	HashCode hc = hf.hashString(value, Charset.defaultCharset());
	long hv = hc.asLong();

	int bucket = (int) hv % this.k;

	bucket = Math.abs(bucket);

	if (hv < this.hashValues[bucket]) {
	    this.hashValues[bucket] = hv;
	}
    }

    @Override
    public long[] getHashValues() {
	if (!densified) {
	    this.densify();
	}
	return this.hashValues;
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

    public float jaccard(MinHash other) {
	return SketchUtils.jaccard(this.getHashValues(), other.getHashValues());
    }

    public void clear() {
	SketchUtils.initializeHashValues(this.k, this.empty);
    }

    @Override
    public void setHashValues(long[] hashValues) {
	this.hashValues = hashValues;
    }

}
