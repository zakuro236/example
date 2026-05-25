package com.example.发布悬赏;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class TestChahao2 {

    static {
        String[] dllPaths = {
            "D:\\MyDemo\\opencv_java490.dll",
            "./opencv_java490.dll",
            System.getProperty("user.dir") + "\\opencv_java490.dll"
        };
        boolean loaded = false;
        for (String path : dllPaths) {
            File dllFile = new File(path);
            if (dllFile.exists()) {
                System.out.println("✅ OpenCV DLL: " + dllFile.getAbsolutePath());
                System.load(dllFile.getAbsolutePath());
                loaded = true;
                break;
            }
        }
        if (!loaded) {
            System.out.println("⚠️ 未找到 OpenCV DLL");
        }
    }

    private static String SCREENSHOT_PATH = "d:/MyDemo/demo1/temp_screen.png";
    private static String ADB = "D:\\leidian\\LDPlayer9\\adb.exe";

    public static void main(String[] args) throws Exception {
        System.out.println("=== 测试 button_chahao2 匹配 ===\n");

        // 获取模板路径
        String baseDir = getProgramDirectory();
        String templatePath = baseDir + File.separator + "templates" + File.separator + "file6" + File.separator + "button_chahao2.png";
        System.out.println("模板路径: " + templatePath);

        Mat template = Imgcodecs.imread(templatePath);
        if (template.empty()) {
            System.out.println("❌ 模板加载失败!");
            return;
        }
        System.out.println("✅ 模板加载成功: " + template.cols() + "x" + template.rows());

        // 截取当前屏幕
        System.out.println("\n5秒后截屏...");
        Thread.sleep(5000);

        takeScreenshot();

        Mat screenshot = Imgcodecs.imread(SCREENSHOT_PATH);
        if (screenshot.empty()) {
            System.out.println("❌ 截图加载失败!");
            return;
        }
        System.out.println("✅ 截图加载成功: " + screenshot.cols() + "x" + screenshot.rows());

        // 预处理
        Mat screenshotBW = removeBackgroundToWhite(screenshot);
        Mat templateBW = removeBackgroundToWhite(template);

        // 保存预处理后的图片方便查看
        Imgcodecs.imwrite("d:/MyDemo/demo1/debug_screenshot_bw.png", screenshotBW);
        Imgcodecs.imwrite("d:/MyDemo/demo1/debug_template_bw.png", templateBW);
        System.out.println("✅ 已保存预处理图片: debug_screenshot_bw.png, debug_template_bw.png");

        // 模板匹配
        Mat result = new Mat();
        Imgproc.matchTemplate(screenshotBW, templateBW, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        System.out.println("\n📊 匹配结果:");
        System.out.println("   最高匹配度: " + String.format("%.4f", mmr.maxVal));
        System.out.println("   阈值: 0.65");

        if (mmr.maxVal >= 0.65) {
            int cx = (int)(mmr.maxLoc.x + template.cols() / 2);
            int cy = (int)(mmr.maxLoc.y + template.rows() / 2);
            System.out.println("   ✅ 匹配成功! 位置: (" + cx + ", " + cy + ")");

            // 尝试找所有匹配
            System.out.println("\n📍 遍历所有匹配点:");
            int count = 0;
            for (int y = 0; y < result.rows(); y++) {
                for (int x = 0; x < result.cols(); x++) {
                    double val = result.get(y, x)[0];
                    if (val >= 0.65) {
                        int matchX = x + template.cols() / 2;
                        int matchY = y + template.rows() / 2;
                        System.out.println("   [" + (++count) + "] 位置: (" + matchX + ", " + matchY + ") 匹配度: " + String.format("%.4f", val));
                    }
                }
            }
        } else {
            System.out.println("   ❌ 未达到阈值!");
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

    private static void takeScreenshot() {
        try {
            Process p = Runtime.getRuntime().exec(ADB + " shell screencap -p /sdcard/screen.png");
            p.waitFor();
            p = Runtime.getRuntime().exec(ADB + " pull /sdcard/screen.png " + SCREENSHOT_PATH);
            p.waitFor();
            System.out.println("✅ 截图完成");
        } catch (Exception e) {
            System.out.println("❌ 截图失败: " + e.getMessage());
        }
    }

    private static String getProgramDirectory() {
        // 直接使用固定路径
        return "D:\\MyDemo\\demo1";
    }
}
