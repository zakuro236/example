package com.example.自动配置;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathUtil {

    private static String basePath = null;

    /**
     * 获取程序所在目录（JAR或class文件所在目录）
     */
    public static String getBasePath() {
        if (basePath != null) {
            return basePath;
        }

        try {
            // 获取当前类的位置
            String path = PathUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

            File jarFile = new File(path);
            if (jarFile.isFile()) {
                // 运行的是JAR包，取JAR所在目录
                basePath = jarFile.getParent();
            } else {
                // 运行的是class文件，取项目根目录
                basePath = System.getProperty("user.dir");
            }

            // 确保路径以分隔符结尾
            if (!basePath.endsWith(File.separator)) {
                basePath = basePath + File.separator;
            }

            System.out.println("[路径] 程序所在目录: " + basePath);
            return basePath;
        } catch (Exception e) {
            System.out.println("[路径] 获取失败，使用当前工作目录: " + e.getMessage());
            basePath = System.getProperty("user.dir") + File.separator;
            return basePath;
        }
    }

    /**
     * 获取模板目录
     */
    public static String getTemplatePath(String subFolder) {
        return getBasePath() + "templates" + File.separator + subFolder + File.separator;
    }

    /**
     * 获取配置文件目录
     */
    public static String getConfigPath() {
        return getBasePath() + "config" + File.separator;
    }
}