package com.example.师门任务;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ShiMenV2 {

    private static String adb;
    private static String device;
    private static final String SCREENSHOT_PATH = "./temp_screen.png";
    private static final Random random = new Random();
    private static final Map<String, Mat> templateCache = new HashMap<>();

    private static Rect BOTTOM_RIGHT_REGION;

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

        // ========== 添加ADB配置读取 ==========
        com.example.自动配置.AdbConfig.init();
        adb = com.example.自动配置.AdbConfig.getAdbPath();
        device = com.example.自动配置.AdbConfig.getDevice();
        // ==================================

        int width = getScreenWidth();
        int height = getScreenHeight();
        BOTTOM_RIGHT_REGION = new Rect(width / 2, height / 2, width / 2, height / 2);
        System.out.println("   屏幕尺寸: " + width + "x" + height);

        preloadTemplates();

        System.out.println("5秒后开始执行...");
        Thread.sleep(5000);

        boolean success = executeOneQuest();

        if (success) {
            System.out.println("\n✅ 师门任务完成！");
        } else {
            System.out.println("\n❌ 执行失败！");
        }

        cleanup();
    }

    private static boolean executeOneQuest() throws Exception {
        // 步骤1：点击活动
        if (!executeStepWithRetry("点击活动", "button_activity.png", 5000, null)) return false;
        humanDelay(1500, 2000);

        // 步骤2：点击江湖
        if (!executeStepWithRetry("点击江湖", new String[]{"button_jianghu.png", "button_jianghu2.png"}, 5000, "button_activity.png")) return false;
        humanDelay(1000, 1500);

        // 步骤3：点击最左边的前往按钮
        System.out.println("\n--- 步骤3: 点击最左边的前往按钮 ---");
        Point leftmostGo = findLeftmostButton("button_go.png", 5000);
        if (leftmostGo == null) {
            System.out.println("❌ 未找到任何前往按钮");
            return false;
        }
        randomClick((int)leftmostGo.x, (int)leftmostGo.y);
        System.out.println("   ✅ 点击了最左边的前往按钮");
        humanDelay(1000, 1500);

        // 步骤4：点击弹出前往
        if (!executeStepWithRetry("点击弹出前往", "button_go_popup.png", 5000, "button_go.png")) return false;
        humanDelay(1000, 1500);

        // 步骤5：等待60秒检测课业（找到后点两次）
        System.out.println("\n--- 步骤5: 等待60秒，每10秒检测课业按钮 ---");
        System.out.println("   📌 找到课业按钮后：第一次点击 → 等待3秒 → 第二次点击同一位置");

        boolean keYeClicked = false;
        Point keYePos = null;

        for (int checkCount = 1; checkCount <= 6; checkCount++) {
            System.out.println("   第 " + checkCount + " 次检测（已过 " + (checkCount * 10) + " 秒）");

            keYePos = findButtonOnceInRegion("button_keye.png", BOTTOM_RIGHT_REGION, 3000);
            if (keYePos != null) {
                System.out.println("   ✅ 找到课业按钮！位置: (" + (int)keYePos.x + ", " + (int)keYePos.y + ")");

                // 第一次点击
                System.out.println("   📱 第一次点击课业按钮");
                randomClick((int)keYePos.x, (int)keYePos.y);

                // 等待3秒
                System.out.println("   ⏳ 等待3秒...");
                Thread.sleep(3000);

                // 第二次点击同一个位置
                System.out.println("   📱 第二次点击课业按钮（同一位置）");
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

        // 步骤6：等待5秒，点击屏幕中间
        System.out.println("\n--- 步骤6: 等待5秒后点击屏幕中间 ---");
        Thread.sleep(5000);
        clickScreenCenter();

        // ========== 步骤7：6分钟循环检测（每5秒检测一次）==========
        System.out.println("\n--- 步骤7: 进入6分钟循环检测（每5秒检测一次）---");
        System.out.println("   1. 检测 popup_dui2.png → 找 close 点击 → 重新循环");
        System.out.println("   2. 检测 button_buy.png → 点击购买 → 点击确定购买 → 继续循环");
        System.out.println("   3. 检测 button_submit.png → 点击（提交道具）→ 继续循环");
        System.out.println("   4. 检测 button_xingkule.png → 找确定按钮 → 点击 → 结束师门任务");

        long startTime = System.currentTimeMillis();
        long maxWaitTime = 360000; // 6分钟
        int checkCount = 0;

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            checkCount++;
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;

            System.out.println("\n   🔍 第 " + checkCount + " 次检测（已过 " + elapsed + " 秒）");

            takeScreenshot();

            // ========== 优先级0：右下四分之一区域检测 ==========
            // 检测对话框
            Mat duihuakuangTemplate = templateCache.get("button_duihuakuang.png");
            if (duihuakuangTemplate != null) {
                Point duihuakuangPos = findButtonInRegion(SCREENSHOT_PATH, duihuakuangTemplate, "button_duihuakuang.png", BOTTOM_RIGHT_REGION);
                if (duihuakuangPos != null) {
                    System.out.println("   💬 检测到对话框！点击");
                    randomClick((int)duihuakuangPos.x, (int)duihuakuangPos.y);
                    humanDelay(500, 1000);
                }
            }

            // 检测材料弹窗
            Mat cailiaoTemplate = templateCache.get("button_cailiao.png");
            if (cailiaoTemplate != null) {
                Point cailiaoPos = findButtonInRegion(SCREENSHOT_PATH, cailiaoTemplate, "button_cailiao.png", BOTTOM_RIGHT_REGION);
                if (cailiaoPos != null) {
                    System.out.println("   📦 检测到材料弹窗！点击");
                    randomClick((int)cailiaoPos.x, (int)cailiaoPos.y);
                    humanDelay(500, 1000);
                }
            }

            // ========== 优先级1：检测弹窗 ==========
            Mat duiTemplate = templateCache.get("popup_dui2.png");
            if (duiTemplate != null) {
                Point duiPos = findButtonInImage(SCREENSHOT_PATH, duiTemplate, "popup_dui2.png");
                if (duiPos != null) {
                    System.out.println("   📖 检测到答题弹窗！");
                    Mat closeTemplate = templateCache.get("button_close.png");
                    if (closeTemplate != null) {
                        Point closePos = findButtonInImage(SCREENSHOT_PATH, closeTemplate, "button_close.png");
                        if (closePos != null) {
                            System.out.println("   🔔 点击关闭按钮");
                            randomClick((int)closePos.x, (int)closePos.y);
                            humanDelay(500, 1000);
                            System.out.println("   🔄 弹窗已关闭，重新循环");
                            continue;
                        }
                    }
                }
            }

            // ========== 优先级2：检测购买按钮 ==========
            Mat buyTemplate = templateCache.get("button_buy.png");
            if (buyTemplate != null) {
                Point buyPos = findButtonInImage(SCREENSHOT_PATH, buyTemplate, "button_buy.png");
                if (buyPos != null) {
                    System.out.println("   🛒 检测到购买按钮！点击购买");
                    randomClick((int)buyPos.x, (int)buyPos.y);
                    humanDelay(1000, 1500);

                    Mat quedingTemplate = templateCache.get("button_quedinggoumai.png");
                    if (quedingTemplate != null) {
                        Point quedingPos = findButtonInImage(SCREENSHOT_PATH, quedingTemplate, "button_quedinggoumai.png");
                        if (quedingPos != null) {
                            System.out.println("   ✅ 点击确定购买");
                            randomClick((int)quedingPos.x, (int)quedingPos.y);
                            humanDelay(1000, 1500);
                        }
                    }
                    // 购买后继续循环
                }
            }

            // ========== 优先级3：检测提交按钮（只点击，不结束）==========
            Mat submitTemplate = templateCache.get("button_submit.png");
            if (submitTemplate != null) {
                Point submitPos = findButtonInImage(SCREENSHOT_PATH, submitTemplate, "button_submit.png");
                if (submitPos != null) {
                    System.out.println("   📝 检测到提交按钮！点击（提交道具）");
                    randomClick((int)submitPos.x, (int)submitPos.y);
                    humanDelay(1000, 1500);
                    // 继续循环，等待结束标记
                }
            }

            // ========== 优先级4：检测结束标记（找到后结束）==========
            Mat xingkuleTemplate = templateCache.get("button_xingkule.png");
            if (xingkuleTemplate != null) {
                Point xingkulePos = findButtonInImage(SCREENSHOT_PATH, xingkuleTemplate, "button_xingkule.png");
                if (xingkulePos != null) {
                    System.out.println("   🎯 检测到'辛苦了'按钮！");
                    Mat jieshuTemplate = templateCache.get("button_jieshuqueding.png");
                    if (jieshuTemplate != null) {
                        Point jieshuPos = findButtonInImage(SCREENSHOT_PATH, jieshuTemplate, "button_jieshuqueding.png");
                        if (jieshuPos != null) {
                            System.out.println("   ✅ 点击结束确定按钮，师门任务完成");
                            randomClick((int)jieshuPos.x, (int)jieshuPos.y);
                            humanDelay(1000, 1500);
                            return true;  // 结束师门任务
                        } else {
                            System.out.println("   ⚠️ 未找到结束确定按钮");
                        }
                    }
                }
            }

            System.out.println("   ✅ 第 " + checkCount + " 次检测完成");
            Thread.sleep(5000);
        }

        System.out.println("\n   ⏰ 6分钟循环结束");
        return true;
    }

    private static Point findButtonInImage(String screenshotPath, Mat template, String templateName) {
        Mat screenshot = null;
        Mat screenshotBW = null;
        Mat templateBW = null;
        Mat result = null;
        try {
            screenshot = Imgcodecs.imread(screenshotPath);
            if (screenshot.empty()) return null;

            screenshotBW = removeBackgroundToWhite(screenshot);
            templateBW = removeBackgroundToWhite(template);

            result = new Mat();
            Imgproc.matchTemplate(screenshotBW, templateBW, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            double threshold = 0.65;
            if (templateName.equals("button_xingkule.png") || templateName.equals("button_jieshuqueding.png")) {
                threshold = 0.45;
            }
            if (templateName.equals("popup_dui2.png")) {
                threshold = 0.55;
            }

            if (mmr.maxVal >= threshold) {
                int cx = (int)(mmr.maxLoc.x + template.cols() / 2);
                int cy = (int)(mmr.maxLoc.y + template.rows() / 2);
                System.out.println("      📊 " + templateName + " 匹配度: " + String.format("%.3f", mmr.maxVal));
                return new Point(cx, cy);
            }
            return null;
        } finally {
            if (screenshot != null) screenshot.release();
            if (screenshotBW != null) screenshotBW.release();
            if (templateBW != null) templateBW.release();
            if (result != null) result.release();
        }
    }

    /**
     * 在指定区域内查找按钮
     * @param screenshotPath 截图路径
     * @param template 模板
     * @param templateName 模板名称
     * @param region 区域 (x, y, width, height)
     * @return 找到的位置或null
     */
    private static Point findButtonInRegion(String screenshotPath, Mat template, String templateName, Rect region) {
        Mat screenshot = null;
        Mat screenshotBW = null;
        Mat templateBW = null;
        Mat result = null;
        try {
            screenshot = Imgcodecs.imread(screenshotPath);
            if (screenshot.empty()) return null;

            // 裁剪出指定区域
            Rect roi = new Rect(
                Math.max(0, region.x),
                Math.max(0, region.y),
                Math.min(screenshot.cols() - region.x, region.width),
                Math.min(screenshot.rows() - region.y, region.height)
            );
            Mat roiImage = new Mat(screenshot, roi);

            screenshotBW = removeBackgroundToWhite(roiImage);
            templateBW = removeBackgroundToWhite(template);

            result = new Mat();
            Imgproc.matchTemplate(screenshotBW, templateBW, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            double threshold = 0.65;
            if (mmr.maxVal >= threshold) {
                int cx = (int)(mmr.maxLoc.x + template.cols() / 2 + region.x);
                int cy = (int)(mmr.maxLoc.y + template.rows() / 2 + region.y);
                System.out.println("      📊 [区域] " + templateName + " 匹配度: " + String.format("%.3f", mmr.maxVal));
                return new Point(cx, cy);
            }
            return null;
        } finally {
            if (screenshot != null) screenshot.release();
            if (screenshotBW != null) screenshotBW.release();
            if (templateBW != null) templateBW.release();
            if (result != null) result.release();
        }
    }

    private static Point findLeftmostButton(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();
            Mat template = templateCache.get(templateName);
            if (template == null) {
                Thread.sleep(200);
                continue;
            }
            List<Point> allMatches = findAllButtons(templateName);
            if (!allMatches.isEmpty()) {
                allMatches.sort((a, b) -> Double.compare(a.x, b.x));
                return allMatches.get(0);
            }
            Thread.sleep(300);
        }
        return null;
    }

    private static List<Point> findAllButtons(String templateName) throws Exception {
        List<Point> results = new ArrayList<>();
        Mat template = templateCache.get(templateName);
        if (template == null) return results;

        Mat screenshot = Imgcodecs.imread(SCREENSHOT_PATH);
        if (screenshot.empty()) return results;

        Mat screenshotBW = removeBackgroundToWhite(screenshot);
        Mat templateBW = removeBackgroundToWhite(template);

        Mat result = new Mat();
        Imgproc.matchTemplate(screenshotBW, templateBW, result, Imgproc.TM_CCOEFF_NORMED);

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

        screenshot.release();
        screenshotBW.release();
        templateBW.release();
        result.release();
        return results;
    }

    private static Mat removeBackgroundToWhite(Mat src) {
        Mat gray = new Mat();
        Mat mask = new Mat();
        Mat result = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(gray, mask, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 25, 10);
        Imgproc.cvtColor(mask, result, Imgproc.COLOR_GRAY2BGR);
        gray.release();
        mask.release();
        return result;
    }

    private static boolean executeStepWithRetry(String stepName, String template, int timeoutMs, String fallbackTemplate) throws Exception {
        return executeStepWithRetry(stepName, new String[]{template}, timeoutMs, fallbackTemplate);
    }

    private static boolean executeStepWithRetry(String stepName, String[] templates, int timeoutMs, String fallbackTemplate) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                System.out.println("   🔄 " + stepName + " 第 " + attempt + " 次重试");
                humanDelay(800, 1500);
            }
            for (String tpl : templates) {
                Point pos = findButtonOnce(tpl, timeoutMs);
                if (pos != null) {
                    randomClick((int)pos.x, (int)pos.y);
                    System.out.println("   ✅ " + stepName + " 成功");
                    return true;
                }
            }
            if (attempt < 2 && fallbackTemplate != null) {
                Point fallbackPos = findButtonOnce(fallbackTemplate, 2000);
                if (fallbackPos != null) {
                    System.out.println("   🔙 检测到回退按钮，重新点击");
                    randomClick((int)fallbackPos.x, (int)fallbackPos.y);
                    humanDelay(1000, 1500);
                }
            }
        }
        if (fallbackTemplate != null) {
            Point fallbackPos = findButtonOnce(fallbackTemplate, 2000);
            if (fallbackPos != null) {
                System.out.println("   🔙 " + stepName + " 失败，但回退按钮存在，重新点击回退按钮");
                randomClick((int)fallbackPos.x, (int)fallbackPos.y);
                humanDelay(1000, 1500);
                return executeStepWithRetry(stepName, templates, timeoutMs, fallbackTemplate);
            }
        }
        System.out.println("   ❌ " + stepName + " 失败，且回退按钮不存在");
        return false;
    }

    private static Point findButtonOnce(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();
            Mat template = templateCache.get(templateName);
            if (template == null) break;
            Mat screenshot = Imgcodecs.imread(SCREENSHOT_PATH);
            if (screenshot.empty()) break;
            Mat screenshotBW = removeBackgroundToWhite(screenshot);
            Mat templateBW = removeBackgroundToWhite(template);
            Mat result = new Mat();
            Imgproc.matchTemplate(screenshotBW, templateBW, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            screenshot.release();
            screenshotBW.release();
            templateBW.release();
            result.release();
            double threshold = 0.65;
            if (mmr.maxVal >= threshold) {
                int cx = (int)(mmr.maxLoc.x + template.cols() / 2);
                int cy = (int)(mmr.maxLoc.y + template.rows() / 2);
                return new Point(cx, cy);
            }
            Thread.sleep(300);
        }
        return null;
    }

    private static Point findButtonOnceInRegion(String templateName, Rect region, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (true) {
            if (timeoutMs > 0 && System.currentTimeMillis() - startTime > timeoutMs) break;
            takeScreenshot();
            Mat template = templateCache.get(templateName);
            if (template == null) break;
            Mat screenshot = Imgcodecs.imread(SCREENSHOT_PATH);
            if (screenshot.empty()) break;
            Mat roi = new Mat(screenshot, region);
            Mat roiBW = removeBackgroundToWhite(roi);
            Mat templateBW = removeBackgroundToWhite(template);
            Mat result = new Mat();
            Imgproc.matchTemplate(roiBW, templateBW, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            roi.release();
            roiBW.release();
            templateBW.release();
            result.release();
            screenshot.release();
            double threshold = 0.65;
            if (mmr.maxVal >= threshold) {
                int cx = region.x + (int)(mmr.maxLoc.x + template.cols() / 2);
                int cy = region.y + (int)(mmr.maxLoc.y + template.rows() / 2);
                return new Point(cx, cy);
            }
            if (timeoutMs == 0) break;
            Thread.sleep(300);
        }
        return null;
    }

    private static void clickScreenCenter() throws Exception {
        int width = getScreenWidth();
        int height = getScreenHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int randomX = centerX + (random.nextInt(11) - 5);
        int randomY = centerY + (random.nextInt(11) - 5);
        System.out.println("   🎯 点击屏幕中心: (" + randomX + ", " + randomY + ")");
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
        if (min >= max) {
            try { Thread.sleep(min); } catch (InterruptedException e) {}
            return;
        }
        try { Thread.sleep(min + random.nextInt(max - min)); } catch (InterruptedException e) {}
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

    private static String getProgramDirectory() {
        // 清理路径末尾的反斜杠和点号
        String userDir = System.getProperty("user.dir");
        userDir = userDir.replaceAll("[\\\\/]+\\.?$", "");
        
        // 如果user.dir下有templates，直接返回
        if (new File(userDir, "templates").exists()) {
            return userDir;
        }
        
        // 否则向上查找有templates的目录
        File parent = new File(userDir).getParentFile();
        while (parent != null) {
            if (new File(parent, "templates").exists()) {
                return parent.getAbsolutePath();
            }
            parent = parent.getParentFile();
        }
        
        return userDir;
    }

    /**
     * 安全读取图片（处理中文路径问题）
     */
    private static Mat imreadSafe(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("   ⚠️ 文件不存在: " + path);
            return new Mat();
        }
        
        try {
            // 始终使用临时文件方式，确保中文路径能正确读取
            Path tempPath = Files.createTempFile("tpl_", ".png");
            Files.copy(file.toPath(), tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Mat mat = Imgcodecs.imread(tempPath.toString());
            Files.deleteIfExists(tempPath);
            return mat;
        } catch (Exception e) {
            System.out.println("   ⚠️ 读取失败: " + path);
            e.printStackTrace();
            return new Mat();
        }
    }

    private static void preloadTemplates() {
        System.out.println("📦 预加载模板...");

        // 自动获取程序所在目录
        String baseDir = getProgramDirectory();
        String basePath = baseDir + File.separator + "templates" + File.separator + "file3" + File.separator;
        System.out.println("   程序目录: " + baseDir);
        System.out.println("   模板路径: " + basePath);

        String[] templates = {
                "button_activity.png",
                "button_jianghu.png", "button_jianghu2.png",
                "button_go.png", "button_go_popup.png",
                "button_keye.png",
                "button_submit.png",
                "popup_dui2.png",
                "button_close.png",
                "button_xingkule.png",
                "button_jieshuqueding.png",
                "button_buy.png",
                "button_quedinggoumai.png",
                "button_duihuakuang.png",
                "button_cailiao.png"
        };
        for (String t : templates) {
            Mat mat = imreadSafe(basePath + t);
            if (!mat.empty()) {
                templateCache.put(t, mat);
                System.out.println("   ✅ " + t);
            } else {
                System.out.println("   ⚠️ 未找到: " + basePath + t);
            }
        }
    }

    static void cleanup() {
        new File(SCREENSHOT_PATH).delete();
        for (Mat m : templateCache.values()) m.release();
        templateCache.clear();
    }
}