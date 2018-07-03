package lazo.sketch;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.stream.LongStream;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

public class MinHash implements Sketch {

    private final long mersennePrime = (1 << 61) - 1;
    private final int maxHash = (1 << 32) - 1;
    private final int hashRange = (1 << 32);

    private int seed;
    private int k;
    private HashFunction hf;

    private long[] a;
    private long[] b;
    private long[] hashValues;

    public MinHash(int k) {
	this.k = k;
	this.seed = 666;
	this.hf = SketchUtils.initializeHashFunction(HashFunctionType.MURMUR3, this.k);
	this.hashValues = SketchUtils.initializeHashValues(k, Long.MAX_VALUE);
	this.initializePermutations();
    }

    public MinHash(int k, int seed, HashFunctionType hashFunctionType) {
	this.k = k;
	this.seed = seed;
	this.hf = SketchUtils.initializeHashFunction(hashFunctionType, this.seed);
	this.hashValues = SketchUtils.initializeHashValues(k, Long.MAX_VALUE);
	this.initializePermutations();
    }

    public MinHash(long[] hashValues, int seed, HashFunction hashFunction) {
	this.k = hashValues.length;
	this.seed = seed;
	this.hf = hashFunction;
	this.initializePermutations();
	this.hashValues = hashValues;
    }

    private void initializePermutations() {
	Random gen = new Random(this.seed);
	a = new long[k];
	b = new long[k];
	LongStream as = gen.longs(k, 1, mersennePrime);
	LongStream bs = gen.longs(k, 0, mersennePrime);
	a = as.toArray();
	b = bs.toArray();
    }

    @Override
    public long[] getHashValues() {
	return hashValues;
    }

    @Override
    public void update(String value) {
	HashCode hc = hf.hashString(value, Charset.defaultCharset());
	long hv = hc.asLong();

	for (int i = 0; i < k; i++) {
	    long kHashValue = Math.floorMod((a[i] * hv + b[i]), this.mersennePrime);
	    // long kHashValue = (a[i] * hv + b[i]) % this.mersennePrime;
	    hashValues[i] = hashValues[i] < kHashValue ? hashValues[i] : kHashValue;
	}
    }

    public float jaccard(MinHash other) {
	return SketchUtils.jaccard(this.getHashValues(), other.getHashValues());
    }

    public MinHash merge(MinHash other) {
	long[] otherHashValues = other.getHashValues();
	if (this.hashValues.length != otherHashValues.length) {
	    // TODO
	}
	long[] mergedHashValues = new long[k];
	for (int i = 0; i < k; i++) {
	    if (this.hashValues[i] < otherHashValues[i]) {
		mergedHashValues[i] = this.hashValues[i];
	    } else {
		mergedHashValues[i] = otherHashValues[i];
	    }
	}
	return new MinHash(mergedHashValues, this.k, this.hf);
    }

    public void clear() {
	SketchUtils.initializeHashValues(this.k, this.maxHash);
    }

}
