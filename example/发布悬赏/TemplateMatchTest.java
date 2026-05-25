package com.example.发布悬赏;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.Random;

public class TemplateMatchTest {

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
    static final String 输出目录 = "D:\\MyDemo\\debug\\";
    static final String 模板目录 = "D:\\MyDemo\\templates\\file6\\";
    static final Random random = new Random();

    // 匹配度阈值
    static final double 阈值 = 0.35;

    // 颜色
    static final Scalar 绿色 = new Scalar(0, 255, 0);
    static final Scalar 红色 = new Scalar(255, 0, 0);

    public static void main(String[] args) {
        System.out.println("=== 模板匹配测试 ===\n");

        // 创建输出目录
        new File(输出目录).mkdirs();

        // 判断参数
        if (args.length > 0) {
            System.out.println("参数: " + args[0]);
        }
        System.out.println("\n可用模板:");
        listTemplates(模板目录);

        String templateName = args.length > 0 ? args[0] : "button_chahao1.png";
        if (!templateName.endsWith(".png")) {
            templateName += ".png";
        }
        String templateNameNoExt = templateName.replace(".png", "");

        System.out.println("\n模板: " + templateName);
        System.out.println("输出目录: " + 输出目录);

        // 1. 截图
        System.out.println("\n正在截图...");
        String screenPath = 输出目录 + "1_original_screen.png";
        takeScreenshot(screenPath);

        // 2. 加载截图
        Mat fullScreen = Imgcodecs.imread(screenPath);
        if (fullScreen.empty()) {
            System.out.println("❌ 截图加载失败: " + screenPath);
            return;
        }
        System.out.println("截图尺寸: " + fullScreen.cols() + "x" + fullScreen.rows());

        // 3. 处理截图（去背景）
        Mat screenProcessed = removeBackgroundToWhite(fullScreen);
        String screenProcessedPath = 输出目录 + "2_screen_processed_" + templateNameNoExt + ".png";
        Imgcodecs.imwrite(screenProcessedPath, screenProcessed);
        System.out.println("📸 去背景截图已保存: " + screenProcessedPath);

        // 4. 加载模板
        Mat template = Imgcodecs.imread(模板目录 + templateName);
        if (template.empty()) {
            System.out.println("❌ 模板不存在: " + 模板目录 + templateName);
            fullScreen.release();
            screenProcessed.release();
            return;
        }
        System.out.println("模板尺寸: " + template.cols() + "x" + template.rows());

        // 5. 处理模板（去背景）
        Mat templateProcessed = removeBackgroundToWhite(template);
        String templateProcessedPath = 输出目录 + "3_template_processed_" + templateNameNoExt + ".png";
        Imgcodecs.imwrite(templateProcessedPath, templateProcessed);
        System.out.println("📸 去背景模板已保存: " + templateProcessedPath);

        // 6. 匹配
        Mat result = new Mat();
        Imgproc.matchTemplate(screenProcessed, templateProcessed, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        double matchValue = mmr.maxVal;
        int x = (int) (mmr.maxLoc.x + template.cols() / 2);
        int y = (int) (mmr.maxLoc.y + template.rows() / 2);

        System.out.println("\n=== 匹配结果 ===");
        System.out.println("匹配度: " + String.format("%.4f", matchValue));
        System.out.println("位置: (" + x + ", " + y + ")");

        // 7. 画框标记
        Mat display = fullScreen.clone();
        if (matchValue >= 阈值) {
            System.out.println("✅ 匹配成功!");

            Imgproc.rectangle(display,
                    mmr.maxLoc,
                    new Point(mmr.maxLoc.x + template.cols(), mmr.maxLoc.y + template.rows()),
                    绿色, 3);

            Imgproc.circle(display, new Point(x, y), 10, 绿色, -1);

            Imgproc.putText(display,
                    templateName + " " + String.format("%.3f", matchValue),
                    new Point(mmr.maxLoc.x, mmr.maxLoc.y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, 绿色, 2);

            // 8. 保存结果
            String resultPath = 输出目录 + "4_result_" + templateNameNoExt + ".png";
            Imgcodecs.imwrite(resultPath, display);
            System.out.println("💾 结果已保存: " + resultPath);

            // 9. 点击
            System.out.println("\n3秒后点击...");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {}

            click(x, y);
            System.out.println("✅ 已点击: (" + x + ", " + y + ")");
        } else {
            System.out.println("❌ 匹配失败 (低于阈值 " + 阈值 + ")");

            // 画框标记最高匹配位置
            Imgproc.rectangle(display,
                    mmr.maxLoc,
                    new Point(mmr.maxLoc.x + template.cols(), mmr.maxLoc.y + template.rows()),
                    红色, 3);

            Imgproc.putText(display,
                    "Best: " + String.format("%.3f", matchValue),
                    new Point(mmr.maxLoc.x, mmr.maxLoc.y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, 红色, 2);

            // 保存结果
            String resultPath = 输出目录 + "4_result_" + templateNameNoExt + ".png";
            Imgcodecs.imwrite(resultPath, display);
            System.out.println("💾 结果已保存: " + resultPath);
        }

        System.out.println("\n=== 生成的文件 ===");
        System.out.println("1. " + screenPath);
        System.out.println("2. " + screenProcessedPath);
        System.out.println("3. " + templateProcessedPath);
        System.out.println("4. " + 输出目录 + "4_result_" + templateNameNoExt + ".png");

        // 释放资源
        fullScreen.release();
        screenProcessed.release();
        template.release();
        templateProcessed.release();
        result.release();
        display.release();
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

    private static void takeScreenshot(String outputPath) {
        try {
            // 先删除本地文件
            new File(outputPath).delete();

            Process p1 = Runtime.getRuntime().exec(ADB_PATH + " -s " + DEVICE + " shell screencap /sdcard/temp.png");
            p1.waitFor();

            Process p2 = Runtime.getRuntime().exec(ADB_PATH + " -s " + DEVICE + " pull /sdcard/temp.png " + outputPath);
            p2.waitFor();

            Process p3 = Runtime.getRuntime().exec(ADB_PATH + " -s " + DEVICE + " shell rm /sdcard/temp.png");
            p3.waitFor();

            File f = new File(outputPath);
            if (f.exists()) {
                System.out.println("截图成功: " + f.getAbsolutePath() + " (" + f.length() + " bytes)");
            } else {
                System.out.println("截图文件不存在!");
            }
        } catch (Exception e) {
            System.out.println("截图失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void click(int x, int y) {
        try {
            int offsetX = random.nextInt(11) - 5;
            int offsetY = random.nextInt(11) - 5;
            String cmd = ADB_PATH + " -s " + DEVICE + " shell input tap " + (x + offsetX) + " " + (y + offsetY);
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            System.out.println("点击失败: " + e.getMessage());
        }
    }

    private static void listTemplates(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
            if (files != null) {
                for (File f : files) {
                    System.out.println("  - " + f.getName());
                }
            }
        }
    }
}
