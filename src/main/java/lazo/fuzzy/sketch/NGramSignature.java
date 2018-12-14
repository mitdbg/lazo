package lazo.fuzzy.sketch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lazo.sketch.LazoSketch;
import lazo.sketch.Sketch;
import lazo.sketch.SketchType;

public class NGramSignature implements Signature {

    private final int ORIGINAL_STRING = 0;
    // number of ngram-groups
    private int n;
    // size of sketch
    private int k;
    private Map<Integer, LazoSketch> sketches;

    public int getK() {
	return k;
    }

    @Override
    public int getN() {
	return this.n;
    }

    public NGramSignature(int n, int k) {
	this.n = n;
	this.k = k;

	this.sketches = new HashMap<>();
	// Fill in sketches and cardinalities
	for (int ngramSize = 2; ngramSize < n + 1; ngramSize++) {
	    LazoSketch sketch = new LazoSketch(k, SketchType.MINHASH);
	    // Minimum is 2 and maximum is the actual 'n' provided
	    this.setSketch(ngramSize, sketch);
	}
	// We add an additional sketch for the entire string
	this.setSketch(ORIGINAL_STRING, new LazoSketch(k, SketchType.MINHASH));
    }

    public void setSketch(int ngramSize, LazoSketch sketch) {
	this.sketches.put(ngramSize, sketch);
    }

    public void update(String s) {
	// Expand the string into n-grams, send to sketch, and estimate
	// cardinality
	for (int ngramSize = 2; ngramSize < n + 1; ngramSize++) {
	    List<String> ngrams = StringUtils.extractNGram(s, ngramSize);
	    Sketch sketch = this.sketches.get(ngramSize);
	    for (String ngram : ngrams) {
		sketch.update(ngram);
	    }
	}
	this.sketches.get(ORIGINAL_STRING).update(s);
    }

    @Override
    public LazoSketch getSketch(int ngramSize) {
	return this.sketches.get(ngramSize);
    }

    @Override
    public long getCardinality(int ngramSize) {
	return sketches.get(ngramSize).getCardinality();
    }

}
