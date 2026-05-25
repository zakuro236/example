package com.example.师门任务;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.*;

public class ShiMenTask {

    private static String adb = "D:\\leidian\\LDPlayer9\\adb.exe";
    private static String device = "emulator-5554";
    private static final String SCREENSHOT_PATH = "./temp_screen.png";
    private static final Random random = new Random();
    private static final Map<String, Mat> templateCache = new HashMap<>();
    private static final String TEMPLATE_PATH = "D:\\MyDemo\\file\\file3\\";

    static {
        loadOpenCvDll();
    }

    private static void loadOpenCvDll() {
        String[] dllPaths = {
            "D:\\MyDemo\\opencv_java490.dll",
            "./opencv_java490.dll",
            System.getProperty("user.dir") + "\\opencv_java490.dll"
        };
        for (String path : dllPaths) {
            File dllFile = new File(path);
            if (dllFile.exists()) {
                System.out.println("✅ OpenCV DLL: " + dllFile.getAbsolutePath());
                System.load(dllFile.getAbsolutePath());
                return;
            }
        }
        System.out.println("⚠️ 尝试默认路径...");
        try { System.load(dllPaths[0]); } catch (UnsatisfiedLinkError e) { System.out.println("❌ DLL加载失败!"); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 师门任务脚本 ===\n");

        preloadTemplates();

        System.out.println("5秒后开始执行...");
        Thread.sleep(5000);

        boolean success = executeFullQuest();

        if (success) {
            System.out.println("\n✅ 师门任务完成！");
        } else {
            System.out.println("\n❌ 执行失败！");
        }

        cleanup();
    }

    private static boolean executeFullQuest() throws Exception {
        // 1. 点击活动
        System.out.println("--- 步骤1: 点击活动 ---");
        if (!clickButton("活动", "button_activity.png", 8000)) return false;
        humanDelay(1500, 2500);

        // 2. 点击江湖（支持两种样式）
        System.out.println("--- 步骤2: 点击江湖 ---");
        if (!clickButtonWithAlternatives("江湖", new String[]{"button_jianghu.png", "button_jianghu2.png"}, 5000)) {
            System.out.println("❌ 江湖按钮点击失败");
            return false;
        }
        humanDelay(1000, 1500);

        // 3. 找到所有前往按钮，点击最左边的一个
        System.out.println("--- 步骤3: 点击最左边的前往按钮 ---");
        Point leftmostGo = findLeftmostButton("button_go.png", 5000);
        if (leftmostGo == null) {
            System.out.println("❌ 未找到前往按钮");
            return false;
        }
        randomClick((int)leftmostGo.x, (int)leftmostGo.y);
        humanDelay(1000, 1500);

        // 4. 点击另一种前往（弹出样式）
        System.out.println("--- 步骤4: 点击另一种前往（弹出样式）---");
        Point goPopupPos = findButtonOnce("button_go_popup.png", 5000);
        if (goPopupPos == null) {
            System.out.println("❌ 未找到弹出前往按钮");
            return false;
        }
        // 点击按钮下方区域
        int clickX = (int)goPopupPos.x;
        int clickY = (int)goPopupPos.y + 20;
        randomClick(clickX, clickY);
        humanDelay(1000, 1500);

        // 5. 等待60秒，每10秒检测课业按钮
        System.out.println("\n--- 步骤5: 等待60秒，每10秒检测课业按钮 ---");

        boolean keYeClicked = false;
        for (int checkCount = 1; checkCount <= 6; checkCount++) {
            System.out.println("   第 " + checkCount + " 次检测（已过 " + (checkCount * 10) + " 秒）");

            Point keYePos = findButtonOnce("button_keye.png", 3000);
            if (keYePos != null) {
                System.out.println("   ✅ 找到课业按钮！");
                randomClick((int)keYePos.x, (int)keYePos.y);
                keYeClicked = true;
                break;
            }

            if (checkCount < 6) {
                System.out.println("   ⏳ 未找到，等待10秒...");
                Thread.sleep(10000);
            }
        }

        if (!keYeClicked) {
            System.out.println("   ⚠️ 60秒内未找到课业按钮，继续执行");
        }

        // 6. 等待5秒，点击屏幕中间
        System.out.println("\n--- 步骤6: 等待5秒后点击屏幕中间 ---");
        Thread.sleep(5000);
        clickScreenCenter();

        // 7. 循环检测提交（第一次提交 → 检测5星 → 第二次提交结束）
        System.out.println("\n--- 步骤7: 循环检测提交 ---");

        boolean hasSeen55 = false;
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 360000;  // 6分钟
        boolean taskCompleted = false;

        while (System.currentTimeMillis() - startTime < maxWaitTime && !taskCompleted) {
            // 检测提交按钮
            Point submitPos = findButtonOnce("button_submit.png", 0);
            if (submitPos != null) {
                if (hasSeen55) {
                    System.out.println("   ✅ 检测到提交按钮（已看到5星标记）！第二次提交，任务完成");
                    randomClick((int)submitPos.x, (int)submitPos.y);
                    taskCompleted = true;
                    break;
                } else {
                    System.out.println("   📝 检测到提交按钮，第一次提交");
                    randomClick((int)submitPos.x, (int)submitPos.y);
                    humanDelay(1000, 1500);
                }
            }

            // 检测5星标记
            Point pos55 = findButtonOnce("button_5_5.png", 0);
            if (pos55 != null && !hasSeen55) {
                System.out.println("   ⭐ 检测到5星标记！");
                hasSeen55 = true;
                humanDelay(500, 1000);
            }

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsed % 30 == 0 && elapsed > 0) {
                System.out.println("   ⏳ 任务进行中... " + elapsed + " 秒");
            }

            Thread.sleep(2000);
        }

        // 8. 结束流程
        System.out.println("\n--- 步骤8: 结束流程 ---");

        // 检测辛苦了按钮
        System.out.println("   查找 button_xingkule.png...");
        Point xingkulePos = findButtonOnce("button_xingkule.png", 10000);
        if (xingkulePos != null) {
            System.out.println("   ✅ 找到 button_xingkule.png");

            // 检测结束确定按钮
            System.out.println("   查找 button_jieshuqueding.png...");
            Point jieshuPos = findButtonOnce("button_jieshuqueding.png", 5000);
            if (jieshuPos != null) {
                System.out.println("   ✅ 找到 button_jieshuqueding.png，点击结束");
                randomClick((int)jieshuPos.x, (int)jieshuPos.y);
                humanDelay(1000, 1500);
            } else {
                System.out.println("   ⚠️ 未找到 button_jieshuqueding.png");
            }
        } else {
            System.out.println("   ⚠️ 未找到 button_xingkule.png");
        }

        return true;
    }

    /**
     * 带备选模板的点击
     */
    private static boolean clickButtonWithAlternatives(String name, String[] templates, int timeoutMs) throws Exception {
        System.out.println("\n--- 点击: " + name + " ---");

        for (String tpl : templates) {
            Point pos = findButtonOnce(tpl, timeoutMs);
            if (pos != null) {
                System.out.println("   ✅ 找到按钮: " + tpl);
                randomClick((int)pos.x, (int)pos.y);
                return true;
            }
        }
        return false;
    }

    /**
     * 查找最左边的按钮
     */
    private static Point findLeftmostButton(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();
            Mat template = templateCache.get(templateName);
            if (template == null) {
                Thread.sleep(500);
                continue;
            }

            List<Point> allMatches = findAllButtons(SCREENSHOT_PATH, template, templateName);
            if (!allMatches.isEmpty()) {
                // 按X坐标排序，取最小的
                allMatches.sort((a, b) -> Double.compare(a.x, b.x));
                return allMatches.get(0);
            }

            Thread.sleep(500);
        }
        return null;
    }

    /**
     * 找一次按钮（超时内循环）
     */
    private static Point findButtonOnce(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (timeoutMs > 0 && System.currentTimeMillis() - startTime > timeoutMs) {
                break;
            }

            takeScreenshot();
            Mat template = templateCache.get(templateName);
            if (template == null) {
                Thread.sleep(500);
                continue;
            }

            Point pos = findButton(SCREENSHOT_PATH, template, templateName);
            if (pos != null) {
                return pos;
            }

            Thread.sleep(300);
        }
        return null;
    }

    /**
     * 查找所有匹配的按钮
     */
    private static List<Point> findAllButtons(String screenshotPath, Mat template, String templateName) {
        List<Point> results = new ArrayList<>();
        Mat screenshot = null;
        Mat result = null;

        try {
            screenshot = Imgcodecs.imread(screenshotPath);
            if (screenshot.empty()) return results;

            result = new Mat();
            Imgproc.matchTemplate(screenshot, template, result, Imgproc.TM_CCOEFF_NORMED);

            double threshold = 0.65;

            for (int y = 0; y < result.rows(); y++) {
                for (int x = 0; x < result.cols(); x++) {
                    double matchValue = result.get(y, x)[0];
                    if (matchValue >= threshold) {
                        int cx = x + template.cols() / 2;
                        int cy = y + template.rows() / 2;

                        boolean duplicate = false;
                        for (Point p : results) {
                            if (Math.abs(p.x - cx) < 30 && Math.abs(p.y - cy) < 30) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            results.add(new Point(cx, cy));
                        }
                    }
                }
            }

        } finally {
            if (screenshot != null) screenshot.release();
            if (result != null) result.release();
        }

        return results;
    }

    /**
     * 点击屏幕中间
     */
    private static void clickScreenCenter() throws Exception {
        int width = getScreenWidth();
        int height = getScreenHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int randomX = centerX + (random.nextInt(11) - 5);
        int randomY = centerY + (random.nextInt(11) - 5);

        System.out.println("   🎯 屏幕中心: (" + centerX + ", " + centerY + ") → 点击: (" + randomX + ", " + randomY + ")");
        randomClick(randomX, randomY);
    }

    private static int getScreenWidth() throws Exception {
        Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell wm size");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        p.waitFor();
        if (line != null && line.contains("x")) {
            return Integer.parseInt(line.split("x")[0].replaceAll("[^0-9]", ""));
        }
        return 1280;
    }

    private static int getScreenHeight() throws Exception {
        Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell wm size");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        p.waitFor();
        if (line != null && line.contains("x")) {
            String[] parts = line.split("x");
            return Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
        }
        return 720;
    }

    static Point findButton(String screenshotPath, Mat template, String templateName) {
        Mat screenshot = null;
        Mat result = null;
        try {
            screenshot = Imgcodecs.imread(screenshotPath);
            if (screenshot.empty()) return null;

            result = new Mat();
            Imgproc.matchTemplate(screenshot, template, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            double threshold = 0.65;

            if (mmr.maxVal >= threshold) {
                int cx = (int)(mmr.maxLoc.x + template.cols() / 2);
                int cy = (int)(mmr.maxLoc.y + template.rows() / 2);
                System.out.println("   📊 " + templateName + " 匹配度: " + String.format("%.3f", mmr.maxVal));
                return new Point(cx, cy);
            }
            return null;
        } finally {
            if (screenshot != null) screenshot.release();
            if (result != null) result.release();
        }
    }

    /**
     * 安全执行ADB命令，消费输出流防止进程死锁
     */
    static void execAdb(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(cmd);
        new Thread(() -> { try { p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream()); } catch (Exception e) {} }).start();
        new Thread(() -> { try { p.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream()); } catch (Exception e) {} }).start();
        p.waitFor();
    }

    static void randomClick(int x, int y) throws Exception {
        int ox = x + (random.nextInt(7) - 3);
        int oy = y + (random.nextInt(7) - 3);
        execAdb(adb + " -s " + device + " shell input tap " + ox + " " + oy);
        System.out.println("   📱 点击: (" + ox + ", " + oy + ")");
    }

    static void humanDelay(int min, int max) {
        try { Thread.sleep(min + random.nextInt(max - min)); } catch (InterruptedException e) {}
    }

    private static boolean clickButton(String name, String templateName, int timeoutMs) throws Exception {
        System.out.println("\n--- 点击: " + name + " ---");

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();
            Mat template = templateCache.get(templateName);
            if (template == null) {
                Thread.sleep(500);
                continue;
            }

            Point pos = findButton(SCREENSHOT_PATH, template, templateName);
            if (pos != null) {
                randomClick((int)pos.x, (int)pos.y);
                return true;
            }

            Thread.sleep(500);
        }

        System.out.println("   ❌ 超时未找到: " + name);
        return false;
    }

    static void takeScreenshot() throws Exception {
        String cmd = adb + " -s " + device + " exec-out screencap -p";
        ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(SCREENSHOT_PATH)) {
            p.getInputStream().transferTo(fos);
        }
        if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new Exception("截图超时");
        }
    }

    private static void preloadTemplates() {
        System.out.println("📦 预加载模板...");
        System.out.println("   模板路径: " + TEMPLATE_PATH);

        String[] templates = {
                "button_activity.png",
                "button_jianghu.png",
                "button_jianghu2.png",
                "button_go.png",
                "button_go_popup.png",
                "button_keye.png",
                "button_submit.png",
                "button_5_5.png",
                "button_xingkule.png",
                "button_jieshuqueding.png"
        };
        for (String t : templates) {
            Mat mat = Imgcodecs.imread(TEMPLATE_PATH + t);
            if (!mat.empty()) {
                templateCache.put(t, mat);
                System.out.println("   ✅ " + t + " (尺寸: " + mat.cols() + "x" + mat.rows() + ")");
            } else {
                System.out.println("   ⚠️ 未找到: " + TEMPLATE_PATH + t);
            }
        }
    }

    static void cleanup() {
        new File(SCREENSHOT_PATH).delete();
        for (Mat m : templateCache.values()) m.release();
        templateCache.clear();
        System.out.println("\n🧹 已清理资源");
    }
}