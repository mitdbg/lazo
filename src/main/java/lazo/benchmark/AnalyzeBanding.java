package lazo.benchmark;

public class AnalyzeBanding {

    // integration precision
    private float IP = 0.001f;

    private float computeFalsePositiveProbability(float threshold, int bands, int rows) {
	float start = 0.0f;
	float end = threshold;
	float area = 0.0f;
	float x = start;
	while (x < end) {
	    area += 1 - Math.pow((1 - Math.pow(x + 0.5 * IP, rows)), bands) * IP;
	    x = x + IP;
	}
	return area;
    }

    private float computeFalseNegativeProbability(float threshold, int bands, int rows) {
	float start = threshold;
	float end = 1.0f;
	float area = 0.0f;
	float x = start;
	while (x < end) {
	    area += 1 - (1 - Math.pow((1 - Math.pow(x + 0.5 * IP, rows)), bands) * IP);
	    x = x + IP;
	}
	return area;
    }

    private BandRow computeOptimalParameters(float threshold, int k, float fp_rate, float fn_rate) {
	float minError = Float.MAX_VALUE;
	int optimalBands = 0;
	int optimalRows = 0;

	int maximumRows = 0;
	for (int band = 1; band < k + 1; band++) {
	    maximumRows = (int) (k / band);
	    for (int rows = 1; rows < maximumRows + 1; rows++) {
		float falsePositives = this.computeFalsePositiveProbability(threshold, band, rows);
		float falseNegatives = this.computeFalseNegativeProbability(threshold, band, rows);
		float error = fp_rate * falsePositives + fn_rate * falseNegatives;
		if (error < minError) {
		    minError = error;
		    optimalBands = band;
		    optimalRows = rows;
		}
	    }
	}
	return new BandRow(optimalBands, optimalRows, minError);
    }

    class BandRow {
	public int bands;
	public int rows;
	public float error;

	public BandRow(int bands, int rows, float error) {
	    this.bands = bands;
	    this.rows = rows;
	    this.error = error;
	}
    }

    public static void main(String args[]) {
	AnalyzeBanding ab = new AnalyzeBanding();

	float thresholds[] = new float[] { 0.0f, 0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f, 0.35f, 0.4f, 0.45f, 0.5f, 0.55f,
		0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f };
	// int ks[] = new int[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 };
	int ks[] = new int[] { 64 };

	for (float threshold : thresholds) {
	    for (int k : ks) {
		BandRow br = ab.computeOptimalParameters(threshold, k, 0.5f, 0.5f);
		int bands = br.bands;
		int rows = br.rows;
		float error = br.error;
		System.out.println(threshold + "." + k + " b/r: " + bands + "/" + rows + " error: " + error);
	    }
	}

    }
}
