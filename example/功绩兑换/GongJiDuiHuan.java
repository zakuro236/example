package com.example.功绩兑换;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GongJiDuiHuan {

    static class TeePrintStream extends PrintStream {
        private final PrintStream branch;
        public TeePrintStream(PrintStream main, PrintStream branch) {
            super(main);
            this.branch = branch;
        }
        @Override
        public void println(String x) {
            super.println(x);
            branch.println(x);
        }
        @Override
        public void print(String s) {
            super.print(s);
            branch.print(s);
        }
        @Override
        public void println(int x) {
            super.println(x);
            branch.println(x);
        }
    }

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
        PrintStream out = new PrintStream(new FileOutputStream("d:/MyDemo/gongji_log.txt"));
        System.setOut(new TeePrintStream(System.out, out));
        System.setErr(new TeePrintStream(System.err, out));

        try {
            System.out.println("=== 功绩兑换脚本 ===\n");

            com.example.自动配置.AdbConfig.init();
            adb = com.example.自动配置.AdbConfig.getAdbPath();
            device = com.example.自动配置.AdbConfig.getDevice();

            int width = getScreenWidth();
            int height = getScreenHeight();
            System.out.println("   屏幕尺寸: " + width + "x" + height);

            preloadTemplates();

            System.out.println("5秒后开始执行...");
            Thread.sleep(5000);

            boolean success = execute();

            if (success) {
                System.out.println("\n✅ 功绩兑换完成！");
            } else {
                System.out.println("\n❌ 执行失败！");
            }

            cleanup();
        } catch (Exception e) {
            System.out.println("❌ 功绩兑换异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            out.close();
        }
    }

    private static boolean execute() throws Exception {
        // 步骤1：点击背包
        if (!clickButton("点击背包", "button_bei_bao.png", 8000)) return false;
        humanDelay(1500, 2000);

        // 步骤2：点击积分
        if (!clickButton("点击积分", "button_ji_fen.png", 8000)) return false;
        humanDelay(1500, 2000);

        // 步骤3：点击江湖
        if (!clickButton("点击江湖", "button_jiang_hu.png", 8000)) return false;
        humanDelay(1500, 2000);

        // 步骤4：查找功绩和兑换，同一排则点击兑换
        System.out.println("\n--- 查找功绩兑换 ---");
        if (!clickGongJiDuiHuan()) return false;
        humanDelay(1500, 2000);

        // 步骤5：点击铜钱
        if (!clickButton("点击铜钱", "button_tong_qian.png", 5000)) {
            System.out.println("   ⚠️ 未找到铜钱，跳过");
        }
        humanDelay(800, 1200);

        // 步骤6：点击输入
        if (!clickButton("点击输入", "button_shu_ru.png", 5000)) {
            System.out.println("   ⚠️ 未找到输入，跳过");
        }
        humanDelay(800, 1200);

        // 步骤7：点击5按钮3次（输入555）
        System.out.println("\n--- 输入555 ---");
        takeScreenshot();
        Point fivePos = findButtonOnce("button_5.png", 3000);
        if (fivePos != null) {
            System.out.println("   ✅ 找到5按钮位置");
            for (int i = 0; i < 3; i++) {
                randomClick((int)fivePos.x, (int)fivePos.y);
                System.out.println("   ✅ 输入5 第" + (i+1) + "次");
                humanDelay(200, 300);
            }
        } else {
            System.out.println("   ⚠️ 未找到5按钮");
        }

        // 步骤8：点击2次功绩兑换
        System.out.println("\n--- 点击2次功绩兑换 ---");
        for (int i = 1; i <= 2; i++) {
            if (!clickButton("点击功绩兑换", "button_gong_ji_dui_huan.png", 5000)) {
                System.out.println("   ⚠️ 第" + i + "次点击功绩兑换失败");
                break;
            }
            System.out.println("   ✅ 点击功绩兑换 第" + i + "次");
            humanDelay(800, 1200);
        }

        // 步骤9：点击叉号
        if (!clickButton("点击叉号", "button_cha_hao.png", 3000)) {
            System.out.println("   ⚠️ 未找到叉号");
        }

        return true;
    }

    /**
     * 查找功绩和兑换按钮，如果它们在同一排，则点击兑换
     */
    private static boolean clickGongJiDuiHuan() throws Exception {
        takeScreenshot();
        Point gongJiPos = findButtonOnce("button_gong_ji.png", 5000);
        Point duiHuanPos = findButtonOnce("button_dui_huan.png", 5000);

        if (gongJiPos != null && duiHuanPos != null) {
            // 检查是否在同一排（y坐标差小于阈值，比如30像素）
            double yDiff = Math.abs(gongJiPos.y - duiHuanPos.y);
            System.out.println("   功绩位置: (" + (int)gongJiPos.x + ", " + (int)gongJiPos.y + ")");
            System.out.println("   兑换位置: (" + (int)duiHuanPos.x + ", " + (int)duiHuanPos.y + ")");
            System.out.println("   Y坐标差: " + (int)yDiff);

            if (yDiff < 50) {
                System.out.println("   ✅ 功绩和兑换在同一排，点击兑换");
                randomClick((int)duiHuanPos.x, (int)duiHuanPos.y);
                return true;
            } else {
                System.out.println("   ⚠️ 功绩和兑换不在同一排");
            }
        } else {
            System.out.println("   ⚠️ 未找到功绩或兑换按钮");
        }
        return false;
    }

    private static boolean clickButton(String stepName, String templateName, int timeoutMs) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                System.out.println("   🔄 " + stepName + " 第 " + attempt + " 次重试");
                humanDelay(800, 1500);
            }
            takeScreenshot();
            Point pos = findButtonOnce(templateName, timeoutMs);
            if (pos != null) {
                randomClick((int)pos.x, (int)pos.y);
                System.out.println("   ✅ " + stepName + " 成功");
                return true;
            }
        }
        System.out.println("   ❌ " + stepName + " 失败");
        return false;
    }

    private static Point findButtonOnce(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
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

    private static void preloadTemplates() {
        System.out.println("📦 预加载模板...");

        String baseDir = getProgramDirectory();
        String basePath = baseDir + "/templates/file9/";
        System.out.println("   程序目录: " + baseDir);
        System.out.println("   模板路径: " + basePath);

        String[] templates = {
            "button_bei_bao.png",
            "button_ji_fen.png",
            "button_jiang_hu.png",
            "button_gong_ji.png",
            "button_dui_huan.png",
            "button_tong_qian.png",
            "button_shu_ru.png",
            "button_5.png",
            "button_gong_ji_dui_huan.png",
            "button_cha_hao.png"
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

    private static void humanDelay(int minMs, int maxMs) {
        try {
            if (minMs == maxMs) {
                Thread.sleep(minMs);
            } else {
                int delay = minMs + random.nextInt(maxMs - minMs);
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {}
    }

    /**
     * 安全执行ADB命令，消费输出流防止进程死锁
     */
    private static void execAdb(String cmd) throws Exception {
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

    private static void randomClick(int x, int y) {
        int offsetX = random.nextInt(11) - 5;
        int offsetY = random.nextInt(11) - 5;
        String cmd = String.format("%s -s %s shell input tap %d %d", adb, device, x + offsetX, y + offsetY);
        try {
            execAdb(cmd);
        } catch (Exception e) {
            System.out.println("   ⚠️ 点击失败: " + e.getMessage());
        }
    }

    private static boolean takeScreenshot() {
        try {
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

    private static int getScreenWidth() throws Exception {
        Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell wm size");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        reader.close();
        if (line != null && line.contains("x")) {
            String[] parts = line.replaceAll("\\D+", " ").trim().split("\\s+");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        }
        return 1280;
    }

    private static int getScreenHeight() throws Exception {
        Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell wm size");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        reader.close();
        if (line != null && line.contains("x")) {
            String[] parts = line.replaceAll("\\D+", " ").trim().split("\\s+");
            if (parts.length >= 1) {
                return Integer.parseInt(parts[0]);
            }
        }
        return 720;
    }

    static void cleanup() {
        new File(SCREENSHOT_PATH).delete();
        for (Mat m : templateCache.values()) m.release();
        templateCache.clear();
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
}
