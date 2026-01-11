package com.example.twentyfourgame;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    static class Problem {
        List<Fraction> numbers;
        String solution;
        public Problem(List<Fraction> n, String s) { this.numbers = n; this.solution = s; }
    }

    private static final String GITHUB_API_URL = "https://api.github.com/repos/zhangchenchengSJTU/LA-2025-2026-1--MATH1205H-04/contents/assets/24";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvScore, tvTimer, tvAvgTime;
    private Button[] cardButtons = new Button[5];
    private Button btnAdd, btnSub, btnMul, btnDiv;
    private Button btnUndo, btnReset, btnRedo, btnMenu;
    private Button btnTry, btnHintStruct, btnAnswer, btnShare, btnSkip;

    private Fraction[] cardValues = new Fraction[5];
    private Fraction[] initialValues = new Fraction[5];
    private Stack<Fraction[]> undoStack = new Stack<>();
    private Stack<Fraction[]> redoStack = new Stack<>();
    private int solvedCount = 0;
    private long startTime, gameStartTime;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private List<Problem> problemSet = new ArrayList<>();
    private int currentProblemIndex = -1;
    private String currentFileName = "随机(4数)";
    private String currentLevelSolution = null;
    private int currentNumberCount = 4;
    private int selectedFirstIndex = -1;
    private String selectedOperator = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initSidebar();
        initListeners();

        gameStartTime = System.currentTimeMillis();
        loadFirstAvailableFile();
        startTimer();
    }

    // 读取 assets 下的 md 文件并转为简单的 HTML
    private CharSequence loadMarkdownFromAssets(String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = getAssets().open(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                // ... (前面的 1, 2, 3 步代码保持不变) ...

                // 1. 处理标题 (# )
                if (line.startsWith("# ")) {
                    line = "<h3>" + line.substring(2) + "</h3>";
                }
                // 2. 处理分割线 (---)
                else if (line.startsWith("---")) {
                    line = "<hr>";
                }
                // 3. 处理列表 (- )
                else if (line.startsWith("- ")) {
                    line = "• " + line.substring(2) + "<br>";
                }

                // 4. 处理粗体 (**文字**)
                line = line.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");

                // --- 新增：5. 处理链接 [文字](url) ---
                // 正则解释：\[(.*?)\] 匹配方括号文字，\((.*?)\) 匹配圆括号链接
                line = line.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href=\"$2\">$1</a>");

                // 6. 普通换行 (Markdown换行需要加<br>)
                if (!line.startsWith("<h") && !line.startsWith("<hr") && !line.isEmpty()) {
                    line += "<br>";
                }

                sb.append(line).append("\n");
            }
            br.close();
        } catch (Exception e) {
            return "无法读取说明书: " + e.getMessage();
        }
        // 使用 Android 自带的 HTML 解析器渲染
        return Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY);
    }

    private void showHelpDialog() {
        CharSequence helpContent = loadMarkdownFromAssets("help.md");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("游戏指南")
                .setMessage(helpContent)
                .setPositiveButton("开始挑战", null)
                .create(); // 这里改用 create() 先生成对象，不直接 show

        dialog.show(); // 先显示出来

        // --- 关键代码：让链接可点击 ---
        // 获取 AlertDialg 显示文字的 TextView (系统 ID 为 android.R.id.message)
        TextView msgView = dialog.findViewById(android.R.id.message);
        if (msgView != null) {
            // 激活链接点击功能
            msgView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            // 可选：把链接颜色设为蓝色，防止默认颜色看不清
            msgView.setLinkTextColor(Color.BLUE);
        }
    }



    // --- 网络同步 ---
    private void syncFromGitHub() {
        Toast.makeText(this, "正在连接 GitHub...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String jsonStr = downloadString(GITHUB_API_URL);
                if (jsonStr == null) throw new Exception("无法获取列表");
                JSONArray jsonArray = new JSONArray(jsonStr);
                int downloadCount = 0;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String name = item.getString("name");
                    String downloadUrl = item.getString("download_url");
                    if (name.endsWith(".txt")) {
                        String content = downloadString(downloadUrl);
                        if (content != null) {
                            saveToInternalStorage(name, content);
                            downloadCount++;
                        }
                    }
                }
                int finalCount = downloadCount;
                runOnUiThread(() -> {
                    Toast.makeText(this, "更新完成，下载了 " + finalCount + " 个文件", Toast.LENGTH_LONG).show();
                    initSidebar();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "更新失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String downloadString(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                is.close();
                return sb.toString();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private void saveToInternalStorage(String fileName, String content) {
        try {
            FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private InputStream getFileInputStream(String fileName) {
        try {
            File file = new File(getFilesDir(), fileName);
            if (file.exists()) return new FileInputStream(file);
            return getAssets().open(fileName);
        } catch (Exception e) { return null; }
    }

    // --- 侧边栏 ---
    private void initSidebar() {
        Menu menu = navigationView.getMenu();
        menu.clear();

        // 功能区
        menu.add(Menu.NONE, 888, Menu.NONE, "📖 游戏说明书"); // 新增说明书入口
        menu.add(Menu.NONE, 999, Menu.NONE, "☁️ 从 GitHub 更新题库");
        menu.add(Menu.NONE, 0, Menu.NONE, "🎲 随机 (4数)");
        menu.add(Menu.NONE, 1, Menu.NONE, "🎲 随机 (5数)");

        Set<String> fileSet = new HashSet<>();
        try {
            String[] assets = getAssets().list("");
            if (assets != null) for (String f : assets) if (f.endsWith(".txt")) fileSet.add(f);
            String[] downloaded = fileList();
            if (downloaded != null) for (String f : downloaded) if (f.endsWith(".txt")) fileSet.add(f);
        } catch (Exception e) {}

        List<String> sortedFiles = new ArrayList<>(fileSet);
        Collections.sort(sortedFiles);

        int id = 2;
        for (String f : sortedFiles) menu.add(Menu.NONE, id++, Menu.NONE, "📄 " + f);

        navigationView.setNavigationItemSelectedListener(item -> {
            String t = item.getTitle().toString();
            if (t.contains("游戏说明书")) {
                showHelpDialog(); // 显示说明书
            } else if (t.contains("从 GitHub 更新")) {
                syncFromGitHub();
            } else {
                if (t.equals("🎲 随机 (4数)")) switchToRandomMode(4);
                else if (t.equals("🎲 随机 (5数)")) switchToRandomMode(5);
                else loadProblemSet(t.substring(t.indexOf(" ") + 1));
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btn_menu);
        tvScore = findViewById(R.id.tv_score);
        tvTimer = findViewById(R.id.tv_timer);
        tvAvgTime = findViewById(R.id.tv_avg_time);

        cardButtons[0] = findViewById(R.id.card_1);
        cardButtons[1] = findViewById(R.id.card_2);
        cardButtons[2] = findViewById(R.id.card_3);
        cardButtons[3] = findViewById(R.id.card_4);
        cardButtons[4] = findViewById(R.id.card_5);

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

    private void initListeners() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            cardButtons[i].setOnClickListener(v -> onCardClicked(idx));
        }
        btnAdd.setOnClickListener(v -> toggleOperator("+", btnAdd));
        btnSub.setOnClickListener(v -> toggleOperator("-", btnSub));
        btnMul.setOnClickListener(v -> toggleOperator("*", btnMul));
        btnDiv.setOnClickListener(v -> toggleOperator("/", btnDiv));

        btnUndo.setOnClickListener(v -> undo());
        btnReset.setOnClickListener(v -> resetLevel());
        btnRedo.setOnClickListener(v -> recover());

        btnTry.setOnClickListener(v -> doTry());
        btnHintStruct.setOnClickListener(v -> showStructureHint());
        btnAnswer.setOnClickListener(v -> showAnswer());
        btnShare.setOnClickListener(v -> doShare());
        btnSkip.setOnClickListener(v -> startNewGame());
    }

    private void performCalculation(int idx1, int idx2, String op) {
        Fraction f1 = cardValues[idx1];
        Fraction f2 = cardValues[idx2];
        Fraction result = null;
        try {
            switch (op) {
                case "+": result = f1.add(f2); break;
                case "-": result = f1.sub(f2); break;
                case "*": result = f1.multiply(f2); break;
                case "/": result = f1.divide(f2); break;
            }
        } catch (ArithmeticException e) {
            Toast.makeText(this, "除数不能为0", Toast.LENGTH_SHORT).show(); return;
        }
        saveToUndo();
        redoStack.clear();
        cardValues[idx2] = result;
        cardValues[idx1] = null;
        resetUIStyles();
        selectCard(idx2);
        checkWin();
    }

    private void saveToUndo() {
        Fraction[] state = new Fraction[5];
        for (int i=0; i<5; i++) state[i] = cardValues[i];
        undoStack.push(state);
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            Fraction[] current = new Fraction[5];
            for(int i=0; i<5; i++) current[i] = cardValues[i];
            redoStack.push(current);
            Fraction[] prev = undoStack.pop();
            for(int i=0; i<5; i++) cardValues[i] = prev[i];
            resetUIStyles();
        }
    }

    private void recover() {
        if (!redoStack.isEmpty()) {
            saveToUndo();
            Fraction[] next = redoStack.pop();
            for(int i=0; i<5; i++) cardValues[i] = next[i];
            resetUIStyles();
        }
    }

    private void resetLevel() {
        saveToUndo();
        redoStack.clear();
        for(int i=0; i<5; i++) cardValues[i] = initialValues[i];
        resetUIStyles();
        Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show();
    }

    private void refreshCards() {
        if (currentNumberCount == 4) {
            cardButtons[4].setVisibility(View.GONE);
        } else {
            cardButtons[4].setVisibility(View.VISIBLE);
        }
        for (int i = 0; i < 5; i++) {
            if (currentNumberCount == 4 && i == 4) continue;
            if (cardValues[i] != null) {
                cardButtons[i].setVisibility(View.VISIBLE);
                cardButtons[i].setText(cardValues[i].toString());
            } else {
                cardButtons[i].setVisibility(View.INVISIBLE);
            }
        }
    }

    private void startNewGame() {
        generateLevel();
        for(int i=0; i<5; i++) initialValues[i] = cardValues[i];
        selectedFirstIndex = -1;
        selectedOperator = null;
        undoStack.clear();
        redoStack.clear();
        startTime = System.currentTimeMillis();
        resetUIStyles();
    }

    private void generateLevel() {
        for(int i=0; i<5; i++) cardValues[i] = null;
        if (!problemSet.isEmpty()) {
            currentProblemIndex++;
            if (currentProblemIndex >= problemSet.size()) {
                currentProblemIndex = 0;
                Collections.shuffle(problemSet);
            }
            Problem prob = problemSet.get(currentProblemIndex);
            currentNumberCount = prob.numbers.size();
            for (int i = 0; i < currentNumberCount; i++) cardValues[i] = prob.numbers.get(i);
            currentLevelSolution = prob.solution;
        } else {
            Random rand = new Random();
            while(true) {
                List<Fraction> nums = new ArrayList<>();
                for(int i=0; i<currentNumberCount; i++) nums.add(new Fraction(rand.nextInt(13)+1, 1));
                String sol = Solver.solve(nums);
                if(sol!=null) {
                    for(int i=0; i<currentNumberCount; i++) cardValues[i] = nums.get(i);
                    currentLevelSolution = sol;
                    break;
                }
            }
        }
        refreshCards();
    }

    private void loadFirstAvailableFile() {
        String[] savedFiles = fileList();
        if (savedFiles != null) {
            for(String f : savedFiles) if(f.endsWith(".txt")) { loadProblemSet(f); return; }
        }
        try {
            String[] files = getAssets().list("");
            if (files!=null) for(String f : files) if(f.endsWith(".txt")) { loadProblemSet(f); return; }
        } catch(Exception e){}
        switchToRandomMode(4);
    }

    private void switchToRandomMode(int count) {
        problemSet.clear();
        currentNumberCount = count;
        currentFileName = "随机(" + count + "数)";
        btnMenu.setText("☰ 模式: " + currentFileName);
        startNewGame();
    }

    private void loadProblemSet(String fileName) {
        problemSet.clear();
        try {
            InputStream is = getFileInputStream(fileName);
            if (is == null) throw new Exception("File not found");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            Pattern listPattern = Pattern.compile("\\[\'(.*?)\'\\]");

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("->");
                if (parts.length < 2) continue;

                String numsPart = parts[0];
                String solution = parts[1].trim();
                Matcher m = listPattern.matcher(numsPart);
                if (m.find()) {
                    String[] rawNums = m.group(1).split(",");
                    List<Fraction> fracs = new ArrayList<>();
                    for (String s : rawNums) fracs.add(Fraction.parse(s.trim().replace("\'", "")));
                    if (fracs.size() == 4 || fracs.size() == 5) {
                        problemSet.add(new Problem(fracs, solution));
                    }
                }
            }
            br.close();
            Collections.shuffle(problemSet);
            currentFileName = fileName.replace(".txt", "");
            btnMenu.setText("☰ 模式: " + currentFileName);
            Toast.makeText(this, "加载成功", Toast.LENGTH_SHORT).show();
            startNewGame();
        } catch (Exception e) {
            e.printStackTrace();
            switchToRandomMode(4);
        }
    }

    private void checkWin() {
        int count = 0;
        Fraction last = null;
        for (Fraction f : cardValues) if (f != null) { count++; last = f; }
        if (count == 1 && last.isValue(24)) {
            Toast.makeText(this, "成功！", Toast.LENGTH_SHORT).show();
            solvedCount++;
            updateScoreBoard();
            new Handler().postDelayed(this::startNewGame, 1200);
        }
    }

    private void updateScoreBoard() {
        tvScore.setText("已解: " + solvedCount);
        long totalSeconds = (System.currentTimeMillis() - gameStartTime) / 1000;
        long avg = solvedCount > 0 ? totalSeconds / solvedCount : 0;
        tvAvgTime.setText("平均: " + avg + "s");
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long levelSeconds = (now - startTime) / 1000;
                tvTimer.setText(levelSeconds + "s");
                long totalSeconds = (now - gameStartTime) / 1000;
                long avg = solvedCount > 0 ? totalSeconds / solvedCount : 0;
                tvAvgTime.setText("平均: " + avg + "s");
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void doTry() {
        List<Fraction> nums = new ArrayList<>();
        List<Integer> idxs = new ArrayList<>();
        for(int i=0; i<5; i++) if(cardValues[i]!=null) { nums.add(cardValues[i]); idxs.add(i); }
        if(nums.size()<2) return;
        for(int i=0; i<nums.size(); i++) {
            for(int j=0; j<nums.size(); j++) {
                if(i==j) continue;
                Fraction a=nums.get(i), b=nums.get(j);
                Fraction[] res = {a.add(b), a.sub(b), a.multiply(b), (b.num!=0?a.divide(b):null)};
                for(Fraction r : res) {
                    if(r==null) continue;
                    List<Fraction> next = new ArrayList<>();
                    next.add(r);
                    for(int k=0; k<nums.size(); k++) if(k!=i && k!=j) next.add(nums.get(k));
                    if(Solver.solve(next)!=null) {
                        resetSelection();
                        cardButtons[idxs.get(i)].setBackgroundColor(Color.parseColor("#FFC0CB"));
                        cardButtons[idxs.get(j)].setBackgroundColor(Color.parseColor("#FFC0CB"));
                        Toast.makeText(this, "试试这两个", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }
        Toast.makeText(this, "建议撤销", Toast.LENGTH_SHORT).show();
    }

    private String getCurrentSolution() {
        int count = 0;
        for (Fraction f : cardValues) if (f != null) count++;
        if (count == currentNumberCount && currentLevelSolution != null) return currentLevelSolution;
        List<Fraction> nums = new ArrayList<>();
        for (Fraction f : cardValues) if (f != null) nums.add(f);
        return Solver.solve(nums);
    }

    private void onCardClicked(int index) {
        if (selectedFirstIndex == -1) selectCard(index);
        else if (selectedFirstIndex == index) resetSelection();
        else {
            if (selectedOperator == null) selectCard(index);
            else performCalculation(selectedFirstIndex, index, selectedOperator);
        }
    }

    private void selectCard(int index) {
        for(Button b : cardButtons) b.setBackgroundColor(Color.LTGRAY);
        selectedFirstIndex = index;
        if (index != -1) cardButtons[index].setBackgroundColor(Color.GREEN);
    }

    private void toggleOperator(String op, Button btn) {
        if (selectedFirstIndex == -1) return;
        btnAdd.setBackgroundColor(Color.LTGRAY);
        btnSub.setBackgroundColor(Color.LTGRAY);
        btnMul.setBackgroundColor(Color.LTGRAY);
        btnDiv.setBackgroundColor(Color.LTGRAY);
        if (op.equals(selectedOperator)) selectedOperator = null;
        else {
            selectedOperator = op;
            btn.setBackgroundColor(Color.BLUE);
        }
    }

    private void resetSelection() {
        selectCard(-1);
        selectedOperator = null;
        btnAdd.setBackgroundColor(Color.LTGRAY);
        btnSub.setBackgroundColor(Color.LTGRAY);
        btnMul.setBackgroundColor(Color.LTGRAY);
        btnDiv.setBackgroundColor(Color.LTGRAY);
    }

    private void resetUIStyles() { resetSelection(); refreshCards(); }

    private void showStructureHint() {
        String sol = getCurrentSolution();
        if (sol == null) { Toast.makeText(this, "无解", Toast.LENGTH_SHORT).show(); return; }
        String struct = sol.replaceAll("\\d+/\\d+|\\d+", "🐈");
        new AlertDialog.Builder(this).setTitle("结构提示").setMessage(struct).setPositiveButton("OK", null).show();
    }

    private void showAnswer() {
        String sol = getCurrentSolution();
        new AlertDialog.Builder(this).setTitle("答案").setMessage(sol!=null?sol:"无解").setPositiveButton("OK", null).show();
    }

    private void doShare() {
        StringBuilder sb = new StringBuilder("24点挑战:\n");
        for (Fraction f : cardValues) if (f!=null) sb.append("🐈").append(f).append("\n");
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("24Game", sb.toString()));
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }
}
