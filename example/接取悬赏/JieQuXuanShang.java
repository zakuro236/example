package com.example.接取悬赏;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JieQuXuanShang {

    // 同时输出到控制台和文件的PrintStream
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
    private static Point lastFoundPos = null; // 最后找到的按钮位置

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
        // 同时输出到控制台和文件
        PrintStream out = new PrintStream(new FileOutputStream("d:/MyDemo/jiequ_log.txt"));
        System.setOut(new TeePrintStream(System.out, out));
        System.setErr(new TeePrintStream(System.err, out));

        try {
            System.out.println("=== 接取悬赏脚本 ===\n");

            // 初始化ADB配置
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
                System.out.println("\n✅ 接取悬赏完成！");
            } else {
                System.out.println("\n❌ 执行失败！");
            }

            cleanup();
        } catch (Exception e) {
            System.out.println("❌ 接取悬赏异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            out.close();
        }
    }

    private static boolean execute() throws Exception {
        // 步骤1：查找并存储活动位置
        Point huoDongPos = null;
        if (!clickButtonWithStore("点击活动", "button_huo_dong.png", 8000)) return false;
        huoDongPos = lastFoundPos;
        humanDelay(1500, 2000);

        // 记录叉号位置
        takeScreenshot();
        Point chahaoPos = findButtonOnce("button_cha_hao.png", 2000);
        if (chahaoPos != null) {
            System.out.println("   📍 记录叉号位置: (" + (int)chahaoPos.x + ", " + (int)chahaoPos.y + ")");
        } else {
            System.out.println("   ⚠️ 未找到叉号位置！");
        }

        // 步骤2：点击悬赏（找不到则重新点活动，防止网卡了没反应）
        boolean xuanShangSuccess = false;
        for (int retryHuoDong = 0; retryHuoDong < 3; retryHuoDong++) {
            if (clickButton("点击悬赏", "button_xuan_shang.png", 5000)) {
                xuanShangSuccess = true;
                break;
            }
            if (retryHuoDong < 2) {
                System.out.println("   ⚠️ 找到活动但未出现悬赏，可能网卡了，重新点击活动...");
                if (huoDongPos != null) {
                    randomClick((int)huoDongPos.x, (int)huoDongPos.y);
                } else {
                    clickButton("重新点击活动", "button_huo_dong.png", 5000);
                }
                humanDelay(2000, 2500);
            }
        }
        if (!xuanShangSuccess) {
            System.out.println("❌ 连续3次点击活动后仍无法找到悬赏");
            return false;
        }
        humanDelay(1500, 2000);

        // 步骤3：点击分类
        if (!clickButton("点击分类", "button_fen_lei.png", 8000)) return false;
        humanDelay(1500, 2000);

        // 步骤4：点击新秀阴阳流
        if (!clickButton("点击新秀阴阳流", "button_xin_xiu_yin_yang_liu.png", 8000)) return false;
        humanDelay(1500, 2000);

        // 步骤5：5秒内连续点击10次下一页
        System.out.println("\n========== 连续点击下一页 ==========");
        long startTime = System.currentTimeMillis();
        int clickCount = 0;
        while (System.currentTimeMillis() - startTime < 5000) {
            takeScreenshot();
            Point pos = findButtonOnce("button_xia_yi_ye.png", 1000);
            if (pos != null) {
                randomClick((int)pos.x, (int)pos.y);
                clickCount++;
                System.out.println("   ✅ 点击下一页 第" + clickCount + "次");
                humanDelay(400, 600);
            }
        }
        System.out.println("   📊 共点击了 " + clickCount + " 次下一页");

        // 步骤6：循环10次接取悬赏
        System.out.println("\n========== 循环接取悬赏 ==========");
        for (int i = 1; i <= 10; i++) {
            System.out.println("\n========== 第 " + i + "/10 次 ==========");

            // 随机点击接取
            if (!clickButton("随机点击接取", "button_jie_qu.png", 5000)) {
                System.out.println("   ⚠️ 未找到接取按钮，跳过");
            }
            humanDelay(800, 1200);

            // 点击押金
            if (!clickButton("点击押金", "button_ya_jin.png", 5000)) {
                System.out.println("   ⚠️ 未找到押金按钮，跳过");
            }
            humanDelay(800, 1200);
        }

        // 步骤7：点击存储的叉号位置2次
        if (chahaoPos != null) {
            System.out.println("\n========== 关闭操作 ==========");
            System.out.println("   ✅ 点击叉号 第1次");
            randomClick((int)chahaoPos.x, (int)chahaoPos.y);
            humanDelay(2000, 2000); // 等2秒

            System.out.println("   ✅ 点击叉号 第2次");
            randomClick((int)chahaoPos.x, (int)chahaoPos.y);
        }

        return true;
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

    /**
     * 点击按钮并存储找到的位置（用于后续重试）
     */
    private static boolean clickButtonWithStore(String stepName, String templateName, int timeoutMs) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                System.out.println("   🔄 " + stepName + " 第 " + attempt + " 次重试");
                humanDelay(800, 1500);
            }
            takeScreenshot();
            Point pos = findButtonOnce(templateName, timeoutMs);
            if (pos != null) {
                lastFoundPos = pos;
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
            double threshold = 0.55;
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

        // 模板路径在 d:\MyDemo\templates\file7
        String baseDir = getProgramDirectory();
        String basePath = baseDir + "/templates/file7/";
        System.out.println("   程序目录: " + baseDir);
        System.out.println("   模板路径: " + basePath);

        String[] templates = {
            "button_huo_dong.png",
            "button_xuan_shang.png",
            "button_fen_lei.png",
            "button_xin_xiu_yin_yang_liu.png",
            "button_xia_yi_ye.png",
            "button_jie_qu.png",
            "button_ya_jin.png",
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
