package com.example.demo1;

import com.example.自动配置.aaa;
import com.example.主任务程序.MainTaskGUI;

import javax.swing.*;
import java.awt.*;

public class Launcher {
    public static void main(String[] args) {
        // 设置允许图形界面
        System.setProperty("java.awt.headless", "false");

        // 设置 Windows 外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 在 Swing 线程中启动 GUI
        SwingUtilities.invokeLater(() -> {
            // 第一步：打开 ADB 配置工具
            System.out.println("\n========================================");
            System.out.println("  第一步：配置 ADB");
            System.out.println("========================================\n");

            aaa configWindow = new aaa();
            configWindow.setVisible(true);

            // 等待配置窗口关闭
            waitForWindowClose(configWindow);

            // 第二步：打开主程序
            System.out.println("\n========================================");
            System.out.println("  第二步：启动主程序");
            System.out.println("========================================\n");

            new MainTaskGUI();
        });
    }

    private static void waitForWindowClose(Window window) {
        while (window.isVisible()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}