package com.example.主任务程序;

import com.example.师门任务.ShiMenV2;
import com.example.帮派任务.bprwV2;
import com.example.茶馆说书.v1;
import com.example.白榜追击.BaiBangZhuiJi;
import com.example.发布悬赏.FaBuXuanShang;
import com.example.接取悬赏.JieQuXuanShang;
import com.example.悬赏搬砖.XuanShangBanZuan;
import com.example.功绩兑换.GongJiDuiHuan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MainTaskGUI extends JFrame {

    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JButton upButton;
    private JButton downButton;
    private JButton executeButton;
    private JTextArea logArea;

    // 任务映射
    private final Map<String, Runnable> taskMap = new LinkedHashMap<>();

    public MainTaskGUI() {
        initTaskMap();
        initUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("一梦江湖全自动任务脚本");
        setSize(600, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initTaskMap() {
        taskMap.put("师门任务", () -> {
            try {
                ShiMenV2.main(new String[]{});
                appendLog("✅ 师门任务执行完成");
            } catch (Exception e) {
                appendLog("❌ 师门任务执行失败: " + e.getMessage());
            }
        });
        taskMap.put("帮派任务", () -> {
            try {
                bprwV2.main(new String[]{});
                appendLog("✅ 帮派任务执行完成");
            } catch (Exception e) {
                appendLog("❌ 帮派任务执行失败: " + e.getMessage());
            }
        });
        taskMap.put("茶馆说书", () -> {
            try {
                v1.main(new String[]{});
                appendLog("✅ 茶馆说书执行完成");
            } catch (Exception e) {
                appendLog("❌ 茶馆说书执行失败: " + e.getMessage());
            }
        });
        taskMap.put("白榜追击", () -> {
            try {
                BaiBangZhuiJi.main(new String[]{});
                appendLog("✅ 白榜追击执行完成");
            } catch (Exception e) {
                appendLog("❌ 白榜追击执行失败: " + e.getMessage());
            }
        });
        taskMap.put("发布悬赏", () -> {
            try {
                FaBuXuanShang.main(new String[]{});
                appendLog("✅ 发布悬赏执行完成");
            } catch (Exception e) {
                appendLog("❌ 发布悬赏执行失败: " + e.getMessage());
            }
        });
        taskMap.put("接取悬赏", () -> {
            try {
                JieQuXuanShang.main(new String[]{});
                appendLog("✅ 接取悬赏执行完成");
            } catch (Exception e) {
                appendLog("❌ 接取悬赏执行失败: " + e.getMessage());
            }
        });
        taskMap.put("悬赏搬砖", () -> {
            try {
                XuanShangBanZuan.main(new String[]{});
                appendLog("✅ 悬赏搬砖执行完成");
            } catch (Exception e) {
                appendLog("❌ 悬赏搬砖执行失败: " + e.getMessage());
            }
        });
        taskMap.put("功绩兑换", () -> {
            try {
                GongJiDuiHuan.main(new String[]{});
                appendLog("✅ 功绩兑换执行完成");
            } catch (Exception e) {
                appendLog("❌ 功绩兑换执行失败: " + e.getMessage());
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 顶部面板：标题
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(70, 130, 180));
        JLabel titleLabel = new JLabel("一梦江湖全自动任务脚本");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        topPanel.add(titleLabel);
        add(topPanel, BorderLayout.NORTH);

        // 中间面板：任务列表
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("任务列表（勾选并调整顺序）"));

        // 表格模型：列名
        String[] columnNames = {"选择", "序号", "任务名称"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 1) return Integer.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // 只有选择列可编辑
            }
        };

        // 初始化任务列表
        int index = 1;
        for (String taskName : taskMap.keySet()) {
            tableModel.addRow(new Object[]{false, index++, taskName});
        }

        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(30);
        taskTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        taskTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        taskTable.getColumnModel().getColumn(2).setPreferredWidth(300);

        JScrollPane tableScroll = new JScrollPane(taskTable);
        centerPanel.add(tableScroll, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        upButton = new JButton("↑ 上移");
        downButton = new JButton("↓ 下移");
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
        centerPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // 右侧面板：控制按钮
        JPanel rightPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPanel.setPreferredSize(new Dimension(150, 0));

        executeButton = new JButton("▶ 执行选中任务");
        executeButton.setBackground(new Color(60, 179, 113));
        executeButton.setForeground(Color.WHITE);
        executeButton.setFont(new Font("微软雅黑", Font.BOLD, 14));

        JButton selectAllButton = new JButton("☑ 全选");
        JButton clearAllButton = new JButton("☐ 全不选");

        rightPanel.add(executeButton);
        rightPanel.add(selectAllButton);
        rightPanel.add(clearAllButton);
        add(rightPanel, BorderLayout.EAST);

        // 底部面板：日志区域
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("执行日志"));
        bottomPanel.setPreferredSize(new Dimension(0, 200));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        bottomPanel.add(logScroll, BorderLayout.CENTER);

        // 清空日志按钮
        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearLogButton = new JButton("清空日志");
        logButtonPanel.add(clearLogButton);
        bottomPanel.add(logButtonPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // 事件监听
        upButton.addActionListener(e -> moveRow(-1));
        downButton.addActionListener(e -> moveRow(1));
        executeButton.addActionListener(e -> executeSelectedTasks());
        selectAllButton.addActionListener(e -> selectAll(true));
        clearAllButton.addActionListener(e -> selectAll(false));
        clearLogButton.addActionListener(e -> logArea.setText(""));

        // 刷新序号
        refreshIndexes();
    }

    /**
     * 移动行
     */
    private void moveRow(int direction) {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先选中要移动的任务行", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int newRow = selectedRow + direction;
        if (newRow < 0 || newRow >= tableModel.getRowCount()) {
            return;
        }

        // 交换数据
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            Object temp = tableModel.getValueAt(selectedRow, col);
            tableModel.setValueAt(tableModel.getValueAt(newRow, col), selectedRow, col);
            tableModel.setValueAt(temp, newRow, col);
        }

        taskTable.setRowSelectionInterval(newRow, newRow);
        refreshIndexes();
    }

    /**
     * 刷新序号列
     */
    private void refreshIndexes() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(i + 1, i, 1);
        }
    }

    /**
     * 全选/全不选
     */
    private void selectAll(boolean select) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(select, i, 0);
        }
    }

    /**
     * 执行选中的任务
     */
    private void executeSelectedTasks() {
        // 收集选中的任务（按当前表格顺序）
        List<String> selectedTasks = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (isSelected != null && isSelected) {
                String taskName = (String) tableModel.getValueAt(i, 2);
                selectedTasks.add(taskName);
            }
        }

        if (selectedTasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少选择一个任务", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 确认执行
        StringBuilder confirmMsg = new StringBuilder("将按以下顺序执行任务：\n\n");
        for (int i = 0; i < selectedTasks.size(); i++) {
            confirmMsg.append("  ").append(i + 1).append(". ").append(selectedTasks.get(i)).append("\n");
        }
        confirmMsg.append("\n是否继续？");

        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg.toString(), "确认执行", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 禁用执行按钮，防止重复点击
        executeButton.setEnabled(false);

        // 在新线程中执行任务（避免卡界面）
        new Thread(() -> {
            appendLog("========== 开始执行任务 ==========");
            appendLog("执行顺序：");
            for (int i = 0; i < selectedTasks.size(); i++) {
                appendLog("  " + (i + 1) + ". " + selectedTasks.get(i));
            }
            appendLog("");

            long totalStartTime = System.currentTimeMillis();
            int completedCount = 0;

            for (int i = 0; i < selectedTasks.size(); i++) {
                String taskName = selectedTasks.get(i);
                appendLog("\n▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄");
                appendLog("  执行第 " + (i + 1) + "/" + selectedTasks.size() + " 个任务：" + taskName);
                appendLog("▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀");

                long taskStartTime = System.currentTimeMillis();

                Runnable task = taskMap.get(taskName);
                if (task != null) {
                    try {
                        task.run();
                        completedCount++;
                        long taskTime = (System.currentTimeMillis() - taskStartTime) / 1000;
                        appendLog("  任务耗时: " + formatTime(taskTime));
                    } catch (Exception e) {
                        appendLog("  ❌ 任务执行异常: " + e.getMessage());
                    }
                } else {
                    appendLog("  ❌ 未找到任务: " + taskName);
                }

                // 任务之间等待3秒
                if (i < selectedTasks.size() - 1) {
                    appendLog("\n⏳ 等待3秒后继续...");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            long totalTime = (System.currentTimeMillis() - totalStartTime) / 1000;
            appendLog("\n========== 任务执行完成 ==========");
            appendLog("完成数量: " + completedCount + "/" + selectedTasks.size());
            appendLog("总耗时: " + formatTime(totalTime));

            // 恢复执行按钮
            SwingUtilities.invokeLater(() -> executeButton.setEnabled(true));
        }).start();
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return minutes + "分" + remainingSeconds + "秒";
        }
        return remainingSeconds + "秒";
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(MainTaskGUI::new);
    }
}