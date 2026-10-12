---
creature_id: hydra
creature_name: Hydra / 九头蛇
design_version: v3
design_status: needs_user_review
modeling_allowed: false
current_v2_assets_status: rejected
blocking_reason: current concept and blockbench preview do not establish valid head-neck-body connections
design_sheet: docs/美术资源/greek_creatures/design_sheets/hydra_v3_design_sheet.svg
design_sheet_status: candidate_needs_user_review
design_correctness_audit: docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md
design_approval_package: docs/preview_assets/greek_creatures/design_approval/pegasus_hydra_design_approval_package.json
design_approval_board: docs/preview_assets/greek_creatures/design_approval/hydra_v3_design_approval_board.png
design_approval_status: approval_pending
design_correctness_verdict: direction_correct_but_detail_insufficient
design_detail_sheet: docs/美术资源/greek_creatures/design_sheets/hydra_v3_topology_lock.md
design_detail_sheet_status: draft_requires_user_approval
design_detail_board: docs/preview_assets/greek_creatures/design_approval/detail_boards/hydra_v3_topology_detail_board.png
design_detail_board_package: docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_hydra_detail_design_boards.json
design_detail_board_status: draft_requires_user_approval
---

# Hydra / 九头蛇 V3 设计蓝图

## 当前复核结论

用户指出当前九头蛇头和身体没有连接，且整体畸形。复核后确认：旧 V2/当前实现不正确；本 V3 蓝图的方向正确，但当前审批图仍偏结构示意，不能充分证明每个头、颈段、颈根 socket 和身体肩带之间的真实体积连接。

因此当前结论不是“V3 已正确可建模”，而是：必须先用 `hydra_v3_topology_lock.md` 锁定九头拓扑、颈段数量、socket 位置、重叠规则和头身比例，并用 `hydra_v3_topology_detail_board.png` 形成可审查细化设计板，再重新出可审批设计图。未完成前，`modeling_allowed=false` 必须保持。

## 设计判定

当前 V2 九头蛇设计不正确。它不能继续作为 `.bbmodel`、Java model、贴图或动画同步依据。

V3 的设计目标是：先固定“低伏长身、多头蛇颈从同一肩带生长、每个头都与颈和身体有实体连接”的神话怪物结构，再进行建模。压迫感来自蛇形身体、头颈数量、错层扇形和毒性细节，不来自臃肿大躯干或巨大方头。

## 头数决策

- V3 默认采用显性 `9` 个头，以匹配“九头蛇”中文命名和神话直觉。
- 如果后续用户明确要求贴近《神话时代》可读性版本，可在设计阶段改为 `5` 个主战头；这个决定必须发生在建模前。
- 当前状态下禁止继续使用旧 V2 的 `5` 个大方头作为默认正确设计。

## 目标体量

- Entity ID: `mythic_age:hydra`
- 碰撞盒目标：`3.2w x 2.8h`
- 视觉高度目标：`2.8` 到 `3.1` 格
- 体态基准：低伏蛇蜥/沼泽怪，横向和纵深优先，高度来自多条颈的扇形抬升。
- 身体关系：
  - 主躯干长度约为身体高度的 `3.0` 到 `4.0` 倍。
  - 主躯干高度不超过整体视觉高度的 `0.32`。
  - 肩带宽度必须足够承载 9 个颈根，但不能变成正面大箱体。
  - 每个头部为楔形蛇头，头宽约为肩带总宽的 `0.12` 到 `0.18`；中央头允许略大，但不得超过相邻头的 `1.18` 倍。
  - 每条颈至少 `3` 段：`neck_root`、`neck_mid`、`neck_tip`，必须与肩带和头部重叠连接。

## 轮廓语言

### 正面

- 第一眼必须读到 9 个蛇头呈扇形展开，而不是一个大躯干上插几根柱子。
- 中央头最高，内侧头略低，中外侧头错层，最外侧头向外张开。
- 9 个颈根从肩带/胸前背板分散生出，每个颈根可见，不允许头悬空。
- 身体正面是低伏承托，不是主视觉大箱体。

### 侧面

- 侧面必须读成长身、低肩、长尾、多条蛇颈前探。
- 头颈从肩部向前上方弯曲，不能变成竖直柱。
- 至少能看到中央颈、近侧 3 条颈和远侧错层颈影。
- 尾巴长度接近或超过主躯干长度，向后逐渐变细。

### 45 度

- 45 度是最终结构验收角：必须同时看出 9 头错层、颈根连接、低伏身体、背脊鳞和长尾。
- 每个头的眼、嘴、獠牙或角/毒腺至少有一种高对比细节可读。
- 头部不能互相糊成一堵墙。

## 部件清单

- `root`
- `root/body/slender_body`
- `root/body/chest_keel`
- `root/body/shoulder_saddle`
- `root/body/neck_socket_01_09`
- `root/body/spine_crests_01_08`
- `root/body/tail_base`
- `root/body/tail_mid`
- `root/body/tail_tip`
- `root/legs/front_left`
- `root/legs/front_right`
- `root/legs/back_left`
- `root/legs/back_right`
- `root/necks/head_01_center/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`
- `root/necks/head_02_inner_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`
- `root/necks/head_03_inner_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`
- `root/necks/head_04_mid_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`
- `root/necks/head_05_mid_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`
- `root/necks/head_06_outer_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`
- `root/necks/head_07_outer_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`
- `root/necks/head_08_rear_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`
- `root/necks/head_09_rear_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/eyes/fangs/horns`

## Blockbench 层级要求

- `body` group 负责低伏躯干、胸骨、肩带、9 个颈根 socket、背脊鳞、尾巴和四肢。
- `necks` group 下必须有 9 个独立 head group。
- 每个 head group 必须包含：
  - `neck_root`
  - `neck_mid`
  - `neck_tip`
  - `head_core`
  - `upper_jaw`
  - `lower_jaw`
  - `left_eye`
  - `right_eye`
  - `left_fang`
  - `right_fang`
  - `horns` 或 `poison_glands`
- `neck_root` 必须与对应 `neck_socket` 和 `shoulder_saddle` 在几何上重叠或接触。
- `neck_tip` 必须与 `head_core` 在几何上重叠或接触。
- 禁止只有命名而没有实体连接的头颈结构。

## UV 计划

| UV 区 | 内容 | 约束 |
| --- | --- | --- |
| `head_front_01_09` | 每个蛇头的眼、眉、上颚、下颚、嘴线、獠牙 | 每个头都有固定面部区，不允许复制到错位面。 |
| `jaw_side_01_09` | 侧面上下颚和嘴角 | 侧面必须读出蛇头楔形，不是方块头。 |
| `neck_scale_01_09` | 颈部鳞片、毒性喉线 | 每条颈至少两段鳞片带，方向顺着颈段。 |
| `neck_socket_band` | 9 个颈根 socket 和肩带阴影 | 用于证明颈根从身体长出。 |
| `body_side` | 低伏深绿鳞片身体 | 身体不得是无细节深绿大块。 |
| `spine_crests` | 背脊鳞和毒性高光 | 从肩到尾连续。 |
| `tail` | 长尾鳞片和尾尖 | 尾巴逐段变细。 |
| `legs_claws` | 四肢、爪、腹部阴影 | 四肢支撑低伏体态。 |

## 纹理细节

- 主色：深沼泽绿，建议 `#234a2e`、`#1a3823`。
- 阴影：近黑绿，建议 `#0f1f14`。
- 高光：毒性黄绿，建议 `#78b356`、`#a2d95f`。
- 眼睛：亮黄或黄绿，小但高对比。
- 口腔：深红黑或深紫黑，不能和身体绿混成一块。
- 獠牙：偏白，必须出现在每个头部正面或侧面。
- 鳞片：身体大鳞片、颈部横向鳞带、背脊鳞三层分开。

## 动画约束

- 待机：9 个头错相轻摆，身体低伏不膨胀。
- 行走：四肢和身体轻微摆动，尾巴拖曳。
- 攻击：中央头和左右内侧头前探，外侧头保持扇形威胁。
- 特殊攻击：多个头错相张嘴，不能所有头同步成一堵墙。
- 任何动画状态下，头-颈、颈-肩连接不得断开。

## 设计通过条件

- 正面不看标签也能识别为多头九头蛇。
- 侧面不是胖蜥蜴，而是低伏长身、长尾、多颈前探。
- 9 个头都能找到对应颈根，且颈根都连接到肩带。
- 每个头有眼、上下颚、嘴线、獠牙和角/毒腺。
- 身体、颈、头比例协调；不能用大头代替细节。
- 45 度能看出多头错层而不是头部墙。

## 设计否决条件

- 头和身体没有连接，或连接只靠贴图/命名暗示。
- 身体臃肿，正面主视觉是大躯干。
- 头部太大，造成身子大头小。
- 头部太小或五官缺失，导致只像柱子顶端的绿块。
- 多头糊成一排墙，无法分辨每个头。
- 侧面只有一个头可读，其余头颈消失。

## 建模前审批

当前状态：`needs_user_review`。

在用户确认该 V3 蓝图和拓扑锁定表前，不允许重新生成 `.bbmodel`、不允许同步 Java model、不允许把现有 V2 产物标为修复完成。
