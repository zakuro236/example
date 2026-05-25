package com.example.test;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 对话框/弹窗模板检测测试
 * 截取当前屏幕，用 file3 中所有模板进行匹配，输出每个模板的匹配度
 */
public class DialogDetectionTest {

    private static String adb;
    private static String device;
    private static final String SCREENSHOT_PATH = "./temp_screen.png";
    private static final Random random = new Random();

    static {
        // 加载 OpenCV DLL
        String[] dllPaths = {
            "D:\\MyDemo\\opencv_java490.dll",
            "./opencv_java490.dll",
            System.getProperty("user.dir") + "\\opencv_java490.dll"
        };
        for (String path : dllPaths) {
            File f = new File(path);
            if (f.exists()) {
                System.out.println("✅ OpenCV DLL: " + f.getAbsolutePath());
                System.load(f.getAbsolutePath());
                break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 对话框/弹窗模板检测测试 ===\n");

        // 1. 加载 ADB 配置
        com.example.自动配置.AdbConfig.init();
        adb = com.example.自动配置.AdbConfig.getAdbPath();
        device = com.example.自动配置.AdbConfig.getDevice();

        if (adb == null || adb.isEmpty()) {
            System.out.println("❌ ADB 未配置");
            return;
        }
        System.out.println("✅ ADB: " + adb);
        System.out.println("   设备: " + device);

        // 2. 取得屏幕尺寸（用于区域裁剪）
        int scrW = getScreenSize(adb, device, true);
        int scrH = getScreenSize(adb, device, false);
        Rect bottomRight = new Rect(scrW / 2, scrH / 2, scrW / 2, scrH / 2);
        System.out.println("   屏幕: " + scrW + "x" + scrH);
        System.out.println("   右下区域: x=" + bottomRight.x + " y=" + bottomRight.y
                           + " w=" + bottomRight.width + " h=" + bottomRight.height);

        // 3. 加载模板
        String baseDir = getProgramDirectory();
        String templateDir = baseDir + File.separator + "templates" + File.separator + "file3" + File.separator;
        System.out.println("\n📦 加载模板目录: " + templateDir);

        File dir = new File(templateDir);
        Map<String, Mat> templates = new LinkedHashMap<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File f : files) {
                    Mat mat = imreadSafe(f.getAbsolutePath());
                    if (!mat.empty()) {
                        templates.put(f.getName(), mat);
                        System.out.println("   ✅ " + f.getName() + " (" + mat.cols() + "x" + mat.rows() + ")");
                    }
                }
            }
        }
        System.out.println("   共加载 " + templates.size() + " 个模板\n");

        if (templates.isEmpty()) {
            System.out.println("❌ 没有找到模板文件！");
            return;
        }

        // 4. 循环检测（按任意键停止）
        System.out.println("=".repeat(60));
        System.out.println("  开始每 3 秒截屏检测，Ctrl+C 停止");
        System.out.println("=".repeat(60));

        for (int round = 1; ; round++) {
            System.out.println("\n--- 第 " + round + " 轮检测 ---");

            if (!takeScreenshot()) {
                System.out.println("   ⚠️ 截图失败，2秒后重试...");
                Thread.sleep(2000);
                continue;
            }

            Mat screenshot = Imgcodecs.imread(SCREENSHOT_PATH);
            if (screenshot.empty()) {
                System.out.println("   ⚠️ 读截图失败");
                Thread.sleep(2000);
                continue;
            }

            // 收集所有匹配结果
            List<MatchResult> results = new ArrayList<>();

            for (Map.Entry<String, Mat> entry : templates.entrySet()) {
                String name = entry.getKey();
                Mat tpl = entry.getValue();

                // === 全图匹配 ===
                double fullScore = matchTemplate(screenshot, tpl);
                results.add(new MatchResult(name, "全图", fullScore));

                // === 右下区域匹配 ===
                double regionScore = matchTemplateRegion(screenshot, tpl, bottomRight);
                results.add(new MatchResult(name, "右下", regionScore));
            }

            screenshot.release();

            // 按分数降序排列
            results.sort((a, b) -> Double.compare(b.score, a.score));

            // 输出 Top 10
            System.out.println("   📊 匹配度排行（Top 10）：");
            System.out.println("   " + "-".repeat(55));
            System.out.printf("   %-35s %-6s %8s\n", "模板名", "区域", "匹配度");
            System.out.println("   " + "-".repeat(55));
            for (int i = 0; i < Math.min(10, results.size()); i++) {
                MatchResult r = results.get(i);
                String flag = r.score >= 0.60 ? " ⬅️ 高" : (r.score >= 0.45 ? " ⬅️ 中" : "");
                System.out.printf("   %-35s %-6s %8.4f%s\n", r.name, r.region, r.score, flag);
            }

            // 输出所有达到阈值的
            System.out.println("\n   🎯 可能匹配的（score >= 0.45）：");
            boolean found = false;
            for (MatchResult r : results) {
                if (r.score >= 0.45) {
                    found = true;
                    System.out.printf("      %-35s [%4s] 匹配度=%.4f\n", r.name, r.region, r.score);
                }
            }
            if (!found) {
                System.out.println("      (无)");
            }

            System.out.println("\n   ⏳ 3秒后下一轮...");
            Thread.sleep(3000);
        }
    }

    // ==================== 工具方法 ====================

    static void execAdb(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(cmd);
        new Thread(() -> { try { p.getInputStream().transferTo(OutputStream.nullOutputStream()); } catch (Exception e) {} }).start();
        new Thread(() -> { try { p.getErrorStream().transferTo(OutputStream.nullOutputStream()); } catch (Exception e) {} }).start();
        p.waitFor();
    }

    static boolean takeScreenshot() {
        try {
            new File(SCREENSHOT_PATH).delete();
            execAdb(adb + " -s " + device + " shell screencap /sdcard/temp.png");
            Thread.sleep(200);
            execAdb(adb + " -s " + device + " pull /sdcard/temp.png " + SCREENSHOT_PATH);
            Runtime.getRuntime().exec(adb + " -s " + device + " shell rm /sdcard/temp.png");
            return new File(SCREENSHOT_PATH).exists() && new File(SCREENSHOT_PATH).length() > 0;
        } catch (Exception e) {
            System.out.println("   ❌ 截图异常: " + e.getMessage());
            return false;
        }
    }

    static Mat removeBackgroundToWhite(Mat src) {
        Mat gray = new Mat();
        Mat mask = new Mat();
        Mat result = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(gray, mask, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 25, 10);
        Imgproc.cvtColor(mask, result, Imgproc.COLOR_GRAY2BGR);
        gray.release();
        mask.release();
        return result;
    }

    /** 全图匹配，返回最高分数 */
    static double matchTemplate(Mat screenshot, Mat template) {
        Mat scrBW = null, tplBW = null, result = null;
        try {
            scrBW = removeBackgroundToWhite(screenshot);
            tplBW = removeBackgroundToWhite(template);
            result = new Mat();
            Imgproc.matchTemplate(scrBW, tplBW, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            return mmr.maxVal;
        } catch (Exception e) {
            return 0;
        } finally {
            if (scrBW != null) scrBW.release();
            if (tplBW != null) tplBW.release();
            if (result != null) result.release();
        }
    }

    /** 指定区域匹配，返回最高分数 */
    static double matchTemplateRegion(Mat screenshot, Mat template, Rect region) {
        Mat scrBW = null, tplBW = null, result = null;
        try {
            // 裁剪 ROI
            int rx = Math.max(0, region.x);
            int ry = Math.max(0, region.y);
            int rw = Math.min(screenshot.cols() - rx, region.width);
            int rh = Math.min(screenshot.rows() - ry, region.height);
            Rect roi = new Rect(rx, ry, rw, rh);
            Mat roiMat = new Mat(screenshot, roi);

            scrBW = removeBackgroundToWhite(roiMat);
            tplBW = removeBackgroundToWhite(template);
            result = new Mat();
            Imgproc.matchTemplate(scrBW, tplBW, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            return mmr.maxVal;
        } catch (Exception e) {
            return 0;
        } finally {
            if (scrBW != null) scrBW.release();
            if (tplBW != null) tplBW.release();
            if (result != null) result.release();
        }
    }

    static int getScreenSize(String adb, String device, boolean isWidth) throws Exception {
        Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell wm size");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
            while (r.readLine() != null) {}
        }
        p.waitFor();
        String output = sb.toString().trim();
        if (output.contains("x")) {
            String[] parts = output.split("x");
            return Integer.parseInt(parts[isWidth ? 0 : 1].replaceAll("[^0-9]", ""));
        }
        return isWidth ? 1280 : 720;
    }

    static String getProgramDirectory() {
        String userDir = System.getProperty("user.dir");
        userDir = userDir.replaceAll("[\\\\/]+\\.?$", "");
        if (new File(userDir, "templates").exists()) return userDir;
        File parent = new File(userDir).getParentFile();
        while (parent != null) {
            if (new File(parent, "templates").exists()) return parent.getAbsolutePath();
            parent = parent.getParentFile();
        }
        return userDir;
    }

    static Mat imreadSafe(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("   ⚠️ 文件不存在: " + path);
            return new Mat();
        }
        try {
            Path tempPath = Files.createTempFile("tpl_", ".png");
            Files.copy(file.toPath(), tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Mat mat = Imgcodecs.imread(tempPath.toString());
            Files.deleteIfExists(tempPath);
            return mat;
        } catch (Exception e) {
            System.out.println("   ⚠️ 读取失败: " + path);
            return new Mat();
        }
    }

    // ==================== 内部类 ====================

    static class MatchResult {
        String name;
        String region;
        double score;

        MatchResult(String name, String region, double score) {
            this.name = name;
            this.region = region;
            this.score = score;
        }
    }
}
