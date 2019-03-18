package lazo.sketch;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import lazo.sketch.MinHash;

public class MinHashTest {

    @Test
    public void testHashPermutationsAreDeterministic() {
	MinHash mh1 = new MinHash(64);
	MinHash mh2 = new MinHash(64);

	// Create two identical minhash sketches
	for (int i = 0; i < 50; i++) {
	    mh1.update(new Integer(i).toString());
	    mh2.update(new Integer(i).toString());
	}

	// Check they are indeed the same
	assertTrue(Arrays.equals(mh1.getHashValues(), mh2.getHashValues()));
    }

    @Test
    public void testJaccardOfIdenticalMinHashIsOne() {
	MinHash mh1 = new MinHash(64);
	MinHash mh2 = new MinHash(64);

	// Create two identical minhash sketches
	for (int i = 0; i < 50; i++) {
	    mh1.update(new Integer(i).toString());
	    mh2.update(new Integer(i).toString());
	}

	// Check they are indeed the same
	assertTrue(mh1.jaccard(mh2) == 1);
    }

    @Test
    public void testJaccardOfHalfSimilarMinHashIsHalf() {
	MinHash mh1 = new MinHash(12);
	MinHash mh2 = new MinHash(12);

	// half of the values are the same
	long hv1[] = new long[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
	long hv2[] = new long[] { 22, 33, 44, 55, 66, 77, 6, 7, 8, 9, 10, 11 };

	mh1.setHashValues(hv1);
	mh2.setHashValues(hv2);

	// Check they are half similar
	assertTrue(mh1.jaccard(mh2) == 0.5f);
    }

    @Test
    public void testJaccardOfDifferentMinHashIsZero() {
	MinHash mh1 = new MinHash(12);
	MinHash mh2 = new MinHash(12);

	// no value is the same
	long hv1[] = new long[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
	long hv2[] = new long[] { 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };

	mh1.setHashValues(hv1);
	mh2.setHashValues(hv2);

	// Check they are 0 similar
	assertTrue(mh1.jaccard(mh2) == 0.0f);
    }

    @Test
    public void testMerge() {
	MinHash mh1 = new MinHash(12);
	MinHash mh2 = new MinHash(12);

	// Each MinHash contains exactly half of the min values
	long hv1[] = new long[] { 0, 1, 2, 3, 4, 5, 18, 19, 20, 21, 22, 23 };
	long hv2[] = new long[] { 12, 13, 14, 15, 16, 17, 6, 7, 8, 9, 10, 11 };

	mh1.setHashValues(hv1);
	mh2.setHashValues(hv2);

	// Merge both minhashes
	MinHash mh3 = mh1.merge(mh2);

	// Check they are half similar
	assertTrue(mh3.jaccard(mh1) == 0.5f);
    }

}
