====================================
   一梦江湖脚本 - 自动更新说明
====================================

>>> 目录结构 <<<

update/
├── version.txt        ← 版本号 + 文件列表（主程序读取此文件判断是否有更新）
├── README.txt         ← 本说明文件
└── bin/               ← 编译好的 .class 文件（放到 classes/ 目录即可热更新）
    └── com/example/
        ├── 师门任务/ShiMenV2.class
        ├── 帮派任务/bprwV2.class
        ├── 茶馆说书/v1.class
        ├── 白榜追击/BaiBangZhuiJi.class
        ├── 发布悬赏/FaBuXuanShang.class
        ├── 接取悬赏/JieQuXuanShang.class
        ├── 悬赏搬砖/XuanShangBanZuan.class
        └── 功绩兑换/GongJiDuiHuan.class


>>> version.txt 格式说明 <<<

VERSION=1.0.1          ← 版本号（用户可读）
CODE=2                 ← 版本编号（整数，越大越新。改代码后 +1）
DATE=2026-05-25        ← 更新日期
FILES=xxx.class,...    ← 本次更新的文件列表（完整路径，逗号分隔）

⚠️ 重要：CODE 必须递增！主程序通过比较 CODE 判断是否有新版本。


>>> 发布流程 <<<

1. 修改 Java 代码
2. 运行 mvn compile     （只编译，不打包）
3. 运行 copy-update.bat 1.0.2   （把 .class 文件复制到 update/bin/ 并更新 version.txt）
                            （参数是新的版本号）

4. 把整个 update/ 目录上传到 GitHub 仓库根目录
   https://github.com/zakuro236/example/tree/main/update/

5. 用户启动程序时自动检测到更新，弹窗提示下载


>>> 用户端更新原理 <<<

主程序启动 → 从 jsDelivr CDN 下载 update/version.txt
→ 比较 CODE 值 → 发现新版本 → 弹窗确认
→ 下载 update/bin/ 下的 .class 文件 → 保存到 classes/ 目录
→ 提示重启 → 重启后热更新生效

（classes/ 目录优先级高于 ymjh-script.jar，所以新 .class 会覆盖 JAR 中的旧版）


>>> jsDelivr CDN 链接 <<<

版本文件: https://cdn.jsdelivr.net/gh/zakuro236/example@main/update/version.txt
文件下载: https://cdn.jsdelivr.net/gh/zakuro236/example@main/update/bin/{路径}

示例:
https://cdn.jsdelivr.net/gh/zakuro236/example@main/update/bin/com/example/师门任务/ShiMenV2.class
