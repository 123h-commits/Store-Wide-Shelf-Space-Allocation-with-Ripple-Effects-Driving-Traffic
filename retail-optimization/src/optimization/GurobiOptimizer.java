package optimization;

import com.gurobi.*;
import model.Product;
import model.Shelf;
import analysis.TrafficPredictor;


//import gurobi.*;
import com.gurobi.gurobi.GRB;
import java.util.*;
import com.gurobi.gurobi.*;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBQuadExpr;
import com.gurobi.gurobi.GRBVar;
import java.util.function.Consumer;
import com.gurobi.gurobi.GRBEnv.*;


/**
 * 核心优化器，使用Gurobi求解
 */
public class GurobiOptimizer {
    private final List<Shelf> shelves;
    private final List<Product> products;
    private final TrafficPredictor trafficPredictor;

    public GurobiOptimizer(List<Shelf> shelves, List<Product> products, TrafficPredictor trafficPredictor) {
        this.shelves = shelves;
        this.products = products;
        this.trafficPredictor = trafficPredictor;
    }

    public OptimizationResult solve(Map<Integer, Integer> inputAllocation, double minSpaceFlexibility) {
        GRBEnv env = null;
        GRBModel model = null;
        try {
            env = new GRBEnv();
            model = new GRBModel(env);

            model.set(GRB.StringAttr.ModelName, "RetailSpaceAllocation");
            model.set(GRB.DoubleParam.TimeLimit, 36000.0); // 10小时
            model.set(GRB.IntParam.OutputFlag, 1); // 输出求解过程

            int numShelves = shelves.size();
            int numProducts = products.size();

            GRBVar[][] xVars = new GRBVar[numShelves][numProducts];
            GRBVar[][] sVars = new GRBVar[numShelves][numProducts];

            for (int b = 0; b < numShelves; b++) {
                for (int p = 0; p < numProducts; p++) {
                    xVars[b][p] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + b + "_" + p);
                    sVars[b][p] = model.addVar(0.0, shelves.get(b).getCapacity(), 0.0, GRB.CONTINUOUS, "s_" + b + "_" + p);
                }
            }

            // 目标函数
            GRBQuadExpr objective = new GRBQuadExpr();
            Map<Integer, Double> theta_b = new HashMap<>();
            for (Shelf shelf : shelves) {
                theta_b.put(shelf.getId(), trafficPredictor.predictTrafficDensity(shelf, products, inputAllocation));
            }

            for (int b = 0; b < numShelves; b++) {
                Shelf shelf = shelves.get(b);
                double C_b = shelf.getCapacity();
                double theta_b_val = theta_b.get(shelf.getId());

                for (int p = 0; p < numProducts; p++) {
                    Product product = products.get(p);
                    double rho_p = product.getProfitMargin();
                    double i_p = product.getImpulseRate();

                    GRBQuadExpr term = new GRBQuadExpr();
                    term.addTerm(rho_p * i_p * C_b * theta_b_val, sVars[b][p], xVars[b][p]);
                    objective.add(term);
                }
            }
            model.setObjective(objective, GRB.MAXIMIZE);

            // 约束
            for (int b = 0; b < numShelves; b++) {
                GRBLinExpr shelfSum = new GRBLinExpr();
                for (int p = 0; p < numProducts; p++) {
                    shelfSum.addTerm(1.0, xVars[b][p]);
                }
                model.addConstr(shelfSum, GRB.LESS_EQUAL, 1.0, "OneProductPerShelf_" + b);
            }

            for (int p = 0; p < numProducts; p++) {
                GRBLinExpr productSum = new GRBLinExpr();
                for (int b = 0; b < numShelves; b++) {
                    productSum.addTerm(1.0, xVars[b][p]);
                }
                model.addConstr(productSum, GRB.LESS_EQUAL, 1.0, "OneShelfPerProduct_" + p);
            }

            for (int b = 0; b < numShelves; b++) {
                GRBLinExpr spaceUsed = new GRBLinExpr();
                for (int p = 0; p < numProducts; p++) {
                    spaceUsed.addTerm(1.0, sVars[b][p]);
                }
                model.addConstr(spaceUsed, GRB.LESS_EQUAL, shelves.get(b).getCapacity(), "Capacity_" + b);
            }

            for (int b = 0; b < numShelves; b++) {
                for (int p = 0; p < numProducts; p++) {
                    Product product = products.get(p);
                    double minSpace = Math.max(1.0, product.getSalesVolume() / 1000.0 * (1.0 - minSpaceFlexibility));
                    double maxSpace = minSpace * 1.2;

                    GRBLinExpr sUbExpr = new GRBLinExpr();
                    sUbExpr.addTerm(maxSpace, xVars[b][p]);
                    model.addConstr(sVars[b][p], GRB.LESS_EQUAL, sUbExpr, "s_ub_" + b + "_" + p);
                    GRBLinExpr sLbExpr = new GRBLinExpr();
                    sLbExpr.addTerm(minSpace, xVars[b][p]);
                    model.addConstr(sVars[b][p], GRB.GREATER_EQUAL, sLbExpr, "s_lb_" + b + "_" + p);
                }
            }

            model.optimize();

            double optimizedObjective = model.get(GRB.DoubleAttr.ObjVal);
            Map<Integer, Integer> optimizedAllocation = new HashMap<>();
            Map<Integer, Double> allocatedSpace = new HashMap<>();

            for (int b = 0; b < numShelves; b++) {
                for (int p = 0; p < numProducts; p++) {
                    if (xVars[b][p].get(GRB.DoubleAttr.X) > 0.5) {
                        optimizedAllocation.put(shelves.get(b).getId(), products.get(p).getId());
                        allocatedSpace.put(shelves.get(b).getId(), sVars[b][p].get(GRB.DoubleAttr.X));
                        break;
                    }
                }
            }

            double currentObjective = calculateNonlinearObjective(inputAllocation);
            return new OptimizationResult(
                optimizedAllocation, allocatedSpace, optimizedObjective, currentObjective,
                (optimizedObjective - currentObjective) / currentObjective * 100, model.get(GRB.DoubleAttr.Runtime)
            );

        } catch (GRBException e) {
            System.err.println("Gurobi error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (model != null) model.dispose();
            } catch (Exception e) {
                System.err.println("Error disposing model: " + e.getMessage());
            }
            try {
                if (env != null) env.dispose();
            } catch (Exception e) {
                System.err.println("Error disposing env: " + e.getMessage());
            }
        }
    }

    private double calculateNonlinearObjective(Map<Integer, Integer> allocation) {
        double total = 0.0;
        for (Map.Entry<Integer, Integer> entry : allocation.entrySet()) {
            int shelfId = entry.getKey();
            int productId = entry.getValue();

            Shelf shelf = shelves.stream().filter(s -> s.getId() == shelfId).findFirst().orElse(null);
            Product product = products.stream().filter(p -> p.getId() == productId).findFirst().orElse(null);

            if (shelf == null || product == null) continue;

            double rho_p = product.getProfitMargin();
            double i_p = product.getImpulseRate();
            double s_pb = product.getSalesVolume() / 1000.0;
            double C_b = shelf.getCapacity();
            double x_bg = 1.0;
            double theta_b = 0.5; // 简化

            total += rho_p * i_p * s_pb * C_b * x_bg * theta_b;
        }
        return total;
    }
}