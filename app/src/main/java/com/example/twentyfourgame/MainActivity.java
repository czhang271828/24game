package com.example.twentyfourgame;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvScore, tvTimer, tvAvgTime;

    // 核心卡片组件 (数组大小为 5)
    private ViewGroup[] cardViews = new ViewGroup[5];
    private TextView[] tvNums = new TextView[5];    // 分子
    private TextView[] tvDenoms = new TextView[5];  // 分母
    private View[] dividers = new View[5];          // 分数线

    private Button btnAdd, btnSub, btnMul, btnDiv;
    private Button btnUndo, btnReset, btnRedo, btnMenu;
    private Button btnTry, btnHintStruct, btnAnswer, btnShare, btnSkip;

    // 逻辑组件
    private GameManager gameManager;
    private ProblemRepository repository;

    // UI 状态
    private long startTime, gameStartTime;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private int selectedFirstIndex = -1;
    private String selectedOperator = null;
    private String currentFileName = "休闲随机(4数)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new ProblemRepository(this);
        gameManager = new GameManager();

        initViews();
        initSidebar();
        initListeners();

        gameStartTime = System.currentTimeMillis();
        switchToRandomMode(4);
        startTimer();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btn_menu);
        tvScore = findViewById(R.id.tv_score);
        tvTimer = findViewById(R.id.tv_timer);
        tvAvgTime = findViewById(R.id.tv_avg_time);

        // 绑定 5 张卡片
        int[] cardIds = {R.id.card_1, R.id.card_2, R.id.card_3, R.id.card_4, R.id.card_5};
        int[] numIds = {R.id.tv_num_1, R.id.tv_num_2, R.id.tv_num_3, R.id.tv_num_4, R.id.tv_num_5};
        int[] divIds = {R.id.divider_1, R.id.divider_2, R.id.divider_3, R.id.divider_4, R.id.divider_5};
        int[] denIds = {R.id.tv_denom_1, R.id.tv_denom_2, R.id.tv_denom_3, R.id.tv_denom_4, R.id.tv_denom_5};

        for (int i = 0; i < 5; i++) {
            cardViews[i] = findViewById(cardIds[i]);
            tvNums[i] = findViewById(numIds[i]);
            dividers[i] = findViewById(divIds[i]);
            tvDenoms[i] = findViewById(denIds[i]);
        }

        btnAdd = findViewById(R.id.btn_op_add);
        btnSub = findViewById(R.id.btn_op_sub);
        btnMul = findViewById(R.id.btn_op_mul);
        btnDiv = findViewById(R.id.btn_op_div);

        btnUndo = findViewById(R.id.btn_undo);
        btnReset = findViewById(R.id.btn_reset);
        btnRedo = findViewById(R.id.btn_redo);

        btnTry = findViewById(R.id.btn_try);
        btnHintStruct = findViewById(R.id.btn_hint_struct);
        btnAnswer = findViewById(R.id.btn_answer);
        btnShare = findViewById(R.id.btn_share);
        btnSkip = findViewById(R.id.btn_skip);
    }

    // --- 核心修复: 使用 setTint 改变颜色，保留 XML 中定义的圆角 ---
    private void setCardColor(View view, int color) {
        if (view == null) return;
        Drawable bg = view.getBackground();
        if (bg != null) {
            // mutate() 很重要，防止修改影响到其他复用该资源的视图
            bg.mutate().setTint(color);
        } else {
            // 如果没有背景drawable，才回退到简单的背景色设置
            view.setBackgroundColor(color);
        }
    }

    private void updateCardDisplay(int index, Fraction f) {
        if (f == null) {
            cardViews[index].setVisibility(View.INVISIBLE);
            return;
        }

        cardViews[index].setVisibility(View.VISIBLE);
        tvNums[index].setText(String.valueOf(f.num));

        if (f.den == 1) {
            // 整数：隐藏横线和分母
            dividers[index].setVisibility(View.GONE);
            tvDenoms[index].setVisibility(View.GONE);
        } else {
            // 分数：显示竖式结构
            dividers[index].setVisibility(View.VISIBLE);
            tvDenoms[index].setVisibility(View.VISIBLE);
            tvDenoms[index].setText(String.valueOf(f.den));
        }
    }

    private void refreshUI() {
        // --- 修复: 通用的显隐逻辑，支持 3, 4, 5 张牌 ---
        for (int i = 0; i < 5; i++) {
            // 如果卡片索引超出了当前游戏设定的数量，则隐藏 (例如 3数模式下，索引 3,4 被隐藏)
            if (i >= gameManager.currentNumberCount) {
                cardViews[i].setVisibility(View.GONE);
            } else {
                updateCardDisplay(i, gameManager.cardValues[i]);
            }
        }
        updateScoreBoard();
    }

    private void onCardClicked(int index) {
        if (selectedFirstIndex == -1) {
            selectCard(index);
        } else if (selectedFirstIndex == index) {
            resetSelection();
        } else {
            if (selectedOperator == null) {
                selectCard(index);
            } else {
                try {
                    boolean success = gameManager.performCalculation(selectedFirstIndex, index, selectedOperator);
                    if (success) {
                        resetSelection();
                        refreshUI();
                        selectCard(index);
                        checkWin();
                    }
                } catch (ArithmeticException e) {
                    Toast.makeText(this, "除数不能为0", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void checkWin() {
        if (gameManager.checkWin()) {
            Toast.makeText(this, "成功！", Toast.LENGTH_SHORT).show();
            gameManager.solvedCount++;
            updateScoreBoard();
            new Handler().postDelayed(() -> {
                // 确保这里判断的是 "休闲随机"
                gameManager.startNewGame(currentFileName.startsWith("休闲随机"));
                resetSelection();
                startTime = System.currentTimeMillis();
                refreshUI();
            }, 1200);
        }
    }

    private void startNewGameLocal() {
        // 确保这里判断的是 "休闲随机"
        gameManager.startNewGame(currentFileName.startsWith("休闲随机"));
        startTime = System.currentTimeMillis();
        resetSelection();
        refreshUI();
    }

    // --- 修改: initSidebar 增加设置入口 ---
    private void initSidebar() {
        Menu menu = navigationView.getMenu();
        menu.clear();

        // 1. 设置
        menu.add(Menu.NONE, 777, Menu.NONE, "⚙️ 题目筛选设置");
        // (提示：你可以把设置改名为 "⚙️ 题库筛选设置"，暗示仅对题库有效，不过不改也行)

        menu.add(Menu.NONE, 888, Menu.NONE, "📖 游戏说明书");
        menu.add(Menu.NONE, 999, Menu.NONE, "☁️ 从 GitHub 更新题库");

        // --- 修改 2: 改名为 ☕ 休闲随机 ---
        menu.add(Menu.NONE, -1, Menu.NONE, "☕ 休闲随机 (3数)");
        menu.add(Menu.NONE, 0, Menu.NONE, "☕ 休闲随机 (4数)");
        menu.add(Menu.NONE, 1, Menu.NONE, "☕ 休闲随机 (5数)");

        List<String> files = repository.getAvailableFiles();
        int id = 2;
        for (String f : files) menu.add(Menu.NONE, id++, Menu.NONE, "📄 " + f);

        navigationView.setNavigationItemSelectedListener(item -> {
            String t = item.getTitle().toString();
            int itemId = item.getItemId();

            if (itemId == 777) {
                showSettingsDialog();
            } else if (t.contains("游戏说明书")) {
                showHelpDialog();
            } else if (t.contains("从 GitHub 更新")) {
                syncFromGitHub();
            } else {
                // --- 修改 3: 适配新名字的判断 ---
                if (t.contains("休闲随机 (3数)")) switchToRandomMode(3);
                else if (t.contains("休闲随机 (4数)")) switchToRandomMode(4);
                else if (t.contains("休闲随机 (5数)")) switchToRandomMode(5);
                else loadProblemSet(t.substring(t.indexOf(" ") + 1));
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });
    }

    // --- 新增: 显示设置对话框 ---
    private void showSettingsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);

        android.widget.RadioGroup rgMax = view.findViewById(R.id.rg_max_number);
        android.widget.CheckBox cbTrivial = view.findViewById(R.id.cb_ban_trivial);

        // 递进层级控件
        android.widget.RadioGroup rgDiff = view.findViewById(R.id.rg_difficulty);
        android.widget.RadioButton rbDiff2 = view.findViewById(R.id.rb_diff_2); // Level 2 RadioButton

        // 进阶面板
        View layoutAdvanced = view.findViewById(R.id.layout_advanced_options);
        android.widget.CheckBox cbRational = view.findViewById(R.id.cb_rational);
        android.widget.CheckBox cbStorm = view.findViewById(R.id.cb_storm);

        // --- 回显数据 ---
        GameSettings s = gameManager.settings;

        // Max Number
        if(s.maxNumber == 10) rgMax.check(R.id.rb_10);
        else if(s.maxNumber == 13) rgMax.check(R.id.rb_13);
        else if(s.maxNumber == 20) rgMax.check(R.id.rb_20);
        else rgMax.check(R.id.rb_no_limit);

        // Trivial
        cbTrivial.setChecked(s.banTrivialMult);

        // Difficulty Level
        if (s.difficultyMode == 0) rgDiff.check(R.id.rb_diff_0);
        else if (s.difficultyMode == 1) rgDiff.check(R.id.rb_diff_1);
        else if (s.difficultyMode == 2) rgDiff.check(R.id.rb_diff_2);

        // Advanced Options
        cbRational.setChecked(s.enableRationalCalc);
        cbStorm.setChecked(s.enableDivisionStorm);

        // --- 核心交互：根据层级控制进阶面板显隐 ---
        // 初始化状态
        layoutAdvanced.setVisibility(s.difficultyMode == 2 ? View.VISIBLE : View.GONE);

        rgDiff.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_diff_2) {
                // 选中 "必须含除法"，显示进阶
                layoutAdvanced.setVisibility(View.VISIBLE);
            } else {
                // 否则隐藏，并建议取消勾选防止逻辑残留（可选）
                layoutAdvanced.setVisibility(View.GONE);
                cbRational.setChecked(false);
                cbStorm.setChecked(false);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("筛选设置 (对 ☕ 休闲随机模式无效)")
                .setView(view)
                .setPositiveButton("应用", (dialog, which) -> {
                    // 保存设置
                    int checkedId = rgMax.getCheckedRadioButtonId();
                    if(checkedId == R.id.rb_10) s.maxNumber = 10;
                    else if(checkedId == R.id.rb_13) s.maxNumber = 13;
                    else if(checkedId == R.id.rb_20) s.maxNumber = 20;
                    else s.maxNumber = 999;

                    s.banTrivialMult = cbTrivial.isChecked();

                    int diffId = rgDiff.getCheckedRadioButtonId();
                    if (diffId == R.id.rb_diff_0) s.difficultyMode = 0;
                    else if (diffId == R.id.rb_diff_1) s.difficultyMode = 1;
                    else if (diffId == R.id.rb_diff_2) s.difficultyMode = 2;

                    s.enableRationalCalc = cbRational.isChecked();
                    s.enableDivisionStorm = cbStorm.isChecked();

                    // 应用并刷新
                    Toast.makeText(this, "正在应用筛选...", Toast.LENGTH_SHORT).show();
                    if (!currentFileName.startsWith("休闲随机")) {
                        gameManager.applyFilter();
                    }

                    boolean success = gameManager.startNewGame(currentFileName.startsWith("休闲随机"));
                    if (success) {
                        refreshUI();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("⚠️ 无法生成题目")
                                .setMessage("当前题库中没有符合该条件的题目。\n\n提示：txt题库的格式必须严格为 \" / \" (带空格) 才能被识别为除法运算。")
                                .setPositiveButton("我知道了", null)
                                .show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }


    private void syncFromGitHub() {
        Toast.makeText(this, "正在连接 GitHub...", Toast.LENGTH_SHORT).show();
        repository.syncFromGitHub(new ProblemRepository.SyncCallback() {
            @Override
            public void onSuccess(int count) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "更新完成，下载了 " + count + " 个文件", Toast.LENGTH_LONG).show();
                    initSidebar();
                });
            }
            @Override
            public void onFail(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "更新失败: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void loadProblemSet(String fileName) {
        try {
            List<Problem> problems = repository.loadProblemSet(fileName);
            gameManager.setProblemSet(problems);
            currentFileName = fileName.replace(".txt", "");
            btnMenu.setText("☰ 模式: " + currentFileName);
            Toast.makeText(this, "加载成功", Toast.LENGTH_SHORT).show();
            startNewGameLocal();
        } catch (Exception e) {
            e.printStackTrace();
            switchToRandomMode(4);
        }
    }

    private void loadFirstAvailableFile() {
        List<String> files = repository.getAvailableFiles();
        if (!files.isEmpty()) {
            loadProblemSet(files.get(0));
        } else {
            switchToRandomMode(4);
        }
    }

    private void switchToRandomMode(int count) {
        gameManager.currentNumberCount = count;
        // 更新显示的模式名称
        currentFileName = "休闲随机(" + count + "数)";
        btnMenu.setText("☰ 模式: " + currentFileName);
        startNewGameLocal();
    }

    private void showHelpDialog() {
        CharSequence helpContent = MarkdownUtils.loadMarkdownFromAssets(this, "help.md");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("游戏指南")
                .setMessage(helpContent)
                .setPositiveButton("开始挑战", null)
                .create();
        dialog.show();
        TextView msgView = dialog.findViewById(android.R.id.message);
        if (msgView != null) {
            msgView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            msgView.setLinkTextColor(Color.BLUE);
        }
    }

    private void doTry() {
        List<Fraction> nums = new ArrayList<>();
        List<Integer> idxs = new ArrayList<>();
        // 收集当前有效的卡片
        for(int i=0; i<5; i++) {
            if(gameManager.cardValues[i] != null) {
                nums.add(gameManager.cardValues[i]);
                idxs.add(i);
            }
        }

        if(nums.size() < 2) return;

        // 暴力遍历所有两两组合
        for(int i=0; i<nums.size(); i++) {
            for(int j=0; j<nums.size(); j++) {
                if(i == j) continue;
                Fraction a = nums.get(i);
                Fraction b = nums.get(j);

                Fraction[] results = {a.add(b), a.sub(b), a.multiply(b), (b.num!=0 ? a.divide(b) : null)};

                for(Fraction r : results) {
                    if(r == null) continue;
                    List<Fraction> nextStepNums = new ArrayList<>();
                    nextStepNums.add(r);
                    for(int k=0; k<nums.size(); k++) {
                        if(k!=i && k!=j) nextStepNums.add(nums.get(k));
                    }

                    if(Solver.solve(nextStepNums) != null) {
                        resetSelection();
                        // --- 修复: 使用 setCardColor 保持圆角 ---
                        setCardColor(cardViews[idxs.get(i)], Color.parseColor("#FFC0CB")); // 粉色
                        setCardColor(cardViews[idxs.get(j)], Color.parseColor("#FFC0CB"));
                        Toast.makeText(this, "试试这两个", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }
        Toast.makeText(this, "当前局面可能无解，建议撤销", Toast.LENGTH_SHORT).show();
    }

    private void showStructureHint() {
        String sol = gameManager.getOrCalculateSolution();
        if (sol == null) {
            Toast.makeText(this, "无解或计算中", Toast.LENGTH_SHORT).show();
            return;
        }
        String struct = sol.replaceAll("\\d+/\\d+|\\d+", "🐈");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("结构提示")
                .setMessage(struct)
                .setPositiveButton("OK", null)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            params.y = 200;
            dialog.getWindow().setAttributes(params);
        }
    }

    // --- 修复: 选中逻辑使用 setCardColor ---
    private void selectCard(int index) {
        // 重置颜色 (灰色 #CCCCCC)
        for(ViewGroup v : cardViews) setCardColor(v, Color.parseColor("#CCCCCC"));

        selectedFirstIndex = index;

        // 选中颜色 (绿色)
        if (index != -1) setCardColor(cardViews[index], Color.GREEN);
    }

    private void resetSelection() {
        selectCard(-1);
        selectedOperator = null;
        btnAdd.setBackgroundColor(Color.LTGRAY);
        btnSub.setBackgroundColor(Color.LTGRAY);
        btnMul.setBackgroundColor(Color.LTGRAY);
        btnDiv.setBackgroundColor(Color.LTGRAY);
    }

    private void updateScoreBoard() {
        tvScore.setText("已解: " + gameManager.solvedCount);
        long totalSeconds = (System.currentTimeMillis() - gameStartTime) / 1000;
        long avg = gameManager.solvedCount > 0 ? totalSeconds / gameManager.solvedCount : 0;
        tvAvgTime.setText("平均: " + avg + "s");
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long levelSeconds = (now - startTime) / 1000;
                tvTimer.setText(levelSeconds + "s");
                updateScoreBoard();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void initListeners() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        for (int i = 0; i < 5; i++) {
            final int idx = i;
            // 绑定到 ViewGroup
            cardViews[i].setOnClickListener(v -> onCardClicked(idx));
        }

        View.OnClickListener opListener = v -> {
            String op = "+";
            if (v == btnSub) op = "-";
            else if (v == btnMul) op = "*";
            else if (v == btnDiv) op = "/";

            if (selectedFirstIndex == -1) return;
            resetOpColors();
            if (op.equals(selectedOperator)) selectedOperator = null;
            else {
                selectedOperator = op;
                v.setBackgroundColor(Color.BLUE);
            }
        };
        btnAdd.setOnClickListener(opListener);
        btnSub.setOnClickListener(opListener);
        btnMul.setOnClickListener(opListener);
        btnDiv.setOnClickListener(opListener);

        btnUndo.setOnClickListener(v -> { if(gameManager.undo()) { refreshUI(); resetSelection(); } });
        btnRedo.setOnClickListener(v -> { if(gameManager.redo()) { refreshUI(); resetSelection(); } });
        btnReset.setOnClickListener(v -> { gameManager.resetCurrentLevel(); refreshUI(); resetSelection(); Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show(); });

        btnSkip.setOnClickListener(v -> startNewGameLocal());
        btnTry.setOnClickListener(v -> doTry());
        btnHintStruct.setOnClickListener(v -> showStructureHint());

        btnAnswer.setOnClickListener(v -> {
            String sol = gameManager.getOrCalculateSolution();
            new AlertDialog.Builder(this).setTitle("答案").setMessage(sol!=null?sol:"无解").setPositiveButton("OK", null).show();
        });

        btnShare.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder("24点挑战:\n");
            for (Fraction f : gameManager.cardValues) if (f!=null) sb.append("🐈").append(f).append("\n");
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("24Game", sb.toString()));
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
        });
    }

    private void resetOpColors() {
        btnAdd.setBackgroundColor(Color.LTGRAY);
        btnSub.setBackgroundColor(Color.LTGRAY);
        btnMul.setBackgroundColor(Color.LTGRAY);
        btnDiv.setBackgroundColor(Color.LTGRAY);
    }
}
