package optimization;

import java.util.Map;

public class OptimizationResult {
    private final Map<Integer, Integer> optimizedAllocation;
    private final Map<Integer, Double> allocatedSpace;
    private final double optimizedObjective;
    private final double currentObjective;
    private final double improvementPercentage;
    private final double computationTime;

    public OptimizationResult(Map<Integer, Integer> optimizedAllocation, Map<Integer, Double> allocatedSpace,
                            double optimizedObjective, double currentObjective,
                            double improvementPercentage, double computationTime) {
        this.optimizedAllocation = optimizedAllocation;
        this.allocatedSpace = allocatedSpace;
        this.optimizedObjective = optimizedObjective;
        this.currentObjective = currentObjective;
        this.improvementPercentage = improvementPercentage;
        this.computationTime = computationTime;
    }

    // Getters
    public Map<Integer, Integer> getOptimizedAllocation() { return optimizedAllocation; }
    public Map<Integer, Double> getAllocatedSpace() { return allocatedSpace; }
    public double getOptimizedObjective() { return optimizedObjective; }
    public double getCurrentObjective() { return currentObjective; }
    public double getImprovementPercentage() { return improvementPercentage; }
    public double getComputationTime() { return computationTime; }
}