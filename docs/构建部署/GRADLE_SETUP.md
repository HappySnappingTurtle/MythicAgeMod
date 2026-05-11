# Gradle 8.2 编译配置指南

## 📋 概述

本项目可以使用 Gradle 8.2 或项目 Gradle Wrapper 进行编译。若使用本地安装的 Gradle，可通过 `GRADLE_HOME` 或 `GRADLE_BIN` 指定路径。

## 🔧 编译方法

### 方法一：使用编译脚本（推荐）

项目根目录下的 `build-with-gradle8.sh` 脚本已配置好所有必要的设置：

```bash
# 完整编译
./build-with-gradle8.sh build

# 清理编译
./build-with-gradle8.sh clean

# 清理后重新编译
./build-with-gradle8.sh clean build

# 运行测试
./build-with-gradle8.sh test

# 生成开发环境
./build-with-gradle8.sh genSources
```

### 方法二：直接使用 Gradle 命令

```bash
# 使用 Gradle Wrapper
./gradlew build

# 或者设置本地 Gradle 环境变量后使用
export GRADLE_HOME="/path/to/gradle-8.2"
export PATH="$GRADLE_HOME/bin:$PATH"
gradle build
```

## 📦 编译输出

编译成功后，生成的 Mod 文件将位于：
- `build/libs/mythic_age-1.0.0.jar` - 主要的 Mod 文件
- `build/libs/mythic_age-1.0.0-sources.jar` - 源代码包（如果生成）

## 🎯 常用编译任务

| 任务 | 命令 | 描述 |
|------|------|------|
| 完整编译 | `./build-with-gradle8.sh build` | 编译并生成 Mod JAR 文件 |
| 清理 | `./build-with-gradle8.sh clean` | 清理所有编译产物 |
| 测试 | `./build-with-gradle8.sh test` | 运行单元测试 |
| 生成源码 | `./build-with-gradle8.sh genSources` | 生成开发环境源码 |
| 生成 Eclipse 项目 | `./build-with-gradle8.sh genEclipseRuns` | 生成 Eclipse 运行配置 |
| 生成 IntelliJ 项目 | `./build-with-gradle8.sh genIntellijRuns` | 生成 IntelliJ 运行配置 |

## 🔍 版本信息

当前配置的 Gradle 版本：
- **Gradle**: 8.2
- **Kotlin**: 1.8.20
- **Groovy**: 3.0.17
- **JVM**: 17.0.15+
- **操作系统**: Mac OS X

## ⚠️ 注意事项

1. **JVM 版本**: 确保使用 Java 17 或更高版本
2. **网络连接**: 首次编译时需要下载依赖，确保网络连接正常
3. **内存设置**: 如果遇到内存不足，可以在 `gradle.properties` 中调整 JVM 参数

## 🛠️ 故障排除

### 编译失败
```bash
# 清理并重新编译
./build-with-gradle8.sh clean build

# 如果仍然失败，检查 Java 版本
java --version
```

### 依赖下载失败
```bash
# 强制刷新依赖
./build-with-gradle8.sh build --refresh-dependencies
```

### 权限问题
```bash
# 确保脚本有执行权限
chmod +x build-with-gradle8.sh
```

## 📁 项目结构

```
MythicAgeMod/
├── build-with-gradle8.sh          # 编译脚本
├── build.gradle                   # Gradle 构建配置
├── gradle.properties              # Gradle 属性配置
├── src/
│   └── main/
│       ├── java/                  # Java 源代码
│       └── resources/             # 资源文件
├── build/                         # 编译输出目录
│   └── libs/                      # 生成的 JAR 文件
└── README.md                      # 项目说明文档
```

## 🚀 快速开始

1. **克隆或下载项目**
2. **进入项目目录**
   ```bash
   cd MythicAgeMod
   ```
3. **执行编译**
   ```bash
   ./build-with-gradle8.sh build
   ```
4. **查看结果**
   ```bash
   ls -la build/libs/
   ```

## 📝 开发环境设置

### IntelliJ IDEA
```bash
./build-with-gradle8.sh genIntellijRuns
```
然后在 IntelliJ 中打开项目文件夹。

### Eclipse
```bash
./build-with-gradle8.sh genEclipseRuns
```
然后在 Eclipse 中导入项目。

### VS Code
直接打开项目文件夹，VS Code 应该能自动识别 Gradle 项目。

---

**配置完成日期**: 2024年
**Gradle 版本**: 8.2
**项目类型**: Fabric Minecraft Mod
**目标版本**: Minecraft 1.20.1
