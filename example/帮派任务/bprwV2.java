package com.example.帮派任务;

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

public class bprwV2 {

    private static String adb;
    private static String device;
    private static final String SCREENSHOT_PATH = "./temp_screen.png";
    private static final Random random = new Random();
    private static final Map<String, Mat> templateCache = new HashMap<>();

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
        System.out.println("=== 帮派任务脚本 ===\n");

        // 加载ADB配置
        com.example.自动配置.AdbConfig.init();
        adb = com.example.自动配置.AdbConfig.getAdbPath();
        device = com.example.自动配置.AdbConfig.getDevice();

        if (adb == null || adb.isEmpty() || device == null || device.isEmpty()) {
            System.out.println("❌ ADB配置无效，请先运行配置工具");
            return;
        }

        preloadTemplates();

        // ===== ADB 连接检查 =====
        System.out.println("\n🔍 检查ADB连接...");
        try {
            execAdb(adb + " devices");
            System.out.println("✅ ADB连接正常\n");
        } catch (Exception e) {
            System.out.println("❌ ADB连接异常: " + e.getMessage());
            System.out.println("请确认模拟器已启动并开启ADB调试\n");
            cleanup();
            return;
        }

        System.out.println("5秒后开始执行...");
        Thread.sleep(5000);

        boolean success = executeFullQuest();

        if (success) {
            System.out.println("\n✅ 帮派任务完成！");
        } else {
            System.out.println("\n❌ 执行失败！");
        }

        cleanup();
    }

    private static boolean executeFullQuest() throws Exception {
        // 步骤1：点击活动
        System.out.println("--- 步骤1: 点击活动 ---");
        if (!clickButton("button_activity.png", 5000)) return false;
        humanDelay(1500, 2000);

        // 步骤2：点击帮派
        System.out.println("--- 步骤2: 点击帮派 ---");
        if (!clickButtonWithAlternatives(new String[]{"button_bangpai1.png", "button_bangpai2.png"}, 5000)) {
            System.out.println("⚠️ 帮派按钮未找到，继续执行");
        }
        humanDelay(1000, 1500);

        // 步骤3：点击帮派任务前往
        System.out.println("--- 步骤3: 点击帮派任务前往 ---");
        Point qianwangPos = findButtonOnce("button_bpqianwang.png", 5000);
        if (qianwangPos == null) {
            System.out.println("❌ 未找到帮派任务前往按钮");
            return false;
        }
        int clickX = (int)qianwangPos.x;
        int clickY = (int)qianwangPos.y + random.nextInt(4);
        randomClick(clickX, clickY);
        System.out.println("   ✅ 点击了帮派任务前往");
        humanDelay(1000, 1500);

        // 等待跑图
        System.out.println("\n--- 等待跑图（最长60秒，每5秒检测）---");
        Point rwPos = null;
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 60000;

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            rwPos = findButtonOnce("button_bprw.png", 0);
            if (rwPos != null) {
                System.out.println("   ✅ 检测到帮派任务按钮");
                break;
            }
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("   ⏳ 跑图中... " + elapsed + " 秒");
            Thread.sleep(5000);
        }

        if (rwPos == null) {
            System.out.println("❌ 60秒内未检测到帮派任务按钮");
            return false;
        }

        // 步骤4：点击帮派任务
        System.out.println("--- 步骤4: 点击帮派任务 ---");
        randomClick((int)rwPos.x, (int)rwPos.y);
        System.out.println("   ✅ 点击了帮派任务");
        humanDelay(1000, 1500);

        // 步骤5：点击确定
        System.out.println("--- 步骤5: 点击确定 ---");
        clickButton("button_qd1.png", 5000);
        humanDelay(1000, 1500);

        // ========== 步骤6：主循环检测 ==========
        System.out.println("\n--- 步骤6: 进入主循环检测（最长6分钟）---");
        System.out.println("   检测到提交按钮 → 点击 → 等待25秒 → 点击两下屏幕中间 → 结束");
        System.out.println("   检测到购买按钮 → 点击购买 → 等待60秒 → 检测提交按钮 → 点击 → 结束");

        long loopStartTime = System.currentTimeMillis();
        long maxLoopTime = 360000;
        int checkCount = 0;

        while (System.currentTimeMillis() - loopStartTime < maxLoopTime) {
            checkCount++;
            long elapsed = (System.currentTimeMillis() - loopStartTime) / 1000;
            System.out.println("\n   🔍 第 " + checkCount + " 次检测（已过 " + elapsed + " 秒）");

            // 确保截图成功
            if (!takeScreenshot()) {
                System.out.println("   ⚠️ 截图失败，重试...");
                Thread.sleep(2000);
                continue;
            }

            // 1. 先检测提交按钮
            Mat submitTemplate = templateCache.get("button_submit.png");
            if (submitTemplate != null) {
                Point submitPos = findButtonInImage(SCREENSHOT_PATH, submitTemplate, "button_submit.png");
                if (submitPos != null) {
                    System.out.println("   📝 检测到提交按钮！位置: (" + (int)submitPos.x + ", " + (int)submitPos.y + ")");
                    randomClick((int)submitPos.x, (int)submitPos.y);
                    humanDelay(1000, 1500);

                    // 等待25秒（留5秒给最后两下点击）
                    System.out.println("   ⏳ 等待25秒...");
                    for (int i = 0; i < 25; i++) {
                        Thread.sleep(1000);
                        if (i % 5 == 0 && i > 0) {
                            System.out.println("      已等待 " + i + " 秒");
                        }
                    }

                    // 点击两下屏幕中间
                    int width = getScreenWidth();
                    int height = getScreenHeight();
                    int centerX = width / 2;
                    int centerY = height / 2;

                    System.out.println("   🖱️ 点击屏幕中间第1下: (" + centerX + ", " + centerY + ")");
                    randomClick(centerX, centerY);
                    Thread.sleep(500);

                    System.out.println("   🖱️ 点击屏幕中间第2下: (" + centerX + ", " + centerY + ")");
                    randomClick(centerX, centerY);
                    Thread.sleep(500);

                    System.out.println("   ✅ 帮派任务完成");
                    return true;
                }
            }

            // 2. 检测购买按钮
            Mat buyTemplate = templateCache.get("button_buy.png");
            if (buyTemplate != null) {
                Point buyPos = findButtonInImage(SCREENSHOT_PATH, buyTemplate, "button_buy.png");
                if (buyPos != null) {
                    System.out.println("   🛒 检测到购买按钮！位置: (" + (int)buyPos.x + ", " + (int)buyPos.y + ")");
                    randomClick((int)buyPos.x, (int)buyPos.y);
                    humanDelay(1000, 1500);

                    // 重新截图找确定购买按钮
                    takeScreenshot();
                    Mat quedingTemplate = templateCache.get("button_quedinggoumai.png");
                    if (quedingTemplate != null) {
                        Point quedingPos = findButtonInImage(SCREENSHOT_PATH, quedingTemplate, "button_quedinggoumai.png");
                        if (quedingPos != null) {
                            System.out.println("   ✅ 点击确定购买");
                            randomClick((int)quedingPos.x, (int)quedingPos.y);
                            humanDelay(1000, 1500);
                        } else {
                            System.out.println("   ⚠️ 未找到确定购买按钮，但继续执行");
                        }
                    }

                    System.out.println("\n   ⏳ 购买完成，等待60秒跑图...");
                    for (int i = 0; i < 60; i++) {
                        Thread.sleep(1000);
                        if (i % 10 == 0 && i > 0) {
                            System.out.println("      已等待 " + i + " 秒");
                        }
                    }

                    System.out.println("\n   🔍 跑图完成，开始检测提交按钮（最长60秒）...");
                    long submitStartTime = System.currentTimeMillis();
                    boolean submitFound = false;

                    while (System.currentTimeMillis() - submitStartTime < 60000) {
                        if (!takeScreenshot()) {
                            Thread.sleep(1000);
                            continue;
                        }

                        Point submitAfterBuy = findButtonInImage(SCREENSHOT_PATH, submitTemplate, "button_submit.png");
                        if (submitAfterBuy != null) {
                            System.out.println("   📝 检测到提交按钮！位置: (" + (int)submitAfterBuy.x + ", " + (int)submitAfterBuy.y + ")");
                            randomClick((int)submitAfterBuy.x, (int)submitAfterBuy.y);
                            humanDelay(1000, 1500);

                            // 等待25秒 + 点击两下屏幕中间
                            System.out.println("   ⏳ 等待25秒...");
                            for (int i = 0; i < 25; i++) {
                                Thread.sleep(1000);
                                if (i % 5 == 0 && i > 0) {
                                    System.out.println("      已等待 " + i + " 秒");
                                }
                            }

                            int width = getScreenWidth();
                            int height = getScreenHeight();
                            int centerX = width / 2;
                            int centerY = height / 2;

                            System.out.println("   🖱️ 点击屏幕中间第1下: (" + centerX + ", " + centerY + ")");
                            randomClick(centerX, centerY);
                            Thread.sleep(500);

                            System.out.println("   🖱️ 点击屏幕中间第2下: (" + centerX + ", " + centerY + ")");
                            randomClick(centerX, centerY);
                            Thread.sleep(500);

                            System.out.println("   ✅ 帮派任务完成");
                            submitFound = true;
                            break;
                        }

                        Thread.sleep(2000);
                    }

                    if (submitFound) {
                        return true;
                    } else {
                        System.out.println("   ❌ 60秒内未检测到提交按钮，任务失败");
                        return false;
                    }
                }
            }

            System.out.println("   ⏳ 未检测到提交按钮或购买按钮，等待10秒...");
            Thread.sleep(10000);
        }

        System.out.println("\n   ⏰ 6分钟循环结束，未检测到提交按钮");
        return false;
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
            if (templateName.equals("button_quedinggoumai.png")) {
                threshold = 0.50;
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

    private static boolean clickButtonWithAlternatives(String[] templates, int timeoutMs) throws Exception {
        for (String tpl : templates) {
            if (clickButton(tpl, timeoutMs)) {
                return true;
            }
        }
        return false;
    }

    private static boolean clickButton(String templateName, int timeoutMs) throws Exception {
        System.out.println("   [clickButton] 开始查找: " + templateName + " (超时" + timeoutMs + "ms)");
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                if (!takeScreenshot()) {
                    System.out.println("   ⚠️ 截图失败，200ms后重试...");
                    Thread.sleep(200);
                    continue;
                }

                Mat template = templateCache.get(templateName);
                if (template == null) {
                    System.out.println("   ⚠️ 模板 " + templateName + " 未缓存，200ms后重试...");
                    Thread.sleep(200);
                    continue;
                }

                Point pos = findButton(SCREENSHOT_PATH, template, templateName);
                if (pos != null) {
                    randomClick((int)pos.x, (int)pos.y);
                    return true;
                }

                Thread.sleep(300);
            } catch (Exception e) {
                System.out.println("   ❌ 检测异常: " + e.getMessage());
                Thread.sleep(500);
            }
        }
        System.out.println("   ⏰ " + templateName + " 检测超时（" + timeoutMs + "ms）");
        return false;
    }

    private static Point findButtonOnce(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (timeoutMs > 0 && System.currentTimeMillis() - startTime > timeoutMs) {
                break;
            }

            if (!takeScreenshot()) {
                Thread.sleep(200);
                continue;
            }

            Mat template = templateCache.get(templateName);
            if (template == null) {
                Thread.sleep(200);
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

    static Point findButton(String screenshotPath, Mat template, String templateName) {
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

            double threshold;
            // 活动按钮使用低阈值
            if (templateName.equals("button_activity.png")) {
                threshold = 0.30;
            } else {
                threshold = 0.65;
            }

            if (mmr.maxVal >= threshold) {
                int cx = (int)(mmr.maxLoc.x + template.cols() / 2);
                int cy = (int)(mmr.maxLoc.y + template.rows() / 2);
                System.out.println("   📊 " + templateName + " 匹配度: " + String.format("%.3f", mmr.maxVal));
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

    private static Mat removeBackgroundToWhite(Mat src) {
        Mat gray = new Mat();
        Mat mask = new Mat();
        Mat result = new Mat();

        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(gray, mask, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 25, 10);
        Imgproc.cvtColor(mask, result, Imgproc.COLOR_GRAY2BGR);

        gray.release();
        mask.release();

        return result;
    }

    /**
     * 安全执行ADB命令（ProcessBuilder + 超时，30秒超时防永久阻塞）
     */
    static void execAdb(String cmd) throws Exception {
        long t0 = System.currentTimeMillis();
        System.out.println("   [ADB] >>> " + cmd.substring(0, Math.min(100, cmd.length())));
        ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try {
            new Thread(() -> {
                try { p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream()); } catch (Exception e) {}
            }).start();
            if (!p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new Exception("ADB命令超时(30s): " + cmd.substring(0, Math.min(80, cmd.length())));
            }
        } finally {
            if (p.isAlive()) p.destroyForcibly();
        }
        long elapsed = System.currentTimeMillis() - t0;
        System.out.println("   [ADB] <<< 耗时 " + elapsed + "ms, exit=" + p.exitValue());
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

    /**
     * 使用 exec-out 直传截图（绕过shell层，不用手机临时文件，更快更稳）
     */
    private static boolean takeScreenshot() {
        try {
            System.out.println("   [截图] exec-out直传...");
            File oldFile = new File(SCREENSHOT_PATH);
            if (oldFile.exists()) oldFile.delete();

            String cmd = adb + " -s " + device + " exec-out screencap -p";
            System.out.println("   [ADB] >>> " + cmd);
            long t0 = System.currentTimeMillis();

            ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();

            // 直接读取PNG字节流写入本地文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(SCREENSHOT_PATH)) {
                p.getInputStream().transferTo(fos);
            }

            if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                System.out.println("   ❌ exec-out超时(10s)");
                return false;
            }

            long elapsed = System.currentTimeMillis() - t0;
            File screenshotFile = new File(SCREENSHOT_PATH);
            boolean ok = screenshotFile.exists() && screenshotFile.length() > 0;
            System.out.println("   [ADB] <<< 耗时 " + elapsed + "ms, size=" + (ok ? screenshotFile.length() : 0) + " bytes, ok=" + ok);
            return ok;

        } catch (Exception e) {
            System.out.println("   ❌ 截图异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    private static int getScreenWidth() throws Exception {
        Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell wm size");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        // 也消费完stderr
        try (BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
            while (err.readLine() != null) {}
        }
        p.waitFor();
        String output = sb.toString().trim();
        if (output.contains("x")) {
            String[] parts = output.split("x");
            return Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
        }
        return 1280;
    }

    private static int getScreenHeight() throws Exception {
        Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell wm size");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        // 也消费完stderr
        try (BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
            while (err.readLine() != null) {}
        }
        p.waitFor();
        String output = sb.toString().trim();
        if (output.contains("x")) {
            String[] parts = output.split("x");
            return Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
        }
        return 720;
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
        String templatePath = baseDir + File.separator + "templates" + File.separator + "file1" + File.separator;
        System.out.println("   程序目录: " + baseDir);
        System.out.println("   模板路径: " + templatePath);

        String[] templates = {
                "button_activity.png",
                "button_bangpai1.png", "button_bangpai2.png",
                "button_bpqianwang.png",
                "button_bprw.png",
                "button_qd1.png",
                "button_buy.png",
                "button_quedinggoumai.png",
                "button_submit.png"
        };
        for (String t : templates) {
            Mat mat = imreadSafe(templatePath + t);
            if (!mat.empty()) {
                templateCache.put(t, mat);
                System.out.println("   ✅ " + t + " (尺寸: " + mat.cols() + "x" + mat.rows() + ")");
            } else {
                System.out.println("   ⚠️ 未找到: " + templatePath + t);
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