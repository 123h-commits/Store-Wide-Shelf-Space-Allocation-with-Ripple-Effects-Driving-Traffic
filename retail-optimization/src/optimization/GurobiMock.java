// src/optimization/GurobiMock.java
package optimization;

/**
 * 一个模拟的 Gurobi 接口，用于在没有真实 Gurobi 的情况下编译和运行。
 * 它不进行真正的优化，而是返回一个预设的“优化”结果。
 */
public class GurobiMock {
    public static class GRBEnv {
        public GRBEnv() {}
        public void dispose() {}
    }

    public static class GRBModel {
        public GRBModel(GRBEnv env) {}
        public void dispose() {}
        public void setObjective(Object objective, int max) {}
        public void addConstr(Object expr, int type, double rhs, String name) {}
        public void set(String attrName, Object value) {}
        public double get(int attr) { return 0; }
        public void optimize() {}
    }

    public static class GRB {
        public static final int MAXIMIZE = 1;
        public static final int LESS_EQUAL = 1;
        public static final int BINARY = 1;
        public static final int CONTINUOUS = 1;
        public static final int DoubleAttr = 1;
        public static final int ObjVal = 1;
        public static final int Runtime = 1;
    }

    public static class GRBVar {}
    public static class GRBQuadExpr {}
    public static class GRBLinExpr {}
    public static class GRBException extends Exception {
        public GRBException(String msg) { super(msg); }
    }
}