package lazo.index;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

    // TODO:

}
