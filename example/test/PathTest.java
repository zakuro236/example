package com.example.test;

import java.io.File;
import java.net.URLDecoder;

/**
 * 测试程序目录定位和路径拼接
 */
public class PathTest {

    public static void main(String[] args) {
        System.out.println("========== 路径测试 ==========\n");
        
        // 测试1: 当前工作目录
        System.out.println("【测试1】当前工作目录 (user.dir)");
        System.out.println("  " + System.getProperty("user.dir"));
        System.out.println("  new File(\".\").getAbsoluteFile(): " + new File(".").getAbsoluteFile());
        System.out.println();
        
        // 测试2: 向上查找exe
        System.out.println("【测试2】向上查找 一梦江湖脚本.exe");
        File currentDir = new File(".").getAbsoluteFile();
        File parent = currentDir;
        File foundExe = null;
        for (int i = 0; i < 10; i++) {
            File exeFile = new File(parent, "一梦江湖脚本.exe");
            System.out.println("  检查: " + parent.getAbsolutePath() + " -> " + exeFile.getAbsolutePath());
            if (exeFile.exists()) {
                foundExe = parent;
                System.out.println("  ✅ 找到exe所在目录: " + parent.getAbsolutePath());
                break;
            }
            if (parent.getParentFile() == null) {
                System.out.println("  已到根目录，停止查找");
                break;
            }
            parent = parent.getParentFile();
        }
        System.out.println();
        
        // 测试3: 模拟各任务的模板路径
        String baseDir = foundExe != null ? foundExe.getAbsolutePath() : System.getProperty("user.dir");
        System.out.println("【测试3】模拟各任务模板路径 (baseDir=" + baseDir + ")");
        System.out.println();
        
        // 师门任务
        String shimenPath = baseDir + File.separator + "templates" + File.separator + "file3" + File.separator + "button_activity.png";
        System.out.println("  师门任务: " + shimenPath);
        System.out.println("  文件存在: " + new File(shimenPath).exists());
        System.out.println();
        
        // 帮派任务
        String bangpaiPath = baseDir + File.separator + "templates" + File.separator + "file1" + File.separator + "button_activity.png";
        System.out.println("  帮派任务: " + bangpaiPath);
        System.out.println("  文件存在: " + new File(bangpaiPath).exists());
        System.out.println();
        
        // 茶馆说书
        String chaguanPath = baseDir + File.separator + "templates" + File.separator + "file5" + File.separator + "button_activity.png";
        System.out.println("  茶馆说书: " + chaguanPath);
        System.out.println("  文件存在: " + new File(chaguanPath).exists());
        System.out.println();
        
        // 白榜追击
        String baibangPath = baseDir + File.separator + "templates" + File.separator + "file4" + File.separator + "button_activity.png";
        System.out.println("  白榜追击: " + baibangPath);
        System.out.println("  文件存在: " + new File(baibangPath).exists());
        System.out.println();
        
        // 测试4: config路径
        String configPath = baseDir + File.separator + "config" + File.separator + "adb_config.properties";
        System.out.println("【测试4】Config路径");
        System.out.println("  " + configPath);
        System.out.println("  文件存在: " + new File(configPath).exists());
        System.out.println();
        
        System.out.println("========== 测试完成 ==========");
    }
}
