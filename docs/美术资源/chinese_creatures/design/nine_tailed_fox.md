---
creature_id: nine_tailed_fox
creature_name: Nine-tailed Fox / 九尾狐
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

# Nine-tailed Fox / 九尾狐 设计包

## 目标体量
- Entity ID: `mythic_age:nine_tailed_fox`
- 碰撞盒目标：`1.2w x 1.4h`
- 视觉高度目标：约 `1.55` 格
- 比例说明：灵狐，中型四足，九尾扇形必须成为主要轮廓。

## 轮廓语言
- 正面：狐脸尖耳和胸毛。
- 侧面：四足狐身和多尾侧影。
- 45 度：九尾扇形最醒目。

## 概念设计图
- 文件：`docs/美术资源/chinese_creatures/concepts/nine_tailed_fox_concept.png`
- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。
- 三视角：正面、侧面、45 度必须同时能识别该生物。

## V2 造型冻结
- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。
- 正面必须读到狐脸、尖耳、红眼和白胸毛，不得像普通狗或狼。
- 狐脸主体必须轻巧，识别来自尖耳、眼、鼻口和九尾扇形，不能用大头压过身体。
- 背面和 45 度必须读到九尾扇形；运行模型必须有 9 条独立尾部或等价可数结构。
- 浅金毛、红眼、粉红尾尖和白胸毛必须在纹理上分区清楚。

## 核心识别特征
- 九条尾巴
- 狐脸尖耳
- 红眼

## 部件清单
- `fox_body`
- `shoulder_fur`
- `head`
- `snout`
- `nose`
- `red_eyes`
- `ears`
- `chest_fur`
- `nine_individual_tails`
- `tail_tips`
- `legs`
- `paws`

## Blockbench 层级
- `root/body`
- `root/head`
- `root/tails`
- `root/legs`

## 建模硬性要求
- tails 必须 9 条独立可数并形成扇形
- ears、snout、red_eyes 和 chest_fur 必须独立
- 身体轻盈且爪部小巧

## UV 计划
- face_front: red eyes, dark nose and fox snout
- chest_fur: bright white chest patch
- tails: nine individually countable pale-gold tails with pink tips
- legs: warm fur with darker paws

## 纹理细节
- 主色：`#f0d2a2`
- 阴影：`#8b4f35`
- 强调色：`#e84a5f`
- 细节规则：浅金毛、红眼、深色鼻口、粉红尾尖、白胸毛和深色小爪必须分区清楚。

## 动画约束
- 轻快步态；攻击时尾部扇动；魅惑技能时尾巴展开。

## 返工触发条件
- 像普通狐狸
- 尾巴不明显
- 没有尖耳

## 验收口径
- 先验收概念设计图，再进入 Blockbench 建模。
- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。
- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。
- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。
