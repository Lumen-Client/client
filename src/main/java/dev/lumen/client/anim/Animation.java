package dev.lumen.client.anim;

/**
 * Delta-time driven, frame-rate independent animation value.
 * Instances are mutated in place - never allocate inside render loops.
 */
public class Animation {
    private final float durationSeconds;
    private final EasingFn easing;

    private float progress;
    private float value;
    private boolean increasing = true;
    private long lastNanos;

    public Animation(float durationSeconds) {
        this(durationSeconds, Easing::easeOutCubic);
    }

    public Animation(float durationSeconds, EasingFn easing) {
        this.durationSeconds = durationSeconds;
        this.easing = easing;
        this.lastNanos = System.nanoTime();
    }

    public void setTarget(boolean on) {
        if (on != increasing) {
            float v = value;
            if (v > 0.0001f && v < 0.9999f) {
                progress = solveProgress(v, on);
            }
            increasing = on;
            lastNanos = System.nanoTime();
        }
    }

    /**
     * Re-derives the raw progress for a reversed target so the eased value
     * stays continuous - animations can be interrupted mid-flight without
     * any visual jump.
     */
    private float solveProgress(float targetValue, boolean on) {
        float lo = 0.0f;
        float hi = 1.0f;
        for (int i = 0; i < 12; i++) {
            float mid = (lo + hi) * 0.5f;
            float val = on ? easing.apply(mid) : 1.0f - easing.apply(1.0f - mid);
            if (val < targetValue) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) * 0.5f;
    }

    public void setTarget(boolean on, boolean instant) {
        this.increasing = on;
        if (instant) {
            progress = on ? 1.0f : 0.0f;
            value = progress;
        }
        lastNanos = System.nanoTime();
    }

    public void snap(boolean on) {
        setTarget(on, true);
    }

    public void update() {
        long now = System.nanoTime();
        float delta = (now - lastNanos) / 1_000_000_000.0f;
        lastNanos = now;
        if (delta > 0.25f) delta = 0.25f;
        float step = delta / durationSeconds;
        progress = increasing ? Math.min(1.0f, progress + step) : Math.max(0.0f, progress - step);
        value = increasing ? easing.apply(progress) : 1.0f - easing.apply(1.0f - progress);
    }

    public float getValue() {
        if (lastNanos == 0) {
            lastNanos = System.nanoTime();
        }
        return value;
    }

    public boolean isAtTarget() {
        return increasing ? progress >= 1.0f : progress <= 0.0f;
    }

    public boolean isTargetOn() {
        return increasing;
    }

    public interface EasingFn {
        float apply(float t);
    }
}
