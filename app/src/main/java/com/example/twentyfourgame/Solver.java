// Solver.java (重构后)
package com.example.twentyfourgame;

import java.util.ArrayList;
import java.util.List;

public class Solver {

    static class Expr {
        Number val;
        String str;
        Expr(Number v, String s) { val = v; str = s; }
    }

    // 求解入口，接受一个 Number 列表
    public static String solve(List<Number> nums) {
        List<Expr> list = new ArrayList<>();
        for (Number n : nums) {
            list.add(new Expr(n, n.toDisplayString()));
        }
        return solveExpr(list);
    }

    private static String solveExpr(List<Expr> list) {
        if (list.size() == 1) {
            Number finalVal = list.get(0).val;
            // 检查最终结果是否为 24
            if (finalVal != null && finalVal.isEqualTo(24)) {
                return list.get(0).str;
            }
            return null;
        }

        // 递归穷举所有组合
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < list.size(); j++) {
                if (i == j) continue;

                Expr a = list.get(i);
                Expr b = list.get(j);

                List<Expr> next = new ArrayList<>();
                for (int k = 0; k < list.size(); k++) {
                    if (k != i && k != j) {
                        next.add(list.get(k));
                    }
                }

                // 尝试四则运算
                Number addRes = a.val.add(b.val);
                Number subRes = a.val.subtract(b.val);
                Number mulRes = a.val.multiply(b.val);
                Number divRes = a.val.divide(b.val);

                // 检查并递归
                String r;
                if ((r = checkAndRecurse(next, addRes, "+", a, b)) != null) return r;
                if ((r = checkAndRecurse(next, subRes, "-", a, b)) != null) return r;
                if ((r = checkAndRecurse(next, mulRes, "*", a, b)) != null) return r;
                if ((r = checkAndRecurse(next, divRes, "/", a, b)) != null) return r;
            }
        }
        return null;
    }

    private static String checkAndRecurse(List<Expr> next, Number res, String op, Expr a, Expr b) {
        // 过滤无效结果 (如除以零产生的 NaN 或 null)
        if (res == null || (res instanceof ComplexNumber && ((ComplexNumber) res).isNaN())) {
            return null;
        }

        List<Expr> branch = new ArrayList<>(next);
        branch.add(new Expr(res, "(" + a.str + op + b.str + ")"));
        return solveExpr(branch);
    }
}
