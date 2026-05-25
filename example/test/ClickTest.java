package com.example.test;

import java.io.File;

/**
 * ADB点击测试
 */
public class ClickTest {
    
    private static final String ADB = "D:/leidian/LDPlayer9/adb.exe";
    private static final String DEVICE = "emulator-5554";
    
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
                System.out.println("✅ OpenCV加载成功: " + org.opencv.core.Core.getVersionString());
                break;
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("  ADB点击功能测试");
        System.out.println("=".repeat(50));
        System.out.println();
        
        // 测试点击
        System.out.println("正在点击屏幕中心 (800, 450)...");
        click(800, 450);
        
        System.out.println("\n等待2秒...");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        
        // 截图看看效果
        System.out.println("\n截图查看效果...");
        takeScreenshot();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  点击测试完成");
        System.out.println("=".repeat(50));
    }
    
    public static void click(int x, int y) {
        try {
            String cmd = ADB + " -s " + DEVICE + " shell input tap " + x + " " + y;
            System.out.println("执行: " + cmd);
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            System.out.println("✅ 点击成功: (" + x + ", " + y + ")");
        } catch (Exception e) {
            System.out.println("❌ 点击失败: " + e.getMessage());
        }
    }
    
    private static void takeScreenshot() {
        try {
            Process p1 = Runtime.getRuntime().exec(ADB + " -s " + DEVICE + " shell screencap -p /sdcard/screenshot.png");
            p1.waitFor();
            
            Process p2 = Runtime.getRuntime().exec(ADB + " -s " + DEVICE + " pull /sdcard/screenshot.png ./click_test.png");
            p2.waitFor();
            
            File f = new File("./click_test.png");
            if (f.exists()) {
                System.out.println("✅ 截图已保存: click_test.png");
            }
        } catch (Exception e) {
            System.out.println("❌ 截图失败: " + e.getMessage());
        }
    }
}
