package lazo.index;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import lazo.sketch.MinHashOptimal;

public class MinHashOptimalTest {

    @Test
    public void testJaccardOfIdenticalMinHashIsOne() {
	MinHashOptimal mh1 = new MinHashOptimal(64);
	MinHashOptimal mh2 = new MinHashOptimal(64);

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
	MinHashOptimal mh1 = new MinHashOptimal(12);
	MinHashOptimal mh2 = new MinHashOptimal(12);

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
	MinHashOptimal mh1 = new MinHashOptimal(12);
	MinHashOptimal mh2 = new MinHashOptimal(12);

	// no value is the same
	long hv1[] = new long[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
	long hv2[] = new long[] { 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };

	mh1.setHashValues(hv1);
	mh2.setHashValues(hv2);

	// Check they are 0 similar
	assertTrue(mh1.jaccard(mh2) == 0.0f);
    }

    @Test
    public void testDensificationWorks() {
	MinHashOptimal mh1 = new MinHashOptimal(512);

	// Create a sketch with fewer values than K
	// i.e., most values are empty at this point
	for (int i = 0; i < 16; i++) {
	    mh1.update(new Integer(i).toString());
	}

	long hv1[] = mh1.getHashValues();

	for (int i = 0; i < hv1.length; i++) {
	    // no value should remain empty
	    assertTrue(hv1[i] != mh1.getEmptyValue());
	}
    }

}
