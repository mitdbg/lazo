package lazo.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

import lazo.index.LazoIndex;
import lazo.index.LazoIndexBase;
import lazo.sketch.LazoSketch;
import lazo.sketch.SketchType;

public class IndexBenchmark {

    private static long mersennePrime = ((long) 1 << 61) - 1;
    private static Random gen = new Random(111);

    public static List<LazoSketch> getSketches(int number, int k) {
	// create bunch of random sketches -- probably not many collisions
	List<LazoSketch> sketches = new ArrayList<>();
	for (int i = 0; i < number; i++) {
	    long[] a = new long[k];
	    LongStream as = gen.longs(k, 1, mersennePrime);
	    a = as.toArray();
	    List<String> strings = new ArrayList<>();
	    for (Long l : a) {
		strings.add(l.toString());
	    }
	    LazoSketch ls = new LazoSketch(k, SketchType.MINHASH);
	    for (String s : strings) {
		ls.update(s);
	    }
	    sketches.add(ls);
	}
	return sketches;
    }

    public static void main(String args[]) {

	int k = 512;

	LazoIndex index = new LazoIndex(k);
	LazoIndexBase index2 = new LazoIndexBase(k);

	List<LazoSketch> sketches = getSketches(1000, k);

	long s = System.currentTimeMillis();
	// insertion
	for (int i = 0; i < sketches.size(); i++) {
	    index2.insert(i, sketches.get(i));
	}
	long e = System.currentTimeMillis();
	// querying
	float js_threshold = 0.5f;
	float jcx_threshold = 0;
	for (int i = 0; i < sketches.size(); i++) {
	    index2.query(sketches.get(i), js_threshold, jcx_threshold);
	}
	long e2 = System.currentTimeMillis();

	long indexing = (e - s);
	long querying = (e2 - e);

	s = System.currentTimeMillis();
	// insertion
	for (int i = 0; i < sketches.size(); i++) {
	    index.insert(i, sketches.get(i));
	}
	e = System.currentTimeMillis();
	// querying
	for (int i = 0; i < sketches.size(); i++) {
	    index.query(sketches.get(i), js_threshold, jcx_threshold);
	}
	e2 = System.currentTimeMillis();

	long indexing2 = (e - s);
	long querying2 = (e2 - e);

	System.out.println("Time to index base: " + indexing);
	System.out.println("Time to query base: " + querying);
	System.out.println("Time to index lazo: " + indexing2);
	System.out.println("Time to query lazo: " + querying2);
    }
}
