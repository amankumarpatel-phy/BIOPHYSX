package in.aman.biophysx;

import java.util.ArrayDeque;

/**
 * Non-clinical optical-signal quality helper for measurement guidance.
 * It intentionally reports signal quality, not health status or diagnosis.
 */
public final class PpgSignalEngine {
    public static final class Result {
        public final String label;
        public final double score;
        public final double amplitude;
        public final double variation;
        public final double stability;

        Result(String label, double score, double amplitude, double variation, double stability) {
            this.label = label;
            this.score = score;
            this.amplitude = amplitude;
            this.variation = variation;
            this.stability = stability;
        }
    }

    private final ArrayDeque<Double> values = new ArrayDeque<>();
    private final int maxSize;

    public PpgSignalEngine(int maxSize) {
        this.maxSize = Math.max(40, maxSize);
    }

    public void clear() { values.clear(); }

    public int size() { return values.size(); }

    public void add(double x) {
        if (!Double.isFinite(x)) return;
        values.addLast(x);
        while (values.size() > maxSize) values.removeFirst();
    }

    public Result evaluate(boolean fingerPresent) {
        if (!fingerPresent) return new Result("Place finger", 0.0, 0.0, 0.0, 0.0);
        if (values.size() < 20) return new Result("Stabilizing", 0.15, 0.0, 0.0, 0.0);

        double mean = 0.0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            mean += v;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        mean /= values.size();

        double variance = 0.0;
        Double previous = null;
        double diffSum = 0.0;
        int diffCount = 0;
        for (double v : values) {
            double d = v - mean;
            variance += d * d;
            if (previous != null) {
                diffSum += Math.abs(v - previous);
                diffCount++;
            }
            previous = v;
        }
        variance /= values.size();
        double sd = Math.sqrt(variance);
        double amplitude = Math.max(0.0, max - min);
        double variation = sd / Math.max(1.0, Math.abs(mean));

        // Smaller neighbour-to-neighbour movement relative to pulse amplitude
        // is treated as a more stable optical window for guidance purposes.
        double avgStep = diffCount == 0 ? 0.0 : diffSum / diffCount;
        double stability = 1.0 - Math.min(1.0, avgStep / Math.max(20.0, amplitude * 0.90));
        stability = Math.max(0.0, stability);

        double amplitudeScore = Math.min(1.0, amplitude / 2600.0);
        double variationScore = Math.min(1.0, variation / 0.07);
        double score = 0.55 * amplitudeScore + 0.25 * variationScore + 0.20 * stability;

        final String label;
        if (score < 0.22) label = "Weak signal";
        else if (score < 0.48) label = "Stabilizing";
        else if (score < 0.75) label = "Usable signal";
        else label = "Strong signal";

        return new Result(label, score, amplitude, variation, stability);
    }
}
