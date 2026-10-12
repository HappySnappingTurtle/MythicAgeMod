---
creature_id: scarab_guardian
creature_name: Scarab Guardian / 圣甲虫守卫
design_status: needs_user_review
design_sheet_status: needs_user_review
design_approval_status: approval_pending
design_correctness_verdict: not_reviewed
design_detail_sheet_status: needs_user_review
design_detail_board_status: needs_user_review
modeling_allowed: false
design_package_status: generated_reference_needs_human_approval
approval_basis: human_design_review_required_before_model_uv_texture_animation_or_runtime_sync
current_v2_assets_status: generated_reference_only
blocking_reason: design package exists but has not been formally approved for 1.0 modeling
---

# Scarab Guardian / 圣甲虫守卫 设计包

## 目标体量
- Entity ID: `mythic_age:scarab_guardian`
- 碰撞盒目标：`1.8w x 1.2h`
- 视觉高度目标：约 `1.35` 格
- 比例说明：低伏宽体，比蜘蛛更甲壳化，背甲和角必须醒目。

## 轮廓语言
- 正面：宽甲壳、前角和六足展开。
- 侧面：低伏甲虫身、前角前伸。
- 45 度：背甲分片和后足可见。

## 概念设计图
- 文件：`docs/美术资源/egyptian_creatures/concepts/scarab_guardian_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须先读到低伏宽甲壳、前角、六足外扩和金色圣甲虫背甲。
- 背甲必须有分层金色甲片/太阳符号，不能只是绿色椭圆块。
- 六足与身体必须分离，前角侧面要明显前伸，不能读成普通蜘蛛。

## 核心识别特征
- 圣甲虫轮廓
- 金色背甲/太阳符号
- 六足和前角分离

## 部件清单
- `shell`
- `gold_carapace`
- `carapace_ridge`
- `head`
- `left_mandible`
- `right_mandible`
- `horn`
- `horn_tip`
- `left_eye`
- `right_eye`
- `front_left_leg`
- `front_right_leg`
- `mid_left_leg`
- `mid_right_leg`
- `back_left_leg`
- `back_right_leg`
- `left_shell_plate`
- `right_shell_plate`
- `rear_shell_plate`
- `scarab_sigil`
- `left_mandible_tip`
- `right_mandible_tip`
- `front_left_claw`
- `front_right_claw`
- `mid_left_claw`
- `mid_right_claw`
- `back_left_claw`
- `back_right_claw`

## Blockbench 层级
- `root/body`
- `root/head`
- `root/legs`

## 建模硬性要求
- shell 与 gold_carapace 分层
- 六足必须外扩
- horn 必须正侧面可见

## UV 计划
- shell: emerald panels
- gold_carapace: scarab sigil
- horn: gold tip
- eyes: dark beads

## 纹理细节
- 主色：`#1f4a3f`
- 阴影：`#0b1715`
- 强调色：`#d2a94b`
- 细节规则：深绿甲壳、金色背甲符号、黑色小眼，腿部用深色分节。

## 动画约束
- 六足交替爬行；攻击时前角突刺；防御时背甲稳定不变形。

## 返工触发条件
- 像普通蜘蛛
- 没有背甲符号
- 腿和身体糊成一块

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
