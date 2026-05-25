package com.example.自动配置;

import java.io.*;
import java.util.Date;
import java.util.Properties;

public class AdbConfig {

    private static String adbPath = null;
    private static String device = null;
    private static boolean initialized = false;

    private static final String CONFIG_FILE = "./config/adb_config.properties";
    private static boolean dirSet = false;

    /**
     * 设置工作目录到exe所在位置
     */
    private static void setWorkingDirectory() {
        if (dirSet) return;
        
        File currentDir = new File(".").getAbsoluteFile();
        
        // 检查当前目录是否有config和templates
        File configFile = new File(currentDir, "config\\adb_config.properties");
        File templatesDir = new File(currentDir, "templates");
        
        if (configFile.exists() && templatesDir.exists()) {
            System.out.println("📁 工作目录: " + currentDir.getAbsolutePath() + " (本地)");
            dirSet = true;
            return;
        }
        
        // 向上查找一梦江湖脚本.exe所在目录
        File parent = currentDir.getParentFile();
        while (parent != null) {
            File exeFile = new File(parent, "一梦江湖脚本.exe");
            File cfg = new File(parent, "config\\adb_config.properties");
            File tmpl = new File(parent, "templates");
            if (exeFile.exists() && cfg.exists() && tmpl.exists()) {
                System.setProperty("user.dir", parent.getAbsolutePath());
                System.setProperty("user.home", parent.getAbsolutePath());
                System.out.println("📁 已切换工作目录到: " + parent.getAbsolutePath());
                dirSet = true;
                break;
            }
            parent = parent.getParentFile();
        }
    }

    /**
     * 初始化配置（在脚本main方法开头调用）
     */
    public static void init() {
        if (initialized) return;

        // ========== 最重要：设置工作目录到exe所在位置 ==========
        setWorkingDirectory();
        // =====================================================

        loadConfig();

        if (adbPath == null || adbPath.isEmpty() || device == null || device.isEmpty()) {
            System.out.println("⚠️ 未找到ADB配置，正在启动配置工具...");
            launchConfigTool();
            loadConfig(); // 重新加载
        }

        if (adbPath != null && !adbPath.isEmpty() && device != null && !device.isEmpty()) {
            System.out.println("✅ ADB配置加载成功");
            System.out.println("   ADB路径: " + adbPath);
            System.out.println("   设备: " + device);
        } else {
            System.out.println("❌ ADB配置失败，请手动运行配置工具");
        }

        initialized = true;
    }

    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(in);
                adbPath = props.getProperty("adb.path");
                device = props.getProperty("device");
            } catch (Exception e) {
                System.out.println("读取配置失败: " + e.getMessage());
            }
        }
    }

    /**
     * 启动配置工具 - 同一JVM中运行，不阻塞
     */
    private static void launchConfigTool() {
        try {
            System.out.println("在当前JVM中启动配置窗口...");

            // 使用反射在同一JVM中启动配置窗口，避免ProcessBuilder的问题
            Class<?> clazz = Class.forName("com.example.自动配置.aaa");
            java.lang.reflect.Method mainMethod = clazz.getMethod("main", String[].class);
            // 在新线程中运行配置窗口，不阻塞当前线程
            new Thread(() -> {
                try {
                    mainMethod.invoke(null, (Object) new String[]{});
                } catch (Exception e) {
                    System.out.println("启动配置窗口失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }, "ConfigWindow-Thread").start();

            // 等待配置文件被创建（最多等待60秒）
            System.out.println("等待配置文件创建...");
            long startTime = System.currentTimeMillis();
            while (!new File(CONFIG_FILE).exists() && (System.currentTimeMillis() - startTime) < 60000) {
                Thread.sleep(500);
                loadConfig(); // 尝试重新加载
                if (adbPath != null && !adbPath.isEmpty() && device != null && !device.isEmpty()) {
                    System.out.println("配置文件已检测到");
                    return;
                }
            }

            // 再次加载配置
            loadConfig();

            // 如果还是没配置，创建一个带默认值的配置文件
            if (adbPath == null || adbPath.isEmpty() || device == null || device.isEmpty()) {
                System.out.println("配置窗口超时，使用默认配置...");
                createDefaultConfig();
            }
        } catch (Exception e) {
            System.out.println("启动配置工具失败: " + e.getMessage());
            e.printStackTrace();
            // 创建默认配置
            createDefaultConfig();
        }
    }

    /**
     * 创建默认配置文件
     */
    private static void createDefaultConfig() {
        try {
            File configDir = new File("./config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            // 尝试自动查找adb
            String adbPath = findAdbInSystem();
            String device = adbPath != null ? "127.0.0.1:5555" : "";

            Properties props = new Properties();
            props.setProperty("adb.path", adbPath != null ? adbPath : "./adb.exe");
            props.setProperty("device", device);
            props.setProperty("emulator", "自动检测");
            props.setProperty("install.path", "");
            props.setProperty("exe.path", "");
            props.setProperty("last_update", new Date().toString());
            props.setProperty("auto_configured", "true");

            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                props.store(out, "ADB默认配置 - 请手动配置");
            }

            adbPath = props.getProperty("adb.path");
            device = props.getProperty("device");

            System.out.println("已创建默认配置文件");
            System.out.println("ADB路径: " + adbPath);
            System.out.println("设备: " + device);
        } catch (Exception e) {
            System.out.println("创建默认配置失败: " + e.getMessage());
        }
    }

    /**
     * 在系统中自动查找ADB
     */
    private static String findAdbInSystem() {
        String[] searchPaths = {
            System.getProperty("user.home") + "\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe",
            "C:\\Program Files\\Android\\adb.exe",
            "C:\\Android\\adb.exe",
            "D:\\Android\\Sdk\\platform-tools\\adb.exe"
        };

        for (String path : searchPaths) {
            File adbFile = new File(path);
            if (adbFile.exists()) {
                return adbFile.getAbsolutePath();
            }
        }

        // 尝试从PATH环境变量中查找
        try {
            Process process = Runtime.getRuntime().exec("where adb");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception e) {
            // ignore
        }

        return null;
    }

    public static String getAdbPath() {
        init();
        return adbPath;
    }

    public static String getDevice() {
        init();
        return device;
    }

    public static boolean isConfigured() {
        init();
        return adbPath != null && !adbPath.isEmpty() && device != null && !device.isEmpty();
    }

    public static void printConfig() {
        System.out.println("当前ADB配置:");
        System.out.println("  ADB路径: " + getAdbPath());
        System.out.println("  设备: " + getDevice());
    }
}