package com.example.twentyfourgame;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProblemFilter {

    public static boolean isValid(List<Fraction> numbers, String solution, GameSettings settings) {
        if (solution == null || solution.equals("No Solution")) return false;

        // --- 0. 数字上界 (核心修复) ---
        // 逻辑: 如果题目中全是整数 (den==1)，则应用上界检查。
        //       如果题目中包含任意一个分数 (den!=1)，则视为“分数模式”，跳过上界检查。
        if (settings.maxNumber != 999) {
            boolean hasFraction = false;
            for (Fraction f : numbers) {
                if (f.den != 1) {
                    hasFraction = true;
                    break;
                }
            }

            // 只有在“纯整数”情况下才检查大小
            if (!hasFraction) {
                for (Fraction f : numbers) {
                    if (f.num > settings.maxNumber) return false;
                }
            }
        }

        // --- 1. 禁止平凡乘法 ---
        if (settings.banTrivialMult) {
            String s = solution.replace(" ", "");
            if (s.endsWith("*1") || s.endsWith("*2") || s.endsWith("*3") || s.endsWith("*4") ||
                    s.endsWith("*6") || s.endsWith("*8") || s.endsWith("*12")) return false;
            if (s.startsWith("1*") || s.startsWith("2*") || s.startsWith("3*") || s.startsWith("4*") ||
                    s.startsWith("6*") || s.startsWith("8*") || s.startsWith("12*")) return false;
        }

        // --- 运算符判定 (使用正则精准匹配) ---

        // 匹配 " / " (前后有空白字符的除号)，避免匹配到分数 "1/3"
        // 如果文件格式非常紧凑如 "1/3/4"，可能没有空格，这时可以用更复杂的正则，
        // 但根据你的描述，txt中解答是有空格的。
        // 保险起见，我们计算：总斜杠数 - 分数线数 = 运算符除号数

        int totalSlashes = countOccurrences(solution, "/");
        int fractionSlashes = countRegexMatches(solution, "\\d+/\\d+"); // 匹配 1/3 这种结构

        // 真正的除法运算数量
        int opDivCount = totalSlashes - fractionSlashes;
        // 如果减出来小于0 (极端情况)，修正为0
        if (opDivCount < 0) opDivCount = 0;

        boolean hasOpDiv = opDivCount > 0;
        boolean hasMul = solution.contains("*");
        boolean hasAdd = solution.contains("+");
        boolean hasSub = solution.contains("-");

        // --- 层级递进逻辑 ---

        // Level 1: 禁止纯加减
        if (settings.difficultyMode >= 1) {
            if (!hasMul && !hasOpDiv) return false;
        }

        // Level 2: 必须含除法
        if (settings.difficultyMode >= 2) {
            if (!hasOpDiv) return false;

            // 进阶 A: 出现有理分数加减
            if (settings.enableRationalCalc) {
                if (!hasAdd && !hasSub) return false;
            }

            // 进阶 B: 除法风暴
            if (settings.enableDivisionStorm) {
                int n = numbers.size();
                if (opDivCount < (n - 2)) return false;
            }
        }

        return true;
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

    private static int countRegexMatches(String str, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }
}
