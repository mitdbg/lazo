package lazo.benchmark;

import java.util.HashSet;
import java.util.Set;

public class Pair<X, Y> {

    public final X x;
    public final Y y;

    public Pair(X x, Y y) {
	this.x = x;
	this.y = y;
    }

    @Override
    public int hashCode() {
	int a = 0;
	int b = 0;
	// Sort, a is the smaller, b is the larger
	if ((int) this.x < (int) this.y) {
	    a = (int) this.x;
	    b = (int) this.y;
	} else {
	    a = (int) this.y;
	    b = (int) this.x;
	}
	// Cantor pairing function
	// https://en.wikipedia.org/wiki/Pairing_function
	int ab = a + b;
	int ab_one = a + b + 1;
	int halved = (ab * ab_one) / 2;
	return halved + b;
    }

    @Override
    public boolean equals(Object other) {
	return other.hashCode() == this.hashCode();
    }

    public static void main(String args[]) {
	Set<Pair<Integer, Integer>> set = new HashSet<>();
	Pair<Integer, Integer> a = new Pair<>(1, 2);
	Pair<Integer, Integer> b = new Pair<>(2, 1);
	set.add(a);
	System.out.println(set.contains(b));
	set.add(b);
	System.out.println(set.size());
    }
}
