package com.example.门客设宴;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.*;
import java.util.*;

public class TestMenKeSheYan {

    private static String adb = "D:\\leidian\\LDPlayer9\\adb.exe";
    private static String device = "emulator-5554";
    private static final String SCREENSHOT_PATH = "./temp_screen.png";
    private static final Random random = new Random();
    private static final Map<String, Mat> templateCache = new HashMap<>();
    private static final String TEMPLATE_PATH = "D:\\MyDemo\\file\\file2\\";

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
        System.out.println("=== 门客设宴完整流程 ===\n");

        preloadTemplates();

        System.out.println("5秒后开始执行...");
        Thread.sleep(5000);

        boolean success = executeFullQuest();

        if (success) {
            System.out.println("\n✅ 门客设宴完成！");
        } else {
            System.out.println("\n❌ 执行失败！");
        }

        cleanup();
    }

    private static boolean executeFullQuest() throws Exception {
        // 1. 点击活动
        if (!clickButton("活动", "button_activity.png", 8000)) return false;
        humanDelay(1500, 2500);

        // 2. 尝试点击帮派（找不到就跳过）
        System.out.println("\n--- 尝试点击帮派（找不到就跳过）---");
        boolean bangFound = clickButtonWithSkip("帮派", 5000);
        if (!bangFound) {
            System.out.println("   ⚠️ 帮派按钮未找到，可能已在帮派界面，继续执行");
        }

        // 3. 点击门客设宴前往
        if (!clickMenKeSheYanGo()) return false;

        // 4. 等待NPC列表加载
        System.out.println("\n--- 等待NPC列表加载 ---");
        humanDelay(2000, 3000);

        // 5. 随机点击一个"前往邀约"
        if (!clickRandomInviteButton()) return false;

        // 6. 点击"邀请赴宴"（60秒超时）
        if (!clickInviteToParty()) return false;

        // 7. 点击"确认邀约"
        if (!clickConfirmInvite()) return false;

        return true;
    }

    /**
     * 点击按钮（必点，找不到就失败）
     */
    private static boolean clickButton(String name, String templateName, int timeoutMs) throws Exception {
        System.out.println("\n--- 点击: " + name + " ---");

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();

            Mat template = templateCache.get(templateName);
            if (template == null) {
                System.out.println("   ❌ 模板不存在: " + templateName);
                return false;
            }

            Point pos = findButton(SCREENSHOT_PATH, template, templateName);
            if (pos != null) {
                System.out.println("   ✅ 找到按钮: " + name);

                int randomX = random.nextInt(11) - 5;
                int randomY = random.nextInt(11) - 5;
                int clickX = (int)pos.x + randomX;
                int clickY = (int)pos.y + randomY;

                randomClick(clickX, clickY);
                humanDelay(1000, 2000);
                return true;
            }

            Thread.sleep(500);
        }

        System.out.println("   ❌ 超时未找到: " + name);
        return false;
    }

    /**
     * 点击按钮（可选，找不到就跳过）
     */
    private static boolean clickButtonWithSkip(String name, int timeoutMs) throws Exception {
        System.out.println("\n--- 尝试: " + name + " ---");

        String[] templatesToTry;
        if (name.equals("帮派")) {
            templatesToTry = new String[]{"button_bangpai2.png", "button_bangpai.png"};
        } else {
            templatesToTry = new String[]{};
            return false;
        }

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();

            for (String tpl : templatesToTry) {
                Mat template = templateCache.get(tpl);
                if (template == null) continue;

                Point pos = findButton(SCREENSHOT_PATH, template, tpl);
                if (pos != null) {
                    System.out.println("   ✅ 找到按钮: " + name + " (使用模板: " + tpl + ")");

                    int randomX = random.nextInt(11) - 5;
                    int randomY = random.nextInt(11) - 5;
                    int clickX = (int)pos.x + randomX;
                    int clickY = (int)pos.y + randomY;

                    randomClick(clickX, clickY);
                    humanDelay(1000, 2000);
                    return true;
                }
            }

            Thread.sleep(500);
        }

        System.out.println("   ⏳ 未找到: " + name);
        return false;
    }

    /**
     * 点击门客设宴前往（向下偏移5-15px）
     */
    private static boolean clickMenKeSheYanGo() throws Exception {
        String name = "门客设宴前往";
        String templateName = "button_mksy_go.png";
        int timeoutMs = 60000;

        System.out.println("\n--- 点击: " + name + " ---");

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();

            Mat template = templateCache.get(templateName);
            if (template == null) {
                System.out.println("   ❌ 模板不存在: " + templateName);
                return false;
            }

            Point pos = findButton(SCREENSHOT_PATH, template, templateName);
            if (pos != null) {
                System.out.println("   ✅ 找到按钮: " + name);
                System.out.println("   🎯 按钮中心: (" + (int)pos.x + ", " + (int)pos.y + ")");

                int randomX = random.nextInt(11) - 5;
                int randomY = 5 + random.nextInt(11);
                int clickX = (int)pos.x + randomX;
                int clickY = (int)pos.y + randomY;

                System.out.println("   🎯 随机偏移: X" + (randomX >= 0 ? "+" : "") + randomX +
                        ", Y+" + randomY);
                System.out.println("   🎯 点击位置: (" + clickX + ", " + clickY + ")");

                randomClick(clickX, clickY);
                humanDelay(1000, 2000);
                return true;
            }

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsed % 5 == 0 && elapsed > 0) {
                System.out.println("   ⏳ 等待... " + elapsed + " 秒");
            }

            Thread.sleep(500);
        }

        System.out.println("   ❌ 超时未找到: " + name);
        return false;
    }

    /**
     * 随机点击一个"前往邀约"按钮
     */
    private static boolean clickRandomInviteButton() throws Exception {
        String inviteName = "前往邀约";
        String inviteTemplate = "button_invite.png";
        int timeoutMs = 10000;

        System.out.println("\n--- 随机点击: " + inviteName + " ---");

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();

            Mat template = templateCache.get(inviteTemplate);
            if (template == null) {
                System.out.println("   ❌ 模板不存在: " + inviteTemplate);
                return false;
            }

            List<Point> foundButtons = findAllButtons(SCREENSHOT_PATH, template, inviteTemplate);

            if (!foundButtons.isEmpty()) {
                System.out.println("   ✅ 找到 " + foundButtons.size() + " 个" + inviteName + "按钮");

                Point selected = foundButtons.get(random.nextInt(foundButtons.size()));
                int index = foundButtons.indexOf(selected) + 1;

                System.out.println("   🎯 随机选择第 " + index + " 个");
                System.out.println("   🎯 按钮中心: (" + (int)selected.x + ", " + (int)selected.y + ")");

                int randomX = random.nextInt(11) - 5;
                int randomY = random.nextInt(11) - 5;
                int clickX = (int)selected.x + randomX;
                int clickY = (int)selected.y + randomY;

                System.out.println("   🎯 点击位置: (" + clickX + ", " + clickY + ")");
                randomClick(clickX, clickY);
                humanDelay(1500, 2500);
                return true;
            }

            System.out.println("   ⏳ 未找到" + inviteName + "按钮，等待...");
            Thread.sleep(1000);
        }

        System.out.println("   ❌ 超时未找到任何" + inviteName + "按钮");
        return false;
    }

    /**
     * 点击"邀请赴宴"按钮（60秒超时）
     */
    private static boolean clickInviteToParty() throws Exception {
        String name = "邀请赴宴";
        String templateName = "button_invite_party.png";
        int timeoutMs = 60000;  // 60秒超时

        System.out.println("\n--- 点击: " + name + " ---");
        System.out.println("   ⏳ 等待跑图，最长60秒...");

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();

            Mat template = templateCache.get(templateName);
            if (template == null) {
                System.out.println("   ❌ 模板不存在: " + templateName);
                return false;
            }

            Point pos = findButton(SCREENSHOT_PATH, template, templateName);
            if (pos != null) {
                System.out.println("   ✅ 找到按钮: " + name);
                System.out.println("   🎯 按钮中心: (" + (int)pos.x + ", " + (int)pos.y + ")");

                int randomX = random.nextInt(11) - 5;
                int randomY = random.nextInt(11) - 5;
                int clickX = (int)pos.x + randomX;
                int clickY = (int)pos.y + randomY;

                System.out.println("   🎯 点击位置: (" + clickX + ", " + clickY + ")");
                randomClick(clickX, clickY);
                humanDelay(1500, 2500);
                return true;
            }

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsed % 10 == 0 && elapsed > 0) {
                System.out.println("   ⏳ 等待跑图中... " + elapsed + " 秒");
            }

            Thread.sleep(1000);
        }

        System.out.println("   ❌ 超时未找到: " + name);
        return false;
    }

    /**
     * 点击"确认邀约"按钮
     */
    private static boolean clickConfirmInvite() throws Exception {
        String name = "确认邀约";
        String templateName = "button_confirm_invite.png";
        int timeoutMs = 10000;  // 10秒超时

        System.out.println("\n--- 点击: " + name + " ---");

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            takeScreenshot();

            Mat template = templateCache.get(templateName);
            if (template == null) {
                System.out.println("   ❌ 模板不存在: " + templateName);
                return false;
            }

            Point pos = findButton(SCREENSHOT_PATH, template, templateName);
            if (pos != null) {
                System.out.println("   ✅ 找到按钮: " + name);
                System.out.println("   🎯 按钮中心: (" + (int)pos.x + ", " + (int)pos.y + ")");

                int randomX = random.nextInt(11) - 5;
                int randomY = random.nextInt(11) - 5;
                int clickX = (int)pos.x + randomX;
                int clickY = (int)pos.y + randomY;

                System.out.println("   🎯 点击位置: (" + clickX + ", " + clickY + ")");
                randomClick(clickX, clickY);
                humanDelay(1000, 2000);
                return true;
            }

            Thread.sleep(500);
        }

        System.out.println("   ❌ 超时未找到: " + name);
        return false;
    }

    /**
     * 查找截图中所有匹配的按钮
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

            System.out.println("   📊 匹配到 " + results.size() + " 个位置");

        } finally {
            if (screenshot != null) screenshot.release();
            if (result != null) result.release();
        }

        return results;
    }

    /**
     * 图像识别核心方法（动态阈值）
     */
    static Point findButton(String screenshotPath, Mat template, String templateName) {
        Mat screenshot = null;
        Mat result = null;
        try {
            screenshot = Imgcodecs.imread(screenshotPath);
            if (screenshot.empty()) return null;
            result = new Mat();
            Imgproc.matchTemplate(screenshot, template, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            double threshold;
            switch (templateName) {
                case "button_activity.png":
                    threshold = 0.30;
                    break;
                case "button_bangpai.png":
                    threshold = 0.60;
                    break;
                case "button_bangpai2.png":
                    threshold = 0.60;
                    break;
                case "button_mksy_go.png":
                    threshold = 0.50;
                    break;
                case "button_invite.png":
                    threshold = 0.65;
                    break;
                case "button_invite_party.png":
                    threshold = 0.65;  // 保持原阈值
                    break;
                case "button_confirm_invite.png":
                    threshold = 0.65;
                    break;
                default:
                    threshold = 0.65;
            }

            if (mmr.maxVal >= threshold) {
                int cx = (int)(mmr.maxLoc.x + template.cols() / 2);
                int cy = (int)(mmr.maxLoc.y + template.rows() / 2);
                System.out.println("   📊 " + templateName + " 匹配度: " + String.format("%.3f", mmr.maxVal));
                return new Point(cx, cy);
            }
            System.out.println("   📊 " + templateName + " 匹配度: " + String.format("%.3f", mmr.maxVal) + " (低于阈值 " + threshold + ")");
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

    static void takeScreenshot() throws Exception {
        execAdb(adb + " -s " + device + " shell screencap /sdcard/temp.png");
        execAdb(adb + " -s " + device + " pull /sdcard/temp.png " + SCREENSHOT_PATH);
    }

    private static void preloadTemplates() {
        System.out.println("📦 预加载模板...");
        System.out.println("   模板路径: " + TEMPLATE_PATH);

        String[] templates = {
                "button_activity.png",
                "button_bangpai.png",
                "button_bangpai2.png",
                "button_mksy_go.png",
                "button_invite.png",
                "button_invite_party.png",
                "button_confirm_invite.png"
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