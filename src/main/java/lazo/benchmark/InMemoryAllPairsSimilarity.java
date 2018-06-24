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

public class InMemoryAllPairsSimilarity {

    private CsvParser parser;
    private Map<Integer, String> hashIdToName;

    public InMemoryAllPairsSimilarity() {
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

    public Map<Integer, Set<String>> obtainColumns(File[] files) {
	Map<Integer, Set<String>> tableSets = new HashMap<>();

	for (int i = 0; i < files.length; i++) {
	    File file = files[i];
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
	    for (int k = 1; k < allRows.size(); k++) {
		String[] row = allRows.get(k);
		for (int j = 0; j < row.length; j++) {
		    // add value to correct column
		    tableSets.get(indexToHashId.get(j)).add(row[j]);
		}
	    }
	}

	return tableSets;
    }

    private float computeJS(Set<String> a, Set<String> b) {
	// more efficient union and ix
	float js = 0;
	Set<String> smaller = null;
	Set<String> larger = null;
	if (a.size() >= b.size()) {
	    smaller = b;
	    larger = a;
	} else {
	    smaller = a;
	    larger = b;
	}
	int hits = 0;
	for (String s : smaller) {
	    if (larger.contains(s)) {
		hits += 1;
	    }
	}
	int ix = hits;
	int union = (smaller.size() + larger.size()) - ix;
	js = ix / union;
	return js;
    }

    public List<Tuple<Integer, Integer>> computeAllPairs(File[] files, float threshold) {
	List<Tuple<Integer, Integer>> similarPairs = new ArrayList<>();
	// Read all columns first
	Map<Integer, Set<String>> allColumns = obtainColumns(files);
	// All-pairs with columns in-memory -> avoid repeated IO cost
	for (Entry<Integer, Set<String>> ea : allColumns.entrySet()) {
	    Set<String> a = ea.getValue();
	    int aKey = ea.getKey();
	    for (Entry<Integer, Set<String>> eb : allColumns.entrySet()) {
		int bKey = eb.getKey();
		Set<String> b = eb.getValue();
		float js = computeJS(a, b);
		// Set<String> union = Sets.union(a, b);
		// Set<String> intersection = Sets.intersection(a, b);
		// float js = intersection.size() / union.size();
		if (js >= threshold) {
		    similarPairs.add(new Tuple<Integer, Integer>(aKey, bKey));
		}
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

	InMemoryAllPairsSimilarity aps = new InMemoryAllPairsSimilarity();

	if (args.length < 3) {
	    System.out.println("Usage: <inputPath> <outputPath> <similarityThreshold>");
	}

	String inputPath = args[0];
	String outputPath = args[1];
	float similarityThreshold = Float.parseFloat(args[2]);

	File[] filesInPath = aps.enumerateFiles(inputPath);
	System.out.println("Found " + filesInPath.length + " files to process");
	long start = System.currentTimeMillis();
	List<Tuple<Integer, Integer>> output = aps.computeAllPairs(filesInPath, similarityThreshold);
	long end = System.currentTimeMillis();
	for (Tuple<Integer, Integer> pair : output) {
	    int xid = pair.x;
	    int yid = pair.y;
	    String xname = aps.hashIdToName.get(xid);
	    String yname = aps.hashIdToName.get(yid);
	    System.out.println(xname + " ~= " + yname);
	}
	System.out.println("Total time: " + (end - start));
	System.out.println("Total sim pairs: " + output.size());
    }
}
