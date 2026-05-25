package com.example.白榜追击;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.File;
//匹配四张头像
public class AvatarMatchAll {

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

    static final String ADB_PATH = "D:\\leidian\\LDPlayer9\\adb.exe";
    static final String DEVICE = "emulator-5554";
    static final String 截图路径 = "./debug_screen.png";
    static final String 模板目录 = "D:\\MyDemo\\file\\file4\\";

    // 四个模板
    static final String[] 模板 = {
            "button_avatar1.png",
            "button_avatar2.png",
            "button_avatar3.png",
            "button_avatar4.png"
    };

    // 匹配度阈值
    static final double 阈值 = 0.45;

    // 不同颜色用于显示不同模板
    static final Scalar[] 颜色 = {
            new Scalar(255, 0, 0),     // 红
            new Scalar(0, 255, 0),     // 绿
            new Scalar(0, 0, 255),     // 蓝
            new Scalar(255, 255, 0)    // 黄
    };

    public static void main(String[] args) {
        System.out.println("=== 全图四头像去背景高对比度匹配 ===\n");

        // 1. 截图
        takeScreenshot();

        // 2. 加载原图
        Mat fullScreen = Imgcodecs.imread(截图路径);
        if (fullScreen.empty()) {
            System.out.println("❌ 截图加载失败");
            return;
        }

        System.out.println("截图尺寸: " + fullScreen.cols() + "x" + fullScreen.rows());

        // 3. 处理整张截图
        Mat screenProcessed = removeBackgroundToWhite(fullScreen);
        Imgcodecs.imwrite("screen_processed.png", screenProcessed);
        System.out.println("📸 去背景后的截图已保存: screen_processed.png");

        Mat display = fullScreen.clone();

        System.out.println("\n开始全图匹配...\n");

        // 4. 遍历四个模板
        for (int i = 0; i < 模板.length; i++) {
            String templateName = 模板[i];

            System.out.println("匹配模板: " + templateName);

            // 加载模板
            Mat template = Imgcodecs.imread(模板目录 + templateName);
            if (template.empty()) {
                System.out.println("  ❌ 模板不存在: " + 模板目录 + templateName);
                continue;
            }

            // 处理模板
            Mat templateProcessed = removeBackgroundToWhite(template);

            // 全图匹配
            Mat result = new Mat();
            Imgproc.matchTemplate(screenProcessed, templateProcessed, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            double matchValue = mmr.maxVal;
            int x = (int) mmr.maxLoc.x;
            int y = (int) mmr.maxLoc.y;

            System.out.println("  匹配度: " + String.format("%.4f", matchValue));
            System.out.println("  位置: (" + x + ", " + y + ")");

            if (matchValue >= 阈值) {
                System.out.println("  ✅ 匹配成功！");

                // 在显示图上画框
                Imgproc.rectangle(display,
                        new Point(x, y),
                        new Point(x + template.cols(), y + template.rows()),
                        颜色[i % 颜色.length], 2);
                Imgproc.putText(display,
                        templateName + " " + String.format("%.2f", matchValue),
                        new Point(x, y - 5),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 颜色[i % 颜色.length], 2);
            } else {
                System.out.println("  ❌ 匹配失败 (低于阈值 " + 阈值 + ")");
            }

            System.out.println();

            // 释放资源
            template.release();
            templateProcessed.release();
            result.release();
        }

        // 5. 保存结果
        Imgcodecs.imwrite("result_all.png", display);
        System.out.println("💾 结果已保存到 result_all.png");

        fullScreen.release();
        screenProcessed.release();
        display.release();
    }

    /**
     * 去背景：图形变白色，背景变黑色
     */
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

    private static void takeScreenshot() {
        try {
            new java.io.File(截图路径).delete();
            Runtime.getRuntime().exec(ADB_PATH + " -s " + DEVICE + " shell screencap /sdcard/temp.png").waitFor();
            Runtime.getRuntime().exec(ADB_PATH + " -s " + DEVICE + " pull /sdcard/temp.png " + 截图路径).waitFor();
            Runtime.getRuntime().exec(ADB_PATH + " -s " + DEVICE + " shell rm /sdcard/temp.png").waitFor();
            System.out.println("截图成功: " + new java.io.File(截图路径).getAbsolutePath());
        } catch (Exception e) {
            System.out.println("截图失败: " + e.getMessage());
        }
    }
}