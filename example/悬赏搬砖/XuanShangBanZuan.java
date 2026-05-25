package com.example.悬赏搬砖;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class XuanShangBanZuan {

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
    private static final int MAX_REWARDS = 10; // 最大悬赏次数
    private static Point storedHuoDongPos = null; // 存储的活动按钮位置
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
        PrintStream out = new PrintStream(new FileOutputStream("d:/MyDemo/banzuan_log.txt"));
        System.setOut(new TeePrintStream(System.out, out));
        System.setErr(new TeePrintStream(System.err, out));

        try {
            System.out.println("=== 悬赏搬砖脚本 ===\n");

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

            execute();

            System.out.println("\n✅ 悬赏搬砖完成！");

            cleanup();
        } catch (Exception e) {
            System.out.println("❌ 悬赏搬砖异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            out.close();
        }
    }

    private static void execute() throws Exception {
        int rewardCount = 0;
        Point chahaoPos = null; // 存储叉号位置

        while (rewardCount < MAX_REWARDS) {
            rewardCount++;
            System.out.println("\n" + "=".repeat(50));
            System.out.println("========== 第 " + rewardCount + "/" + MAX_REWARDS + " 次悬赏 ==========");
            System.out.println("=".repeat(50));

            // ===== 步骤1：点击活动（找不到则结束） =====
            boolean clickSuccess = false;
            if (storedHuoDongPos != null) {
                // 使用存储的活动坐标点击
                System.out.println("   ✅ 使用存储的活动坐标: (" + (int)storedHuoDongPos.x + ", " + (int)storedHuoDongPos.y + ")");
                randomClick((int)storedHuoDongPos.x, (int)storedHuoDongPos.y);
                clickSuccess = true;
            } else {
                // 第一次，查找并存储活动坐标
                if (clickButtonWithStore("点击活动", "button_huo_dong.png", 8000)) {
                    storedHuoDongPos = lastFoundPos; // 存储找到的位置
                    if (storedHuoDongPos != null) {
                        System.out.println("   📍 存储活动位置: (" + (int)storedHuoDongPos.x + ", " + (int)storedHuoDongPos.y + ")");
                    }
                    clickSuccess = true;
                }
            }
            if (!clickSuccess) {
                System.out.println("❌ 无法找到活动按钮，尝试点击叉号结束");
                clickStoredChahao(chahaoPos);
                break;
            }
            humanDelay(1500, 2000);

            // 记录叉号位置
            takeScreenshot();
            chahaoPos = findButtonOnce("button_cha_hao.png", 2000);
            if (chahaoPos != null) {
                System.out.println("   📍 记录叉号位置: (" + (int)chahaoPos.x + ", " + (int)chahaoPos.y + ")");
            }

            // ===== 步骤2：点击悬赏（如果找不到，说明活动没反应/网卡了，重新点击活动） =====
            boolean xuanShangSuccess = false;
            for (int retryHuoDong = 0; retryHuoDong < 3; retryHuoDong++) {
                if (clickButton("点击悬赏", "button_xuan_shang.png", 5000)) {
                    xuanShangSuccess = true;
                    break;
                }
                // 点击了活动但找不到悬赏 → 可能是网卡了没反应，重新点活动
                if (retryHuoDong < 2) {
                    System.out.println("   ⚠️ 找到活动但未出现悬赏，可能网卡了，重新点击活动...");
                    if (storedHuoDongPos != null) {
                        randomClick((int)storedHuoDongPos.x, (int)storedHuoDongPos.y);
                    } else {
                        clickButton("重新点击活动", "button_huo_dong.png", 5000);
                    }
                    humanDelay(2000, 2500);
                }
            }
            if (!xuanShangSuccess) {
                System.out.println("❌ 连续3次点击活动后仍无法找到悬赏");
                clickStoredChahao(chahaoPos);
                break;
            }
            humanDelay(1500, 2000);

            // ===== 步骤3：检测前往按钮是否存在 =====
            takeScreenshot();
            Point qianWangPos = findButtonOnce("button_qian_wang.png", 2000);
            if (qianWangPos == null) {
                // 没找到前往按钮，说明搬砖完了
                System.out.println("❌ 未找到前往按钮，搬砖完成！");
                System.out.println(">>> 点击两次叉号返回主界面...");
                clickStoredChahao(chahaoPos);
                clickStoredChahao(chahaoPos);
                break; // 退出循环，结束程序
            }

            // ===== 步骤4：点击前往 =====
            if (!clickButton("点击前往", "button_qian_wang.png", 8000)) {
                System.out.println("❌ 无法点击前往");
                clickStoredChahao(chahaoPos);
                break;
            }
            humanDelay(1500, 2000);

            // ===== 步骤5：60秒跑图找新秀 =====
            System.out.println("\n--- 开始跑图寻找新秀 (60秒) ---");
            long startTime = System.currentTimeMillis();
            boolean foundXinXiu = false;

            while (System.currentTimeMillis() - startTime < 60000) {
                takeScreenshot();
                Point xinXiuPos = findButtonOnce("button_xin_xiu.png", 1000);
                if (xinXiuPos != null) {
                    System.out.println("   ✅ 找到新秀!");
                    randomClick((int)xinXiuPos.x, (int)xinXiuPos.y);
                    humanDelay(1500, 2000);
                    foundXinXiu = true;

                    // 检测是否出现创建队伍
                    takeScreenshot();
                    Point createTeamPos = findButtonOnce("button_chuang_jian_dui_wu.png", 2000);
                    if (createTeamPos != null) {
                        System.out.println("   检测到创建队伍按钮，执行创建队伍流程...");

                        // 点击创建队伍
                        if (clickButton("点击创建队伍", "button_chuang_jian_dui_wu.png", 3000)) {
                            humanDelay(1000, 1500);
                        }

                        // 点击叉号
                        if (clickButton("点击叉号", "button_cha_hao.png", 3000)) {
                            humanDelay(1000, 1500);
                        }

                        // 点击对话
                        if (clickButton("点击对话", "button_dui_hua.png", 3000)) {
                            humanDelay(1000, 1500);
                        }

                        // 再次点击新秀
                        if (clickButton("点击新秀", "button_xin_xiu.png", 3000)) {
                            humanDelay(1000, 1500);
                        }
                    }
                    break;
                }
                humanDelay(500, 800);
            }

            if (!foundXinXiu) {
                System.out.println("   ⚠️ 60秒内未找到新秀");
                clickStoredChahao(chahaoPos);
                break;
            }

            // ===== 步骤6：点击副本 =====
            if (!clickButton("点击副本", "button_fu_ben.png", 8000)) {
                System.out.println("❌ 无法点击副本");
                clickStoredChahao(chahaoPos);
                break;
            }
            humanDelay(2000, 2500);

            // ===== 步骤7：检测并存储退出按钮位置 =====
            takeScreenshot();
            Point exitPos = findButtonOnce("button_tui_chu.png", 5000);
            if (exitPos != null) {
                System.out.println("   📍 存储退出按钮位置: (" + (int)exitPos.x + ", " + (int)exitPos.y + ")");
            } else {
                System.out.println("   ⚠️ 未找到退出按钮");
                clickStoredChahao(chahaoPos);
                break;
            }

            // ===== 步骤8：进入循环 - 每隔10秒检测钟表 =====
            System.out.println("\n--- 进入战斗循环，检测钟表 ---");
            boolean foundClock = false;

            while (true) {
                Thread.sleep(10000); // 等待10秒

                takeScreenshot();
                Point clockPos = findButtonOnce("button_zhong_biao.png", 2000);
                if (clockPos != null) {
                    System.out.println("   ✅ 检测到钟表，完成悬赏!");
                    foundClock = true;

                    // 点击存储的退出按钮
                    if (exitPos != null) {
                        System.out.println("   ✅ 点击退出按钮");
                        randomClick((int)exitPos.x, (int)exitPos.y);
                        humanDelay(1500, 2000);
                    }

                    // 检测确定按钮，如果没有则回退重试
                    int confirmRetry = 0;
                    while (confirmRetry < 3) {
                        takeScreenshot();
                        Point quedingPos = findButtonOnce("button_que_ding.png", 2000);
                        if (quedingPos != null) {
                            System.out.println("   ✅ 点击确定");
                            randomClick((int)quedingPos.x, (int)quedingPos.y);
                            System.out.println("   ✅ 完成第 " + rewardCount + " 次悬赏!");
                            humanDelay(2000, 2500);
                            break;
                        } else {
                            confirmRetry++;
                            if (confirmRetry >= 3) {
                                System.out.println("   ❌ 连续3次未找到确定按钮，回退重试");
                                foundClock = false;
                                break;
                            }
                            // 回退到检测钟表状态
                            System.out.println("   ⚠️ 未找到确定按钮，重新检测钟表...");
                            takeScreenshot();
                            clockPos = findButtonOnce("button_zhong_biao.png", 2000);
                            if (clockPos == null) {
                                System.out.println("   ⏰ 钟表已消失，等待10秒...");
                                Thread.sleep(10000);
                                continue;
                            }
                            System.out.println("   ✅ 再次检测到钟表，重新点击退出按钮");
                            // 再次点击退出按钮
                            if (exitPos != null) {
                                randomClick((int)exitPos.x, (int)exitPos.y);
                                humanDelay(1500, 2000);
                            }
                        }
                    }

                    if (foundClock) break;
                } else {
                    System.out.println("   ⏰ 10秒后继续检测...");
                }
            }

            // 如果完成10次，退出循环
            if (rewardCount >= MAX_REWARDS) {
                break;
            }
        }

        // ===== 结束操作：点击叉号两次 =====
        System.out.println("\n========== 结束操作 ==========");
        clickStoredChahao(chahaoPos);
    }

    /**
     * 点击存储的叉号位置两次
     */
    private static void clickStoredChahao(Point chahaoPos) {
        if (chahaoPos != null) {
            System.out.println("   ✅ 点击叉号 第1次");
            randomClick((int)chahaoPos.x, (int)chahaoPos.y);
            humanDelay(2000, 2000);

            System.out.println("   ✅ 点击叉号 第2次");
            randomClick((int)chahaoPos.x, (int)chahaoPos.y);
            humanDelay(1000, 1500);
        }
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
                lastFoundPos = pos; // 记录位置
                randomClick((int)pos.x, (int)pos.y);
                System.out.println("   ✅ " + stepName + " 成功");
                return true;
            }
        }
        System.out.println("   ❌ " + stepName + " 失败");
        return false;
    }

    /**
     * 点击按钮并存储找到的位置
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
                lastFoundPos = pos; // 存储找到的位置
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

        // 模板路径在 d:\MyDemo\templates\file8
        String baseDir = getProgramDirectory();
        String basePath = baseDir + "/templates/file8/";
        System.out.println("   程序目录: " + baseDir);
        System.out.println("   模板路径: " + basePath);

        String[] templates = {
            "button_huo_dong.png",
            "button_xuan_shang.png",
            "button_qian_wang.png",
            "button_xin_xiu.png",
            "button_chuang_jian_dui_wu.png",
            "button_cha_hao.png",
            "button_dui_hua.png",
            "button_fu_ben.png",
            "button_tui_chu.png",
            "button_que_ding.png",
            "button_zhong_biao.png"
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
