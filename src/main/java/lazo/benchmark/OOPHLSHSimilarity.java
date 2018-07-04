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

import lazo.index.MinHashLSH;
import lazo.sketch.MinHashOptimal;
import lazo.sketch.Sketch;

public class OOPHLSHSimilarity {

    // metrics
    private long io_time;
    private long index_time;
    private long query_time;
    private long post_time;

    private CsvParser parser;
    private Map<Integer, String> hashIdToName;

    private Map<String, File> nameToFile = new HashMap<>();

    public OOPHLSHSimilarity() {
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
	long s = System.currentTimeMillis();
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
	long e = System.currentTimeMillis();
	this.io_time += (e - s);
	return tableSets;
    }

    public Set<Pair<Integer, Integer>> computeAllPairs(File[] files, float threshold, int k) {
	Set<Pair<Integer, Integer>> similarPairs = new HashSet<>();
	MinHashLSH index = new MinHashLSH(threshold, k);
	// Create sketches and index
	Map<Integer, Sketch> idToSketch = new HashMap<>();
	for (int i = 0; i < files.length; i++) {
	    System.out.println("Processing: " + i + "/" + files.length);
	    System.out.println(files[i].getAbsolutePath());
	    // Read file
	    Map<Integer, Set<String>> table = obtainColumns(files[i]);
	    // Compute mh and insert to index
	    long s = System.currentTimeMillis();
	    for (Entry<Integer, Set<String>> e : table.entrySet()) {
		int id = e.getKey();
		MinHashOptimal mh = new MinHashOptimal(k);
		Set<String> values = e.getValue();
		boolean valid = false;
		int count = 0;
		while (count < 6) {
		    count++;
		    for (String value : values) {
			if (value != null) {
			    mh.update(value);
			    valid = true;
			}
		    }
		}
		if (valid) {
		    index.insert(id, mh);
		    idToSketch.put(id, mh);
		}
	    }
	    long e = System.currentTimeMillis();
	    this.index_time += (e - s);
	}
	// Query to retrieve pairs
	long s = System.currentTimeMillis();
	for (Entry<Integer, Sketch> e : idToSketch.entrySet()) {
	    int id = e.getKey();
	    MinHashOptimal mh = (MinHashOptimal) e.getValue();
	    Set<Object> candidates = index.query(mh);
	    for (Object o : candidates) {
		if (id != (int) o) {
		    similarPairs.add(new Pair<Integer, Integer>(id, (int) o));
		}
	    }
	}
	long e = System.currentTimeMillis();
	this.query_time += (e - s);
	return similarPairs;
    }

    private Map<Integer, Set<String>> getTable(String table, Map<String, Map<Integer, Set<String>>> cache) {
	if (cache.containsKey(table)) {
	    return cache.get(table);
	}
	File f = this.nameToFile.get(table);
	Map<Integer, Set<String>> cols = this.obtainColumns(f);
	cache.put(table, cols);
	return cols;
    }

    public Set<Pair<Integer, Integer>> postProcessing(Set<Pair<Integer, Integer>> candidates, float threshold) {
	Set<Pair<Integer, Integer>> verifiedPairs = new HashSet<>();
	Map<String, Map<Integer, Set<String>>> cache = new HashMap<>();
	for (Pair<Integer, Integer> candidate : candidates) {
	    String fullName1 = this.hashIdToName.get(candidate.x);
	    String tokens1[] = fullName1.split("->");
	    String tableName1 = tokens1[0];
	    Map<Integer, Set<String>> tableX = this.getTable(tableName1, cache);
	    String fullName2 = this.hashIdToName.get(candidate.y);
	    String tokens2[] = fullName2.split("->");
	    String tableName2 = tokens2[0];
	    Map<Integer, Set<String>> tableY = this.getTable(tableName2, cache);
	    float realJS = Utils.computeJS(tableX.get(candidate.x), tableY.get(candidate.y));
	    if (realJS >= threshold) {
		verifiedPairs.add(candidate);
	    }
	}
	return verifiedPairs;
    }

    public static void main(String args[]) {

	OOPHLSHSimilarity oss = new OOPHLSHSimilarity();

	if (args.length < 3) {
	    System.out.println("Usage: <inputPath> <outputPath> <similarityThreshold> <minhash-permutations>");
	}

	String inputPath = args[0];
	String outputPath = args[1];
	float similarityThreshold = Float.parseFloat(args[2]);
	int k = Integer.parseInt(args[3]);

	File[] filesInPath = oss.enumerateFiles(inputPath);
	for (File f : filesInPath) {
	    oss.nameToFile.put(f.getName(), f);
	}
	System.out.println("Found " + filesInPath.length + " files to process");
	long start = System.currentTimeMillis();
	Set<Pair<Integer, Integer>> output = oss.computeAllPairs(filesInPath, similarityThreshold, k);

	long s = System.currentTimeMillis();
	Set<Pair<Integer, Integer>> cleanOutput = oss.postProcessing(output, similarityThreshold);
	long e = System.currentTimeMillis();
	oss.post_time = (e - s);

	long end = System.currentTimeMillis();

	for (Pair<Integer, Integer> pair : cleanOutput) {
	    int xid = pair.x;
	    int yid = pair.y;
	    String xname = oss.hashIdToName.get(xid);
	    String yname = oss.hashIdToName.get(yid);
	    System.out.println(xname + " ~= " + yname);
	}
	System.out.println("Total time: " + (end - start));
	System.out.println("io time: " + (oss.io_time));
	System.out.println("index time: " + (oss.index_time));
	System.out.println("query time: " + (oss.query_time));
	System.out.println("post time: " + (oss.post_time));
	System.out.println("Total sim candidates: " + output.size());
	System.out.println("Total sim pairs: " + cleanOutput.size());

	// Write output in format x,y for all pairs
	File f = new File(outputPath);
	BufferedWriter bw = null;
	try {
	    bw = new BufferedWriter(new FileWriter(f));
	    for (Pair<Integer, Integer> pair : cleanOutput) {
		int xid = pair.x;
		int yid = pair.y;
		String line = xid + "," + yid + '\n';
		bw.write(line);
	    }
	    bw.flush();
	    bw.close();
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
	System.out.println("Results output to: " + outputPath);
    }

}
