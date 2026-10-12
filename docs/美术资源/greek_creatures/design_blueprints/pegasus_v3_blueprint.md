---
creature_id: pegasus
creature_name: Pegasus / 天马
design_version: v3
design_status: needs_user_review
modeling_allowed: false
current_v2_assets_status: rejected
blocking_reason: current concept and blockbench preview have malformed horse proportions
design_sheet: docs/美术资源/greek_creatures/design_sheets/pegasus_v3_design_sheet.svg
design_sheet_status: candidate_needs_user_review
design_correctness_audit: docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md
design_approval_package: docs/preview_assets/greek_creatures/design_approval/pegasus_hydra_design_approval_package.json
design_approval_board: docs/preview_assets/greek_creatures/design_approval/pegasus_v3_design_approval_board.png
design_approval_status: approval_pending
design_correctness_verdict: direction_correct_but_detail_insufficient
design_detail_sheet: docs/美术资源/greek_creatures/design_sheets/pegasus_v3_proportion_lock.md
design_detail_sheet_status: draft_requires_user_approval
design_detail_board: docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_v3_proportion_detail_board.png
design_detail_board_package: docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_hydra_detail_design_boards.json
design_detail_board_status: draft_requires_user_approval
---

# Pegasus / 天马 V3 设计蓝图

## 当前复核结论

用户指出当前天马模型头部过大、整体已经畸形。复核后确认：旧 V2/当前实现不正确；本 V3 蓝图的文字方向正确，但当前审批图仍偏技术示意，不能直接作为最终建模依据。

因此当前结论不是“V3 已正确可建模”，而是：必须先用 `pegasus_v3_proportion_lock.md` 锁定原版马式身体比例、头颈位置、翼根和骑乘空间，并用 `pegasus_v3_proportion_detail_board.png` 形成可审查细化设计板，再重新出可审批设计图。未完成前，`modeling_allowed=false` 必须保持。

## 设计判定

当前 V2 天马设计不正确。它不能继续作为 `.bbmodel`、Java model、贴图或动画同步依据。

V3 的设计目标是：先做一匹比例正确、纯白、有明确头部和马体结构的飞行坐骑，再在这个马体上添加可飞行、可载人飞行的双翼。识别顺序必须是“马体正确 -> 头颈正确 -> 翅膀正确 -> 骑乘飞行可信”，不能用大头、竖直头颈或宽翼片补识别度。

## 目标体量

- Entity ID: `mythic_age:pegasus`
- 碰撞盒目标：`1.7w x 2.1h`
- 视觉高度目标：`2.15` 到 `2.25` 格
- 体态基准：以原版成年马的横向马体比例为基准，整体更轻盈、更神圣，但不能离开马的基础解剖。
- 身体关系：
  - 马身主躯干长度约为躯干高度的 `1.8` 到 `2.2` 倍。
  - 胸腔和臀部形成水平长条马体，不允许身体短、头颈竖直堆高。
  - 头部主块宽度约为身体宽度的 `0.35` 到 `0.45`。
  - 头部主块高度约为胸腔高度的 `0.45` 到 `0.60`，包含耳朵后总高也不得像人形头那样压过颈部。
  - 吻部长度约为头部主块长度的 `0.55` 到 `0.75`，形成马脸，而不是方形玩具头。

## 轮廓语言

### 正面

- 第一眼必须看到纯白马头、左右耳、两侧深色眼点、窄胸和对称翼根。
- 头不能被翅膀、骑乘点或鬃毛遮住。
- 翅膀从肩后两侧打开，外轮廓高于背线但不得压住头部。

### 侧面

- 侧面是最重要验收角度：必须读成一匹白马。
- 头颈线从肩胛/鬐甲处向前上方自然伸出，再连接到前伸马头。
- 禁止竖直塔状脖子；头部中心必须在胸腔前方，而不是在身体中线上方。
- 翅膀挂在肩后，不是从腹部或尾部伸出；骑乘空间在背中部，不能挤占翼根。

### 45 度

- 能同时看到马头、胸、四肢、尾巴和双翼分层。
- 头颈不能被近侧翼片遮没。
- 纯白大面必须通过浅灰阴影读出结构，不得像空白块。

## 部件清单

- `root`
- `root/body/body_barrel`
- `root/body/chest`
- `root/body/rump`
- `root/body/withers`
- `root/body/saddle_space`
- `root/head_neck/neck_lower`
- `root/head_neck/neck_upper`
- `root/head_neck/neck_mane_strip`
- `root/head_neck/head_core`
- `root/head_neck/muzzle`
- `root/head_neck/nose_bridge`
- `root/head_neck/left_eye`
- `root/head_neck/right_eye`
- `root/head_neck/left_nostril`
- `root/head_neck/right_nostril`
- `root/head_neck/mouth_line`
- `root/head_neck/left_ear`
- `root/head_neck/right_ear`
- `root/wings/left_wing/shoulder_joint`
- `root/wings/left_wing/upper_wing`
- `root/wings/left_wing/secondary_wing`
- `root/wings/left_wing/coverts`
- `root/wings/left_wing/primary_feathers_01_06`
- `root/wings/right_wing/shoulder_joint`
- `root/wings/right_wing/upper_wing`
- `root/wings/right_wing/secondary_wing`
- `root/wings/right_wing/coverts`
- `root/wings/right_wing/primary_feathers_01_06`
- `root/legs/front_left/upper_leg/lower_leg/hoof`
- `root/legs/front_right/upper_leg/lower_leg/hoof`
- `root/legs/back_left/upper_leg/lower_leg/hoof`
- `root/legs/back_right/upper_leg/lower_leg/hoof`
- `root/tail/tail_base`
- `root/tail/tail_plume_segments`

## Blockbench 层级要求

- `body` group 只负责马体、胸、臀、鬐甲、骑乘空间。
- `head_neck` group 必须从 `withers` 位置出发，包含两段或三段斜向颈段，最后连接马头。
- `wings` group 挂在肩胛后方；每侧至少有 `shoulder_joint`、`upper_wing`、`secondary_wing`、`coverts`、`primary_feathers_01_06`。
- `legs` group 必须四条腿完整，马蹄与小腿分离。
- `tail` group 从臀部后上方连接，尾鬃分段。
- 禁止建模时临时添加未在本蓝图命名的关键结构；如需新增，先更新蓝图。

## UV 计划

| UV 区 | 内容 | 约束 |
| --- | --- | --- |
| `horse_head_front` | 左右眼、鼻梁、鼻孔、嘴线 | 眼点必须固定在头部两侧前上区域，不能漂到颈或翼上。 |
| `muzzle_side` | 细长吻部侧面、浅灰鼻口线 | 吻部要让侧面读成马脸。 |
| `neck_side` | 长颈浅灰肌理、鬃毛根部 | 颈部不能是一整块无纹理白柱。 |
| `body_side` | 白马躯干、胸腔、臀部、浅灰分面 | 必须保留纯白设定，但不能空白。 |
| `wing_top` | 翼肩、上翼、次翼、覆羽 | 翼根阴影明确，挂在肩后。 |
| `primary_feathers` | 每侧至少 6 片主羽 | 每片羽毛有灰线、高光和独立边界。 |
| `legs_hooves` | 四肢浅灰关节、白蹄或浅灰蹄 | 腿不能像四根白柱。 |
| `tail_mane` | 鬃毛和尾鬃分段 | 纯白到浅灰层次，不能加入彩色装饰。 |

## 纹理细节

- 主色：纯白和暖白，建议 `#f7f7f0`、`#ffffff`。
- 阴影：浅灰，建议 `#c8cdd0`、`#aeb5b8`。
- 眼睛：小型深色高对比点，建议 `#242830`。
- 鼻孔和口线：浅灰或暗灰短线，不能用大黑块。
- 不使用金色、蓝色或大面积装饰色；天马身份来自白马和翅膀，而不是饰品。
- 白色大面必须有分面阴影；任何视角不得出现“贴图像透明/空白”的感觉。

## 动画约束

- 待机：头颈轻微呼吸，翅膀小幅收放，身体稳定。
- 行走：四腿遵循马步，头颈同步轻微摆动。
- 飞行：翼肩先动，上翼和主羽有层级拍动；身体保持马体姿态，不像鸟身。
- 玩家骑乘飞行：`saddle_space` 稳定；玩家位置不得遮住头颈或翼根；翼拍不穿过玩家主体。

## 设计通过条件

- 侧面不看标签也能认出“白马 + 双翼”。
- 头部是细长马头，不是大方头，也不是看不见头。
- 颈部自然从肩胛伸出，不是竖直白柱。
- 翅膀有翼肩、次翼、覆羽和至少 6 片主羽。
- 纯白身体有浅灰结构线，不出现空白贴图感。
- 骑乘点和飞行能力同时成立。

## 设计否决条件

- 头颈竖直塔状。
- 头部过大，或为了看清五官牺牲马体比例。
- 头部过小或被翼片遮住，导致天马看不出头。
- 翅膀只是水平薄片或一组白条，没有羽毛层级。
- 骑乘空间与翼根、头颈互相遮挡。
- 纯白贴图没有浅灰分面，渲染后像空白。

## 建模前审批

当前状态：`needs_user_review`。

在用户确认该 V3 蓝图和比例锁定表前，不允许重新生成 `.bbmodel`、不允许同步 Java model、不允许把现有 V2 产物标为修复完成。
