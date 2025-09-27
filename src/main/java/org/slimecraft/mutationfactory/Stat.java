package org.slimecraft.mutationfactory;

public class Stat {
    public static Stat EMPTY = new Stat(0);

    private final float baseValue;
    private final float currentValue;

    public Stat(float baseValue) {
        this.baseValue = baseValue;
        this.currentValue = baseValue;
    }

    public float getBaseValue() {
        return this.baseValue;
    }

    public float getCurrentValue() {
        return this.currentValue;
    }
}
