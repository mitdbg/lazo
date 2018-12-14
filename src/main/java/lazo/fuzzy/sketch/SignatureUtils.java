package lazo.fuzzy.sketch;

import lazo.sketch.LazoSketch;
import lazo.sketch.SketchUtils;

public class SignatureUtils {

    public static NGramSignature merge(NGramSignature a, NGramSignature b) {
	int ak = a.getK();
	int bk = b.getK();
	assert (ak == bk);

	int an = a.getN();
	int bn = b.getK();
	assert (an == bn);

	NGramSignature merged = new NGramSignature(ak, an);
	for (int ngramSize = 2; ngramSize < an + 1; ngramSize++) {
	    LazoSketch als = a.getSketch(ngramSize);
	    LazoSketch bls = b.getSketch(ngramSize);
	    if (als == null && bls != null) {
		merged.setSketch(ngramSize, bls);
	    } else if (bls == null && als != null) {
		merged.setSketch(ngramSize, als);
	    } else {
		LazoSketch mergeSketch = SketchUtils.merge(als, bls);
		merged.setSketch(ngramSize, mergeSketch);
	    }
	}
	return merged;
    }

    public static float calculateContainment(NGramSignature a, NGramSignature b) {
	int ak = a.getK();
	int bk = b.getK();
	assert (ak == bk);

	int an = a.getN();
	int bn = b.getK();
	assert (an == bn);

	float aggrMetric = 0;
	for (int ngramSize = 2; ngramSize < an + 1; ngramSize++) {
	    long aHV[] = a.getSketch(ngramSize).getHashValues();
	    long bHV[] = b.getSketch(ngramSize).getHashValues();
	    int matches = 0;
	    for (int i = 0; i < aHV.length; i++) {
		if (aHV[i] == bHV[i])
		    matches++;
	    }
	    float jsMetric = matches / aHV.length;
	    // apply lazo method -- FIXME: merging may need to merge
	    // cardinalities as well to do this right
	    // aggregated as in the original one

	}

	return 0f;
    }

}
