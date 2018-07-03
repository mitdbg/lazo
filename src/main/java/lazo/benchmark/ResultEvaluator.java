package lazo.benchmark;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ResultEvaluator {

    public static void main(String args[]) {

	ResultEvaluator re = new ResultEvaluator();

	String file1 = args[0]; // ground truth file
	String file2 = args[1]; // results

	Set<Pair<Integer, Integer>> pairs1 = new HashSet<>();
	Set<Pair<Integer, Integer>> pairs2 = new HashSet<>();
	try {
	    BufferedReader br1 = new BufferedReader(new FileReader(file1));
	    String line = null;
	    while ((line = br1.readLine()) != null) {
		String tokens[] = line.split(",");
		int x = Integer.parseInt(tokens[0]);
		int y = Integer.parseInt(tokens[1]);
		Pair<Integer, Integer> pair = new Pair<>(x, y);
		pairs1.add(pair);
	    }
	    BufferedReader br2 = new BufferedReader(new FileReader(file2));
	    line = null;
	    while ((line = br2.readLine()) != null) {
		String tokens[] = line.split(",");
		int x = Integer.parseInt(tokens[0]);
		int y = Integer.parseInt(tokens[1]);
		Pair<Integer, Integer> pair = new Pair<>(x, y);
		pairs2.add(pair);
	    }
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	double correct = 0;
	for (Pair<Integer, Integer> result : pairs2) {
	    if (pairs1.contains(result)) {
		correct += 1;
	    }
	}
	double precision = (double) (correct / pairs2.size());
	double recall = (double) (correct / pairs1.size());
	System.out.println("P/R: " + precision + "/" + recall);
    }
}
