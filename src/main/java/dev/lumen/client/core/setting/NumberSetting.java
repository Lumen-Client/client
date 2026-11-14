package dev.lumen.client.core.setting;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberSetting extends Setting {
    private static final DecimalFormat[] FORMATS = buildFormats();

    private final double min;
    private final double max;
    private final double step;
    private final int decimals;
    private double value;
    private final double defaultValue;

    public NumberSetting(String name, double defaultValue, double min, double max, double step, int decimals) {
        super(name, "");
        this.min = min;
        this.max = max;
        this.step = step;
        this.decimals = decimals;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        this(name, defaultValue, min, max, step, 1);
    }

    private static DecimalFormat[] buildFormats() {
        DecimalFormat[] formats = new DecimalFormat[4];
        for (int i = 0; i < 4; i++) {
            StringBuilder sb = new StringBuilder("0");
            if (i > 0) {
                sb.append('.');
                for (int j = 0; j < i; j++) sb.append('0');
            }
            formats[i] = new DecimalFormat(sb.toString());
            formats[i].setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
        }
        return formats;
    }

    public double get() {
        return value;
    }

    public float getFloat() {
        return (float) value;
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    public void set(double value) {
        double snapped = Math.round(value / step) * step;
        this.value = Math.max(min, Math.min(max, snapped));
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public int getDecimals() {
        return decimals;
    }

    public String getDisplayValue() {
        return FORMATS[Math.min(decimals, 3)].format(value);
    }

    public float getPercent() {
        return (float) ((value - min) / (max - min));
    }

    @Override
    public void reset() {
        value = defaultValue;
    }
}
