# Mythic Age / 神话时代

Minecraft 生存模组，目标是在同一世界中呈现希腊、埃及、北欧、中华与亚特兰蒂斯的神话文明。通过村庄、神庙、恩宠、一次性神力、神话单位与试炼推进游戏。

**当前版本：`0.1.0-alpha.A3`，阶段 A 试玩候选，仅希腊 MVP。** 这不是五文明完整版，也不表示 G-A 用户试玩已通过。建议新建世界；不要直接覆盖旧版或重要存档。

## 环境与依赖

以下为本项目锁定、用于构建与测试的版本，不代表各依赖的最新版本。

| 组件 | 版本 | 要求 |
|---|---|---|
| Minecraft Java Edition | 1.21.1 | 必需 |
| Java | 21 | 必需 |
| NeoForge | 21.1.259 | 必需 |
| GeckoLib | 4.9.3（NeoForge / 1.21.1） | 必需 |
| Curios | 9.5.1+1.21.1（NeoForge） | 必需 |
| Patchouli | 1.21.1-93-NEOFORGE | 必需 |
| JEI | 19.51.0.418（NeoForge / 1.21.1） | 可选；开发运行时已配置 |

使用各依赖正式的 NeoForge 运行 jar，不要拿 API-only、sources、Fabric 或 Forge 版本代替。依赖不打包进本模组 jar；客户端与专服都安装三个必需模组。

## 阶段 A 已有内容

- 希腊村庄：居民生产、逐块施工与原地升级、生活、卫兵、委托、善恶与贡献；村庄界面（地图、建设、村民、仓库、委托、信仰），可点名下一个项目；地面可见的村界提示。
- 村庄神庙（等级看卫城）、供奉者、村民祈祷与献供、恩宠结算，远古、古典和英雄阶段的仪式。
- 七个一次性神力；弥诺陶洛斯、天马与半人马召唤；天马骑乘与侦察。
- 五种希腊野外生物、生态点、遗迹、克诺索斯迷宫及弥诺陶洛斯试炼。
- 三件圣物的佩戴/供奉模式，锡与青铜、希腊建材、橄榄/葡萄及基础装备。
- J 信仰面板、Patchouli《神话手札》框架、图鉴点亮、三语 UI，以及可选 JEI 配方提示。

手札首次入世界发放；丢失后可用书和羽毛再合成。第一步找希腊村庄的神庙祭司，受祝福后成为这个村的供奉者。

## 安装与构建

普通玩家：安装上述 Minecraft/NeoForge 环境，把 `mythic_age-0.1.0-alpha.A3.jar` 与必需依赖放进该实例的 `mods/`，新建世界。多人游戏的客户端与专服需使用一致的本模组版本。

开发构建（仓库根目录；先确保 Java 21）：

```sh
./gradlew build
# 产物：neoforge/build/libs/mythic_age-0.1.0-alpha.A3.jar
./gradlew :neoforge:runClient
# 专服首次运行须由操作者阅读并同意 Minecraft EULA
./gradlew :neoforge:runServer
```

Gradle wrapper 锁定 9.2.1。依赖已缓存时可加 `--offline`，首次安装依赖不能靠此选项。macOS 可在当前终端用 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` 选择 Java 21，不必更改系统默认 Java。

## 验证与开发工具

```sh
./gradlew :core:test :neoforge:runData
python3 tools/check_content.py
python3 tools/check_equipment.py
python3 tools/blockbench/check_rig.py
python3 -m unittest discover -s tools -p 'test_*.py'
python3 -m unittest discover -s tools/blockbench -p 'test_*.py'
./gradlew :neoforge:runGameTestServer
./gradlew build
```

`core` 不依赖 Minecraft；`neoforge` 负责游戏接入。语言由 datagen 生成，修改 `LangKeys` 或内容源，不只编辑 generated JSON。

`tools/server_smoke.py` 在本机启动一次性专服、测试命令和存档退出，并恢复配置。`tools/ma_server.py` 与 `tools/ma_remote.py` 用于开发实机；离线测试服仅绑定 127.0.0.1。遥控 agent/devtools 不进入发行 jar，不是给公网服务器开放的远程管理服务。

管理员可用 `/mythicage dev perf start 300`、`/mythicage dev perf report` 采样；报告明确区分每刻已覆盖代码与世界生成，不能把零样本视为达标。报告在实例的 `logs/mythic_age_perf.txt`。

## 已知范围与待验收事项

- 手札叙述正文正在由作者重写，若干章节仍缺页；现有文案是占位，不是定稿。英日叙述翻译待中文稿与分工确认。
- 其余四文明、航海、希腊后续 Boss/神器/完整神话时代属于后续阶段，不要把未开放项当成阶段 A 已实现内容。
- 部分界面数据关闭重开才刷新，中文换行仍有少量标点/数字断行瑕疵。
- 自动化、开发客户端和专服记录不能替代干净实例安装测试及用户从零试玩。G-A 反馈后再打发布标签，不提前宣称已发布。
- 仓库工作区内的试玩指南、反馈模板和验证记录位于 `docs/重构/试玩/阶段A/`；依项目约定 `docs/` 不进 Git，因此仅克隆源码不会带上这些本地资料。

## English

Mythic Age is a survival-oriented mythology mod. This **0.1.0-alpha.A3 playtest candidate** contains the Greek MVP, not all five planned civilizations. It includes autonomous villages that build block by block (with a village screen and map where you can nominate the next project), village temples with devotees, prayer and favor, seven one-shot god powers, three summon types, Greek creatures and ruins, the Knossos Minotaur trial, relics and early equipment.

Use Minecraft **1.21.1**, **Java 21**, and **NeoForge 21.1.259**. Required NeoForge mods: **GeckoLib 4.9.3**, **Curios 9.5.1+1.21.1**, and **Patchouli 1.21.1-93-NEOFORGE**. **JEI 19.51.0.418** is optional. Put the runtime jars in a fresh instance's `mods` folder; back up important worlds. These are pinned project versions, not a claim about the latest available releases.

Build with `./gradlew build`; the playable jar is `neoforge/build/libs/mythic_age-0.1.0-alpha.A3.jar`. `./gradlew :neoforge:runClient` launches the development client. Press **J** for the faith panel; the handbook is granted on first login. Speak to a Greek temple priest to begin.

Handbook prose is unfinished and awaiting the author's text; some chapters are placeholders. Passing automated tests does not constitute final user playtest approval. Other civilizations, sailing and the full Greek progression remain future work. Development remote-control code is excluded from the release jar.

## License

MIT — Copyright (c) 2026 HappySnappingTurtle. See `LICENSE`. Third-party dependencies keep their own licenses.
