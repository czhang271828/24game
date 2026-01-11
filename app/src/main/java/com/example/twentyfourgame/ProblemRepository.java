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

    public void syncFromGitHub(SyncCallback callback) {
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

    public List<Problem> loadProblemSet(String fileName) throws Exception {
        List<Problem> problems = new ArrayList<>();
        InputStream is = getFileInputStream(fileName);
        if (is == null) throw new Exception("File not found");

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        // --- 修改：使用更宽容的正则，只抓取方括号内的内容 ---
        Pattern listPattern = Pattern.compile("\\[(.*?)\\]");

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // 简单分割数字部分和解答部分
            String[] parts = line.split("->");
            if (parts.length < 2) continue;

            Matcher m = listPattern.matcher(parts[0]);
            // 注意：某些格式开头可能是 [序号] [数字]，所以我们找最后一个匹配的 [] 作为数字列表
            String rawNumsStr = null;
            while (m.find()) {
                rawNumsStr = m.group(1); // 持续更新，取最后一个方括号内容
            }

            if (rawNumsStr != null) {
                // 清洗数据：去除单引号、双引号、空格
                String[] rawNums = rawNumsStr.split(",");
                List<Fraction> fracs = new ArrayList<>();
                for (String s : rawNums) {
                    String cleanNum = s.trim().replace("\'", "").replace("\"", "");
                    if (!cleanNum.isEmpty()) {
                        try {
                            fracs.add(Fraction.parse(cleanNum));
                        } catch (Exception e) {
                            // 忽略解析错误的数字
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
