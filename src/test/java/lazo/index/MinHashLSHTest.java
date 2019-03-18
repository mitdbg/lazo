package lazo.index;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import lazo.sketch.MinHash;

public class MinHashLSHTest {

    @Test
    public void testInsertionsAllEqual() {
	MinHashLSH index = new MinHashLSH(0.8f, 64);

	MinHash q_mh = null;
	int num_mhs = 10;

	for (int i = 0; i < num_mhs; i++) {
	    MinHash mh = new MinHash(64);
	    q_mh = mh; // just store one
	    for (int j = 0; j < 50; j++) {
		mh.update(new Integer(j).toString());
	    }
	    index.insert(new Integer(i), mh);
	}

	Set<Object> candidates = index.query(q_mh);

	// find all
	assertTrue(candidates.size() == num_mhs);
    }

    @Test
    public void testInsertionsAllDifferent() {
	MinHashLSH index = new MinHashLSH(0.8f, 64);

	MinHash q_mh = null;
	int num_mhs = 10;
	int k = 0; // offset to find different values

	for (int i = 0; i < num_mhs; i++) {
	    MinHash mh = new MinHash(64);
	    q_mh = mh; // just store one
	    for (int j = k; j < k + 50; j++) {
		mh.update(new Integer(j).toString());
	    }
	    k += 50;
	    index.insert(new Integer(i), mh);
	}

	Set<Object> candidates = index.query(q_mh);

	// only find myself
	assertTrue(candidates.size() == 1);
    }

}
