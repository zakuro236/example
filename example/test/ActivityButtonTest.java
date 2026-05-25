package com.example.test;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ActivityButtonTest {

    private static String adb;
    private static String device;
    private static final String SCREENSHOT_PATH = "./test_screen.png";
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
        System.out.println("❌ DLL加载失败!");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 活动按钮匹配测试 ===\n");

        // 初始化ADB配置
        com.example.自动配置.AdbConfig.init();
        adb = com.example.自动配置.AdbConfig.getAdbPath();
        device = com.example.自动配置.AdbConfig.getDevice();

        // 测试所有file目录的activity按钮
        String[] dirs = {"file1", "file3", "file4", "file5", "file6", "file7", "file8"};
        String[] btnNames = {"button_activity.png", "button_huo_dong.png"};

        System.out.println("5秒后开始截图测试...");
        Thread.sleep(5000);

        // 截图
        takeScreenshot();
        System.out.println("截图已保存到: " + SCREENSHOT_PATH);

        // 测试每个目录的按钮
        for (String dir : dirs) {
            for (String btnName : btnNames) {
                String templatePath = "d:/MyDemo/templates/" + dir + "/" + btnName;
                File f = new File(templatePath);
                if (f.exists()) {
                    System.out.println("\n--- 测试 " + dir + "/" + btnName + " ---");
                    testMatch(templatePath);
                }
            }
        }

        cleanup();
        System.out.println("\n✅ 测试完成!");
    }

    private static void testMatch(String templatePath) {
        Mat template = Imgcodecs.imread(templatePath);
        if (template.empty()) {
            System.out.println("   ❌ 模板读取失败");
            return;
        }

        Mat screenshot = Imgcodecs.imread(SCREENSHOT_PATH);
        if (screenshot.empty()) {
            System.out.println("   ❌ 截图读取失败");
            template.release();
            return;
        }

        Mat screenshotBW = removeBackgroundToWhite(screenshot);
        Mat templateBW = removeBackgroundToWhite(template);
        Mat result = new Mat();

        Imgproc.matchTemplate(screenshotBW, templateBW, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        System.out.println("   最高匹配度: " + String.format("%.2f", mmr.maxVal * 100) + "%");
        System.out.println("   位置: (" + (int)mmr.maxLoc.x + ", " + (int)mmr.maxLoc.y + ")");

        if (mmr.maxVal >= 0.65) {
            System.out.println("   ✅ 匹配成功!");
        } else if (mmr.maxVal >= 0.55) {
            System.out.println("   ⚠️ 匹配度偏低，可能需要降低阈值");
        } else {
            System.out.println("   ❌ 匹配度太低");
        }

        template.release();
        screenshot.release();
        screenshotBW.release();
        templateBW.release();
        result.release();
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

    private static boolean takeScreenshot() {
        try {
            Process p = Runtime.getRuntime().exec(adb + " -s " + device + " shell screencap -p /sdcard/screen.png");
            p.waitFor();
            p = Runtime.getRuntime().exec(adb + " -s " + device + " pull /sdcard/screen.png " + SCREENSHOT_PATH);
            p.waitFor();
            return new File(SCREENSHOT_PATH).exists();
        } catch (Exception e) {
            return false;
        }
    }

    static void cleanup() {
        new File(SCREENSHOT_PATH).delete();
        for (Mat m : templateCache.values()) m.release();
        templateCache.clear();
    }
}
