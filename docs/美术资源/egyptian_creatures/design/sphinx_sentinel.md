---
creature_id: sphinx_sentinel
creature_name: Sphinx Sentinel / 狮身人面守卫
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

# Sphinx Sentinel / 狮身人面守卫 设计包

## 目标体量
- Entity ID: `mythic_age:sphinx_sentinel`
- 碰撞盒目标：`2.2w x 1.8h`
- 视觉高度目标：约 `1.95` 格
- 比例说明：长狮身、法老头饰和人面必须同时可读。

## 轮廓语言
- 正面：人面头饰、宽胸和前爪。
- 侧面：狮身长、尾巴和头饰侧片明显。
- 45 度：狮背和尾端可读。

## 概念设计图
- 文件：`docs/美术资源/egyptian_creatures/concepts/sphinx_sentinel_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 三视角必须同时读到人面、法老头饰和长狮身；不能退化成普通狮子。
- 头饰必须有蓝金侧片和额饰，眼睛/鼻口必须在正面高对比可读。
- 侧面长身、厚前爪和尾巴必须支持狮身人面轮廓，身体不能过短。

## 核心识别特征
- 狮身
- 人面
- 法老头饰

## 部件清单
- `lion_body`
- `chest`
- `human_head`
- `human_face`
- `pharaoh_headdress`
- `headdress_left_panel`
- `headdress_right_panel`
- `headdress_brow`
- `left_eye`
- `right_eye`
- `nose_bridge`
- `mouth_line`
- `front_left_leg`
- `front_right_leg`
- `back_left_leg`
- `back_right_leg`
- `front_paws`
- `back_paws`
- `tail`
- `tail_tuft`
- `headdress_top_band`
- `left_pupil`
- `right_pupil`
- `chin_beard`
- `front_left_paw`
- `front_right_paw`
- `back_left_paw`
- `back_right_paw`
- `front_left_claws`
- `front_right_claws`
- `shoulder_scarab`

## Blockbench 层级
- `root/body`
- `root/head`
- `root/legs`
- `root/tail`

## 建模硬性要求
- head 与 headdress 分离
- 前爪厚重
- 侧面长身明显

## UV 计划
- head_front: human eyes
- headdress: blue-gold stripes
- body_side: sandy lion coat

## 纹理细节
- 主色：`#c79a4a`
- 阴影：`#4d3218`
- 强调色：`#315f7d`
- 细节规则：砂金狮身、蓝金法老头饰、深色人面眼睛和厚前爪。

## 动画约束
- 低伏待机；扑击时前爪前压；谜语/守卫技能时头部稳定。

## 返工触发条件
- 像普通狮子
- 没有人面/头饰
- 身体太短

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
