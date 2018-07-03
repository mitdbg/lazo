package lazo.benchmark;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class AllPairsSimilarity {

    private CsvParser parser;
    private Map<Integer, String> hashIdToName;

    public AllPairsSimilarity() {
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

    private boolean validSet(Set<String> set) {
	boolean valid = false;
	for (String s : set) {
	    if (s != null) {
		valid = true;
		break;
	    }
	}
	return valid;
    }

    public Set<Pair<Integer, Integer>> computeAllPairs(File[] files, float threshold) {
	Set<Pair<Integer, Integer>> similarPairs = new HashSet<>();
	// Set<Pair<Integer, Integer>> seenPairs = new HashSet<>();
	for (int i = 0; i < files.length; i++) {
	    System.out.println("Processing: " + i + "/" + files.length);
	    System.out.println(files[i].getAbsolutePath());
	    // Read file
	    Map<Integer, Set<String>> pivotTable = obtainColumns(files[i]);
	    for (int j = i; j < files.length; j++) { // start from the same
		Map<Integer, Set<String>> table = obtainColumns(files[j]);
		// Compare columns
		for (Entry<Integer, Set<String>> entry : pivotTable.entrySet()) {
		    int pivotKey = entry.getKey();
		    Set<String> a = entry.getValue();
		    if (!validSet(a))
			continue; // discard all-null value columns
		    for (Entry<Integer, Set<String>> entryB : table.entrySet()) {
			int key = entryB.getKey();
			if (pivotKey == key) {
			    continue; // avoid same keys within table
			}
			Set<String> b = entryB.getValue();
			float js = computeJS(a, b);

			if (js >= threshold) {
			    Pair<Integer, Integer> newPair1 = new Pair<Integer, Integer>(pivotKey, key);
			    similarPairs.add(newPair1);
			}
		    }
		}
	    }
	}
	return similarPairs;
    }

    public static void main(String args[]) {

	AllPairsSimilarity aps = new AllPairsSimilarity();

	if (args.length < 3) {
	    System.out.println("Usage: <inputPath> <outputPath> <similarityThreshold>");
	}

	String inputPath = args[0];
	String outputPath = args[1];
	float similarityThreshold = Float.parseFloat(args[2]);

	File[] filesInPath = aps.enumerateFiles(inputPath);
	System.out.println("Found " + filesInPath.length + " files to process");
	long start = System.currentTimeMillis();
	Set<Pair<Integer, Integer>> output = aps.computeAllPairs(filesInPath, similarityThreshold);
	long end = System.currentTimeMillis();
	for (Pair<Integer, Integer> pair : output) {
	    int xid = pair.x;
	    int yid = pair.y;
	    String xname = aps.hashIdToName.get(xid);
	    String yname = aps.hashIdToName.get(yid);
	    System.out.println(xname + " ~= " + yname);
	}
	System.out.println("Total time: " + (end - start));
	System.out.println("Total sim pairs: " + output.size());

	// Write output in format x,y for all pairs
	File f = new File(outputPath);
	BufferedWriter bw = null;
	try {
	    bw = new BufferedWriter(new FileWriter(f));
	    for (Pair<Integer, Integer> pair : output) {
		int xid = pair.x;
		int yid = pair.y;
		String line = xid + "," + yid + '\n';
		bw.write(line);
	    }
	    bw.flush();
	    bw.close();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	System.out.println("Results output to: " + outputPath);
    }
}
