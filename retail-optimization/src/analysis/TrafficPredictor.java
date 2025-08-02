package analysis;

import model.Shelf;
import model.Product;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 流量预测器 - 严格遵循论文Table 3
 */
public class TrafficPredictor {

    private static final double ALPHA = -3.285;
    private static final double BETA1 = 14.751;
    private static final double BETA2 = 5.491;

    public double predictTrafficDensity(Shelf shelf, List<Product> products, Map<Integer, Integer> allocation) {
        double fnorm_b = calculateFnormB(shelf, products, allocation);
        double k_b = calculateKB(shelf);

        double logit_theta = ALPHA + BETA1 * fnorm_b + BETA2 * k_b;
        double theta_b = Math.exp(logit_theta) / (1.0 + Math.exp(logit_theta));
        return Math.max(0.01, Math.min(0.99, theta_b));
    }

    private double calculateFnormB(Shelf shelf, List<Product> products, Map<Integer, Integer> allocation) {
        Integer productId = allocation.get(shelf.getId());
        if (productId == null) return 0.0;

        Product product = products.stream()
            .filter(p -> p.getId() == productId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        double productAttraction = product.getImpulseRate() * product.getSalesVolume();
        double totalAttraction = products.stream()
            .mapToDouble(p -> p.getImpulseRate() * p.getSalesVolume())
            .sum();

        return productAttraction / (totalAttraction + 1e-8);
    }

    private double calculateKB(Shelf shelf) {
        double d0b = shelf.getDistanceToEntrance();
        double dbn = shelf.getDistanceToExit();
        return 1.0 / (d0b + dbn + 1e-8);
    }
}