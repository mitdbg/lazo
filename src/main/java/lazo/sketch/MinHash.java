package lazo.sketch;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.stream.LongStream;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

public class MinHash implements Sketch {

    private final long mersennePrime = ((long) 1 << 61) - 1;

    private int seed;
    private int k;
    private HashFunction hf;

    private long[] a;
    private long[] b;
    private long[] hashValues;

    public MinHash(int k) {
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.seed = 666;
	this.hf = SketchUtils.initializeHashFunction(HashFunctionType.MURMUR3, this.seed);
	this.hashValues = SketchUtils.initializeHashValues(k, Long.MAX_VALUE);
	this.initializePermutations();
    }

    public MinHash(int k, int seed, HashFunctionType hashFunctionType) {
	if (k <= 0) {
	    throw new IllegalArgumentException("The number of permutations must be positive (> 0)");
	}
	this.k = k;
	this.seed = seed;
	this.hf = SketchUtils.initializeHashFunction(hashFunctionType, this.seed);
	this.hashValues = SketchUtils.initializeHashValues(k, Long.MAX_VALUE);
	this.initializePermutations();
    }

    public MinHash(long[] hashValues, int seed, HashFunction hashFunction) {
	if (hashValues.length <= 0) {
	    throw new IllegalArgumentException("The number of hashValues must be positive (> 0)");
	}
	this.k = hashValues.length;
	this.seed = seed;
	this.hf = hashFunction;
	this.initializePermutations();
	this.hashValues = hashValues;
    }

    private void initializePermutations() {
	Random gen = new Random(this.seed);
	LongStream as = gen.longs(this.k, 1, mersennePrime);
	LongStream bs = gen.longs(this.k, 0, mersennePrime);
	this.a = as.toArray();
	this.b = bs.toArray();
    }

    @Override
    public long[] getHashValues() {
	return hashValues;
    }

    @Override
    public void update(String value) {
	if (value == null) {
	    throw new IllegalArgumentException("Value cannot be null");
	}
	HashCode hc = hf.hashString(value, Charset.defaultCharset());
	long hv = hc.asLong();
	for (int i = 0; i < k; i++) {
	    long kHashValue = Math.floorMod((a[i] * hv + b[i]), this.mersennePrime);
	    hashValues[i] = hashValues[i] < kHashValue ? hashValues[i] : kHashValue;
	}
    }

    public float jaccard(MinHash other) {
	return SketchUtils.jaccard(this.getHashValues(), other.getHashValues());
    }

    public MinHash merge(MinHash other) {
	long[] otherHashValues = other.getHashValues();
	if (this.hashValues.length != otherHashValues.length) {
	    throw new IllegalArgumentException("Cannot merge differently-sized MinHash sketches");
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
	SketchUtils.initializeHashValues(this.k, Long.MAX_VALUE);
    }

    @Override
    public void setHashValues(long[] hashValues) {
	if (hashValues.length != this.k) {
	    throw new IllegalArgumentException("Input array size incompatible with this number of permutations (k)");
	}
	this.hashValues = hashValues;
    }

}
