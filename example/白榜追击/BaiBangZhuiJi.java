package com.example.白榜追击;

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

public class BaiBangZhuiJi {

    private static String adb;
    private static String device;
    private static final String SCREENSHOT_PATH = "./temp_screen.png";
    private static final Random random = new Random();
    private static final Map<String, Mat> templateCache = new HashMap<>();

    // 存储攻击按钮模板名称列表（自动读取）
    private static final List<String> attackButtonTemplates = new ArrayList<>();

    // 存储头像模板列表（自动读取）
    private static final List<String> avatarTemplates = new ArrayList<>();

    // 头像到后续按钮的映射
    private static final Map<String, String> AVATAR_TO_NEXT = new HashMap<>();

    static {
        loadOpenCvDll();
    }

    /**
     * 加载 OpenCV DLL（兼容打包后的路径）
     */
    private static void loadOpenCvDll() {
        String[] dllPaths = {
            "D:\\MyDemo\\opencv_java490.dll",
            "./opencv_java490.dll",
            "opencv_java490.dll",
            System.getProperty("user.dir") + "\\opencv_java490.dll"
        };
        
        for (String path : dllPaths) {
            File dllFile = new File(path);
            if (dllFile.exists()) {
                System.out.println("✅ 找到OpenCV DLL: " + dllFile.getAbsolutePath());
                System.load(dllFile.getAbsolutePath());
                return;
            }
        }
        
        System.out.println("⚠️ 未找到OpenCV DLL，尝试默认路径...");
        try {
            System.load(dllPaths[0]);
        } catch (UnsatisfiedLinkError e) {
            System.out.println("❌ OpenCV DLL加载失败!");
            System.out.println("请确保 opencv_java490.dll 在 exe 同目录下");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 白榜追击脚本 ===\n");

        // ========== 添加ADB配置读取 ==========
        com.example.自动配置.AdbConfig.init();
        adb = com.example.自动配置.AdbConfig.getAdbPath();
        device = com.example.自动配置.AdbConfig.getDevice();
        // ==================================

        preloadTemplates();

        // 显示加载的信息
        System.out.println("\n📊 已加载 " + avatarTemplates.size() + " 个头像模板:");
        for (String avatar : avatarTemplates) {
            String nextButton = AVATAR_TO_NEXT.get(avatar);
            System.out.println("   - " + avatar + " → " + nextButton);
        }

        System.out.println("\n📊 已加载 " + attackButtonTemplates.size() + " 个攻击按钮模板:");
        for (String btn : attackButtonTemplates) {
            System.out.println("   - " + btn);
        }
        System.out.println();

        int taskCount = 0;
        int maxTasks = 3;

        while (taskCount < maxTasks) {
            taskCount++;
            System.out.println("\n========== 开始第 " + taskCount + " 次白榜追击 ==========\n");
            if (!executeOneQuest()) {
                System.out.println("\n❌ 第 " + taskCount + " 次任务失败，停止执行");
                break;
            }
            System.out.println("\n✅ 第 " + taskCount + " 次白榜追击完成");
            if (taskCount < maxTasks) {
                System.out.println("\n⏳ 等待5秒后开始下一次任务...");
                humanDelay(5000, 7000);
            }
        }

        System.out.println("\n✅ 所有白榜追击完成！");
        cleanup();
    }

    private static boolean executeOneQuest() throws Exception {
        // 步骤1：点击活动
        System.out.println("--- 步骤1: 点击活动 ---");
        if (!clickButton("button_activity.png", 5000)) return false;
        humanDelay(1500, 2000);

        // 步骤2：点击行当
        System.out.println("--- 步骤2: 点击行当 ---");
        if (!clickButtonWithAlternatives("button_xingdang.png", "button_xingdang2.png", 3000)) {
            System.out.println("   ⚠️ 行当按钮未找到，继续执行");
        }
        humanDelay(1000, 1500);

        // 步骤3：点击揭榜
        System.out.println("--- 步骤3: 点击揭榜 ---");
        if (!clickButton("button_jiebang.png", 5000)) return false;
        humanDelay(1500, 2000);

        // 步骤4：识别头像，如果没有找到则滑动（最多3次）
        System.out.println("\n--- 步骤4: 识别头像 ---");

        String selectedAvatar = null;
        Point selectedPos = null;

        // 最多尝试3次滑动
        int maxSwipeAttempts = 3;
        boolean foundAvatar = false;

        for (int searchAttempt = 0; searchAttempt <= maxSwipeAttempts && !foundAvatar; searchAttempt++) {
            if (searchAttempt > 0) {
                System.out.println("\n   🔄 第 " + searchAttempt + " 次滑动翻页...");
                // 使用画子按钮滑动
                if (!swipeUsingHuazi()) {
                    System.out.println("   ⚠️ 滑动失败");
                    break;
                }
                humanDelay(1500, 2000);
            }

            List<String> foundAvatars = new ArrayList<>();
            List<Point> foundPositions = new ArrayList<>();

            // 只遍历真正的头像模板
            for (String avatar : avatarTemplates) {
                Point pos = findButtonOnce(avatar, 2000);
                if (pos != null) {
                    foundAvatars.add(avatar);
                    foundPositions.add(pos);
                    System.out.println("   ✅ 找到: " + avatar);
                }
            }

            if (!foundAvatars.isEmpty()) {
                int idx = random.nextInt(foundAvatars.size());
                selectedAvatar = foundAvatars.get(idx);
                selectedPos = foundPositions.get(idx);
                System.out.println("\n🎲 随机点击: " + selectedAvatar);
                foundAvatar = true;
                break;
            }

            if (searchAttempt < maxSwipeAttempts) {
                System.out.println("   ⚠️ 未找到任何头像，将进行第 " + (searchAttempt + 1) + " 次滑动...");
            }
        }

        if (!foundAvatar) {
            System.out.println("❌ 多次滑动后仍未找到任何头像");
            return false;
        }

        // 点击选中的头像
        randomClick((int)selectedPos.x, (int)selectedPos.y);
        humanDelay(1000, 1500);

        String nextButton = AVATAR_TO_NEXT.get(selectedAvatar);
        if (nextButton == null) {
            System.out.println("❌ 无法确定后续按钮");
            return false;
        }

        // 步骤5：等待15秒
        System.out.println("\n--- 步骤5: 等待15秒 ---");
        Thread.sleep(15000);

        // 步骤6：找对应的后续按钮（最多3次，找不到就点叉号）
        System.out.println("\n--- 步骤6: 查找 " + nextButton + " ---");

        boolean nextClicked = false;
        for (int attempt = 1; attempt <= 3 && !nextClicked; attempt++) {
            System.out.println("   第 " + attempt + " 次尝试找 " + nextButton);

            Point nextPos = findButtonOnce(nextButton, 5000);
            if (nextPos != null) {
                System.out.println("   ✅ 找到 " + nextButton);
                randomClick((int)nextPos.x, (int)nextPos.y);
                humanDelay(1000, 1500);
                nextClicked = true;
                break;
            }

            System.out.println("   ⚠️ 未找到 " + nextButton + "，尝试关闭弹窗...");
            Point closePos = findButtonOnce("button_close.png", 2000);
            if (closePos != null) {
                randomClick((int)closePos.x, (int)closePos.y);
                humanDelay(1000, 1500);
            }
        }

        if (!nextClicked) {
            System.out.println("❌ 未能点击后续按钮，任务失败");
            return false;
        }

        // 步骤7：等待60秒
        System.out.println("\n--- 步骤7: 等待60秒 ---");
        Thread.sleep(60000);

        // 步骤8：找攻击按钮（最多3次，找不到就点叉号）
        System.out.println("\n--- 步骤8: 查找攻击按钮（共 " + attackButtonTemplates.size() + " 个模板）---");
        for (String btn : attackButtonTemplates) {
            System.out.println("   支持: " + btn);
        }

        boolean attackButtonClicked = false;

        // 最多尝试3次
        for (int attempt = 1; attempt <= 3 && !attackButtonClicked; attempt++) {
            System.out.println("\n   第 " + attempt + " 次尝试查找攻击按钮...");

            // 遍历所有攻击按钮模板
            for (String attackButton : attackButtonTemplates) {
                Point attackPos = findButtonOnce(attackButton, 2000);
                if (attackPos != null) {
                    System.out.println("   ✅ 找到攻击按钮: " + attackButton);
                    System.out.println("   🎯 开始连续点击20次");

                    for (int i = 1; i <= 20; i++) {
                        randomClick((int)attackPos.x, (int)attackPos.y);
                        System.out.println("      第 " + i + " 次点击");
                        if (i < 20) Thread.sleep(500);
                    }
                    attackButtonClicked = true;
                    break;
                }
            }

            // 如果没找到任何攻击按钮，点叉号关闭弹窗
            if (!attackButtonClicked) {
                if (attempt < 3) {
                    System.out.println("   ⚠️ 第 " + attempt + " 次未找到攻击按钮，尝试关闭弹窗...");
                    Point closePos = findButtonOnce("button_close.png", 2000);
                    if (closePos != null) {
                        randomClick((int)closePos.x, (int)closePos.y);
                        System.out.println("   🔔 已关闭弹窗，等待2秒后重试...");
                        Thread.sleep(2000);
                    } else {
                        System.out.println("   ⚠️ 未找到关闭按钮，等待2秒后重试...");
                        Thread.sleep(2000);
                    }
                } else {
                    System.out.println("   ⚠️ 第3次尝试后仍未找到攻击按钮，点击关闭弹窗结束");
                    Point closePos = findButtonOnce("button_close.png", 2000);
                    if (closePos != null) {
                        randomClick((int)closePos.x, (int)closePos.y);
                        System.out.println("   🔔 已关闭弹窗");
                    }
                }
            }
        }

        if (!attackButtonClicked) {
            System.out.println("   ⚠️ 3次尝试后未找到任何攻击按钮，已关闭弹窗");
        }

        return true;
    }

    /**
     * 使用画子按钮滑动（长按画子按钮，然后向左滑动）
     */
    private static boolean swipeUsingHuazi() throws Exception {
        Point huaziPos = findButtonOnce("button_huazi.png", 3000);
        if (huaziPos == null) {
            System.out.println("   ❌ 未找到画子按钮，无法滑动");
            return false;
        }

        System.out.println("   ✅ 找到画子按钮，位置: (" + (int)huaziPos.x + ", " + (int)huaziPos.y + ")");
        System.out.println("   🖱️ 长按画子按钮并滑动到最左侧");

        int startX = (int)huaziPos.x;
        int startY = (int)huaziPos.y;
        int endX = 50;
        int endY = startY;

        String cmd = String.format("%s -s %s shell input swipe %d %d %d %d %d",
                adb, device, startX, startY, endX, endY, 500);
        System.out.println("   📱 长按滑动: (" + startX + "," + startY + ") → (" + endX + "," + endY + ")");
        execAdb(cmd);
        Thread.sleep(800);
        return true;
    }

    private static int getScreenWidth() throws Exception {
        Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell wm size");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        p.waitFor();
        if (line != null && line.contains("x")) {
            String[] parts = line.split("x");
            return Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
        }
        return 1280;
    }

    private static boolean clickButtonWithAlternatives(String primary, String secondary, int timeoutMs) throws Exception {
        if (clickButton(primary, timeoutMs)) return true;
        System.out.println("   🔄 主模板未找到，尝试备选模板: " + secondary);
        return clickButton(secondary, timeoutMs);
    }

    private static boolean clickButton(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();
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

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();
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
                case "button_xingdang.png": threshold = 0.50; break;
                case "button_xingdang2.png": threshold = 0.50; break;
                case "button_jiebang.png": threshold = 0.50; break;
                case "button_close.png": threshold = 0.40; break;
                case "button_huazi.png": threshold = 0.40; break;
                default:
                    if (attackButtonTemplates.contains(templateName)) {
                        threshold = 0.55;
                    } else if (templateName.contains("avatar")) {
                        threshold = 0.65;
                    } else {
                        threshold = 0.40;
                    }
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

    public static Mat removeBackgroundToWhite(Mat src) {
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
        String basePath = baseDir + File.separator + "templates" + File.separator + "file4" + File.separator;
        String attackPath = basePath + "act" + File.separator;

        System.out.println("   程序目录: " + baseDir);
        System.out.println("   模板路径: " + basePath);
        System.out.println("   攻击按钮路径: " + attackPath);

        String[] basicTemplates = {
                "button_activity.png",
                "button_xingdang.png", "button_xingdang2.png",
                "button_jiebang.png",
                "button_close.png", "button_huazi.png"
        };

        for (String t : basicTemplates) {
            Mat mat = imreadSafe(basePath + t);
            if (!mat.empty()) {
                templateCache.put(t, mat);
                System.out.println("   ✅ " + t);
            } else {
                System.out.println("   ⚠️ 未找到: " + basePath + t);
            }
        }

        // 自动读取头像模板
        System.out.println("\n📂 扫描头像模板...");
        File mainFolder = new File(basePath);
        if (mainFolder.exists() && mainFolder.isDirectory()) {
            File[] files = mainFolder.listFiles((dir, name) ->
                    name.toLowerCase().startsWith("button_avatar") &&
                            name.toLowerCase().endsWith(".png") &&
                            !name.matches(".*\\d{2}\\.png$") &&
                            !name.contains("555") && !name.contains("666"));

            if (files != null) {
                for (File file : files) {
                    String templateName = file.getName();
                    Mat mat = imreadSafe(file.getAbsolutePath());
                    if (!mat.empty()) {
                        templateCache.put(templateName, mat);
                        avatarTemplates.add(templateName);
                        System.out.println("   ✅ 头像: " + templateName);
                    }
                }
            }
        }

        // 自动构建头像到后续按钮的映射
        for (String avatar : avatarTemplates) {
            String numStr = avatar.replaceAll("[^0-9]", "");
            String nextButton = "button_avatar" + numStr + numStr + ".png";
            AVATAR_TO_NEXT.put(avatar, nextButton);

            Mat nextMat = imreadSafe(basePath + nextButton);
            if (!nextMat.empty() && !templateCache.containsKey(nextButton)) {
                templateCache.put(nextButton, nextMat);
                System.out.println("   ✅ 后续按钮: " + nextButton);
            } else if (nextMat.empty()) {
                System.out.println("   ⚠️ 后续按钮未找到: " + nextButton);
            }
        }

        // 自动读取攻击按钮文件夹
        System.out.println("\n📂 扫描攻击按钮文件夹: " + attackPath);
        File attackFolder = new File(attackPath);
        if (attackFolder.exists() && attackFolder.isDirectory()) {
            File[] files = attackFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                Arrays.sort(files);
                for (File file : files) {
                    String templateName = file.getName();
                    Mat mat = imreadSafe(file.getAbsolutePath());
                    if (!mat.empty()) {
                        templateCache.put(templateName, mat);
                        attackButtonTemplates.add(templateName);
                        System.out.println("   ✅ 攻击按钮: " + templateName);
                    } else {
                        System.out.println("   ⚠️ 攻击按钮加载失败: " + templateName);
                    }
                }
            }
        } else {
            System.out.println("   ⚠️ 攻击按钮文件夹不存在: " + attackPath);
        }
    }

    static void cleanup() {
        new File(SCREENSHOT_PATH).delete();
        for (Mat m : templateCache.values()) m.release();
        templateCache.clear();
        System.out.println("\n🧹 已清理资源");
    }
}