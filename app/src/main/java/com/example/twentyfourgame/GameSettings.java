package com.example.twentyfourgame;

public class GameSettings {
    public int maxNumber = 13;      // 0. 数字上界

    // 1. 独立逻辑
    public boolean banTrivialMult = false;

    // 递进主层级
    // 0: 无限制
    // 1: 禁止纯加减 (必须有 * 或 /)
    // 2: 必须含除法 (开启后续进阶)
    public int difficultyMode = 0;

    // 进阶选项 (当 difficultyMode == 2 时生效)
    public boolean enableRationalCalc = false;  // 4. 有理分数加减
    public boolean enableDivisionStorm = false; // 5. 除法风暴
}
