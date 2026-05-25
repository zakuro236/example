package com.example.test;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 测试OpenCV对中文路径的支持
 */
public class OpenCVPathTest {
    
    public static void main(String[] args) throws Exception {
        // 加载OpenCV
        System.load(new File("D:\\MyDemo\\opencv_java490.dll").getAbsolutePath());
        System.out.println("OpenCV版本: " + Core.VERSION);
        System.out.println();
        
        String[] paths = {
            "D:\\MyDemo\\发布\\templates\\file3\\button_activity.png",
            "d:\\MyDemo\\demo1\\templates\\file3\\button_activity.png"
        };
        
        for (String p : paths) {
            System.out.println("测试路径: " + p);
            File f = new File(p);
            System.out.println("  Java可读: " + f.canRead());
            
            Mat mat = Imgcodecs.imread(p);
            System.out.println("  OpenCV读取: " + !mat.empty());
            System.out.println("  尺寸: " + mat.cols() + "x" + mat.rows());
            System.out.println();
        }
        
        // 测试临时文件方案
        System.out.println("========== 临时文件方案测试 ==========\n");
        String originalPath = "D:\\MyDemo\\发布\\templates\\file3\\button_activity.png";
        File originalFile = new File(originalPath);
        
        if (originalFile.exists()) {
            Path tempPath = Files.createTempFile("template_", ".png");
            Files.copy(originalFile.toPath(), tempPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("临时文件: " + tempPath);
            
            Mat mat = Imgcodecs.imread(tempPath.toString());
            System.out.println("OpenCV读取: " + !mat.empty());
            System.out.println("尺寸: " + mat.cols() + "x" + mat.rows());
            
            Files.deleteIfExists(tempPath);
            System.out.println("临时文件已删除");
        }
    }
}
