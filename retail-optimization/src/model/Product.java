package model;

/**
 * 产品模型，基于论文Table EC.2中的真实数据
 */
public class Product {
    private final int id;
    private final String name;
    private final String department;
    private final double salesVolume; // λp, 来自Table EC.2
    private final double impulseRate; // ip, 来自Table EC.2
    private final double profitMargin; // ρp

    public Product(int id, String name, String department, double salesVolume, double impulseRate) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.salesVolume = salesVolume;
        this.impulseRate = impulseRate;
        this.profitMargin = calculateProfitMargin(salesVolume);
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public double getSalesVolume() { return salesVolume; }
    public double getImpulseRate() { return impulseRate; }
    public double getProfitMargin() { return profitMargin; }

    private double calculateProfitMargin(double salesVolume) {
        if (salesVolume > 5000) return 0.15;
        if (salesVolume > 2000) return 0.20;
        if (salesVolume > 500) return 0.25;
        return 0.30;
    }
}