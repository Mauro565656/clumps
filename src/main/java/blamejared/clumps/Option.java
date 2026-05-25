package blamejared.clumps;

import java.util.ArrayList;
import java.util.List;

public abstract class Option<T> {
    protected final String name;
    protected T value;

    public Option(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
    public abstract String getDisplayValue();
    public abstract void next();

    public static class BoolOption extends Option<Boolean> {
        public BoolOption(String name, boolean defaultValue) { super(name, defaultValue); }
        @Override
        public void setValue(Boolean value) { this.value = value != null && value; }
        public void next() { value = !value; }
        public String getDisplayValue() { return value ? "ON" : "OFF"; }
    }

    public static class IntOption extends Option<Integer> {
        private final int min, max, step;
        public IntOption(String name, int defaultValue, int min, int max, int step) {
            super(name, defaultValue);
            this.min = min; this.max = max; this.step = step;
        }
        @Override
        public void setValue(Integer value) {
            this.value = value == null ? min : Math.clamp(value, min, max);
        }
        public void next() { value = value + step > max ? min : value + step; }
        public void prev() { value = value - step < min ? max : value - step; }
        public int getMin() { return min; }
        public int getMax() { return max; }
        public String getDisplayValue() { return String.valueOf(value); }
    }

    /**
     * Arbitrary-precision floating-point option. Rendered as a slider + text box
     * pair; the slider snaps to `step`, but the text box accepts any value in
     * [min, max] at full double precision (e.g. 0.000000001). Use this when an
     * option needs precision finer than an int slider can express.
     */
    public static class DoubleOption extends Option<Double> {
        private final double min, max, step;
        public DoubleOption(String name, double defaultValue, double min, double max, double step) {
            super(name, defaultValue);
            this.min = min; this.max = max; this.step = step;
        }
        @Override
        public void setValue(Double value) {
            if (value == null) { this.value = min; return; }
            double v = value;
            if (v < min) v = min;
            if (v > max) v = max;
            // Snap to step to avoid floating point drift like 1.0000000000042
            if (step > 0) {
                v = Math.round(v / step) * step;
                // Re-clamp after rounding
                if (v < min) v = min;
                if (v > max) v = max;
            }
            this.value = v;
        }
        public void next() {
            double nv = value + step;
            value = nv > max ? min : nv;
        }
        public void prev() {
            double nv = value - step;
            value = nv < min ? max : nv;
        }
        public double getMin()  { return min; }
        public double getMax()  { return max; }
        public double getStep() { return step; }
        public String getDisplayValue() {
            // Display with the same number of decimals as the step
            int decimals = 0;
            double s = step;
            while (s < 1.0 && decimals < 10) { s *= 10; decimals++; }
            if (decimals == 0) return String.valueOf((long) Math.round(value));
            return String.format("%." + decimals + "f", value);
        }
    }

    public static List<Option<?>> list(Option<?>... options) {
        List<Option<?>> list = new ArrayList<>();
        for (Option<?> o : options) list.add(o);
        return list;
    }
}
