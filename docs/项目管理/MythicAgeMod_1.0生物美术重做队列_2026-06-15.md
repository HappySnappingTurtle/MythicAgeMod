# MythicAgeMod 1.0 生物美术重做队列（2026-06-15）

本文档由 `tools/creature_art_rework_queue.py audit` 生成。它把自动视觉证据、人工验收台账和天马/九头蛇设计正确性 gate 合并成 30 个生物的后续执行队列。

## 总览

- 队列通过：`False`
- 生物数量：30
- 已人工批准：0
- 未解决：30
- 设计阻断：2
- 非希腊 20 生物美术升级：20
- 优先级统计：`{"P0-DESIGN-BLOCKER": 2, "P0-GREEK-REVIEW": 7, "P0-USER-ACCEPTANCE": 1, "P1-CIV-ART-UPGRADE": 20}`
- 文明统计：`{"atlantean": 5, "chinese": 5, "egyptian": 5, "greek": 10, "nordic": 5}`
- 人工决策统计：`{"blocked_by_design_rejection": 2, "pending_shape_review": 27, "pending_user_acceptance": 1}`
- 天马/九头蛇设计正确性：`blocked_current_design_not_correct_and_v6_spec_not_approved`
- 天马/九头蛇 V6 可建模规格：`modeling_spec_ready_for_user_review` / `modeling_allowed=None`

## 优先级含义

- `P0-DESIGN-BLOCKER`：设计已拒绝，禁止建模，必须先签收设计。
- `P0-USER-ACCEPTANCE`：已修订但等待用户接受或要求返修。
- `P0-GREEK-REVIEW`：希腊 vertical slice 中待人工形体审查。
- `P1-CIV-ART-UPGRADE`：非希腊文明生物需要按文明美术标准升级到 1.0 成品级。

## 队列

| 优先级 | 文明 | 生物 | 决策 | 阶段 | 下一步 |
| --- | --- | --- | --- | --- | --- |
| `P0-GREEK-REVIEW` | greek | `centaur` | `pending_shape_review` | `greek_shape_review_then_targeted_rebuild` | `perform_human_visual_review` |
| `P0-GREEK-REVIEW` | greek | `chimera` | `pending_shape_review` | `greek_shape_review_then_targeted_rebuild` | `perform_human_visual_review` |
| `P0-GREEK-REVIEW` | greek | `colossus` | `pending_shape_review` | `greek_shape_review_then_targeted_rebuild` | `perform_human_visual_review` |
| `P0-USER-ACCEPTANCE` | greek | `cyclops` | `pending_user_acceptance` | `user_acceptance_or_return_to_design` | `obtain_user_acceptance_or_rework` |
| `P0-DESIGN-BLOCKER` | greek | `hydra` | `blocked_by_design_rejection` | `design_rewrite_or_user_approval_before_modeling` | `block_modeling_until_design_approval` |
| `P0-GREEK-REVIEW` | greek | `manticore` | `pending_shape_review` | `greek_shape_review_then_targeted_rebuild` | `perform_human_visual_review` |
| `P0-GREEK-REVIEW` | greek | `medusa` | `pending_shape_review` | `greek_shape_review_then_targeted_rebuild` | `perform_human_visual_review` |
| `P0-GREEK-REVIEW` | greek | `minotaur` | `pending_shape_review` | `greek_shape_review_then_targeted_rebuild` | `perform_human_visual_review` |
| `P0-GREEK-REVIEW` | greek | `nemean_lion` | `pending_shape_review` | `greek_shape_review_then_targeted_rebuild` | `perform_human_visual_review` |
| `P0-DESIGN-BLOCKER` | greek | `pegasus` | `blocked_by_design_rejection` | `design_rewrite_or_user_approval_before_modeling` | `block_modeling_until_design_approval` |
| `P1-CIV-ART-UPGRADE` | egyptian | `anubis_warrior` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | egyptian | `mummy_soldier` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | egyptian | `scarab_guardian` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | egyptian | `sphinx_sentinel` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | egyptian | `sun_falcon` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | nordic | `draugr_warrior` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | nordic | `dwarf_berserker` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | nordic | `fenrir_wolf` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | nordic | `frost_giant` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | nordic | `valkyrie` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | chinese | `azure_dragon` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | chinese | `jiangshi` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | chinese | `nine_tailed_fox` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | chinese | `qilin_guardian` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | chinese | `stone_lion_guardian` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | atlantean | `abyssal_ray` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | atlantean | `crystal_leviathan` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | atlantean | `orichalcum_golem` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | atlantean | `siren_priestess` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |
| `P1-CIV-ART-UPGRADE` | atlantean | `tidal_guardian` | `pending_shape_review` | `civilization_art_upgrade_then_model_uv_texture_runtime_sync` | `perform_human_visual_review` |

## 详细执行项

### P0-DESIGN-BLOCKER

#### greek / `hydra`

- 当前状态：`design_rejected_requires_v6_modeling_spec_approval` / `blocked_by_design_rejection`
- 阶段：`design_rewrite_or_user_approval_before_modeling`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/hydra_visual_review.png`
- 美术重点：
  - V6 默认显性九头；任何五头简化都必须先作为设计变更重新审批。
  - 九个 neck_socket 必须位于 shoulder_saddle，每个 neck_root 与 socket 几何重叠。
  - 每条颈至少 neck_root/neck_mid/neck_tip 三段，neck_tip 与 head_core 重叠。
  - 低伏长身和长尾优先，头部不靠放大表达细节，每个头有眼、颚、嘴线、牙或毒腺。
- 工作项：
  - 审查并签收九头蛇 V6 可建模规格：低伏长身、默认九头、九个 socket、每条颈真实连接身体。
  - 签收 V6 规格合同、规格图和设计正确性 gate 后才允许建模。
  - 从批准设计重新生成 Blockbench 模型、UV 模板、深绿鳞片/毒性高光纹理。
  - 同步 Java runtime model 和多头错相 idle/attack/hurt 动作，保证不断颈。
- 禁止项：
  - 设计未签收前不得继续生成 .bbmodel、纹理、动作或同步 Java model。
  - 不得从当前 V2 产物反推最终设计。
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - pegasus_hydra_design_correctness_gate ok=true
  - greek_design_first_gate ok=true
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### greek / `pegasus`

- 当前状态：`design_rejected_requires_v6_modeling_spec_approval` / `blocked_by_design_rejection`
- 阶段：`design_rewrite_or_user_approval_before_modeling`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/pegasus_visual_review.png`
- 美术重点：
  - V6 必须先成立原版成年马式横向白马身体，再成立双翼飞行坐骑。
  - 头部必须细长、位于胸腔前方，有真实吻部、眼、鼻孔、嘴线和耳，不能大头化。
  - 颈部从鬐甲/肩胛斜向前上方连接头部，不能竖直插在背上。
  - 翼根在肩后，骑乘空间不遮挡头颈或翼根，纯白材质必须有浅灰结构面。
- 工作项：
  - 审查并签收天马 V6 可建模规格：原版成年马式横向身体、细长前伸马头、斜向颈根、肩后翼根、骑乘空间。
  - 签收 V6 规格合同、规格图和设计正确性 gate 后才允许建模。
  - 从批准设计重新生成 Blockbench 模型、UV 模板、纯白但有灰面结构的纹理。
  - 同步 Java runtime model、飞行动作、自主飞行和骑乘飞行坐点。
- 禁止项：
  - 设计未签收前不得继续生成 .bbmodel、纹理、动作或同步 Java model。
  - 不得从当前 V2 产物反推最终设计。
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - pegasus_hydra_design_correctness_gate ok=true
  - greek_design_first_gate ok=true
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

### P0-USER-ACCEPTANCE

#### greek / `cyclops`

- 当前状态：`revision_implemented_pending_user_acceptance` / `pending_user_acceptance`
- 阶段：`user_acceptance_or_return_to_design`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/cyclops_visual_review.png`
- 美术重点：
  - 巨大人形比例，宽肩厚胸，不是普通僵尸放大
  - 头部正面只有一个居中的大眼，必须有瞳孔、眉骨、鼻梁、鼻孔、嘴和牙
  - 单手持巨大粗糙石棒，石棒长度接近身体高度的一半以上，石块头高过肩线
  - 手臂粗长、拳头有手指，腿短而重、脚掌有脚趾，整体像古希腊巨人单位
  - 拒绝条件：看起来只是无武器大人形；单眼被纹理噪声淹没或没有嘴部结构；石棒细得像棍子，或没有石质块面
- 工作项：
  - 让用户审查当前修订板，决定通过、返修或回设计重写。
  - 如果返修，先更新设计包和局部结构拆解，再改模型/UV/纹理/runtime。
  - 重点确认头身比例、标志五官/武器、动作可读性和材质不再占位。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - 用户接受当前修订或明确进入返修分支
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

### P0-GREEK-REVIEW

#### greek / `centaur`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`greek_shape_review_then_targeted_rebuild`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/centaur_visual_review.png`
- 美术重点：
  - 马身加前置人形战士上身的清楚拼接，不允许像普通马背上长方块
  - 人形头脸必须有双眼、鼻梁、嘴线、耳朵和深色头发
  - 弓、弓弦、箭和背部箭袋共同构成半人马弓手识别点
  - 四条马腿、四个独立马蹄和尾巴要在正面/侧面都清楚读出
  - 拒绝条件：看起来只是无头骑手或普通马；人脸没有独立双眼、鼻梁和嘴线；弓、箭、箭袋任一缺失导致战士身份不可读
- 工作项：
  - 人工审查三视图、texture-fit、runtime model 和视觉 review board。
  - 若形体通过，只做 UV/纹理/细节 polish；若不通过，先更新设计包再重建。
  - 检查神话标志部件、眼嘴、武器/翼/尾/蛇发、多头连接和比例不得靠大头补细节。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### greek / `chimera`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`greek_shape_review_then_targeted_rebuild`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/chimera_visual_review.png`
- 美术重点：
  - 狮头主脸、侧探羊头、尾端蛇头三重身份必须同时可读
  - 狮头必须有鬃毛、双眼、鼻口线和獠牙，不能只是普通方块兽头
  - 羊头必须有独立颈部、鼻吻、双眼、双角和胡须，不能贴成身体侧面小块
  - 蛇尾必须从尾段过渡到蛇头，并有上下颚、眼睛和舌头
  - 拒绝条件：只有一个兽头，看不出复合怪；狮头没有明确眼睛、鼻口或獠牙；羊角、羊胡须或羊眼缺失
- 工作项：
  - 人工审查三视图、texture-fit、runtime model 和视觉 review board。
  - 若形体通过，只做 UV/纹理/细节 polish；若不通过，先更新设计包再重建。
  - 检查神话标志部件、眼嘴、武器/翼/尾/蛇发、多头连接和比例不得靠大头补细节。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### greek / `colossus`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`greek_shape_review_then_targeted_rebuild`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/colossus_visual_review.png`
- 美术重点：
  - 青铜神像巨人，体量显著高于其他生物
  - 头冠、面甲、发光眼缝、嘴缝和颊板必须明显，不允许脸部空白铜块
  - 胸甲上板/下板、中心宝石、左右板缝和背板共同形成神像铠甲
  - 手臂和腿像雕像柱体，但必须有肘环、膝环、拳头、手指、大脚和脚趾板
  - 拒绝条件：像普通石巨人而非青铜神像；脸部没有眼缝、嘴缝、面甲或冠饰；躯干大片空白无胸甲板缝/背板
- 工作项：
  - 人工审查三视图、texture-fit、runtime model 和视觉 review board。
  - 若形体通过，只做 UV/纹理/细节 polish；若不通过，先更新设计包再重建。
  - 检查神话标志部件、眼嘴、武器/翼/尾/蛇发、多头连接和比例不得靠大头补细节。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### greek / `manticore`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`greek_shape_review_then_targeted_rebuild`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/manticore_visual_review.png`
- 美术重点：
  - 狮身主体、分段膜翼、上扬蝎尾毒刺必须三者同时明显
  - 脸部必须有双眼、鼻口线、左右獠牙、耳朵和左右 cheek_spike，不能只是普通兽头，也不能靠大头读凶险感
  - 蝎尾必须由 base/rise/curve/hook/sting/tip 形成高过背线的钩状轮廓
  - 翼膜、翼骨、翼爪必须分层，不能用单块平板代表翅膀
  - 拒绝条件：没有翼或翼像贴片；尾刺不明显或没有毒性颜色；头脸没有眼睛、鼻口线或左右獠牙
- 工作项：
  - 人工审查三视图、texture-fit、runtime model 和视觉 review board。
  - 若形体通过，只做 UV/纹理/细节 polish；若不通过，先更新设计包再重建。
  - 检查神话标志部件、眼嘴、武器/翼/尾/蛇发、多头连接和比例不得靠大头补细节。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### greek / `medusa`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`greek_shape_review_then_targeted_rebuild`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/medusa_visual_review.png`
- 美术重点：
  - 人形上身、女性头脸、蛇发冠、蛇形下身必须同时可读
  - 蛇发不是普通头发贴图，必须有六条独立蛇束和六个蛇头端点
  - 脸部必须有石化凝视双眼、瞳孔、鼻梁和嘴，眼部需要强对比；头块宽度必须服从躯干比例
  - 每个蛇头必须至少具备眼、牙、舌的固定 UV 细节
  - 拒绝条件：看起来只是绿色人形；蛇发与头部糊成一块，或缺少六个蛇头端点；脸部五官、瞳孔或石化凝视层不可读
- 工作项：
  - 人工审查三视图、texture-fit、runtime model 和视觉 review board。
  - 若形体通过，只做 UV/纹理/细节 polish；若不通过，先更新设计包再重建。
  - 检查神话标志部件、眼嘴、武器/翼/尾/蛇发、多头连接和比例不得靠大头补细节。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### greek / `minotaur`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`greek_shape_review_then_targeted_rebuild`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/minotaur_visual_review.png`
- 美术重点：
  - 牛头人身必须清楚，不能只是普通人形头部贴牛纹
  - 双角外展、牛吻前突、眉眼、鼻孔和嘴必须独立可读
  - 宽肩厚胸、胸肌、腰带、粗臂、手指、蹄足和牛尾表现强壮迷宫怪
  - 长柄双刃斧必须有左右斧刃、中心连接和尾端配重，不能只是一根棍子
  - 拒绝条件：看起来像普通僵尸或小型村民怪；双角、牛吻、鼻孔、嘴眼或耳朵不清楚；没有武器轮廓或武器细得不可读
- 工作项：
  - 人工审查三视图、texture-fit、runtime model 和视觉 review board。
  - 若形体通过，只做 UV/纹理/细节 polish；若不通过，先更新设计包再重建。
  - 检查神话标志部件、眼嘴、武器/翼/尾/蛇发、多头连接和比例不得靠大头补细节。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### greek / `nemean_lion`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`greek_shape_review_then_targeted_rebuild`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/greek/nemean_lion_visual_review.png`
- 美术重点：
  - 超大型狮子，不是狼/猫科普通小体型
  - 巨大放射状金色鬃毛必须超过头部边界并包住比例合理的头脸
  - 强壮肩背、胸前厚块和背部硬质金色脊线体现不可伤神狮
  - 短吻部、鼻口线、双眼、左右獠牙和每只爪尖都必须可读
  - 拒绝条件：看起来像普通狮子或狼；没有大鬃毛或尾端毛球；爪、眼、吻部或左右獠牙不可读
- 工作项：
  - 人工审查三视图、texture-fit、runtime model 和视觉 review board。
  - 若形体通过，只做 UV/纹理/细节 polish；若不通过，先更新设计包再重建。
  - 检查神话标志部件、眼嘴、武器/翼/尾/蛇发、多头连接和比例不得靠大头补细节。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

### P1-CIV-ART-UPGRADE

#### egyptian / `anubis_warrior`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/anubis_warrior_visual_review.png`
- 美术重点：
  - 胡狼头、长耳、黑金面部、金/青金石护甲、权杖或长矛必须可读。
  - 胡狼头、长吻、长耳、人形身体和埃及金饰必须同时可读，头块宽度不能压过肩胸比例
  - 右手镰剑/权杖必须作为独立武器轮廓
  - 眼睛不能被深色头部淹没，胸饰不能与身体糊成一块
  - 整体是神殿守卫，不是普通亡灵或村民
  - 拒绝条件：看起来像普通僵尸；没有长吻/长耳；武器不可读或没有埃及金饰
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### egyptian / `mummy_soldier`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/mummy_soldier_visual_review.png`
- 美术重点：
  - 绷带层、空洞眼、干枯脸、短矛/盾和破损布条必须分层。
  - 绷带层次
  - 黑眼洞
  - 短矛士兵身份
  - 拒绝条件：像普通僵尸；没有绷带；没有武器
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### egyptian / `scarab_guardian`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/scarab_guardian_visual_review.png`
- 美术重点：
  - 圣甲虫甲壳、分节腿、钳/角、腹甲纹和埃及金色符号必须突出。
  - 圣甲虫轮廓
  - 金色背甲/太阳符号
  - 六足和前角分离
  - 拒绝条件：像普通蜘蛛；没有背甲符号；腿和身体糊成一块
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### egyptian / `sphinx_sentinel`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sphinx_sentinel_visual_review.png`
- 美术重点：
  - 狮身、人面/王冠、前爪、尾和守卫姿态必须一眼可读。
  - 狮身
  - 人面
  - 法老头饰
  - 拒绝条件：像普通狮子；没有人面/头饰；身体太短
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### egyptian / `sun_falcon`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/egyptian/sun_falcon_visual_review.png`
- 美术重点：
  - 猎鹰头、翼羽、太阳圆盘或日光纹、爪和金白羽色必须明确。
  - 太阳盘
  - 隼头尖喙
  - 分段翼羽
  - 拒绝条件：像普通鸟；没有太阳盘；翼没有羽片
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### nordic / `draugr_warrior`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/draugr_warrior_visual_review.png`
- 美术重点：
  - 亡灵脸、塌陷眼、锈甲、破布、武器和寒光符文必须清楚。
  - 北欧亡灵
  - 圆盾
  - 斧头
  - 角盔/铁盔
  - 拒绝条件：像普通僵尸；没有盾或斧；没有北欧头盔
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### nordic / `dwarf_berserker`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/dwarf_berserker_visual_review.png`
- 美术重点：
  - 矮人体型、胡须、圆盔、双斧、厚靴和重甲，不靠大头表现细节。
  - 矮宽比例
  - 大胡须
  - 双斧
  - 圆盔
  - 拒绝条件：像缩小玩家；没有胡须；没有双斧
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### nordic / `fenrir_wolf`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/fenrir_wolf_visual_review.png`
- 美术重点：
  - 巨狼头、长吻、鬃毛、利齿、爪、符文伤痕和低伏攻击姿态。
  - 巨狼
  - 红眼
  - 锁链
  - 强壮下颚
  - 拒绝条件：像普通狼；没有锁链；眼睛不可读
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### nordic / `frost_giant`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/frost_giant_visual_review.png`
- 美术重点：
  - 冰霜巨人体量、冰晶肩背、粗臂、寒霜裂纹和北欧粗糙石/冰材质。
  - 巨人体量
  - 冰须和冰刺
  - 巨大霜石棒
  - 蓝白寒霜配色
  - 拒绝条件：像普通大僵尸；没有冰须或冰刺；武器不可读
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### nordic / `valkyrie`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/nordic/valkyrie_visual_review.png`
- 美术重点：
  - 女武神人形、翼盔、长矛/盾、披风和金属甲胄要区别于普通战士。
  - 翼盔
  - 双翼
  - 长矛
  - 银金甲
  - 拒绝条件：像普通村民；没有翼；没有长矛
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### chinese / `azure_dragon`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/azure_dragon_visual_review.png`
- 美术重点：
  - 青龙分段蛇形龙身、角、须、鬃、爪、鳞片和云雷纹必须分层。
  - 东方龙长身
  - 双角和龙须
  - 青绿色鳞片
  - 无翼飞行
  - 拒绝条件：像普通蛇；没有龙角/龙须；身体没有分段
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### chinese / `jiangshi`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/jiangshi_visual_review.png`
- 美术重点：
  - 僵尸官帽、符箓、青白脸、僵直袖臂、长袍和小跳姿态必须明确。
  - 官帽
  - 符箓
  - 伸直手臂
  - 青白脸
  - 拒绝条件：像普通僵尸；没有符；没有官帽
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### chinese / `nine_tailed_fox`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/nine_tailed_fox_visual_review.png`
- 美术重点：
  - 狐脸、耳、九条独立尾、尾尖层次和妖异眼神不能糊成毛团。
  - 九条尾巴
  - 狐脸尖耳
  - 红眼
  - 拒绝条件：像普通狐狸；尾巴不明显；没有尖耳
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### chinese / `qilin_guardian`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/qilin_guardian_visual_review.png`
- 美术重点：
  - 麒麟角、鬃毛、鳞身、蹄、尾焰/祥云和瑞兽姿态必须清楚。
  - 麒麟鹿角
  - 鳞甲
  - 蹄足
  - 祥瑞配色
  - 拒绝条件：像普通马；没有角；没有鳞片
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### chinese / `stone_lion_guardian`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/chinese/stone_lion_guardian_visual_review.png`
- 美术重点：
  - 石狮卷鬃、宽嘴、獠牙、爪、石雕底座感和守门姿态。
  - 石狮
  - 卷鬃
  - 绣球
  - 石质裂纹
  - 拒绝条件：像普通狮子；没有绣球；没有石纹
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### atlantean / `abyssal_ray`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/abyssal_ray_visual_review.png`
- 美术重点：
  - 深渊鳐宽扁轮廓、翼状鳍、尾刺、生物发光点和黑蓝深海材质。
  - 巨大翼展
  - 扁平鳐身
  - 长尾刺
  - 发光腹纹
  - 拒绝条件：像普通鱼；翼展不明显；没有尾刺
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### atlantean / `crystal_leviathan`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/crystal_leviathan_visual_review.png`
- 美术重点：
  - 海兽长身、晶体背棘、鳍、发光裂纹和深海水晶材质。
  - 长身海兽
  - 水晶背刺
  - 棱镜眼
  - 鳍翼和尾扇
  - 拒绝条件：像普通蛇；没有水晶背刺；没有清楚的头和眼睛
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### atlantean / `orichalcum_golem`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/orichalcum_golem_visual_review.png`
- 美术重点：
  - 山铜板甲、核心光源、粗重四肢、铆钉/板缝和远古机械感。
  - 山铜装甲
  - 发光胸核
  - 巨拳
  - 肩背晶柱
  - 拒绝条件：像普通铁傀儡；没有核心；没有亚特兰蒂斯水晶特征
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### atlantean / `siren_priestess`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/siren_priestess_visual_review.png`
- 美术重点：
  - 海妖祭司人形、鳍、法杖、头饰、鳞片裙和仪式符号。
  - 海妖人形上身
  - 鱼尾
  - 鳍冠
  - 水晶权杖
  - 拒绝条件：像普通女巫；没有鱼尾；没有权杖
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

#### atlantean / `tidal_guardian`

- 当前状态：`pending_human_shape_review` / `pending_shape_review`
- 阶段：`civilization_art_upgrade_then_model_uv_texture_runtime_sync`
- 视觉 board：`docs/preview_assets/creature_visual_acceptance/visual_review_boards/atlantean/tidal_guardian_visual_review.png`
- 美术重点：
  - 潮汐守卫护甲、水波冠、盾/戟、贝壳/珊瑚纹和坚固轮廓。
  - 厚龟甲
  - 蟹钳
  - 水晶甲片
  - 低矮守卫体量
  - 拒绝条件：像普通乌龟；没有双钳；水晶壳不明显
- 工作项：
  - 按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。
  - 必要时重建 Blockbench 模型，固定部件层级和 UV 区域。
  - 按 UV 重绘最终纹理，避免随机面板/占位感。
  - 同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。
- 禁止项：
  - 不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。
  - 不得把自动比例/UV gate 当成人工美术通过。
  - 不得用通用随机面板纹理作为最终资源。
- 退出 gate：
  - human_visual_acceptance_ledger decision=approved
  - checklist 无 pending/fail
  - creature_visual_acceptance_package ok=true
  - runtime model 与 Blockbench 设计一致
  - 构建 ./gradlew build --offline 通过

