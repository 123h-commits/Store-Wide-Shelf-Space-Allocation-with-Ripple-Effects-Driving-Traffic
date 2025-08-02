// src/Main.java
import model.Product;
import model.Shelf;
import analysis.TrafficPredictor;
import optimization.GurobiOptimizer;
import optimization.OptimizationResult;

import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("零售店货架空间优化系统 - 论文结果精确复现");
        System.out.println("复现 Flamand, Ghoniem, and Maddah (Operations Research, 2023)");
        System.out.println("=".repeat(80));

        try {
            // 1. 从 input.txt 读取产品数据
            List<Product> products = readProductsFromFile("retail-optimization\\input.txt");
            System.out.println("已从 input.txt 读取 " + products.size() + " 个产品");

            // 2. 从 shelves.txt 读取货架数据
            List<Shelf> shelves = readShelvesFromFile("retail-optimization\\shelves.txt");
            System.out.println("已从 shelves.txt 读取 " + shelves.size() + " 个货架");

            // 3. 创建流量预测器
            TrafficPredictor trafficPredictor = new TrafficPredictor();

            // 4. 创建初始分配 (Fast Movers Back)
            Map<Integer, Integer> fastMoversBack = createFastMoversBackAllocation(shelves, products);

            // 5. 创建优化器
            GurobiOptimizer optimizer = new GurobiOptimizer(shelves, products, trafficPredictor);

            // 6. 执行优化
            OptimizationResult result = optimizer.solve(fastMoversBack, 0.1);

            // 7. 将结果写入 output.txt
            if (result != null) {
                writeResultToFile("output.txt", result, shelves, products);
                System.out.println("优化成功！结果已写入 output.txt");
                System.out.println("老板，我完成了任务，可以退休了吗？");
            } else {
                System.err.println("优化失败。");
            }

        } catch (Exception e) {
            System.err.println("执行过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Product> readProductsFromFile(String filename) throws IOException {
        List<Product> products = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(filename))) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            String[] parts = line.split(",");
            int id = Integer.parseInt(parts[0].trim());
            String name = parts[1].trim();
            String department = parts[2].trim();
            double salesVolume = Double.parseDouble(parts[3].trim());
            double impulseRate = Double.parseDouble(parts[4].trim());
            products.add(new Product(id, name, department, salesVolume, impulseRate));
        }
        return products;
    }

    private static List<Shelf> readShelvesFromFile(String filename) throws IOException {
        List<Shelf> shelves = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(filename))) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            String[] parts = line.split(",");
            int id = Integer.parseInt(parts[0].trim());
            String type = parts[1].trim();
            double capacity = Double.parseDouble(parts[2].trim());
            double distanceToEntrance = Double.parseDouble(parts[3].trim());
            double distanceToExit = Double.parseDouble(parts[4].trim());
            shelves.add(new Shelf(id, type, capacity, distanceToEntrance, distanceToExit));
        }
        return shelves;
    }

    private static void writeResultToFile(String filename, OptimizationResult result, List<Shelf> shelves, List<Product> products) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Optimization Result");
            writer.println("=".repeat(50));
            writer.printf("Current Objective: %.6f%n", result.getCurrentObjective());
            writer.printf("Optimized Objective: %.6f%n", result.getOptimizedObjective());
            writer.printf("Improvement Percentage: %.1f%%%n", result.getImprovementPercentage());
            writer.printf("Computation Time: %.1f seconds%n", result.getComputationTime());
            writer.println("\nOptimized Allocation:");
            writer.println("Shelf ID, Shelf Type, Product ID, Product Name, Allocated Space");
            for (Map.Entry<Integer, Integer> entry : result.getOptimizedAllocation().entrySet()) {
                Shelf shelf = shelves.stream().filter(s -> s.getId() == entry.getKey()).findFirst().orElse(null);
                Product product = products.stream().filter(p -> p.getId() == entry.getValue()).findFirst().orElse(null);
                if (shelf != null && product != null) {
                    double space = result.getAllocatedSpace().get(shelf.getId());
                    writer.printf("%d, %s, %d, %s, %.2f%n", 
                        shelf.getId(), shelf.getType(), product.getId(), product.getName(), space);
                }
            }
        }
    }
    private static List<Product> createProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product(1, "Product1", "Dept1", 171, 0.23));
        products.add(new Product(2, "Product2", "Dept2", 1636, 0.83));
        products.add(new Product(9, "Bread", "Bakery", 5937, 0.04));
        products.add(new Product(23, "Cigarettes", "Tobacco", 8297, 0.71));
        products.add(new Product(26, "Juice", "Beverages", 3602, 0.89));
        products.add(new Product(27, "Soda", "Beverages", 4692, 0.86));
        products.add(new Product(32, "Cheese", "Dairy", 3276, 0.77));
        products.add(new Product(63, "Vegetables", "Produce", 8417, 0.64));
        products.add(new Product(64, "Water", "Beverages", 4167, 0.02));
        return products;
    }

    private static List<Shelf> createShelves() {
        List<Shelf> shelves = new ArrayList<>();
        for (int i = 1; i <= 34; i++) {
            shelves.add(new Shelf(i, "Ordinary", 10.0, i*5, (35-i)*5));
        }
        return shelves;
    }

    private static Map<Integer, Integer> createFastMoversBackAllocation(List<Shelf> shelves, List<Product> products) {
        Map<Integer, Integer> allocation = new HashMap<>();
        List<Product> fastMovers = products.stream().filter(p -> p.getSalesVolume() > 4000).sorted((p1, p2) -> Double.compare(p2.getSalesVolume(), p1.getSalesVolume())).toList();
        List<Shelf> backShelves = new ArrayList<>(shelves.subList(shelves.size()-fastMovers.size(), shelves.size()));
        for (int i = 0; i < Math.min(fastMovers.size(), backShelves.size()); i++) {
            allocation.put(backShelves.get(i).getId(), fastMovers.get(i).getId());
        }
        return allocation;
    }
}