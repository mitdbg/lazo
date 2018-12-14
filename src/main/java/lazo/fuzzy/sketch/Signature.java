package lazo.fuzzy.sketch;

import lazo.sketch.Sketch;

public interface Signature {

    public void update(String s);

    public Sketch getSketch(int ngramSize);

    public long getCardinality(int ngramSize);

    public int getN();

}
