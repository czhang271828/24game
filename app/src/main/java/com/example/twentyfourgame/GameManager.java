// 完全替换你的 GameManager.java 文件内容
package com.example.twentyfourgame;

import java.util.*;

public class GameManager {
    // 1. 数据结构升级: 使用 List<Number> 代替 Fraction[]
    public List<Number> cardValues = new ArrayList<>();
    private List<Number> initialValues = new ArrayList<>();

    private Stack<List<Number>> undoStack = new Stack<>();
    private Stack<List<Number>> redoStack = new Stack<>();

    public GameSettings settings = new GameSettings();
    private List<Problem> fullProblemSet = new ArrayList<>();
    private List<Problem> filteredProblemSet = new ArrayList<>();

    // 2. 添加游戏模式状态
    public enum GameMode { RATIONAL, GAUSSIAN }
    public GameMode currentMode = GameMode.RATIONAL; // 默认模式

    public int currentNumberCount = 4;
    public int solvedCount = 0;
    private int currentProblemIndex = -1;
    public String currentLevelSolution = null;

    /**
     * 3. 新的、通用的设题入口
     * @param numbers 当前关卡的数字列表
     */
    public void setProblem(List<Number> numbers) {
        undoStack.clear();
        redoStack.clear();
        this.currentNumberCount = numbers.size();
        this.cardValues = new ArrayList<>(numbers);
        this.initialValues = new ArrayList<>(numbers);
        this.currentLevelSolution = Solver.solve(numbers); // 预计算答案
    }

    // setProblemSet 和 applyFilter 保持不变
    public void setProblemSet(List<Problem> problems) {
        this.fullProblemSet = new ArrayList<>(problems);
        applyFilter();
    }

    public void applyFilter() {
        filteredProblemSet.clear();
        for (Problem p : fullProblemSet) {
            // 假设 ProblemFilter.isValid 也能处理 List<Number>
            if (ProblemFilter.isValid(p.numbers, p.solution, settings)) {
                filteredProblemSet.add(p);
            }
        }
        Collections.shuffle(filteredProblemSet);
        currentProblemIndex = -1;
    }

    // startNewGame 现在只决定生成哪种类型的关卡
    public boolean startNewGame(boolean isRandomMode) {
        if (!isRandomMode) {
            if (filteredProblemSet.isEmpty()) return false;
            currentProblemIndex = (currentProblemIndex + 1) % filteredProblemSet.size();
            Problem prob = filteredProblemSet.get(currentProblemIndex);
            setProblem(prob.numbers);
            this.currentLevelSolution = prob.solution;
            return true;
        } else {
            return generateRandomLevel();
        }
    }

    private boolean generateRandomLevel() {
        Random rand = new Random();
        int maxAttempts = 1000;

        for (int i = 0; i < maxAttempts; i++) {
            List<Number> nums = new ArrayList<>();
            if (currentMode == GameMode.RATIONAL) {
                int max = (settings.maxNumber == 999) ? 13 : settings.maxNumber;
                for (int j = 0; j < currentNumberCount; j++) {
                    nums.add(new Fraction(rand.nextInt(max) + 1, 1));
                }
            } else { // GAUSSIAN 模式
                // 为了确保有解，生成共轭对是很好的策略
                nums.add(new ComplexNumber(rand.nextInt(5) + 1, 0)); // 随机实数
                nums.add(new ComplexNumber(rand.nextInt(5) + 1, 0)); // 随机实数
                double realPart = rand.nextInt(3) + 1;
                double imagPart = rand.nextInt(3) + 1;
                nums.add(new ComplexNumber(realPart, imagPart));      // a + bi
                nums.add(new ComplexNumber(realPart, -imagPart));     // a - bi (共轭)
                Collections.shuffle(nums); // 打乱顺序
            }

            if (Solver.solve(nums) != null) {
                setProblem(nums); // 使用新入口点设置游戏
                return true;
            }
        }
        return false;
    }

    // 4. 重构 checkWin
    public boolean checkWin() {
        int count = 0;
        Number last = null;
        for (Number n : cardValues) {
            if (n != null) {
                count++;
                last = n;
            }
        }
        return count == 1 && last != null && last.isEqualTo(24);
    }

    // 5. 重构 performCalculation
    public boolean performCalculation(int idx1, int idx2, String op) {
        if (idx1 >= cardValues.size() || idx2 >= cardValues.size() || cardValues.get(idx1) == null || cardValues.get(idx2) == null) {
            return false;
        }

        Number n1 = cardValues.get(idx1);
        Number n2 = cardValues.get(idx2);
        Number result = null;

        undoStack.push(new ArrayList<>(cardValues));
        redoStack.clear();

        switch (op) {
            case "+": result = n1.add(n2); break;
            case "-": result = n1.subtract(n2); break;
            case "*": result = n1.multiply(n2); break;
            case "/": result = n1.divide(n2); break;
        }

        if (result == null || (result instanceof ComplexNumber && ((ComplexNumber) result).isNaN())) {
            undoStack.pop();
            return false;
        }

        cardValues.set(idx2, result);
        cardValues.set(idx1, null);
        return true;
    }

    // 6. 重构 undo/redo
    public boolean undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(new ArrayList<>(cardValues));
            cardValues = undoStack.pop();
            return true;
        }
        return false;
    }

    public boolean redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(new ArrayList<>(cardValues));
            cardValues = redoStack.pop();
            return true;
        }
        return false;
    }

    public void resetCurrentLevel() {
        undoStack.clear();
        redoStack.clear();
        cardValues = new ArrayList<>(initialValues);
    }

    // 7. 重构 getOrCalculateSolution
    public String getOrCalculateSolution() {
        int count = 0;
        for (Number n : cardValues) if (n != null) count++;
        if (count == currentNumberCount && currentLevelSolution != null) {
            return currentLevelSolution;
        }

        List<Number> currentNums = new ArrayList<>();
        for (Number n : cardValues) if (n != null) currentNums.add(n);
        return Solver.solve(currentNums);
    }
}

