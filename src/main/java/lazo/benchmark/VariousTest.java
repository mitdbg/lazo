package lazo.benchmark;

import lazo.index.LazoIndex;
import lazo.index.LazoIndexBase;

public class VariousTest {

    public static void main(String args[]) {

	int[] ks = new int[] { 64, 128, 256, 512 };
	float[] ds = new float[] { 0.05f, 0.1f };

	for (int i = 0; i < ks.length; i++) {
	    int k = ks[i];
	    for (int j = 0; j < ds.length; j++) {
		float d = ds[j];

		LazoIndexBase lib = new LazoIndexBase(k, d);
		LazoIndex li = new LazoIndex(k, d);

		int libnum = lib.__getNumHashTables();
		int linum = li.__getNumHashTables();
		System.out.println("K: " + k + " D: " + d);
		System.out.println("Base: " + libnum);
		System.out.println("Lazo: " + linum);

	    }
	}

    }
}
