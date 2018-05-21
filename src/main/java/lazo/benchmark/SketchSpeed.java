package lazo.benchmark;

import lazo.sketch.MinHash;
import lazo.sketch.MinHashOptimal;

public class SketchSpeed {

    public static void totalSpeedRandomData(int setCardinality, int numSketches, int k) {

	// Create a dummy set
	String[] sampleSet = new String[setCardinality];
	for (int i = 0; i < setCardinality; i++) {
	    sampleSet[i] = new Integer(i).toString();
	}

	// Measure time to create numSketches with minhash
	long mh_start = System.currentTimeMillis();
	for (int i = 0; i < numSketches; i++) {
	    MinHash mh = new MinHash(k);
	    for (String s : sampleSet) {
		mh.update(s);
	    }
	}
	long mh_end = System.currentTimeMillis();

	// Measure time to create numSketches with minhashOPH
	long oph_start = System.currentTimeMillis();
	for (int i = 0; i < numSketches; i++) {
	    MinHashOptimal mh = new MinHashOptimal(k);
	    for (String s : sampleSet) {
		mh.update(s);
	    }
	    mh.densify();
	}
	long oph_end = System.currentTimeMillis();

	System.out.println("Total MinHash time: " + (mh_end - mh_start));
	System.out.println("Total MinHashOptimal time: " + (oph_end - oph_start));

    }

    public static void main(String args[]) {

	int setCardinality = 100000;
	int numSketches = 1000;
	int k = 512;

	totalSpeedRandomData(setCardinality, numSketches, k);

    }
}
