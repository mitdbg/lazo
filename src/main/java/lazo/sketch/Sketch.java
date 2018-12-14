package lazo.sketch;

public interface Sketch {

    public void update(String s);

    public long[] getHashValues();

    public void setHashValues(long[] hashValues);

}
