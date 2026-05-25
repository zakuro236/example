package com.example.发布悬赏;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.example.自动配置.AdbConfig;

public class FaBuXuanShang {

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
    private static Rect TOP_REGION;
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
        PrintStream out = new PrintStream(new FileOutputStream("d:/MyDemo/fabu_log.txt"));
        System.setOut(new TeePrintStream(System.out, out));
        System.setErr(new TeePrintStream(System.err, out));

        try {
            System.out.println("=== 发布悬赏脚本 ===\n");

            // 初始化ADB配置
            com.example.自动配置.AdbConfig.init();
            adb = com.example.自动配置.AdbConfig.getAdbPath();
            device = com.example.自动配置.AdbConfig.getDevice();

            int width = getScreenWidth();
            int height = getScreenHeight();
            TOP_REGION = new Rect(0, 0, width, height / 2);
            System.out.println("   屏幕尺寸: " + width + "x" + height);

            preloadTemplates();

            System.out.println("5秒后开始执行...");
            Thread.sleep(5000);

            boolean success = execute();

            if (success) {
                System.out.println("\n✅ 发布悬赏完成！");
            } else {
                System.out.println("\n❌ 执行失败！");
            }

            cleanup();
        } catch (Exception e) {
            System.out.println("❌ 发布悬赏异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            out.close();
        }
    }

    private static boolean execute() throws Exception {
        // 调试：打印当前工作目录
        System.out.println(">>> execute() 开始");
        System.out.println(">>> 当前工作目录: " + System.getProperty("user.dir"));
        System.out.println(">>> 程序目录: " + getProgramDirectory());

        // 步骤1：查找并存储活动位置
        Point huoDongPos = null;
        if (!clickButtonWithStore("点击活动", "button_huo_dong.png", 8000)) return false;
        huoDongPos = lastFoundPos;
        humanDelay(1500, 2000);

        // 截图记录close按钮的位置
        takeScreenshot();
        Point closePos = findButtonOnce("button_close.png", 2000);
        if (closePos != null) {
            System.out.println(">>> 记录close位置: (" + (int)closePos.x + ", " + (int)closePos.y + ")");
        } else {
            System.out.println(">>> ⚠️ 未找到close按钮位置！");
        }

        // 步骤2：点击悬赏（找不到则重新点活动，防止网卡了没反应）
        boolean xuanShangSuccess = false;
        for (int retryHuoDong = 0; retryHuoDong < 3; retryHuoDong++) {
            if (clickButton("点击悬赏", "button_xuanshang.png", 5000)) {
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

        // 步骤3：点击发布
        if (!clickButton("点击发布", "button_fabu.png", 8000)) return false;
        humanDelay(1500, 2000);

        // ===== 执行第一个副本（直接点发布悬赏，不用切换）=====
        System.out.println("\n========== 处理第一个副本 ==========");

        // 直接发布悬赏（第一个副本）
        publishAndConfirm();

        // ===== 执行第二个副本（最上面的副本）- 循环处理直到没有确定按钮 =====
        publishSecondFubenLoop();

        // ===== 执行第三个副本（最下面的副本）- 循环处理直到没有确定按钮 =====
        boolean thirdComplete = publishThirdFubenLoop(closePos);
        if (thirdComplete) {
            System.out.println("\n✅ 发布悬赏全部完成！");
            return true;
        }

        // ===== 结束操作 =====
        System.out.println("\n========== 结束操作 ==========");

        // 点击关闭按钮（chahao1）
        for (int i = 0; i < 5; i++) {
            takeScreenshot();
            Point pos = findButtonOnce("button_chahao1.png", 3000);
            if (pos != null) {
                System.out.println("   ✅ 点击关闭按钮 成功");
                randomClick((int)pos.x, (int)pos.y);
                humanDelay(2000, 2500);
                break;
            } else if (i < 4) {
                System.out.println("   🔄 点击关闭按钮 第 " + (i+1) + " 次重试");
                humanDelay(1000, 1500);
            }
        }

        // 再次点击关闭按钮（使用close模板）
        for (int i = 0; i < 5; i++) {
            takeScreenshot();
            Point pos = findButtonOnce("button_close.png", 3000);
            if (pos != null) {
                System.out.println("   ✅ 点击X关闭按钮 成功");
                randomClick((int)pos.x, (int)pos.y);
                humanDelay(2000, 2500);
                break;
            } else if (i < 4) {
                System.out.println("   🔄 点击X关闭按钮 第 " + (i+1) + " 次重试");
                humanDelay(1000, 1500);
            }
        }

        return true;
    }

    /**
     * 发布悬赏：点击发布悬赏，如果出现确定按钮就点击
     * @return true表示点击了确定按钮（有可发布悬赏），false表示没有确定按钮（没有可发布悬赏）
     */
    private static boolean publishAndConfirm() throws Exception {
        // 点击发布悬赏
        if (!clickButton("点击发布悬赏", "button_fabuxuanshang.png", 5000)) {
            return false;
        }
        humanDelay(1000, 1500);

        // 检测钟表图标
        takeScreenshot();
        Point zhongbiaoPos = findButtonOnce("button_zhong_biao.png", 1000);
        if (zhongbiaoPos != null) {
            System.out.println("   ⏰ 检测到钟表，等待5秒...");
            humanDelay(5000, 5000);

            // 连续3次检测退出按钮（活动按钮）
            int notFoundCount = 0;
            for (int i = 0; i < 3; i++) {
                takeScreenshot();
                Point huodongPos = findButtonOnce("button_huo_dong.png", 2000);
                if (huodongPos != null) {
                    notFoundCount = 0;
                    break;
                } else {
                    notFoundCount++;
                    System.out.println("   🔄 未找到活动按钮 第 " + notFoundCount + " 次");
                    humanDelay(1000, 1500);
                }
            }

            // 连续3次都找不到活动按钮，点击退出
            if (notFoundCount >= 3) {
                System.out.println("   ⚠️ 连续3次未找到活动按钮，点击退出");
                takeScreenshot();
                Point tuiChuPos = findButtonOnce("button_tui_chu.png", 2000);
                if (tuiChuPos != null) {
                    randomClick((int)tuiChuPos.x, (int)tuiChuPos.y);
                    humanDelay(2000, 2500);
                }
            }
        }

        // 检测确定按钮
        takeScreenshot();
        Point quedingPos = findButtonOnce("button_queding.png", 500);
        if (quedingPos != null) {
            System.out.println("   ✅ 点击确定");
            randomClick((int)quedingPos.x, (int)quedingPos.y);
            humanDelay(2000, 3000);

            // 悬赏成功，重新点击发布进入发布悬赏界面
            if (!clickButton("点击发布进入发布悬赏界面", "button_fabu.png", 5000)) {
                System.out.println("   ⚠️ 无法进入发布悬赏界面");
            }
            humanDelay(1500, 2000);

            return true;
        } else {
            // 没有确定按钮
            System.out.println("   ⏰ 该副本无可发布悬赏");
            return false;
        }
    }

    /**
     * 处理第二个副本：循环切换→选副本→发布悬赏，直到没有确定按钮
     * 根据匹配数量决定点击哪个副本：
     * - 3个副本：点中间的
     * - 2个副本：点最上面的
     */
    private static void publishSecondFubenLoop() throws Exception {
        int loopCount = 0;

        while (true) {
            loopCount++;
            System.out.println("\n========== 处理第二个副本 第" + loopCount + "次 ==========");

            // 点击切换
            if (!clickButton("点击切换", "button_qiehuan.png", 5000)) {
                System.out.println("   ⚠️ 无法点击切换，结束第二个副本");
                break;
            }
            humanDelay(1000, 1500);

            // 根据匹配数量选择点击位置（中间或最上面）
            if (!clickMiddleOrTopButton("选择第二个副本", "button_fuben20.png", 8000)) {
                System.out.println("   ⚠️ 无法点击副本，结束第二个副本");
                break;
            }
            humanDelay(1500, 2000);

            // 发布悬赏
            boolean foundQueding = publishAndConfirm();
            if (!foundQueding) {
                // 没有检测到确定按钮，说明这个副本悬赏完了
                System.out.println("   ⏰ 该副本悬赏发布完成");
                break;
            }
            // 检测到确定按钮，继续循环
        }
    }

    /**
     * 处理第三个副本：循环切换→选最下面副本→发布悬赏，直到没有确定按钮
     * @param closePos 提前记录的close按钮位置
     * @return true表示完成关闭，false表示异常结束
     */
    private static boolean publishThirdFubenLoop(Point closePos) throws Exception {
        int loopCount = 0;
        System.out.println(">>> publishThirdFubenLoop() 开始, closePos=" + closePos);

        while (true) {
            loopCount++;
            System.out.println("\n========== 处理第三个副本 第" + loopCount + "次 ==========");

            // 点击切换
            if (!clickButton("点击切换", "button_qiehuan.png", 5000)) {
                System.out.println("   ⚠️ 无法点击切换，结束第三个副本");
                return false;
            }
            humanDelay(1000, 1500);

            // 点击最下面的副本
            if (!clickBottommostButton("点击最下面的副本", "button_fuben20.png", 8000)) {
                System.out.println("   ⚠️ 无法点击最下面副本，结束第三个副本");
                return false;
            }
            humanDelay(1500, 2000);

            // 发布悬赏
            System.out.println(">>> 准备调用 publishAndConfirm()");
            boolean foundQueding = publishAndConfirm();
            System.out.println(">>> publishAndConfirm() 返回: " + foundQueding);

            if (!foundQueding) {
                // 没有检测到确定按钮，悬赏次数没了
                System.out.println("   ⏰ 该副本悬赏次数已用完，开始关闭...");

                // 点chahao1
                Point chahaoPos = findButtonOnce("button_chahao1.png", 2000);
                if (chahaoPos != null) {
                    System.out.println("   ✅ 点击chahao1");
                    randomClick((int)chahaoPos.x, (int)chahaoPos.y);
                    humanDelay(1500, 2000);
                }

                // 点记录的close位置一次
                if (closePos != null) {
                    System.out.println("   ✅ 点击记录的close位置");
                    randomClick((int)closePos.x, (int)closePos.y);
                    humanDelay(2000, 2000); // 等2秒

                    // 再点一次close
                    System.out.println("   ✅ 再点一次close");
                    randomClick((int)closePos.x, (int)closePos.y);
                }
                System.out.println(">>> publishThirdFubenLoop() 返回 true");
                return true; // 完成关闭
            }
            // 检测到确定按钮，继续循环
        }
    }

    private static boolean clickButton(String stepName, String templateName, int timeoutMs) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                System.out.println("   🔄 " + stepName + " 第 " + attempt + " 次重试");
                humanDelay(800, 1500);
            }
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

    /**
     * 点击最上面的按钮（顶部区域）
     */
    private static boolean clickTopmostButton(String stepName, String templateName, int timeoutMs) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                System.out.println("   🔄 " + stepName + " 第 " + attempt + " 次重试");
                humanDelay(800, 1500);
            }
            Point pos = findTopmostButton(templateName, timeoutMs);
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
     * 根据匹配数量点击按钮（3个点中间，2个点最上）
     */
    private static boolean clickMiddleOrTopButton(String stepName, String templateName, int timeoutMs) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                System.out.println("   🔄 " + stepName + " 第 " + attempt + " 次重试");
                humanDelay(800, 1500);
            }
            Point pos = findMiddleButton(templateName, timeoutMs);
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
     * 点击最下面的按钮
     */
    private static boolean clickBottommostButton(String stepName, String templateName, int timeoutMs) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                System.out.println("   🔄 " + stepName + " 第 " + attempt + " 次重试");
                humanDelay(800, 1500);
            }
            Point pos = findBottommostButton(templateName, timeoutMs);
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
            double threshold = 0.60;
            if (mmr.maxVal >= threshold) {
                int cx = (int)(mmr.maxLoc.x + template.cols() / 2);
                int cy = (int)(mmr.maxLoc.y + template.rows() / 2);
                return new Point(cx, cy);
            }
            Thread.sleep(300);
        }
        return null;
    }

    /**
     * 带匹配度输出的查找方法
     */
    private static Point findButtonOnceWithMatch(String templateName, int timeoutMs) throws Exception {
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
            double threshold = 0.60;
            if (mmr.maxVal >= threshold) {
                int cx = (int)(mmr.maxLoc.x + template.cols() / 2);
                int cy = (int)(mmr.maxLoc.y + template.rows() / 2);
                System.out.println("      📍 " + templateName + " 匹配度: " + String.format("%.3f", mmr.maxVal));
                return new Point(cx, cy);
            } else {
                System.out.println("      📍 " + templateName + " 最高匹配度: " + String.format("%.3f", mmr.maxVal) + " (未达阈值0.65)");
            }
            Thread.sleep(300);
        }
        return null;
    }

    /**
     * 查找最上面的按钮（Y坐标最小）- 遍历所有匹配点找Y最小的
     */
    private static Point findTopmostButton(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        Point topmost = null;
        double bestMatch = 0;

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

            // 遍历所有匹配点，找到Y坐标最小的
            double threshold = 0.60;
            for (int y = 0; y < result.rows(); y++) {
                for (int x = 0; x < result.cols(); x++) {
                    double matchValue = result.get(y, x)[0];
                    if (matchValue >= threshold) {
                        int cx = x + template.cols() / 2;
                        int cy = y + template.rows() / 2;
                        if (topmost == null || cy < topmost.y) {
                            topmost = new Point(cx, cy);
                            bestMatch = matchValue;
                        }
                    }
                }
            }

            screenshot.release();
            screenshotBW.release();
            templateBW.release();
            result.release();

            if (topmost != null) {
                System.out.println("      📍 找到最上按钮: (" + (int)topmost.x + ", " + (int)topmost.y + ") 匹配度: " + String.format("%.3f", bestMatch));
                return topmost;
            }
            Thread.sleep(300);
        }
        return topmost;
    }

    /**
     * 查找最下面的按钮（Y坐标最大）- 遍历所有匹配点找Y最大的
     */
    private static Point findBottommostButton(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        Point bottommost = null;
        double bestMatch = 0;

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

            // 遍历所有匹配点，找到Y坐标最大的
            double threshold = 0.60;
            for (int y = 0; y < result.rows(); y++) {
                for (int x = 0; x < result.cols(); x++) {
                    double matchValue = result.get(y, x)[0];
                    if (matchValue >= threshold) {
                        int cx = x + template.cols() / 2;
                        int cy = y + template.rows() / 2;
                        if (bottommost == null || cy > bottommost.y) {
                            bottommost = new Point(cx, cy);
                            bestMatch = matchValue;
                        }
                    }
                }
            }

            screenshot.release();
            screenshotBW.release();
            templateBW.release();
            result.release();

            if (bottommost != null) {
                System.out.println("      📍 找到最下按钮: (" + (int)bottommost.x + ", " + (int)bottommost.y + ") 匹配度: " + String.format("%.3f", bestMatch));
                return bottommost;
            }
            Thread.sleep(300);
        }
        return bottommost;
    }

    /**
     * 查找中间的按钮 - 遍历所有匹配点找Y坐标居中的
     */
    private static Point findMiddleButton(String templateName, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        Point middle = null;
        double bestMatch = 0;

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

            // 遍历所有匹配点，找到Y坐标居中的
            List<Point> allMatches = new ArrayList<>();
            double threshold = 0.60;
            for (int y = 0; y < result.rows(); y++) {
                for (int x = 0; x < result.cols(); x++) {
                    double matchValue = result.get(y, x)[0];
                    if (matchValue >= threshold) {
                        int cx = x + template.cols() / 2;
                        int cy = y + template.rows() / 2;
                        boolean duplicate = false;
                        for (Point p : allMatches) {
                            if (Math.abs(p.x - cx) < 30 && Math.abs(p.y - cy) < 30) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            allMatches.add(new Point(cx, cy));
                        }
                    }
                }
            }

            screenshot.release();
            screenshotBW.release();
            templateBW.release();
            result.release();

            if (allMatches.size() >= 3) {
                // 按Y坐标排序，取中间的那个
                allMatches.sort(Comparator.comparingDouble(p -> p.y));
                middle = allMatches.get(1); // 中间的
                System.out.println("      📍 匹配到" + allMatches.size() + "个按钮，点击中间的: (" + (int)middle.x + ", " + (int)middle.y + ")");
                return middle;
            } else if (allMatches.size() == 2) {
                // 只有2个，按最上面的处理
                middle = allMatches.stream().min(Comparator.comparingDouble(p -> p.y)).orElse(null);
                System.out.println("      📍 匹配到" + allMatches.size() + "个按钮，点击最上面的: (" + (int)middle.x + ", " + (int)middle.y + ")");
                return middle;
            } else if (allMatches.size() == 1) {
                middle = allMatches.get(0);
                System.out.println("      📍 只匹配到1个按钮: (" + (int)middle.x + ", " + (int)middle.y + ")");
                return middle;
            }
            Thread.sleep(300);
        }
        return middle;
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

        double threshold = 0.55;
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

        String baseDir = getProgramDirectory();
        String basePath = baseDir + File.separator + "templates" + File.separator + "file6" + File.separator;
        System.out.println("   程序目录: " + baseDir);
        System.out.println("   模板路径: " + basePath);

        String[] templates = {
                "button_huo_dong.png",
                "button_xuanshang.png",
                "button_fabu.png",
                "button_fabuxuanshang.png",
                "button_queding.png",
                "button_qiehuan.png",
                "button_fuben20.png",
                "button_chahao1.png",
                "button_chahao2.png",
                "button_close.png"
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
}
