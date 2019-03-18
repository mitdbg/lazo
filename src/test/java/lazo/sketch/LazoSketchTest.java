package lazo.sketch;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import lazo.sketch.LazoSketch;
import lazo.sketch.SketchType;

public class LazoSketchTest {

    @Test
    public void testCachingCardinalityWorksOnUpdates() {
	LazoSketch mh1 = new LazoSketch(64, SketchType.MINHASH);

	// add 500 values
	for (int i = 0; i < 500; i++) {
	    mh1.update(new Integer(i).toString());
	}

	// check cardinality
	long cardinality = mh1.getCardinality();

	assertTrue(cardinality > 450);

	// add 500 more values
	for (int i = 0; i < 500; i++) {
	    mh1.update(new Integer(i + 500).toString());
	}

	// check cardinality again
	cardinality = mh1.getCardinality();

	assertTrue(cardinality > 950);
    }

    @Test
    public void testMergeCardinalityDiffValues() {
	LazoSketch mh1 = new LazoSketch(64, SketchType.MINHASH);

	// add 5000 values
	for (int i = 0; i < 5000; i++) {
	    mh1.update(new Integer(i).toString());
	}

	LazoSketch mh2 = new LazoSketch(64, SketchType.MINHASH);

	// add 5000 more values
	for (int i = 0; i < 5000; i++) {
	    // add different values than above
	    mh1.update(new Integer(i + 6000).toString());
	}

	LazoSketch mh3 = mh1.merge(mh2);

	// check cardinality again
	long cardinality = mh3.getCardinality();

	assertTrue(cardinality > 4900 && cardinality < 10500);

    }

    @Test
    public void testMergeCardinalitySameValues() {
	LazoSketch mh1 = new LazoSketch(64, SketchType.MINHASH);

	// add 5000 values
	for (int i = 0; i < 5000; i++) {
	    mh1.update(new Integer(i).toString());
	}

	LazoSketch mh2 = new LazoSketch(64, SketchType.MINHASH);

	// add same 5000 values
	for (int i = 0; i < 5000; i++) {
	    // add different values than above
	    mh1.update(new Integer(i).toString());
	}

	LazoSketch mh3 = mh1.merge(mh2);

	// check cardinality again
	long cardinality = mh3.getCardinality();

	assertTrue(cardinality > 4900 && cardinality < 5100);

    }

}
