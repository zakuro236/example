package com.example.自动配置;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.List;

public class aaa extends JFrame {

    private static final Map<String, EmulatorInfo> EMULATOR_CONFIG = new LinkedHashMap<>();

    static {
        EMULATOR_CONFIG.put("雷电模拟器", new EmulatorInfo(
                "雷电模拟器",
                Arrays.asList("雷电模拟器", "LDPlayer", "ldplayer", "leidian"),
                Arrays.asList("dnplayer.exe", "LDPlayer.exe", "LdVBoxHeadless.exe"),
                Arrays.asList("adb.exe")
        ));

        EMULATOR_CONFIG.put("MuMu模拟器", new EmulatorInfo(
                "MuMu模拟器",
                Arrays.asList("MuMu模拟器", "MuMuPlayer", "MuMu", "MuMu12"),
                Arrays.asList("MuMuPlayer.exe", "MuMuPlayer12.exe"),
                Arrays.asList("shell\\adb.exe", "emulator\\nvmer\\adb.exe", "adb.exe")
        ));

        EMULATOR_CONFIG.put("夜神模拟器", new EmulatorInfo(
                "夜神模拟器",
                Arrays.asList("夜神模拟器", "Nox", "NoxPlayer"),
                Arrays.asList("Nox.exe", "NoxPlayer.exe"),
                Arrays.asList("nox_adb.exe", "adb.exe")
        ));

        EMULATOR_CONFIG.put("逍遥模拟器", new EmulatorInfo(
                "逍遥模拟器",
                Arrays.asList("逍遥模拟器", "MEmu", "MEmuPlayer"),
                Arrays.asList("MEmu.exe", "MEmuConsole.exe"),
                Arrays.asList("adb.exe", "memu_adb.exe")
        ));

        EMULATOR_CONFIG.put("蓝叠模拟器", new EmulatorInfo(
                "蓝叠模拟器",
                Arrays.asList("蓝叠模拟器", "BlueStacks", "BlueStacks5"),
                Arrays.asList("HD-Player.exe", "BlueStacks.exe"),
                Arrays.asList("HD-Adb.exe", "adb.exe")
        ));
    }

    static class EmulatorInfo {
        String name;
        List<String> shortcutKeywords;
        List<String> exeNames;
        List<String> adbNames;

        EmulatorInfo(String name, List<String> keywords, List<String> exeNames, List<String> adbNames) {
            this.name = name;
            this.shortcutKeywords = keywords;
            this.exeNames = exeNames;
            this.adbNames = adbNames;
        }
    }

    private JComboBox<String> emulatorCombo;
    private JTextField adbPathField;
    private JTextField installPathField;
    private JTextField exePathField;
    private JComboBox<String> deviceCombo;
    private JTextArea logArea;
    private JButton detectButton;
    private JButton saveButton;
    private JButton testButton;
    private JButton refreshDeviceButton;
    private JProgressBar progressBar;

    private String detectedAdbPath = "";
    private String detectedInstallPath = "";
    private String detectedExePath = "";
    private List<String> detectedDevices = new ArrayList<>();

    public aaa() {
        initUI();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("ADB自动配置工具");
        setSize(650, 650);
        setLocationRelativeTo(null);
        setVisible(true);

        new Thread(this::autoDetect).start();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // 标题
        JLabel titleLabel = new JLabel("ADB自动配置工具");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setForeground(new Color(70, 130, 180));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // 模拟器选择
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("模拟器类型:"), gbc);

        emulatorCombo = new JComboBox<>(EMULATOR_CONFIG.keySet().toArray(new String[0]));
        emulatorCombo.setPreferredSize(new Dimension(250, 30));
        gbc.gridx = 1;
        mainPanel.add(emulatorCombo, gbc);

        // 自动检测按钮
        detectButton = new JButton("🔍 自动检测");
        detectButton.setBackground(new Color(70, 130, 180));
        detectButton.setForeground(Color.BLACK);
        detectButton.setFont(new Font("微软雅黑", Font.BOLD, 12));
        detectButton.addActionListener(e -> new Thread(this::autoDetect).start());
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(detectButton, gbc);

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        gbc.gridy = 3;
        mainPanel.add(progressBar, gbc);

        // EXE路径
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("模拟器EXE:"), gbc);

        exePathField = new JTextField();
        exePathField.setEditable(false);
        exePathField.setPreferredSize(new Dimension(350, 30));
        exePathField.setBackground(new Color(245, 245, 245));
        gbc.gridx = 1;
        mainPanel.add(exePathField, gbc);

        // 安装路径
        gbc.gridy = 5;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("安装路径:"), gbc);

        installPathField = new JTextField();
        installPathField.setEditable(false);
        installPathField.setPreferredSize(new Dimension(350, 30));
        installPathField.setBackground(new Color(245, 245, 245));
        gbc.gridx = 1;
        mainPanel.add(installPathField, gbc);

        // ADB路径
        gbc.gridy = 6;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("ADB路径:"), gbc);

        adbPathField = new JTextField();
        adbPathField.setEditable(false);
        adbPathField.setPreferredSize(new Dimension(350, 30));
        adbPathField.setBackground(new Color(245, 245, 245));
        gbc.gridx = 1;
        mainPanel.add(adbPathField, gbc);

        // 设备面板
        JPanel devicePanel = new JPanel(new BorderLayout());
        devicePanel.setBorder(new TitledBorder("已连接设备"));

        deviceCombo = new JComboBox<>();
        deviceCombo.setPreferredSize(new Dimension(300, 30));

        refreshDeviceButton = new JButton("🔄 刷新设备");
        refreshDeviceButton.setBackground(new Color(100, 100, 100));
        refreshDeviceButton.setForeground(Color.BLACK);
        refreshDeviceButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        refreshDeviceButton.addActionListener(e -> refreshDevices());

        JPanel deviceSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        deviceSelectPanel.add(new JLabel("选择设备:"));
        deviceSelectPanel.add(deviceCombo);
        deviceSelectPanel.add(refreshDeviceButton);

        devicePanel.add(deviceSelectPanel, BorderLayout.NORTH);

        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        mainPanel.add(devicePanel, gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));

        testButton = new JButton("🧪 测试连接");
        testButton.setBackground(new Color(60, 179, 113));
        testButton.setForeground(Color.BLACK);
        testButton.setFont(new Font("微软雅黑", Font.BOLD, 12));
        testButton.addActionListener(e -> testConnection());

        saveButton = new JButton("💾 保存配置");
        saveButton.setBackground(new Color(255, 165, 0));
        saveButton.setForeground(Color.BLACK);
        saveButton.setFont(new Font("微软雅黑", Font.BOLD, 12));
        saveButton.addActionListener(e -> saveConfig());

        JButton cancelButton = new JButton("取消");
        cancelButton.setBackground(new Color(200, 200, 200));
        cancelButton.setForeground(Color.BLACK);
        cancelButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // 日志面板
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("检测日志"));
        logPanel.setPreferredSize(new Dimension(0, 180));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        JButton clearLogButton = new JButton("清空日志");
        clearLogButton.setBackground(new Color(100, 100, 100));
        clearLogButton.setForeground(Color.BLACK);
        clearLogButton.addActionListener(e -> logArea.setText(""));
        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logButtonPanel.add(clearLogButton);
        logPanel.add(logButtonPanel, BorderLayout.SOUTH);

        add(logPanel, BorderLayout.SOUTH);

        emulatorCombo.addActionListener(e -> {
            String selected = (String) emulatorCombo.getSelectedItem();
            if (selected != null) {
                new Thread(() -> autoDetectForEmulator(selected)).start();
            }
        });
    }

    /**
     * 自动检测 - 通过搜索快捷方式找到安装路径
     */
    private void autoDetect() {
        appendLog("================================================");
        appendLog("开始自动检测...");
        appendLog("正在搜索系统中的模拟器快捷方式...");
        appendLog("================================================");

        String selectedEmulator = (String) emulatorCombo.getSelectedItem();
        if (selectedEmulator != null) {
            autoDetectForEmulator(selectedEmulator);
        } else {
            for (String emulator : EMULATOR_CONFIG.keySet()) {
                if (autoDetectForEmulator(emulator)) {
                    emulatorCombo.setSelectedItem(emulator);
                    break;
                }
            }
        }

        if (detectedAdbPath.isEmpty()) {
            appendLog("\n[警告] 自动检测失败。");
            appendLog("[提示] 请手动选择模拟器快捷方式或EXE文件。");
        } else {
            refreshDevices();
        }
    }

    /**
     * 为指定模拟器自动检测
     */
    private boolean autoDetectForEmulator(String emulatorName) {
        EmulatorInfo info = EMULATOR_CONFIG.get(emulatorName);
        if (info == null) return false;

        appendLog("\n[信息] 正在检测 " + emulatorName + "...");

        // 1. 通过快捷方式搜索
        String exePath = findEmulatorByShortcut(info);

        // 2. 如果没找到，通过开始菜单搜索
        if (exePath == null) {
            exePath = findEmulatorInStartMenu(info);
        }

        // 3. 如果还没找到，通过注册表搜索
        if (exePath == null) {
            exePath = findEmulatorInRegistry(info);
        }

        // 4. 如果还没找到，搜索常见安装目录
        if (exePath == null) {
            exePath = findEmulatorInCommonPaths(info);
        }

        if (exePath != null) {
            detectedExePath = exePath;
            exePathField.setText(detectedExePath);

            // 获取安装目录
            File exeFile = new File(exePath);
            detectedInstallPath = exeFile.getParent();
            installPathField.setText(detectedInstallPath);
            appendLog("   [成功] 找到EXE: " + detectedExePath);
            appendLog("   [成功] 安装路径: " + detectedInstallPath);

            // 搜索ADB
            for (String adbName : info.adbNames) {
                File adbFile = findAdbInDirectory(new File(detectedInstallPath), adbName);
                if (adbFile != null) {
                    detectedAdbPath = adbFile.getAbsolutePath();
                    adbPathField.setText(detectedAdbPath);
                    appendLog("   [成功] 找到ADB: " + detectedAdbPath);
                    return true;
                }
            }

            // 深度搜索ADB
            String adbPath = deepSearchAdb(new File(detectedInstallPath), info.adbNames);
            if (adbPath != null) {
                detectedAdbPath = adbPath;
                adbPathField.setText(detectedAdbPath);
                appendLog("   [成功] 找到ADB: " + detectedAdbPath);
                return true;
            }

            appendLog("   [警告] 安装目录中未找到ADB");
        } else {
            appendLog("   [失败] 未找到 " + emulatorName + " 安装");
        }

        return false;
    }

    /**
     * 通过快捷方式搜索模拟器
     */
    private String findEmulatorByShortcut(EmulatorInfo info) {
        appendLog("   正在搜索快捷方式...");

        // 桌面快捷方式
        String desktopPath = System.getProperty("user.home") + "\\Desktop";
        String result = searchShortcutsInDirectory(desktopPath, info);
        if (result != null) return result;

        // 公共桌面
        String publicDesktop = "C:\\Users\\Public\\Desktop";
        result = searchShortcutsInDirectory(publicDesktop, info);
        if (result != null) return result;

        // 开始菜单
        String startMenu = System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu";
        result = searchShortcutsInDirectory(startMenu, info);
        if (result != null) return result;

        // 所有用户开始菜单
        String allUsersStartMenu = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu";
        result = searchShortcutsInDirectory(allUsersStartMenu, info);
        if (result != null) return result;

        return null;
    }

    /**
     * 在目录中搜索快捷方式
     */
    private String searchShortcutsInDirectory(String dirPath, EmulatorInfo info) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) return null;

        try {
            File[] files = dir.listFiles();
            if (files == null) return null;

            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".lnk")) {
                    String fileName = file.getName().toLowerCase();
                    for (String keyword : info.shortcutKeywords) {
                        if (fileName.contains(keyword.toLowerCase())) {
                            String target = resolveShortcut(file);
                            if (target != null) {
                                appendLog("      找到快捷方式: " + file.getName());
                                appendLog("      目标: " + target);

                                for (String exeName : info.exeNames) {
                                    if (target.toLowerCase().contains(exeName.toLowerCase())) {
                                        return target;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 解析快捷方式文件 (.lnk)
     */
    private String resolveShortcut(File lnkFile) {
        try {
            Process ps = Runtime.getRuntime().exec(
                    "powershell -Command \"(New-Object -ComObject WScript.Shell).CreateShortcut('" + lnkFile.getAbsolutePath() + "').TargetPath\""
            );

            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream(), "GBK"));
            String target = reader.readLine();
            ps.waitFor();

            if (target != null && !target.trim().isEmpty() && new File(target.trim()).exists()) {
                return target.trim();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 在开始菜单中搜索
     */
    private String findEmulatorInStartMenu(EmulatorInfo info) {
        appendLog("   正在搜索开始菜单...");

        String[] startMenuPaths = {
                System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs",
                "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs"
        };

        for (String path : startMenuPaths) {
            File startMenu = new File(path);
            if (startMenu.exists()) {
                String result = searchDirectoryForExe(startMenu, info);
                if (result != null) return result;
            }
        }

        return null;
    }

    /**
     * 在目录中递归搜索EXE
     */
    private String searchDirectoryForExe(File dir, EmulatorInfo info) {
        if (!dir.exists() || !dir.isDirectory()) return null;

        try {
            File[] files = dir.listFiles();
            if (files == null) return null;

            for (File file : files) {
                if (file.isDirectory()) {
                    String result = searchDirectoryForExe(file, info);
                    if (result != null) return result;
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".exe")) {
                    String fileName = file.getName().toLowerCase();
                    for (String keyword : info.shortcutKeywords) {
                        if (fileName.contains(keyword.toLowerCase())) {
                            return file.getAbsolutePath();
                        }
                    }
                    for (String exeName : info.exeNames) {
                        if (fileName.equals(exeName.toLowerCase())) {
                            return file.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 在注册表中搜索
     */
    private String findEmulatorInRegistry(EmulatorInfo info) {
        appendLog("   正在搜索注册表...");

        String[] regPaths = {
                "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths",
                "HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\App Paths",
                "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths"
        };

        try {
            for (String exeName : info.exeNames) {
                for (String regPath : regPaths) {
                    Process process = Runtime.getRuntime().exec(
                            "reg query \"" + regPath + "\\" + exeName + "\" /ve"
                    );
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("REG_SZ")) {
                            String[] parts = line.split("\\s+");
                            if (parts.length > 0) {
                                String path = parts[parts.length - 1];
                                File exeFile = new File(path);
                                if (exeFile.exists() && exeFile.isFile()) {
                                    appendLog("      在注册表中找到: " + path);
                                    return path;
                                }
                            }
                        }
                    }
                    process.waitFor();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 在常见安装目录搜索
     */
    private String findEmulatorInCommonPaths(EmulatorInfo info) {
        appendLog("   正在搜索常见安装目录...");

        String[] drives = {"D:", "C:", "E:", "F:"};
        String[] basePaths = {
                "\\Program Files",
                "\\Program Files (x86)",
                "\\",
                "\\Apps",
                "\\Game",
                "\\Games"
        };

        for (String drive : drives) {
            for (String basePath : basePaths) {
                for (String folderName : info.shortcutKeywords) {
                    String path = drive + basePath + "\\" + folderName;
                    File dir = new File(path);
                    if (dir.exists() && dir.isDirectory()) {
                        String exePath = searchDirectoryForExe(dir, info);
                        if (exePath != null) {
                            appendLog("      在目录中找到: " + path);
                            return exePath;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 在目录中查找ADB
     */
    private File findAdbInDirectory(File dir, String adbName) {
        if (!dir.exists() || !dir.isDirectory()) return null;

        File adbFile = new File(dir, adbName);
        if (adbFile.exists() && adbFile.isFile()) {
            return adbFile;
        }
        return null;
    }

    /**
     * 深度搜索ADB文件
     */
    private String deepSearchAdb(File dir, List<String> adbNames) {
        if (dir == null || !dir.exists()) return null;

        try {
            File[] files = dir.listFiles();
            if (files == null) return null;

            for (File file : files) {
                if (file.isDirectory()) {
                    if (file.getName().equals("System32") || file.getName().equals("Windows")) {
                        continue;
                    }
                    String result = deepSearchAdb(file, adbNames);
                    if (result != null) return result;
                } else if (file.isFile()) {
                    for (String adbName : adbNames) {
                        if (file.getName().equalsIgnoreCase(adbName)) {
                            return file.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 刷新设备列表
     */
    private void refreshDevices() {
        if (detectedAdbPath.isEmpty()) {
            appendLog("[警告] 请先配置ADB路径");
            return;
        }

        appendLog("[信息] 正在扫描设备...");
        detectedDevices.clear();
        deviceCombo.removeAllItems();

        try {
            Process process = Runtime.getRuntime().exec(detectedAdbPath + " devices");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.endsWith("device") && !line.startsWith("List")) {
                    String deviceId = line.replace("device", "").trim();
                    detectedDevices.add(deviceId);
                    deviceCombo.addItem(deviceId);
                    appendLog("   [成功] 发现设备: " + deviceId);
                }
            }
            process.waitFor();

            if (detectedDevices.isEmpty()) {
                appendLog("[警告] 未发现设备，请确保模拟器已开启");
                deviceCombo.addItem("未检测到设备");
            } else {
                appendLog("[成功] 共发现 " + detectedDevices.size() + " 个设备");
            }
        } catch (Exception e) {
            appendLog("[错误] 设备扫描失败: " + e.getMessage());
        }
    }

    /**
     * 测试连接
     */
    private void testConnection() {
        if (detectedAdbPath.isEmpty()) {
            appendLog("[错误] 请先配置ADB路径");
            return;
        }

        String device = (String) deviceCombo.getSelectedItem();
        if (device == null || device.equals("未检测到设备")) {
            appendLog("[错误] 请先选择设备");
            return;
        }

        appendLog("[信息] 正在测试连接 " + device + " ...");

        try {
            Process p1 = Runtime.getRuntime().exec(detectedAdbPath + " -s " + device + " shell echo test");
            int code1 = p1.waitFor();

            if (code1 == 0) {
                appendLog("[成功] 连接测试通过！");
            } else {
                appendLog("[错误] 连接测试失败");
            }
        } catch (Exception e) {
            appendLog("[错误] 测试失败: " + e.getMessage());
        }
    }

    /**
     * 保存配置
     */
    private void saveConfig() {
        if (detectedAdbPath.isEmpty()) {
            appendLog("[错误] 请先配置ADB路径");
            return;
        }

        String device = (String) deviceCombo.getSelectedItem();
        if (device == null || device.equals("未检测到设备")) {
            appendLog("[错误] 请先选择设备");
            return;
        }

        try {
            File configDir = new File("./config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            Properties props = new Properties();
            props.setProperty("adb.path", detectedAdbPath);
            props.setProperty("device", device);
            props.setProperty("emulator", (String) emulatorCombo.getSelectedItem());
            props.setProperty("install.path", detectedInstallPath);
            props.setProperty("exe.path", detectedExePath);
            props.setProperty("last_update", new Date().toString());

            try (FileOutputStream out = new FileOutputStream("./config/adb_config.properties")) {
                props.store(out, "ADB配置");
            }

            appendLog("================================================");
            appendLog("[成功] 配置保存成功！");
            appendLog("   模拟器: " + emulatorCombo.getSelectedItem());
            appendLog("   EXE路径: " + detectedExePath);
            appendLog("   安装路径: " + detectedInstallPath);
            appendLog("   ADB路径: " + detectedAdbPath);
            appendLog("   设备: " + device);
            appendLog("================================================");

            JOptionPane.showMessageDialog(this,
                    "配置保存成功！",
                    "成功", JOptionPane.INFORMATION_MESSAGE);
            // 保存成功后关闭窗口
            dispose();

        } catch (Exception e) {
            appendLog("[错误] 保存失败: " + e.getMessage());
        }
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new aaa());
    }
}