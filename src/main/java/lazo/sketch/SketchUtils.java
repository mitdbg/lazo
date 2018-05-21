package lazo.sketch;

import java.util.Arrays;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class SketchUtils {

    public static float jaccard(long[] one, long[] other) {
	if (one.length != other.length) {
	    // TODO
	}
	int k = one.length;
	int hits = 0;
	for (int i = 0; i < k; i++) {
	    if (one[i] == other[i])
		hits++;
	}
	float js = hits / k;
	return js;
    }

    public static HashFunction initializeHashFunction(HashFunctionType hft, int seed) {
	HashFunction hf = null;
	switch (hft) {
	case MURMUR3:
	    hf = Hashing.murmur3_32(seed);
	case SHA1:
	    hf = Hashing.sha256();
	default:
	    // TODO error
	    break;
	}
	return hf;
    }

    public static long[] initializeHashValues(int k, int fillValue) {
	long[] hashValues = new long[k];
	Arrays.fill(hashValues, fillValue);
	return hashValues;
    }
}
