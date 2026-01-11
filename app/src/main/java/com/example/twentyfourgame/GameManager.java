package com.example.twentyfourgame;

import java.util.*;

public class GameManager {
    // 支持最大6个数，这里开辟空间
    public Fraction[] cardValues = new Fraction[6];
    public Fraction[] initialValues = new Fraction[6];

    // --- 核心修复：定义 settings 变量 ---
    public GameSettings settings = new GameSettings();

    private Stack<Fraction[]> undoStack = new Stack<>();
    private Stack<Fraction[]> redoStack = new Stack<>();

    // 保存完整的题库和筛选后的题库
    private List<Problem> fullProblemSet = new ArrayList<>();
    private List<Problem> filteredProblemSet = new ArrayList<>();

    public int currentNumberCount = 4;
    public int solvedCount = 0;
    private int currentProblemIndex = -1;
    public String currentLevelSolution = null;

    // 设置题库
    public void setProblemSet(List<Problem> problems) {
        this.fullProblemSet = new ArrayList<>(problems);
        applyFilter(); // 加载新题库时立即筛选
    }

    // 应用筛选逻辑 (仅对题库有效)
    public void applyFilter() {
        filteredProblemSet.clear();
        for (Problem p : fullProblemSet) {
            if (ProblemFilter.isValid(p.numbers, p.solution, settings)) {
                filteredProblemSet.add(p);
            }
        }
        Collections.shuffle(filteredProblemSet);
        currentProblemIndex = -1;
        solvedCount = 0;
    }

    // 开始新游戏 (返回 boolean 表示是否成功生成)
    public boolean startNewGame(boolean isRandomMode) {
        undoStack.clear();
        redoStack.clear();

        boolean success = generateLevel(isRandomMode);

        if (success) {
            // 确保 initialValues 长度足够
            if (initialValues.length < cardValues.length) {
                initialValues = new Fraction[cardValues.length];
            }
            // 备份初始状态用于重置
            for(int i=0; i<cardValues.length; i++) initialValues[i] = cardValues[i];
            return true;
        } else {
            return false;
        }
    }

    private boolean generateLevel(boolean isRandomMode) {
        // 清空当前牌面
        for(int i=0; i<cardValues.length; i++) cardValues[i] = null;

        if (!isRandomMode) {
            // --- 题库模式 ---
            if (filteredProblemSet.isEmpty()) {
                return false; // 筛选后无题
            } else {
                currentProblemIndex++;
                if (currentProblemIndex >= filteredProblemSet.size()) {
                    currentProblemIndex = 0;
                    Collections.shuffle(filteredProblemSet);
                }
                Problem prob = filteredProblemSet.get(currentProblemIndex);

                currentNumberCount = prob.numbers.size();
                for (int i = 0; i < currentNumberCount; i++) cardValues[i] = prob.numbers.get(i);
                currentLevelSolution = prob.solution;
                return true;
            }
        } else {
            // --- 休闲随机模式 ---
            return generateRandomLevel();
        }
    }

    // 随机生成逻辑
    private boolean generateRandomLevel() {
        Random rand = new Random();
        int attempts = 0;
        int MAX_ATTEMPTS = 1000;

        while(attempts < MAX_ATTEMPTS) {
            attempts++;
            List<Fraction> nums = new ArrayList<>();

            // 休闲模式：仅保留数字上界设置 (默认13)
            int max = (settings.maxNumber == 999) ? 13 : settings.maxNumber;

            for(int i=0; i<currentNumberCount; i++) {
                nums.add(new Fraction(rand.nextInt(max) + 1, 1));
            }

            String sol = Solver.solve(nums);

            // 休闲模式核心：只要有解 (sol != null) 即可，不进行 ProblemFilter 过滤
            if(sol != null) {
                for(int i=0; i<cardValues.length; i++) cardValues[i] = null;
                for(int i=0; i<currentNumberCount; i++) cardValues[i] = nums.get(i);
                currentLevelSolution = sol;
                return true;
            }
        }

        return false;
    }

    public boolean checkWin() {
        int count = 0;
        Fraction last = null;
        for (Fraction f : cardValues) if (f != null) { count++; last = f; }
        return count == 1 && last != null && last.isValue(24);
    }

    public boolean performCalculation(int idx1, int idx2, String op) {
        Fraction f1 = cardValues[idx1];
        Fraction f2 = cardValues[idx2];
        Fraction result = null;

        switch (op) {
            case "+": result = f1.add(f2); break;
            case "-": result = f1.sub(f2); break;
            case "*": result = f1.multiply(f2); break;
            case "/": result = f1.divide(f2); break;
        }

        // 保存撤销状态
        if(undoStack.isEmpty() || !Arrays.equals(undoStack.peek(), cardValues)){
            Fraction[] state = new Fraction[cardValues.length];
            System.arraycopy(cardValues, 0, state, 0, cardValues.length);
            undoStack.push(state);
        }
        redoStack.clear();

        cardValues[idx2] = result;
        cardValues[idx1] = null;
        return true;
    }

    public boolean undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(cardValues.clone());
            cardValues = undoStack.pop();
            return true;
        }
        return false;
    }

    public boolean redo() {
        if (!redoStack.isEmpty()) {
            if(undoStack.isEmpty() || !Arrays.equals(undoStack.peek(), cardValues)){
                Fraction[] state = new Fraction[cardValues.length];
                System.arraycopy(cardValues, 0, state, 0, cardValues.length);
                undoStack.push(state);
            }
            cardValues = redoStack.pop();
            return true;
        }
        return false;
    }

    public void resetCurrentLevel() {
        undoStack.clear();
        redoStack.clear();
        System.arraycopy(initialValues, 0, cardValues, 0, initialValues.length);
    }

    public String getOrCalculateSolution() {
        int count = 0;
        for (Fraction f : cardValues) if (f != null) count++;
        if (count == currentNumberCount && currentLevelSolution != null) return currentLevelSolution;

        List<Fraction> nums = new ArrayList<>();
        for (Fraction f : cardValues) if (f != null) nums.add(f);
        return Solver.solve(nums);
    }
}
