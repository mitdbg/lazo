package lazo.sketch;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.stream.LongStream;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

public class MinHashOptimal implements Sketch {

    private final long mersennePrime = (1 << 61) - 1;
    private final int maxHash = (1 << 32) - 1;
    private final int hashRange = (1 << 32);
    private final int empty = maxHash + 1;

    private final int random;
    private final long theHashValue;

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
	this.hashValues = SketchUtils.initializeHashValues(k, this.maxHash);
	this.initializePermutations();

	Random rnd = new Random(this.seed);
	this.random = rnd.nextInt((int) Math.pow(2, 32)) + 1;
	this.theHashValue = this.random % 2 == 0 ? this.random : this.random + 1;

	this.logPermutations = (int) Math.log(this.k);
    }

    public MinHashOptimal(int k, int seed, HashFunctionType hashFunctionType) {
	this.k = k;
	this.seed = seed;
	this.hf = SketchUtils.initializeHashFunction(hashFunctionType, this.seed);
	this.hashValues = SketchUtils.initializeHashValues(k, this.maxHash);
	this.initializePermutations();

	Random rnd = new Random(this.seed);
	this.random = rnd.nextInt((int) Math.pow(2, 32)) + 1;
	this.theHashValue = this.random % 2 == 0 ? this.random : this.random + 1;

	this.logPermutations = (int) Math.log(this.k);

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

    @Override
    public void update(String value) {
	HashCode hc = hf.hashString(value, Charset.defaultCharset());
	long hv = hc.asLong();

	int bucket = (int) hv % this.k;

	// FIXME: this looks wrong
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
	int newValue = (int) (this.theHashValue * toh << 3) >> (32 - this.logPermutations);
	return newValue;
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
	SketchUtils.initializeHashValues(this.k, this.maxHash);
    }

}
