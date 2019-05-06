package lazo.benchmark;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JCResultEvaluator {

    public static void main(String args[]) {

	JCResultEvaluator re = new JCResultEvaluator();

	String file1 = args[0]; // ground truth file
	String file2 = args[1]; // results

	Map<Integer, List<Integer>> pairs1 = new HashMap<>();
	Map<Integer, List<Integer>> pairs2 = new HashMap<>();
	int pairs1Size = 0;
	int pairs2Size = 0;

	// Set<ContainmentPair> pairs1 = new HashSet<>();
	// Set<ContainmentPair> pairs2 = new HashSet<>();
	try {
	    BufferedReader br1 = new BufferedReader(new FileReader(file1));
	    String line = null;
	    while ((line = br1.readLine()) != null) {
		String tokens[] = line.split(",");
		int x = Integer.parseInt(tokens[0]);
		int y = Integer.parseInt(tokens[1]);

		if (!pairs1.containsKey(x))
		    pairs1.put(x, new ArrayList<>());
		pairs1.get(x).add(y);
		pairs1Size++;

		// ContainmentPair pair = new ContainmentPair(x, y);
		// pairs1.add(pair);
	    }
	    BufferedReader br2 = new BufferedReader(new FileReader(file2));
	    line = null;
	    while ((line = br2.readLine()) != null) {
		String tokens[] = line.split(",");
		int x = Integer.parseInt(tokens[0]);
		int y = Integer.parseInt(tokens[1]);

		if (!pairs2.containsKey(x))
		    pairs2.put(x, new ArrayList<>());
		pairs2.get(x).add(y);
		pairs2Size++;

		// ContainmentPair pair = new ContainmentPair(x, y);
		// pairs2.add(pair);
	    }
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	double correct = 0;

	for (Entry<Integer, List<Integer>> entry : pairs2.entrySet()) {
	    int key = entry.getKey();
	    if (pairs1.containsKey(key)) {
		List<Integer> values = pairs1.get(key);
		for (int candidate : entry.getValue()) {
		    if (values.contains(candidate)) {
			correct += 1;
		    }
		}
	    }
	}

	// for (ContainmentPair result : pairs2) {
	// if (pairs1.contains(result)) {
	// correct += 1;
	// }
	// }

	double precision = (double) (correct / pairs2Size);
	double recall = (double) (correct / pairs1Size);
	System.out.println("Ground truth has: " + pairs1Size);
	System.out.println("P/R: " + precision + "/" + recall);
    }
}
