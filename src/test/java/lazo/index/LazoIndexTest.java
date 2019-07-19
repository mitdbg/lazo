package lazo.index;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import lazo.sketch.LazoSketch;

public class LazoIndexTest {

    @Test
    public void testGCD() {
	LazoIndex li = new LazoIndex(64);

	Integer numbers[] = new Integer[] { 2, 4, 6, 8, 12, 24, 46, 64, 66, 88 };
	int gcd = li.findGCDOf(numbers);
	assertTrue(gcd == 2);

	numbers = new Integer[] { 3, 6, 9, 12, 15, 18, 21, 24, 27, 30 };
	gcd = li.findGCDOf(numbers);
	assertTrue(gcd == 3);
    }
    
    @Test
    public void testUpdate() {
        LazoIndex li = new LazoIndex(64);
        
        // Adding values 0 to 10 to index
        LazoSketch indexSketch = new LazoSketch(64);
        for (int i = 0; i < 11; i++) {
            indexSketch.update(new Integer(i).toString());
        }
        li.insert("test", indexSketch);
        
        // Querying values 11 to 20
        LazoSketch querySketch = new LazoSketch(64);
        for (int i = 11; i < 21; i++) {
            querySketch.update(new Integer(i).toString());
        }
        Set<LazoIndex.LazoCandidate> candidates =
                li.queryContainment(querySketch, 0.0f);
        assertTrue(candidates.size() == 0);
        
        // Querying values 0 to 10
        querySketch = new LazoSketch(64);
        for (int i = 0; i < 11; i++) {
            querySketch.update(new Integer(i).toString());
        }
        candidates = li.queryContainment(querySketch, 0.0f);
        assertTrue(candidates.size() == 1);
        
        // Updating 'test' to only have values 11 to 20
        indexSketch = new LazoSketch(64);
        for (int i = 11; i < 21; i++) {
            indexSketch.update(new Integer(i).toString());
        }
        li.update("test", indexSketch);
        
        // Querying values 11 to 20
        querySketch = new LazoSketch(64);
        for (int i = 11; i < 21; i++) {
            querySketch.update(new Integer(i).toString());
        }
        candidates = li.queryContainment(querySketch, 0.0f);
        assertTrue(candidates.size() == 1);
        
        // Querying values 0 to 10
        querySketch = new LazoSketch(64);
        for (int i = 0; i < 11; i++) {
            querySketch.update(new Integer(i).toString());
        }
        candidates = li.queryContainment(querySketch, 0.0f);
        assertTrue(candidates.size() == 0);
    }

    // TODO:

}
