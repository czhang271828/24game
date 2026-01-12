package com.example.twentyfourgame;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProblemRepository {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/czhang271828/24game/contents/data";
    private Context context;

    public ProblemRepository(Context context) {
        this.context = context;
    }

    public interface SyncCallback {
        void onSuccess(int count);
        void onFail(String error);
    }

    // ... (syncFromGitHub, clearLocalData 等方法保持不变，省略以节省篇幅) ...
    public void syncFromGitHub(SyncCallback callback) {
        // ... 保持原有逻辑 ...
        new Thread(() -> {
            try {
                clearLocalData();
                String jsonStr = downloadString(GITHUB_API_URL);
                if (jsonStr == null) throw new Exception("无法获取文件列表");

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
                callback.onSuccess(downloadCount);
            } catch (Exception e) {
                e.printStackTrace();
                callback.onFail(e.getMessage());
            }
        }).start();
    }

    private void clearLocalData() {
        File dir = context.getFilesDir();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".txt")) file.delete();
            }
        }
    }

    // --- 核心修复：loadProblemSet ---
    public List<Problem> loadProblemSet(String fileName) throws Exception {
        List<Problem> problems = new ArrayList<>();
        InputStream is = getFileInputStream(fileName);
        if (is == null) throw new Exception("File not found");

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        // 1. 用于提取方括号 [] 内容的正则
        Pattern listPattern = Pattern.compile("\\[(.*?)\\]");

        // 2. 核心分词正则：匹配 (元组) 或 '字符串' 或 "字符串" 或 普通数字
        // Group 1: (1, -1) 形式的元组
        // Group 2: '1+i' 形式的单引号字符串
        // Group 3: "1+i" 形式的双引号字符串
        // Group 4: 123 或 1+i (无引号)
        Pattern tokenPattern = Pattern.compile("\\(([^)]+)\\)|'([^']*)'|\"([^\"]*)\"|([^, ]+)");

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("->");
            if (parts.length < 2) continue;

            // 在 "->" 左侧寻找数字列表 [ ... ]
            Matcher listMatcher = listPattern.matcher(parts[0]);
            String rawNumsStr = null;
            while (listMatcher.find()) {
                rawNumsStr = listMatcher.group(1);
            }

            if (rawNumsStr != null) {
                List<Fraction> fracs = new ArrayList<>();
                Matcher tokenMatcher = tokenPattern.matcher(rawNumsStr);

                while (tokenMatcher.find()) {
                    String cleanNum = null;

                    // Case A: 元组 (1, 1) -> 需要转换为 "1+1i" 格式供 Fraction 解析
                    if (tokenMatcher.group(1) != null) {
                        String tupleContent = tokenMatcher.group(1);
                        cleanNum = parseTupleToComplexString(tupleContent);
                    }
                    // Case B: 单引号字符串 '1+i'
                    else if (tokenMatcher.group(2) != null) {
                        cleanNum = tokenMatcher.group(2);
                    }
                    // Case C: 双引号字符串 "1+i"
                    else if (tokenMatcher.group(3) != null) {
                        cleanNum = tokenMatcher.group(3);
                    }
                    // Case D: 普通数字/无引号字符串 123 或 1+i
                    else if (tokenMatcher.group(4) != null) {
                        cleanNum = tokenMatcher.group(4).trim();
                    }

                    if (cleanNum != null && !cleanNum.isEmpty()) {
                        try {
                            // 无论来源格式如何，最终都转为字符串交给 Fraction.parse
                            fracs.add(Fraction.parse(cleanNum));
                        } catch (Exception e) {
                            // System.out.println("解析失败: " + cleanNum);
                        }
                    }
                }

                // 兼容 3-6 个数的题目
                if (fracs.size() >= 3 && fracs.size() <= 6) {
                    problems.add(new Problem(fracs, parts[1].trim()));
                }
            }
        }
        br.close();
        return problems;
    }

    /**
     * 将元组内容 "1, -1" 转换为标准复数字符串 "1-1i" 或 "1-i"
     */
    private String parseTupleToComplexString(String tupleContent) {
        try {
            String[] parts = tupleContent.split(",");
            if (parts.length == 2) {
                int real = Integer.parseInt(parts[0].trim());
                int img = Integer.parseInt(parts[1].trim());

                if (img == 0) return String.valueOf(real); // 纯实数

                StringBuilder sb = new StringBuilder();
                if (real != 0) sb.append(real);

                if (img > 0) {
                    if (real != 0) sb.append("+");
                    if (img == 1) sb.append("i");
                    else sb.append(img).append("i");
                } else {
                    // img < 0
                    if (img == -1) sb.append("-i");
                    else sb.append(img).append("i");
                }

                // 处理 (0, 1) -> "i", (0, -1) -> "-i" 的边界情况已由上述逻辑覆盖
                // 但如果 real=0 且 img!=0，sb 开头可能是空的，这里不用担心，
                // 比如 (0,1) -> 进入 img>0 分支 -> append("i") -> 返回 "i"
                return sb.toString();
            }
        } catch (Exception e) {
            // 解析失败，原样返回试试
        }
        return tupleContent;
    }

    // ... (getAvailableFiles, downloadString, saveToInternalStorage 等保持不变) ...

    public List<String> getAvailableFiles() {
        Set<String> fileSet = new HashSet<>();
        try {
            String[] assets = context.getAssets().list("");
            if (assets != null) for (String f : assets) if (f.endsWith(".txt")) fileSet.add(f);
            String[] downloaded = context.fileList();
            if (downloaded != null) for (String f : downloaded) if (f.endsWith(".txt")) fileSet.add(f);
        } catch (Exception e) {}
        List<String> sortedFiles = new ArrayList<>(fileSet);
        Collections.sort(sortedFiles);
        return sortedFiles;
    }

    private String downloadString(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        if (conn.getResponseCode() == 200) {
            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                return sb.toString();
            }
        }
        return null;
    }

    private void saveToInternalStorage(String fileName, String content) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes());
        }
    }

    private InputStream getFileInputStream(String fileName) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            if (file.exists()) return new FileInputStream(file);
            return context.getAssets().open(fileName);
        } catch (Exception e) { return null; }
    }
}
