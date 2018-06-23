package lazo.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import lazo.index.MinHashLSH;
import lazo.sketch.MinHash;

public class MinHashLSHSimilarity {

    private CsvParser parser;
    private Map<Integer, String> hashIdToName;

    public MinHashLSHSimilarity() {
	// csv parser
	CsvParserSettings settings = new CsvParserSettings();
	settings.getFormat().setLineSeparator("\n");
	this.parser = new CsvParser(settings);

	// id, names, etc
	this.hashIdToName = new HashMap<>();

    }

    private File[] enumerateFiles(String path) {
	File folder = new File(path);
	File[] files = folder.listFiles();
	return files;
    }

    private int hashName(String fileName, String columnName) {
	return (fileName + columnName).hashCode();
    }

    public Reader getReader(File file) throws FileNotFoundException {
	FileReader fr = new FileReader(file);
	BufferedReader br = new BufferedReader(fr);
	return br;
    }

    public Map<Integer, Set<String>> obtainColumns(File file) {
	Map<Integer, Set<String>> tableSets = new HashMap<>();
	Map<Integer, Integer> indexToHashId = new HashMap<>();

	List<String[]> allRows = null;
	try {
	    allRows = parser.parseAll(getReader(file));
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
	String[] header = allRows.get(0);
	int idx = 0;
	for (String columnName : header) {
	    int id = hashName(file.getName(), columnName);
	    tableSets.put(id, new HashSet<>());
	    indexToHashId.put(idx, id);
	    this.hashIdToName.put(id, file.getName() + "->" + columnName);
	    idx++;
	}
	for (int i = 1; i < allRows.size(); i++) {
	    String[] row = allRows.get(i);
	    for (int j = 0; j < row.length; j++) {
		// add value to correct column
		tableSets.get(indexToHashId.get(j)).add(row[j]);
	    }
	}

	return tableSets;
    }

    public List<Tuple<Integer, Integer>> computeAllPairs(File[] files, float threshold, int k) {
	List<Tuple<Integer, Integer>> similarPairs = new ArrayList<>();
	MinHashLSH index = new MinHashLSH(threshold, k);
	for (int i = 0; i < files.length; i++) {
	    System.out.println("Processing: " + i + "/" + files.length);
	    System.out.println(files[i].getAbsolutePath());
	    // Read file
	    Map<Integer, Set<String>> table = obtainColumns(files[i]);
	    // Compute mh and insert to index
	    for (Entry<Integer, Set<String>> e : table.entrySet()) {
		int id = e.getKey();
		MinHash mh = new MinHash(k);
		Set<String> values = e.getValue();
		for (String value : values) {
		    if (value != null) {
			mh.update(value);
		    }
		}
		index.insert(id, mh);
	    }
	}
	return similarPairs;
    }

    public class Tuple<X, Y> {
	public final X x;
	public final Y y;

	public Tuple(X x, Y y) {
	    this.x = x;
	    this.y = y;
	}
    }

    public static void main(String args[]) {

	MinHashLSHSimilarity mls = new MinHashLSHSimilarity();

	if (args.length < 3) {
	    System.out.println("Usage: <inputPath> <outputPath> <similarityThreshold> <minhash-permutations>");
	}

	String inputPath = args[0];
	String outputPath = args[1];
	float similarityThreshold = Float.parseFloat(args[2]);
	int k = Integer.parseInt(args[3]);

	File[] filesInPath = mls.enumerateFiles(inputPath);
	System.out.println("Found " + filesInPath.length + " files to process");
	long start = System.currentTimeMillis();
	List<Tuple<Integer, Integer>> output = mls.computeAllPairs(filesInPath, similarityThreshold, k);
	long end = System.currentTimeMillis();
	for (Tuple<Integer, Integer> pair : output) {
	    int xid = pair.x;
	    int yid = pair.y;
	    String xname = mls.hashIdToName.get(xid);
	    String yname = mls.hashIdToName.get(yid);
	    System.out.println(xname + " ~= " + yname);
	}
	System.out.println("Total time: " + (end - start));
	System.out.println("Total sim pairs: " + output.size());
    }

}
