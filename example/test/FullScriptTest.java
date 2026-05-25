package com.example.test;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.util.*;

/**
 * 完整脚本测试 - 模拟Android脚本执行流程
 * 验证：
 * 1. 模板预加载
 * 2. 循环截图
 * 3. 模板匹配
 * 4. 点击操作
 */
public class FullScriptTest {
    
    private static final String ADB = "D:/leidian/LDPlayer9/adb.exe";
    private static final String DEVICE = "emulator-5554";
    private static final String TEMPLATE_DIR = "D:/MyDemo/templates";
    private static final double THRESHOLD = 0.65;
    
    // 模板缓存
    private static Map<String, Mat> templateCache = new HashMap<>();
    
    static {
        String[] dllPaths = {
            "D:/MyDemo/opencv_java490.dll",
            "./opencv_java490.dll",
            System.getProperty("user.dir") + "/opencv_java490.dll"
        };
        
        for (String path : dllPaths) {
            File dllFile = new File(path);
            if (dllFile.exists()) {
                System.out.println("✅ OpenCV DLL: " + dllFile.getAbsolutePath());
                System.load(dllFile.getAbsolutePath());
                System.out.println("✅ OpenCV: " + Core.getVersionString());
                break;
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  完整脚本流程测试");
        System.out.println("=".repeat(60));
        System.out.println();
        
        // 1. 预加载模板
        System.out.println("[1] 预加载模板...");
        preloadTemplates("file3", new String[]{"button_activity.png", "button_close.png"});
        
        // 2. 执行主循环
        System.out.println("\n[2] 执行主循环（3次）...");
        for (int i = 0; i < 3; i++) {
            System.out.println("\n--- 第 " + (i + 1) + " 次循环 ---");
            
            // 截图
            if (!takeScreenshot()) {
                System.out.println("❌ 截图失败");
                break;
            }
            System.out.println("    ✅ 截图成功");
            
            // 查找按钮
            Point point = findButton("file3/button_activity.png");
            if (point != null) {
                System.out.println("    ✅ 找到按钮: (" + (int)point.x + ", " + (int)point.y + ")");
                
                // 点击
                click((int)point.x, (int)point.y);
                System.out.println("    ✅ 点击完成");
            } else {
                System.out.println("    ⚠️ 未找到按钮");
            }
            
            // 等待
            sleep(1000);
        }
        
        // 3. 清理
        System.out.println("\n[3] 清理资源...");
        cleanup();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  测试完成");
        System.out.println("=".repeat(60));
    }
    
    /**
     * 预加载模板
     */
    private static void preloadTemplates(String dir, String[] names) {
        for (String name : names) {
            String key = dir + "/" + name;
            String path = TEMPLATE_DIR + "/" + key;
            File file = new File(path);
            
            if (file.exists()) {
                Mat template = Imgcodecs.imread(path);
                if (!template.empty()) {
                    templateCache.put(key, template);
                    System.out.println("    ✅ " + name + " (" + template.cols() + "x" + template.rows() + ")");
                } else {
                    System.out.println("    ⚠️ 加载失败: " + name);
                }
            } else {
                System.out.println("    ⚠️ 不存在: " + path);
            }
        }
    }
    
    /**
     * 截图
     */
    private static boolean takeScreenshot() {
        try {
            Process p1 = Runtime.getRuntime().exec(ADB + " -s " + DEVICE + " shell screencap -p /sdcard/screenshot.png");
            p1.waitFor();
            
            Process p2 = Runtime.getRuntime().exec(ADB + " -s " + DEVICE + " pull /sdcard/screenshot.png ./temp_screen.png");
            p2.waitFor();
            
            return new File("./temp_screen.png").exists();
        } catch (Exception e) {
            System.out.println("    ❌ 截图异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 背景处理 - 与Android版本完全一致
     */
    private static Mat removeBackgroundToWhite(Mat src) {
        Mat gray = new Mat();
        Mat mask = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(gray, mask, 255.0, 
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY, 25, 10.0);
        Mat result = new Mat();
        Imgproc.cvtColor(mask, result, Imgproc.COLOR_GRAY2BGR);
        gray.release();
        mask.release();
        return result;
    }
    
    /**
     * 模板匹配 - 与Android版本完全一致
     */
    private static Point matchTemplate(Mat screenshot, Mat template) {
        Mat screenshotBW = removeBackgroundToWhite(screenshot);
        Mat templateBW = removeBackgroundToWhite(template);
        
        Mat result = new Mat();
        Imgproc.matchTemplate(screenshotBW, templateBW, result, Imgproc.TM_CCOEFF_NORMED);
        
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        
        double matchVal = mmr.maxVal;
        System.out.println("    📊 匹配度: " + String.format("%.3f", matchVal) + " (阈值: " + THRESHOLD + ")");
        
        Point point = null;
        if (matchVal >= THRESHOLD) {
            double cx = mmr.maxLoc.x + template.cols() / 2;
            double cy = mmr.maxLoc.y + template.rows() / 2;
            point = new Point(cx, cy);
            System.out.println("    📍 位置: (" + (int)cx + ", " + (int)cy + ")");
        } else if (matchVal >= 0.4) {
            System.out.println("    ⚠️ 低于阈值但有响应");
        }
        
        screenshotBW.release();
        templateBW.release();
        result.release();
        
        return point;
    }
    
    /**
     * 查找按钮
     */
    private static Point findButton(String key) {
        Mat template = templateCache.get(key);
        if (template == null) {
            System.out.println("    ⚠️ 模板不存在: " + key);
            return null;
        }
        
        File screenFile = new File("./temp_screen.png");
        if (!screenFile.exists()) {
            return null;
        }
        
        Mat screenshot = Imgcodecs.imread(screenFile.getAbsolutePath());
        if (screenshot.empty()) {
            return null;
        }
        
        System.out.println("    📱 截图尺寸: " + screenshot.cols() + "x" + screenshot.rows());
        
        Point point = matchTemplate(screenshot, template);
        screenshot.release();
        
        return point;
    }
    
    /**
     * 点击
     */
    private static void click(int x, int y) {
        // 添加随机偏移
        Random rand = new Random();
        int offsetX = rand.nextInt(7) - 3;
        int offsetY = rand.nextInt(7) - 3;
        int clickX = x + offsetX;
        int clickY = y + offsetY;
        
        try {
            String cmd = ADB + " -s " + DEVICE + " shell input tap " + clickX + " " + clickY;
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            System.out.println("    📱 点击: (" + clickX + ", " + clickY + ")");
        } catch (Exception e) {
            System.out.println("    ❌ 点击失败: " + e.getMessage());
        }
    }
    
    /**
     * 等待
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }
    
    /**
     * 清理
     */
    private static void cleanup() {
        for (Mat mat : templateCache.values()) {
            mat.release();
        }
        templateCache.clear();
        System.out.println("    ✅ 清理完成");
    }
}
