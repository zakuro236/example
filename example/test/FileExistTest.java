package com.example.test;

import java.io.File;

/**
 * 测试文件是否存在
 */
public class FileExistTest {
    public static void main(String[] args) {
        String[] paths = {
            "D:\\MyDemo\\发布\\templates\\file3\\button_activity.png",
            "D:\\MyDemo\\发布\\templates\\file1\\button_activity.png",
            "D:\\MyDemo\\发布\\templates\\file5\\button_activity.png",
            "D:\\MyDemo\\发布\\templates\\file4\\button_activity.png"
        };
        
        System.out.println("========== 文件存在性测试 ==========\n");
        for (String p : paths) {
            File f = new File(p);
            System.out.println("路径: " + p);
            System.out.println("存在: " + f.exists());
            System.out.println("是文件: " + f.isFile());
            System.out.println("可读: " + f.canRead());
            System.out.println("大小: " + (f.exists() ? f.length() + " bytes" : "N/A"));
            System.out.println();
        }
    }
}
