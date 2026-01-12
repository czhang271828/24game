package com.example.twentyfourgame;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProblemFilter {

    // 定义平凡乘数集合：1, 2, 3, 4, 6, 8, 12
    private static final int[] TRIVIAL_FACTORS = {1, 2, 3, 4, 6, 8, 12};

    public static boolean isValid(List<Fraction> numbers, String solution, GameSettings settings) {
        if (solution == null || solution.equals("No Solution")) return false;

        // --- 0. 数字上界检查 ---
        if (settings.maxNumber != 999) {
            boolean hasFraction = false;
            for (Fraction f : numbers) {
                if (f.den != 1) {
                    hasFraction = true;
                    break;
                }
            }
            if (!hasFraction) {
                for (Fraction f : numbers) {
                    if (f.num > settings.maxNumber) return false;
                }
            }
        }

        // --- 1. 禁止平凡乘法 (修复版) ---
        if (settings.banTrivialMult) {
            // A. 正则匹配字面量 (修复：加空格判定，避免 *12 被误判为 *2)
            // 匹配结尾是 "* 2" 或 "* (2)" 形式，且前方有非数字边界
            if (matchesTrivialLiteral(solution, true)) return false; // 检查结尾
            if (matchesTrivialLiteral(solution, false)) return false; // 检查开头 (如 2 * ...)

            // B. 尝试检测 (2+4)*(5-1) 这种隐式平凡乘法
            // 这需要解析最后一步运算是否是乘法，且右操作数的值是否平凡
            if (isImplicitTrivialMult(solution)) return false;
        }

        // --- 运算符判定 (修正版) ---

        int totalSlashes = countOccurrences(solution, "/");

        // 修复 1: 正则允许斜杠前后有空格 (\\s*)
        // 这样 "8 / 3" 也能被识别为分数结构，而不仅仅是 "8/3"
        int fractionStructureCount = countRegexMatches(solution, "(\\d+)\\s*/\\s*(\\d+)");

        // 计算纯粹的运算除号 (例如 8 / (1+2))
        // 注意：如果解法写成 (8 / 3)，正则会匹配到，fractionStructureCount 会加 1
        // opDivCount 就会减少，这符合逻辑：8/3 被视为一个“数”而非一步“除法运算”
        int opDivCount = Math.max(0, totalSlashes - fractionStructureCount);

        boolean hasOpDiv = opDivCount > 0;
        boolean hasMul = solution.contains("*");
        boolean hasAdd = solution.contains("+");
        boolean hasSub = solution.contains("-");
        // --- Level 1: 禁止纯加减 ---
        if (settings.difficultyMode >= 1) {
            if (!hasMul && !hasOpDiv) return false;
        }

// Level 2: 必须含除法
        if (settings.difficultyMode >= 2) {
            if (!hasOpDiv) return false;

            // 进阶 A: 出现有理分数加减
            if (settings.enableRationalCalc) {
                // 必须有加法或减法
                if (!hasAdd && !hasSub) return false;

                // 修复 2: 检查是否真正出现了“非整数分数”
                // (8 / 3) 在这里会被正确识别并计算 8%3 != 0，从而返回 true
                if (!containsNonIntegerFraction(solution)) return false;
            }

            // 进阶 B: 除法风暴
            if (settings.enableDivisionStorm) {
                int n = numbers.size();
                if (opDivCount < (n - 2)) return false;
            }
        }

        return true;
    }

    // --- 辅助方法 ---

    /**
     * 检查字符串中是否包含真正的分数 (非整数)
     * 支持带空格的格式，如 "8 / 3"
     */
    private static boolean containsNonIntegerFraction(String solution) {
        // 修改正则：允许 / 前后有空格
        Pattern p = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
        Matcher m = p.matcher(solution);
        while (m.find()) {
            try {
                int num = Integer.parseInt(m.group(1));
                int den = Integer.parseInt(m.group(2));
                // 只有当分子不能被分母整除时 (例如 8/3)，才视为出现了分数
                // 如果是 4/2，则只是普通的除法运算，不算分数题目
                if (den != 0 && num % den != 0) {
                    return true;
                }
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    private static int countRegexMatches(String str, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    /**
     * 使用正则检查字面量的平凡乘法
     * @param checkEnd true查结尾 (* 2), false查开头 (2 *)
     */
    private static boolean matchesTrivialLiteral(String sol, boolean checkEnd) {
        for (int val : TRIVIAL_FACTORS) {
            // Regex解释:
            // \\* 匹配乘号
            // \\s* 允许任意空格
            // val 匹配数字
            // (?![\\d]) 负向先行断言，确保数字后面不是另一个数字 (防止 *2 匹配 *20)
            String pattern;
            if (checkEnd) {
                // 匹配结尾，例如 "... * 2" 或 "... * 2)"
                // 注意：解答可能以括号结尾，如 (...)*2
                pattern = "\\*\\s*" + val + "\\s*$";
            } else {
                // 匹配开头，例如 "2 * ..."
                pattern = "^\\s*" + val + "\\s*\\*";
            }

            if (Pattern.compile(pattern).matcher(sol).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试检测隐式的平凡乘法，如 (A) * (5-1)
     * 这是一个简化的解析，只处理最后一步是乘法的情况
     */
    private static boolean isImplicitTrivialMult(String solution) {
        // 1. 找到主运算符。由于可能有括号，我们从右向左扫描，寻找括号平衡时的 '*'
        int mainMulIndex = findMainMultiplicationIndex(solution);
        if (mainMulIndex == -1) return false;

        // 2. 提取右操作数
        String rightOperand = solution.substring(mainMulIndex + 1).trim();
        String leftOperand = solution.substring(0, mainMulIndex).trim();

        // 3. 尝试计算右操作数的值
        Double rightVal = simpleEval(rightOperand);
        if (rightVal != null && isTrivialValue(rightVal)) return true;

        // 4. 同理检查左操作数 (比如 2 * (3+5))
        Double leftVal = simpleEval(leftOperand);
        if (leftVal != null && isTrivialValue(leftVal)) return true;

        return false;
    }

    private static int findMainMultiplicationIndex(String str) {
        int balance = 0;
        // 从右向左扫描
        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == ')') balance++;
            else if (c == '(') balance--;
            else if (c == '*' && balance == 0) {
                return i;
            }
        }
        return -1; // 没有找到主乘号
    }

    // 极其简化的求值器，只处理 "(5-1)" 或 "3" 这种简单情况
    // 复杂的嵌套不处理，避免过度设计
    private static Double simpleEval(String expr) {
        expr = expr.trim();
        // 去除外层括号 (可能有多个)
        while (expr.startsWith("(") && expr.endsWith(")")) {
            // 确保括号是配对的，例如 "(1+2)*(3+4)" 不能去掉两头
            if (isValidParenthesis(expr.substring(1, expr.length()-1))) {
                expr = expr.substring(1, expr.length() - 1).trim();
            } else {
                break;
            }
        }

        // 如果是纯数字
        if (expr.matches("-?\\d+")) return (double) Integer.parseInt(expr);

        // 简单的 A +/- B 形式
        // 再次扫描找主加减号
        int balance = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') balance++;
            else if (c == '(') balance--;
            else if ((c == '+' || c == '-') && balance == 0) {
                String left = expr.substring(0, i);
                String right = expr.substring(i + 1);
                Double lVal = simpleEval(left);
                Double rVal = simpleEval(right);
                if (lVal != null && rVal != null) {
                    return c == '+' ? lVal + rVal : lVal - rVal;
                }
            }
        }
        return null; // 无法计算 (可能是分数表达式或其他)
    }

    // 检查去括号后的字符串括号是否依然平衡
    private static boolean isValidParenthesis(String str) {
        int balance = 0;
        for (char c : str.toCharArray()) {
            if (c == '(') balance++;
            else if (c == ')') balance--;
            if (balance < 0) return false;
        }
        return balance == 0;
    }

    private static boolean isTrivialValue(double val) {
        for (int t : TRIVIAL_FACTORS) {
            if (Math.abs(val - t) < 0.0001) return true;
        }
        return false;
    }

    private static int countOccurrences(String str, String sub) {
        if (str == null || sub == null || sub.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

}
