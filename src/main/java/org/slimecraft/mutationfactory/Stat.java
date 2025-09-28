package org.slimecraft.mutationfactory;

public class Stat {
    public static Stat EMPTY = new Stat(0, 0);

    private final float baseValue;
    private float currentValue;
    private float incrementValue;
    private int points;

    public Stat(float baseValue, float incrementValue) {
        this.baseValue = baseValue;
        this.currentValue = baseValue;
        this.incrementValue = incrementValue;
        this.points = 0;
    }

    public Stat(float baseValue, float currentValue, float incrementValue, int points) {
        this.baseValue = baseValue;
        this.currentValue = currentValue;
        this.incrementValue = incrementValue;
        this.points = points;
    }

    public void upgrade() {
        this.points++;
        this.currentValue += this.incrementValue;
    }

    public float getBaseValue() {
        return this.baseValue;
    }

    public float getCurrentValue() {
        return this.currentValue;
    }

    public int getPoints() {
        return this.points;
    }
}
