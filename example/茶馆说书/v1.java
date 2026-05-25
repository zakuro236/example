package com.example.茶馆说书;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class v1 {

    private static String adb;
    private static String device;
    private static final String SCREENSHOT_PATH = "./temp_screen.png";
    private static final Random random = new Random();
    private static final Map<String, Mat> templateCache = new HashMap<>();

    // 存储头像位置
    private static List<AvatarInfo> avatarPositions = new ArrayList<>();

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
        System.out.println("=== 茶馆说书脚本 ===\n");

        // ========== 添加ADB配置读取 ==========
        com.example.自动配置.AdbConfig.init();
        adb = com.example.自动配置.AdbConfig.getAdbPath();
        device = com.example.自动配置.AdbConfig.getDevice();
        // ==================================

        preloadTemplates();

        System.out.println("5秒后开始执行...");
        Thread.sleep(5000);

        boolean success = executeFullQuest();

        if (success) {
            System.out.println("\n✅ 茶馆说书完成！");
        } else {
            System.out.println("\n❌ 执行失败！");
        }

        cleanup();
    }

    private static boolean executeFullQuest() throws Exception {
        // ========== 步骤1：点击活动 ==========
        System.out.println("--- 步骤1: 点击活动 ---");
        if (!clickButton("button_activity.png", 5000)) {
            System.out.println("❌ 活动按钮点击失败");
            return false;
        }
        humanDelay(1500, 2000);

        // ========== 步骤2：点击江湖 ==========
        System.out.println("--- 步骤2: 点击江湖 ---");
        if (!clickButtonWithAlternatives(new String[]{"button_jianghu.png", "button_jianghu2.png"}, 5000)) {
            System.out.println("❌ 江湖按钮点击失败");
            return false;
        }
        humanDelay(1000, 1500);

        // ========== 步骤3：点击任务前往 ==========
        System.out.println("--- 步骤3: 点击任务前往 ---");
        Point qianwangPos = findButtonOnce("button_qianwang.png", 5000);
        if (qianwangPos == null) {
            System.out.println("❌ 未找到任务前往按钮");
            return false;
        }
        int clickX = (int)qianwangPos.x;
        int clickY = (int)qianwangPos.y + random.nextInt(4);
        randomClick(clickX, clickY);
        System.out.println("   ✅ 点击了任务前往");
        humanDelay(1000, 1500);

        // ========== 等待跑图，检测进入按钮 ==========
        System.out.println("\n--- 等待跑图（最长60秒，每5秒检测进入按钮）---");
        Point jinruPos = null;
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 60000;

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            jinruPos = findButtonOnce("button_jinru.png", 0);
            if (jinruPos != null) {
                System.out.println("   ✅ 检测到进入按钮");
                break;
            }
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("   ⏳ 跑图中... " + elapsed + " 秒");
            Thread.sleep(5000);
        }

        if (jinruPos == null) {
            System.out.println("❌ 60秒内未检测到进入按钮");
            return false;
        }

        // ========== 步骤4：点击进入按钮 ==========
        System.out.println("--- 步骤4: 点击进入 ---");
        randomClick((int)jinruPos.x, (int)jinruPos.y);
        System.out.println("   ✅ 点击了进入按钮");

        // 等待25秒让界面完全加载
        System.out.println("   ⏳ 等待25秒加载界面...");
        for (int i = 25; i > 0; i--) {
            System.out.print("      " + i + " 秒后继续...\r");
            Thread.sleep(1000);
        }
        System.out.println("\n   ✅ 25秒等待完成");

        // ========== 步骤5：检测一次头像位置并记录 ==========
        System.out.println("\n--- 步骤5: 检测头像位置并记录 ---");

        String[] avatarTemplates = {"button_1.png", "button_2.png"};
        avatarPositions.clear();

        // 截图一次，用于识别所有头像
        if (!takeScreenshot()) {
            System.out.println("❌ 截图失败");
            return false;
        }

        for (String avatar : avatarTemplates) {
            System.out.println("   查找: " + avatar);
            Mat template = templateCache.get(avatar);
            if (template == null) continue;

            Point pos = findButton(SCREENSHOT_PATH, template, avatar);
            if (pos != null) {
                avatarPositions.add(new AvatarInfo(avatar, (int)pos.x, (int)pos.y));
                System.out.println("   ✅ 记录头像: " + avatar + " 位置: (" + (int)pos.x + ", " + (int)pos.y + ")");
            } else {
                System.out.println("   ⚠️ 未找到头像: " + avatar);
            }
        }

        if (avatarPositions.isEmpty()) {
            System.out.println("❌ 未找到任何头像");
            return false;
        }

        System.out.println("   📊 共记录 " + avatarPositions.size() + " 个头像位置");

        // ========== 步骤6：8分钟循环 ==========
        System.out.println("\n--- 步骤6: 进入8分钟循环 ---");
        System.out.println("   每隔5秒随机点击已记录的头像（点击正中间）");
        System.out.println("   每隔10秒检测一次退出按钮，检测到则点击退出并结束");

        long loopStartTime = System.currentTimeMillis();
        long maxLoopTime = 480000; // 8分钟
        int clickCount = 0;
        long lastExitCheck = 0;
        int exitCheckCount = 0;

        while (System.currentTimeMillis() - loopStartTime < maxLoopTime) {
            long elapsed = (System.currentTimeMillis() - loopStartTime) / 1000;

            // 每隔10秒检测一次退出按钮
            if (System.currentTimeMillis() - lastExitCheck >= 10000) {
                lastExitCheck = System.currentTimeMillis();
                exitCheckCount++;

                System.out.print("   🔍 第 " + exitCheckCount + " 次检测退出按钮... ");

                // 截图检测退出按钮
                if (takeScreenshot()) {
                    Mat exitTemplate = templateCache.get("button_tuichu.png");
                    if (exitTemplate != null) {
                        Point exitPos = findButton(SCREENSHOT_PATH, exitTemplate, "button_tuichu.png");
                        if (exitPos != null) {
                            System.out.println("✅ 检测到退出按钮！位置: (" + (int)exitPos.x + ", " + (int)exitPos.y + ")");
                            // 点击退出按钮
                            randomClick((int)exitPos.x, (int)exitPos.y);
                            humanDelay(1000, 1500);
                            System.out.println("   ✅ 已点击退出，任务结束");
                            return true;
                        } else {
                            System.out.println("❌ 未检测到退出按钮");
                        }
                    } else {
                        System.out.println("❌ 退出按钮模板不存在");
                    }
                } else {
                    System.out.println("❌ 截图失败");
                }
            }

            // 随机选择一个已记录的头像，点击正中间（不偏移）
            int idx = random.nextInt(avatarPositions.size());
            AvatarInfo selected = avatarPositions.get(idx);
            clickCount++;

            System.out.println("   🎲 第 " + clickCount + " 次点击，随机选择: " + selected.name +
                    " 位置: (" + selected.x + ", " + selected.y + ")");
            // 点击正中间（不偏移）
            centerClick(selected.x, selected.y);

            // 每30秒输出一次进度
            if (elapsed % 30 == 0 && elapsed > 0) {
                System.out.println("   ⏳ 任务进行中... " + elapsed + " 秒 / 480秒");
            }

            // 等待5秒
            Thread.sleep(5000);
        }

        System.out.println("\n   ⏰ 8分钟循环结束");

        // ========== 步骤7：程序结束 ==========
        System.out.println("\n--- 步骤7: 程序结束 ---");

        return true;
    }

    static class AvatarInfo {
        String name;
        int x, y;

        AvatarInfo(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * 点击正中间（不偏移）
     */
    private static void centerClick(int x, int y) throws Exception {
        execAdb(adb + " -s " + device + " shell input tap " + x + " " + y);
        System.out.println("   📱 点击正中间: (" + x + ", " + y + ")");
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
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
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
                randomClick((int)pos.x, (int)pos.y);
                return true;
            }

            Thread.sleep(300);
        }
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
            switch (templateName) {
                case "button_activity.png": threshold = 0.30; break;
                case "button_jianghu.png": threshold = 0.50; break;
                case "button_jianghu2.png": threshold = 0.50; break;
                case "button_qianwang.png": threshold = 0.50; break;
                case "button_jinru.png": threshold = 0.50; break;
                case "button_tuichu.png": threshold = 0.50; break;
                default:
                    threshold = 0.65;
                    break;
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

    private static boolean takeScreenshot() {
        try {
            new File(SCREENSHOT_PATH).delete();
            String cmd = adb + " -s " + device + " exec-out screencap -p";
            ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(SCREENSHOT_PATH)) {
                p.getInputStream().transferTo(fos);
            }
            if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return new File(SCREENSHOT_PATH).exists() && new File(SCREENSHOT_PATH).length() > 0;
        } catch (Exception e) {
            return false;
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
        String templatePath = baseDir + File.separator + "templates" + File.separator + "file5" + File.separator;
        System.out.println("   程序目录: " + baseDir);
        System.out.println("   模板路径: " + templatePath);

        String[] templates = {
                "button_activity.png",
                "button_jianghu.png", "button_jianghu2.png",
                "button_qianwang.png",
                "button_jinru.png",
                "button_1.png", "button_2.png",
                "button_tuichu.png"
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