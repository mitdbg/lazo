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
import java.util.Map;
import java.util.Set;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class ColInMemoryAllPairsSimilarity {

    // Metrics
    private long io_time;
    private long compare_time;

    private CsvParser parser;
    private Map<Integer, String> hashIdToName;

    public ColInMemoryAllPairsSimilarity() {
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

    private Set<String> readColumnFile(File f) {
	Set<String> strings = new HashSet<>();
	BufferedReader br;
	try {
	    br = new BufferedReader(new FileReader(f));
	    String line = null;
	    while ((line = br.readLine()) != null) {
		strings.add(line);
	    }
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return strings;
    }

    public Map<Integer, Set<String>> obtainColumns(File[] files) {
	long s = System.currentTimeMillis();
	Map<Integer, Set<String>> tableSets = new HashMap<>();
	for (int i = 0; i < files.length; i++) {
	    File file = files[i];
	    Set<String> thiscol = readColumnFile(file);
	    tableSets.put(i, thiscol);
	    this.hashIdToName.put(i, file.getName());
	}
	long e = System.currentTimeMillis();
	this.io_time = (e - s);
	return tableSets;
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
	// Read all columns first
	System.out.println("Reading columns...");
	Map<Integer, Set<String>> allColumns = obtainColumns(files);
	System.out.println("Reading columns...DONE");

	long s = System.currentTimeMillis();
	Object[] colIds = allColumns.keySet().toArray();
	// All-pairs with columns in-memory -> avoid repeated IO cost
	for (int i = 0; i < colIds.length; i++) {
	    int aKey = (Integer) colIds[i];
	    Set<String> a = allColumns.get(aKey);
	    if (!validSet(a)) {
		continue;
	    }
	    // i + 1 to not compare to itself
	    for (int j = (i + 1); j < colIds.length; j++) {
		int bKey = (Integer) colIds[j];
		Set<String> b = allColumns.get(bKey);
		float js_jcx_jcy[] = Utils.computeJSAndJC(a, b);
		float js = js_jcx_jcy[0];
		if (js >= threshold) {
		    similarPairs.add(new Pair<Integer, Integer>(aKey, bKey));
		}
	    }
	}
	long e = System.currentTimeMillis();
	this.compare_time = (e - s);
	return similarPairs;
    }

    public static void main(String args[]) {

	ColInMemoryAllPairsSimilarity aps = new ColInMemoryAllPairsSimilarity();

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
	System.out.println("IO time: " + aps.io_time);
	System.out.println("Comp time: " + aps.compare_time);
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
