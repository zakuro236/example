package com.example.test;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.io.File;

/**
 * 模板匹配测试 - 验证截图和模板匹配功能
 */
public class TemplateMatchTest {
    
    private static final String ADB = "D:/leidian/LDPlayer9/adb.exe";
    private static final String DEVICE = "emulator-5554";
    private static final String TEMPLATE_DIR = "D:/MyDemo/templates";
    
    static {
        // 加载OpenCV
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
                System.out.println("✅ OpenCV加载成功: " + Core.getVersionString());
                break;
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("  模板匹配功能测试");
        System.out.println("=".repeat(50));
        System.out.println();
        
        // 1. 检查ADB连接
        System.out.println("[1] 检查ADB连接...");
        checkAdbConnection();
        
        // 2. 截图测试
        System.out.println("\n[2] 截图测试...");
        if (!takeScreenshot()) {
            System.out.println("❌ 截图失败");
            return;
        }
        System.out.println("✅ 截图成功");
        
        // 3. 测试模板匹配
        System.out.println("\n[3] 模板匹配测试...");
        testTemplateMatch();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  测试完成");
        System.out.println("=".repeat(50));
    }
    
    private static void checkAdbConnection() {
        try {
            Process p = Runtime.getRuntime().exec(ADB + " devices");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    System.out.println("    " + line);
                }
            }
            reader.close();
            p.waitFor();
        } catch (Exception e) {
            System.out.println("❌ ADB错误: " + e.getMessage());
        }
    }
    
    private static boolean takeScreenshot() {
        try {
            Process p1 = Runtime.getRuntime().exec(ADB + " -s " + DEVICE + " shell screencap -p /sdcard/screenshot.png");
            p1.waitFor();
            
            Process p2 = Runtime.getRuntime().exec(ADB + " -s " + DEVICE + " pull /sdcard/screenshot.png ./temp_screen.png");
            p2.waitFor();
            
            File f = new File("./temp_screen.png");
            return f.exists();
        } catch (Exception e) {
            System.out.println("❌ 截图异常: " + e.getMessage());
            return false;
        }
    }
    
    private static void testTemplateMatch() {
        File screenFile = new File("./temp_screen.png");
        if (!screenFile.exists()) {
            System.out.println("⚠️ 需要先截图");
            return;
        }
        
        Mat screenshot = Imgcodecs.imread(screenFile.getAbsolutePath());
        if (screenshot.empty()) {
            System.out.println("❌ 截图加载失败");
            return;
        }
        System.out.println("    截图尺寸: " + screenshot.cols() + "x" + screenshot.rows());
        
        // 测试file3目录的模板
        File templateDir = new File(TEMPLATE_DIR + "/file3");
        if (!templateDir.exists()) {
            System.out.println("⚠️ 模板目录不存在");
            screenshot.release();
            return;
        }
        
        File[] templates = templateDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (templates == null || templates.length == 0) {
            System.out.println("⚠️ 没有模板文件");
            screenshot.release();
            return;
        }
        
        System.out.println("    找到 " + templates.length + " 个模板，测试前5个...");
        
        for (int i = 0; i < Math.min(5, templates.length); i++) {
            File templateFile = templates[i];
            System.out.println("\n    测试: " + templateFile.getName());
            
            Mat template = Imgcodecs.imread(templateFile.getAbsolutePath());
            if (template.empty()) {
                System.out.println("    ⚠️ 模板加载失败");
                continue;
            }
            
            // 灰度化
            Mat gray = new Mat();
            Mat mask = new Mat();
            Imgproc.cvtColor(screenshot, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.adaptiveThreshold(gray, mask, 255.0, 
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 25, 10.0);
            
            Mat tGray = new Mat();
            Mat tMask = new Mat();
            Imgproc.cvtColor(template, tGray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.adaptiveThreshold(tGray, tMask, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 25, 10.0);
            
            // 模板匹配
            Mat result = new Mat();
            Imgproc.matchTemplate(mask, tMask, result, Imgproc.TM_CCOEFF_NORMED);
            
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            
            System.out.println("    匹配度: " + String.format("%.3f", mmr.maxVal));
            if (mmr.maxVal >= 0.65) {
                System.out.println("    ✅ 匹配成功! 位置: (" + (int)mmr.maxLoc.x + ", " + (int)mmr.maxLoc.y + ")");
            } else if (mmr.maxVal >= 0.5) {
                System.out.println("    ⚠️ 可能有匹配");
            } else {
                System.out.println("    ❌ 未匹配");
            }
            
            gray.release();
            mask.release();
            tGray.release();
            tMask.release();
            template.release();
            result.release();
        }
        
        screenshot.release();
    }
}
