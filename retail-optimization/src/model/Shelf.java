package model;

/**
 * 货架模型
 */
public class Shelf {
    private final int id;
    private final String type;
    private final double capacity;
    private final double distanceToEntrance;
    private final double distanceToExit;

    public Shelf(int id, String type, double capacity, double distanceToEntrance, double distanceToExit) {
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.distanceToEntrance = distanceToEntrance;
        this.distanceToExit = distanceToExit;
    }

    // Getters
    public int getId() { return id; }
    public String getType() { return type; }
    public double getCapacity() { return capacity; }
    public double getDistanceToEntrance() { return distanceToEntrance; }
    public double getDistanceToExit() { return distanceToExit; }
}