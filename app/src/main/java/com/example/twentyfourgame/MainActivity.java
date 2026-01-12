// 完全替换你的 MainActivity.java 文件内容
package com.example.twentyfourgame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvScore, tvTimer, tvAvgTime;

    private ViewGroup[] cardViews = new ViewGroup[5];
    private TextView[] tvNums = new TextView[5];
    private TextView[] tvDenoms = new TextView[5];
    private View[] dividers = new View[5];

    private Button btnAdd, btnSub, btnMul, btnDiv;
    private Button btnUndo, btnReset, btnRedo, btnMenu;
    private Button btnTry, btnHintStruct, btnAnswer, btnShare, btnSkip;

    private GameManager gameManager;
    private ProblemRepository repository;

    private long startTime, gameStartTime;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private int selectedFirstIndex = -1;
    private String selectedOperator = null;
    private String currentModeName = "休闲随机(4数)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        repository = new ProblemRepository(this);
        gameManager = new GameManager();

        initViews();
        initSidebar(toolbar); // 传入 toolbar
        initListeners();

        gameStartTime = System.currentTimeMillis();
        switchToRandomMode(4); // 默认启动
        startTimer();
    }

    // ⭐【修改】新增 onBackPressed 方法
    @Override
    public void onBackPressed() {
        // 当返回按钮被按下时，如果抽屉是打开的，则先关闭抽屉
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // 否则，执行默认的返回操作
            super.onBackPressed();
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btn_menu);
        tvScore = findViewById(R.id.tv_score);
        tvTimer = findViewById(R.id.tv_timer);
        tvAvgTime = findViewById(R.id.tv_avg_time);

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

        btnAdd = findViewById(R.id.btn_op_add); btnSub = findViewById(R.id.btn_op_sub);
        btnMul = findViewById(R.id.btn_op_mul); btnDiv = findViewById(R.id.btn_op_div);
        btnUndo = findViewById(R.id.btn_undo); btnReset = findViewById(R.id.btn_reset);
        btnRedo = findViewById(R.id.btn_redo); btnTry = findViewById(R.id.btn_try);
        btnHintStruct = findViewById(R.id.btn_hint_struct); btnAnswer = findViewById(R.id.btn_answer);
        btnShare = findViewById(R.id.btn_share); btnSkip = findViewById(R.id.btn_skip);
    }

    // ⭐【修改】initSidebar 方法的实现
    private void initSidebar(Toolbar toolbar) {
        // 创建 ActionBarDrawerToggle，它会将抽屉和 Toolbar 关联起来
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        // 添加监听器并同步状态，这会在 Toolbar 上显示汉堡包图标
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 为 NavigationView 设置菜单项点击监听器
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initListeners() {
        // ... (您其他的监听器逻辑保持不变)
    }

    // ⭐【修改】实现 OnNavigationItemSelectedListener 接口的方法
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // 在这里处理菜单项的点击事件
        // 例如：
        // if (item.getItemId() == R.id.nav_home) {
        //    // 处理点击 "Home" 的逻辑
        // }

        // 点击菜单项后关闭抽屉
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ... (您所有其他的方法，如 switchToRandomMode, startTimer 等，都保持不变)
}
