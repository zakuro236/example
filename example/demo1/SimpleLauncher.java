package com.example.demo1;

import com.example.自动配置.aaa;
import com.example.主任务程序.MainTaskGUI;
import org.opencv.core.Core;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;

public class SimpleLauncher {
    
    private static File workingDir;
    private static File logFile;
    
    public static void main(String[] args) {
        // ========== 最重要：确定exe所在目录 ==========
        workingDir = new File(".").getAbsoluteFile();
        File exeFile = new File(workingDir, "一梦江湖脚本.exe");
        if (!exeFile.exists()) {
            File parent = workingDir.getParentFile();
            while (parent != null) {
                exeFile = new File(parent, "一梦江湖脚本.exe");
                if (exeFile.exists()) {
                    workingDir = parent;
                    break;
                }
                parent = parent.getParentFile();
            }
        }
        // =============================================
        
        logFile = new File(workingDir, "launcher.log");
        log("========================================");
        log("  程序启动 - " + new java.util.Date());
        log("========================================");
        log("工作目录: " + workingDir.getAbsolutePath());
        log("日志路径: " + logFile.getAbsolutePath());
        log("Java版本: " + System.getProperty("java.version"));
        log("当前目录存在文件: " + (workingDir.listFiles() != null ? workingDir.listFiles().length : 0));
        
        preloadOpenCV();
        
        try {
            System.setProperty("java.awt.headless", "false");
            log("步骤1: 设置图形界面模式");

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            log("步骤2: 设置外观完成");

            SwingUtilities.invokeLater(() -> {
                try {
                    log("步骤3: 开始初始化GUI");
                    
                    File configFile = new File(workingDir, "config\\adb_config.properties");
                    log("配置文件路径: " + configFile.getAbsolutePath());
                    log("配置文件是否存在: " + configFile.exists());

                    if (!configFile.exists()) {
                        log("首次运行：配置 ADB");
                        log("创建配置窗口...");

                        aaa configWindow = new aaa();
                        configWindow.setVisible(true);
                        log("配置窗口已显示");

                        configWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosed(java.awt.event.WindowEvent e) {
                                log("配置窗口已关闭");
                                File cf = new File(workingDir, "config\\adb_config.properties");
                                if (cf.exists()) {
                                    log("创建主程序窗口...");
                                    new MainTaskGUI();
                                    log("主程序窗口已创建");
                                }
                            }
                        });
                        return;
                    } else {
                        log("ADB 已配置，直接启动主程序");
                    }

                    log("创建主程序窗口...");
                    new MainTaskGUI();
                    log("主程序窗口已创建");
                    
                } catch (Exception e) {
                    log("GUI初始化错误: " + e.getMessage());
                    e.printStackTrace();
                    showError(e);
                }
            });
            
            log("步骤4: Swing线程已提交");
            
        } catch (Exception e) {
            log("启动错误: " + e.getMessage());
            e.printStackTrace();
            showError(e);
        }
    }
    
    private static void log(String msg) {
        String logMsg = "[" + System.currentTimeMillis() + "] " + msg;
        System.out.println(logMsg);
        if (logFile != null) {
            try {
                FileWriter fw = new FileWriter(logFile, true);
                fw.write(logMsg + "\n");
                fw.close();
            } catch (Exception e) {}
        }
    }
    
    private static void showError(Exception e) {
        JOptionPane.showMessageDialog(null, 
            "启动错误:\n" + e.getMessage() + "\n\n详情已保存到 launcher.log", 
            "错误", JOptionPane.ERROR_MESSAGE);
    }
    
    private static void preloadOpenCV() {
        log("尝试预加载OpenCV...");
        String[] dllPaths = {
            workingDir.getAbsolutePath() + "\\opencv_java490.dll",
            "opencv_java490.dll",
            "./opencv_java490.dll",
            "D:\\MyDemo\\opencv_java490.dll"
        };
        
        for (String dllPath : dllPaths) {
            File dllFile = new File(dllPath);
            if (dllFile.exists()) {
                try {
                    log("加载OpenCV DLL: " + dllFile.getAbsolutePath());
                    System.load(dllFile.getAbsolutePath());
                    log("OpenCV加载成功! 版本: " + Core.VERSION);
                    return;
                } catch (Exception e) {
                    log("加载失败: " + e.getMessage());
                }
            }
        }
        
        try {
            log("尝试自动加载OpenCV...");
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            log("OpenCV自动加载成功! 版本: " + Core.VERSION);
        } catch (Exception e) {
            log("OpenCV加载警告: " + e.getMessage());
        }
    }
}
