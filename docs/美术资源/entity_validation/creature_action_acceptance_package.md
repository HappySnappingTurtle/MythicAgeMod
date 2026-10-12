# MythicAgeMod 1.0 生物动作验收包

本文档由 `tools/creature_action_acceptance_package.py audit` 生成。静态证据只证明动作和粒子挂点存在，不替代动作预览或实机观察。

## 总览

- 生物数量：30
- 静态动作证据通过：True
- 特征动作合同通过：30/30
- 特征动作合同必检数量：5
- 特征动作合同失败数量：0
- 人工动作验收通过：False
- 需要人工动作验收：30
- MC 连续帧动作证据通过：True
- MC 连续帧覆盖数量：30/30
- MC 连续帧全动作模式覆盖：False
- MC 连续帧全模式覆盖数量：0/30
- MC 连续帧必需模式覆盖：30/168
- MC 连续帧缺失动作模式项：138
- MC 动作捕获执行队列：138
- MC 动作捕获队列按模式：`{"attack_or_special": 30, "death": 30, "flight_or_glide": 7, "hurt": 30, "locomotion": 30, "riding_fly": 1, "signature_multi_part_motion": 6, "swim_or_drift": 4}`
- MC 动作捕获队列按优先级：`{"1": 30, "2": 30, "3": 7, "4": 1, "5": 4, "6": 6, "7": 30, "8": 30}`
- MC 动作捕获批次：16
- MC 动作捕获最低帧预算：1020
- MC 连续帧总帧数：180
- MC 连续帧 manifest：`docs/preview_assets/creature_action_acceptance/minecraft_sequences/motion_sequence_manifest.json`
- 静态动作 storyboard：30 / 30
- Storyboard 总览图：`docs/preview_assets/creature_action_acceptance/motion_storyboard_contact_sheet.png`
- 动作 review board：30 / 30
- Review board 总览图：`docs/preview_assets/creature_action_acceptance/motion_review_board_contact_sheet.png`
- 状态统计：`{"static_action_contract_passed_pending_motion_review": 30}`

## 文明统计

### greek

- 生物数量：10
- 静态证据通过数量：10
- 特征动作合同必检/通过：2 / 10
- MC 连续帧覆盖数量：10
- MC 连续帧全模式覆盖数量：0
- MC 连续帧缺失动作模式项：46
- MC 动作捕获执行队列：46
- MC 动作捕获队列按模式：`{"attack_or_special": 10, "death": 10, "flight_or_glide": 1, "hurt": 10, "locomotion": 10, "riding_fly": 1, "signature_multi_part_motion": 4}`
- MC 动作捕获最低帧预算：340
- 状态统计：`{"static_action_contract_passed_pending_motion_review": 10}`

### egyptian

- 生物数量：5
- 静态证据通过数量：5
- 特征动作合同必检/通过：0 / 5
- MC 连续帧覆盖数量：5
- MC 连续帧全模式覆盖数量：0
- MC 连续帧缺失动作模式项：21
- MC 动作捕获执行队列：21
- MC 动作捕获队列按模式：`{"attack_or_special": 5, "death": 5, "flight_or_glide": 1, "hurt": 5, "locomotion": 5}`
- MC 动作捕获最低帧预算：150
- 状态统计：`{"static_action_contract_passed_pending_motion_review": 5}`

### nordic

- 生物数量：5
- 静态证据通过数量：5
- 特征动作合同必检/通过：0 / 5
- MC 连续帧覆盖数量：5
- MC 连续帧全模式覆盖数量：0
- MC 连续帧缺失动作模式项：21
- MC 动作捕获执行队列：21
- MC 动作捕获队列按模式：`{"attack_or_special": 5, "death": 5, "flight_or_glide": 1, "hurt": 5, "locomotion": 5}`
- MC 动作捕获最低帧预算：150
- 状态统计：`{"static_action_contract_passed_pending_motion_review": 5}`

### chinese

- 生物数量：5
- 静态证据通过数量：5
- 特征动作合同必检/通过：0 / 5
- MC 连续帧覆盖数量：5
- MC 连续帧全模式覆盖数量：0
- MC 连续帧缺失动作模式项：23
- MC 动作捕获执行队列：23
- MC 动作捕获队列按模式：`{"attack_or_special": 5, "death": 5, "flight_or_glide": 1, "hurt": 5, "locomotion": 5, "signature_multi_part_motion": 2}`
- MC 动作捕获最低帧预算：170
- 状态统计：`{"static_action_contract_passed_pending_motion_review": 5}`

### atlantean

- 生物数量：5
- 静态证据通过数量：5
- 特征动作合同必检/通过：3 / 5
- MC 连续帧覆盖数量：5
- MC 连续帧全模式覆盖数量：0
- MC 连续帧缺失动作模式项：27
- MC 动作捕获执行队列：27
- MC 动作捕获队列按模式：`{"attack_or_special": 5, "death": 5, "flight_or_glide": 3, "hurt": 5, "locomotion": 5, "swim_or_drift": 4}`
- MC 动作捕获最低帧预算：210
- 状态统计：`{"static_action_contract_passed_pending_motion_review": 5}`

## MC 动作模式验收标准

| 动作模式 | 验收标准 |
| --- | --- |
| `locomotion` | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 |
| `attack_or_special` | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 |
| `flight_or_glide` | 单位必须处于离地飞行/滑翔状态。；翼、身体、尾或鳍有明确循环动作。；飞行中头脸仍然可读，身体高度和朝向稳定。 |
| `riding_fly` | 天马必须由玩家骑乘并处于可控飞行状态。；玩家坐点稳定，不遮挡白马头颈、翼根和眼嘴细节。；骑乘飞行动作与自主飞行动作都要可区分。 |
| `swim_or_drift` | 单位必须在水中或漂浮状态下捕获。；身体、鳍、尾或裙摆体现滑游/漂移动作，而不是地面走路。；漂浮高度、头脸和核心轮廓在水面/粒子中仍然可读。 |
| `signature_multi_part_motion` | 多头、多尾、蛇发、蝎尾、龙身或翼等标志部件必须错相运动。；九头蛇每条颈和头必须通过 body_overlap/head_overlap 保持连接。；动作不能退化成整个模型一起摆动。 |
| `hurt` | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 |
| `death` | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 |
| `idle` | 截图或连续帧必须无菜单/GUI遮挡。；待机呼吸、尾、翼、鬃毛、蛇发或环境部件有可读微动。；头脸、眼睛、嘴和核心神话特征保持清楚。 |

## MC 动作捕获批次计划

批次按动作风险和验收价值排序；同一批次内可以连续执行，减少反复重启和切换场景。

| 批次 | 优先级 | 动作模式 | 任务数 | 视角 | 最少帧 | 生物 |
| --- | ---: | --- | ---: | --- | ---: | --- |
| P1-locomotion-1 | 1 | `locomotion` | 12 | `side` | 96 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P1-locomotion-2 | 1 | `locomotion` | 12 | `side` | 96 | egyptian:scarab_guardian, egyptian:sphinx_sentinel, egyptian:sun_falcon, greek:centaur, greek:chimera, greek:colossus, greek:cyclops, greek:hydra, greek:manticore, greek:medusa,... |
| P1-locomotion-3 | 1 | `locomotion` | 6 | `side` | 48 | greek:pegasus, nordic:draugr_warrior, nordic:dwarf_berserker, nordic:fenrir_wolf, nordic:frost_giant, nordic:valkyrie |
| P2-attack_or_special-1 | 2 | `attack_or_special` | 10 | `three_quarter` | 80 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P2-attack_or_special-2 | 2 | `attack_or_special` | 10 | `three_quarter` | 80 | egyptian:anubis_warrior, egyptian:mummy_soldier, egyptian:scarab_guardian, egyptian:sphinx_sentinel, egyptian:sun_falcon, greek:centaur, greek:chimera, greek:colossus, greek:cyc... |
| P2-attack_or_special-3 | 2 | `attack_or_special` | 10 | `three_quarter` | 80 | greek:manticore, greek:medusa, greek:minotaur, greek:nemean_lion, greek:pegasus, nordic:draugr_warrior, nordic:dwarf_berserker, nordic:fenrir_wolf, nordic:frost_giant, nordic:va... |
| P3-flight_or_glide-1 | 3 | `flight_or_glide` | 7 | `three_quarter` | 70 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:siren_priestess, chinese:azure_dragon, egyptian:sun_falcon, greek:pegasus, nordic:valkyrie |
| P4-riding_fly-1 | 4 | `riding_fly` | 1 | `three_quarter` | 10 | greek:pegasus |
| P5-swim_or_drift-1 | 5 | `swim_or_drift` | 4 | `three_quarter` | 40 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:siren_priestess, atlantean:tidal_guardian |
| P6-signature_multi_part_motion-1 | 6 | `signature_multi_part_motion` | 6 | `three_quarter` | 60 | chinese:azure_dragon, chinese:nine_tailed_fox, greek:chimera, greek:hydra, greek:manticore, greek:medusa |
| P7-hurt-1 | 7 | `hurt` | 12 | `front` | 72 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P7-hurt-2 | 7 | `hurt` | 12 | `front` | 72 | egyptian:scarab_guardian, egyptian:sphinx_sentinel, egyptian:sun_falcon, greek:centaur, greek:chimera, greek:colossus, greek:cyclops, greek:hydra, greek:manticore, greek:medusa,... |
| P7-hurt-3 | 7 | `hurt` | 6 | `front` | 36 | greek:pegasus, nordic:draugr_warrior, nordic:dwarf_berserker, nordic:fenrir_wolf, nordic:frost_giant, nordic:valkyrie |
| P8-death-1 | 8 | `death` | 12 | `front` | 72 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P8-death-2 | 8 | `death` | 12 | `front` | 72 | egyptian:scarab_guardian, egyptian:sphinx_sentinel, egyptian:sun_falcon, greek:centaur, greek:chimera, greek:colossus, greek:cyclops, greek:hydra, greek:manticore, greek:medusa,... |
| P8-death-3 | 8 | `death` | 6 | `front` | 36 | greek:pegasus, nordic:draugr_warrior, nordic:dwarf_berserker, nordic:fenrir_wolf, nordic:frost_giant, nordic:valkyrie |

## MC 动作捕获执行队列

此队列只描述下一轮实机连续帧应补拍什么；它不代表动作已通过人工验收。

| 优先级 | 文明 | 生物 | 动作模式 | 视角 | 最少帧 | 间隔 tick | 验收要点 | 触发提示 |
| ---: | --- | --- | --- | --- | ---: | ---: | --- | --- |
| 1 | atlantean | `abyssal_ray` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | atlantean | `crystal_leviathan` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | atlantean | `orichalcum_golem` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | atlantean | `siren_priestess` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | atlantean | `tidal_guardian` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | chinese | `azure_dragon` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | chinese | `jiangshi` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | chinese | `nine_tailed_fox` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | chinese | `qilin_guardian` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | chinese | `stone_lion_guardian` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | egyptian | `anubis_warrior` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | egyptian | `mummy_soldier` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | egyptian | `scarab_guardian` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | egyptian | `sphinx_sentinel` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | egyptian | `sun_falcon` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `centaur` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `chimera` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `colossus` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `cyclops` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `hydra` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `manticore` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `medusa` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `minotaur` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `nemean_lion` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | greek | `pegasus` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | nordic | `draugr_warrior` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | nordic | `dwarf_berserker` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | nordic | `fenrir_wolf` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | nordic | `frost_giant` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 1 | nordic | `valkyrie` | `locomotion` | `side` | 8 | 5 | 侧视连续帧能读出完整步态/滑游周期。；脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。；头部比例稳定，运动中不遮挡眼嘴或武器。 | Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side. |
| 2 | atlantean | `abyssal_ray` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | atlantean | `crystal_leviathan` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | atlantean | `orichalcum_golem` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | atlantean | `siren_priestess` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | atlantean | `tidal_guardian` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | chinese | `azure_dragon` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | chinese | `jiangshi` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | chinese | `nine_tailed_fox` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | chinese | `qilin_guardian` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | chinese | `stone_lion_guardian` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | egyptian | `anubis_warrior` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | egyptian | `mummy_soldier` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | egyptian | `scarab_guardian` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | egyptian | `sphinx_sentinel` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | egyptian | `sun_falcon` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `centaur` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `chimera` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `colossus` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `cyclops` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `hydra` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `manticore` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `medusa` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `minotaur` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `nemean_lion` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | greek | `pegasus` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | nordic | `draugr_warrior` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | nordic | `dwarf_berserker` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | nordic | `fenrir_wolf` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | nordic | `frost_giant` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 2 | nordic | `valkyrie` | `attack_or_special` | `three_quarter` | 8 | 3 | 捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。；粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。；特殊动作必须符合设计包的单位身份。 | Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence. |
| 3 | atlantean | `abyssal_ray` | `flight_or_glide` | `three_quarter` | 10 | 4 | 单位必须处于离地飞行/滑翔状态。；翼、身体、尾或鳍有明确循环动作。；飞行中头脸仍然可读，身体高度和朝向稳定。 | Force flying/gliding/drifting state and capture wing, body, tail, or fin motion while airborne. |
| 3 | atlantean | `crystal_leviathan` | `flight_or_glide` | `three_quarter` | 10 | 4 | 单位必须处于离地飞行/滑翔状态。；翼、身体、尾或鳍有明确循环动作。；飞行中头脸仍然可读，身体高度和朝向稳定。 | Force flying/gliding/drifting state and capture wing, body, tail, or fin motion while airborne. |
| 3 | atlantean | `siren_priestess` | `flight_or_glide` | `three_quarter` | 10 | 4 | 单位必须处于离地飞行/滑翔状态。；翼、身体、尾或鳍有明确循环动作。；飞行中头脸仍然可读，身体高度和朝向稳定。 | Force flying/gliding/drifting state and capture wing, body, tail, or fin motion while airborne. |
| 3 | chinese | `azure_dragon` | `flight_or_glide` | `three_quarter` | 10 | 4 | 单位必须处于离地飞行/滑翔状态。；翼、身体、尾或鳍有明确循环动作。；飞行中头脸仍然可读，身体高度和朝向稳定。 | Force flying/gliding/drifting state and capture wing, body, tail, or fin motion while airborne. |
| 3 | egyptian | `sun_falcon` | `flight_or_glide` | `three_quarter` | 10 | 4 | 单位必须处于离地飞行/滑翔状态。；翼、身体、尾或鳍有明确循环动作。；飞行中头脸仍然可读，身体高度和朝向稳定。 | Force flying/gliding/drifting state and capture wing, body, tail, or fin motion while airborne. |
| 3 | greek | `pegasus` | `flight_or_glide` | `three_quarter` | 10 | 4 | 单位必须处于离地飞行/滑翔状态。；翼、身体、尾或鳍有明确循环动作。；飞行中头脸仍然可读，身体高度和朝向稳定。 | Force flying/gliding/drifting state and capture wing, body, tail, or fin motion while airborne. Confirm the horse head stays proportionate and readable while wings flap. |
| 3 | nordic | `valkyrie` | `flight_or_glide` | `three_quarter` | 10 | 4 | 单位必须处于离地飞行/滑翔状态。；翼、身体、尾或鳍有明确循环动作。；飞行中头脸仍然可读，身体高度和朝向稳定。 | Force flying/gliding/drifting state and capture wing, body, tail, or fin motion while airborne. |
| 4 | greek | `pegasus` | `riding_fly` | `three_quarter` | 10 | 4 | 天马必须由玩家骑乘并处于可控飞行状态。；玩家坐点稳定，不遮挡白马头颈、翼根和眼嘴细节。；骑乘飞行动作与自主飞行动作都要可区分。 | Mount the Pegasus with a player, clear overlays, and capture controlled ridden flight with stable rider space. Confirm the horse head stays proportionate and readable while wings flap. |
| 5 | atlantean | `abyssal_ray` | `swim_or_drift` | `three_quarter` | 10 | 4 | 单位必须在水中或漂浮状态下捕获。；身体、鳍、尾或裙摆体现滑游/漂移动作，而不是地面走路。；漂浮高度、头脸和核心轮廓在水面/粒子中仍然可读。 | Place the creature in water or drift state and capture body/fin/tail movement rather than ground walking. |
| 5 | atlantean | `crystal_leviathan` | `swim_or_drift` | `three_quarter` | 10 | 4 | 单位必须在水中或漂浮状态下捕获。；身体、鳍、尾或裙摆体现滑游/漂移动作，而不是地面走路。；漂浮高度、头脸和核心轮廓在水面/粒子中仍然可读。 | Place the creature in water or drift state and capture body/fin/tail movement rather than ground walking. |
| 5 | atlantean | `siren_priestess` | `swim_or_drift` | `three_quarter` | 10 | 4 | 单位必须在水中或漂浮状态下捕获。；身体、鳍、尾或裙摆体现滑游/漂移动作，而不是地面走路。；漂浮高度、头脸和核心轮廓在水面/粒子中仍然可读。 | Place the creature in water or drift state and capture body/fin/tail movement rather than ground walking. |
| 5 | atlantean | `tidal_guardian` | `swim_or_drift` | `three_quarter` | 10 | 4 | 单位必须在水中或漂浮状态下捕获。；身体、鳍、尾或裙摆体现滑游/漂移动作，而不是地面走路。；漂浮高度、头脸和核心轮廓在水面/粒子中仍然可读。 | Place the creature in water or drift state and capture body/fin/tail movement rather than ground walking. |
| 6 | chinese | `azure_dragon` | `signature_multi_part_motion` | `three_quarter` | 10 | 4 | 多头、多尾、蛇发、蝎尾、龙身或翼等标志部件必须错相运动。；九头蛇每条颈和头必须通过 body_overlap/head_overlap 保持连接。；动作不能退化成整个模型一起摆动。 | Trigger the signature moving parts named in the design contract: multi-heads, snake hair, tails, wings, stinger, or body segments. |
| 6 | chinese | `nine_tailed_fox` | `signature_multi_part_motion` | `three_quarter` | 10 | 4 | 多头、多尾、蛇发、蝎尾、龙身或翼等标志部件必须错相运动。；九头蛇每条颈和头必须通过 body_overlap/head_overlap 保持连接。；动作不能退化成整个模型一起摆动。 | Trigger the signature moving parts named in the design contract: multi-heads, snake hair, tails, wings, stinger, or body segments. |
| 6 | greek | `chimera` | `signature_multi_part_motion` | `three_quarter` | 10 | 4 | 多头、多尾、蛇发、蝎尾、龙身或翼等标志部件必须错相运动。；九头蛇每条颈和头必须通过 body_overlap/head_overlap 保持连接。；动作不能退化成整个模型一起摆动。 | Trigger the signature moving parts named in the design contract: multi-heads, snake hair, tails, wings, stinger, or body segments. |
| 6 | greek | `hydra` | `signature_multi_part_motion` | `three_quarter` | 10 | 4 | 多头、多尾、蛇发、蝎尾、龙身或翼等标志部件必须错相运动。；九头蛇每条颈和头必须通过 body_overlap/head_overlap 保持连接。；动作不能退化成整个模型一起摆动。 | Trigger the signature moving parts named in the design contract: multi-heads, snake hair, tails, wings, stinger, or body segments. Confirm each neck remains connected through body_overlap/head_overlap segments. |
| 6 | greek | `manticore` | `signature_multi_part_motion` | `three_quarter` | 10 | 4 | 多头、多尾、蛇发、蝎尾、龙身或翼等标志部件必须错相运动。；九头蛇每条颈和头必须通过 body_overlap/head_overlap 保持连接。；动作不能退化成整个模型一起摆动。 | Trigger the signature moving parts named in the design contract: multi-heads, snake hair, tails, wings, stinger, or body segments. |
| 6 | greek | `medusa` | `signature_multi_part_motion` | `three_quarter` | 10 | 4 | 多头、多尾、蛇发、蝎尾、龙身或翼等标志部件必须错相运动。；九头蛇每条颈和头必须通过 body_overlap/head_overlap 保持连接。；动作不能退化成整个模型一起摆动。 | Trigger the signature moving parts named in the design contract: multi-heads, snake hair, tails, wings, stinger, or body segments. |
| 7 | atlantean | `abyssal_ray` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | atlantean | `crystal_leviathan` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | atlantean | `orichalcum_golem` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | atlantean | `siren_priestess` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | atlantean | `tidal_guardian` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | chinese | `azure_dragon` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | chinese | `jiangshi` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | chinese | `nine_tailed_fox` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | chinese | `qilin_guardian` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | chinese | `stone_lion_guardian` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | egyptian | `anubis_warrior` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | egyptian | `mummy_soldier` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | egyptian | `scarab_guardian` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | egyptian | `sphinx_sentinel` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | egyptian | `sun_falcon` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `centaur` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `chimera` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `colossus` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `cyclops` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `hydra` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `manticore` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `medusa` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `minotaur` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `nemean_lion` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | greek | `pegasus` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | nordic | `draugr_warrior` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | nordic | `dwarf_berserker` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | nordic | `fenrir_wolf` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | nordic | `frost_giant` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 7 | nordic | `valkyrie` | `hurt` | `front` | 6 | 3 | 受击反应要能看出 recoil，且不破坏模型结构。；头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。 | Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity. |
| 8 | atlantean | `abyssal_ray` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | atlantean | `crystal_leviathan` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | atlantean | `orichalcum_golem` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | atlantean | `siren_priestess` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | atlantean | `tidal_guardian` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | chinese | `azure_dragon` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | chinese | `jiangshi` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | chinese | `nine_tailed_fox` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | chinese | `qilin_guardian` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | chinese | `stone_lion_guardian` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | egyptian | `anubis_warrior` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | egyptian | `mummy_soldier` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | egyptian | `scarab_guardian` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | egyptian | `sphinx_sentinel` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | egyptian | `sun_falcon` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `centaur` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `chimera` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `colossus` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `cyclops` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `hydra` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `manticore` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `medusa` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `minotaur` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `nemean_lion` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | greek | `pegasus` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | nordic | `draugr_warrior` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | nordic | `dwarf_berserker` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | nordic | `fenrir_wolf` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | nordic | `frost_giant` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |
| 8 | nordic | `valkyrie` | `death` | `front` | 6 | 3 | 死亡过程和最终姿态必须可读，不能只瞬间消失。；最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。 | Apply controlled lethal damage and capture deathTime collapse through the final pose. |

## 单体清单

| 文明 | 生物 | 状态 | 静态证据 | 特征动作 | MC idle smoke | MC 全模式 | 缺失模式 | Storyboard | Review board | 动作要求 | 设计约束 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| greek | `minotaur` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/minotaur_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/minotaur_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 待机头部低压；行走时粗臂、手指和腿摆动；冲锋时头角前压、身体前倾，战斧保持侧面和 45 度可读轮廓。 |
| greek | `cyclops` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/cyclops_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/cyclops_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 待机轻微呼吸；行走驱动双腿和长臂；攻击时石棒随主手大幅挥击，头部不变形。 |
| greek | `centaur` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/centaur_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/centaur_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 马腿四足错相步态，四蹄随对应腿摆动；人身保持前置稳定；头部跟随视线；远程攻击时右手拉弦、左手稳弓、弓弦和箭发生轻微拉伸/显隐；尾巴持续轻摆。 |
| greek | `hydra` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/hydra_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/hydra_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 各头轻微错相摆动；行走时身体低伏；特殊攻击时中央头前探。 |
| greek | `medusa` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/medusa_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/medusa_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 六条蛇发错相摆动，蛇头端点轻微点头；尾部滑行动画带盘绕摆动；石化凝视/攻击时头部稳定面向目标、石化眼光层显示、前蛇发压低。 |
| greek | `nemean_lion` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/nemean_lion_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/nemean_lion_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 行走四足步态；扑击时身体前倾且前爪下压；鬃毛作为整体随头部轻微跟随但外层保持放射轮廓；尾端毛球随尾部摆动。 |
| greek | `manticore` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/manticore_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/manticore_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 翼膜随左右翼小幅拍动但翼骨保持分段；蝎尾分节待机摆动，特殊攻击时 tail_hook 与 tail_sting 明显前压；头脸随视角转动，耳朵轻微抖动。 |
| greek | `chimera` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/chimera_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/chimera_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 狮头作为主攻击方向随视角转动；羊头较小幅度错相摆动；蛇尾随尾部甩动并独立张颚吐舌；翼只小幅开合保持轮廓稳定。 |
| greek | `colossus` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/colossus_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/colossus_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 步态缓慢重；行走时腿和手臂摆幅小但惯性重；攻击时右臂大幅砸下、左臂平衡，胸体微前倾；死亡时整体倾倒且头冠随身体下沉。 |
| greek | `pegasus` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, flight_or_glide, riding_fly` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/greek/pegasus_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/greek/pegasus_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, flight_or_glide, riding_fly` | 待机轻拍翅；飞行时双翼大幅拍动；骑乘时身体稳定，玩家坐点不遮挡翅根。 |
| egyptian | `anubis_warrior` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/egyptian/anubis_warrior_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/egyptian/anubis_warrior_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 待机守卫姿态；行走长臂摆动；攻击时右手镰剑横扫；审判技能时头部稳定注视目标。 |
| egyptian | `scarab_guardian` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/egyptian/scarab_guardian_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/egyptian/scarab_guardian_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 六足交替爬行；攻击时前角突刺；防御时背甲稳定不变形。 |
| egyptian | `mummy_soldier` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/egyptian/mummy_soldier_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/egyptian/mummy_soldier_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 待机摇晃；行走拖步；攻击时短矛前刺。 |
| egyptian | `sphinx_sentinel` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/egyptian/sphinx_sentinel_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/egyptian/sphinx_sentinel_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 低伏待机；扑击时前爪前压；谜语/守卫技能时头部稳定。 |
| egyptian | `sun_falcon` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, flight_or_glide` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/egyptian/sun_falcon_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/egyptian/sun_falcon_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, flight_or_glide` | 待机轻拍翼；飞行大幅拍翼；俯冲攻击时头和喙前压。 |
| nordic | `frost_giant` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/nordic/frost_giant_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/nordic/frost_giant_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 待机呼吸重；行走长臂摆动；攻击时霜石棒下砸；特殊震地时身体前压、头部稳定。 |
| nordic | `draugr_warrior` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/nordic/draugr_warrior_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/nordic/draugr_warrior_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 拖步行走；攻击时斧头横劈；防御时盾臂保持在身体前侧。 |
| nordic | `valkyrie` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, flight_or_glide` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/nordic/valkyrie_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/nordic/valkyrie_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, flight_or_glide` | 待机轻拍翼；飞行翼大幅摆动；攻击时长矛前刺。 |
| nordic | `fenrir_wolf` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/nordic/fenrir_wolf_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/nordic/fenrir_wolf_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 潜行式行走；扑咬时头前冲；尾部低摆。 |
| nordic | `dwarf_berserker` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/nordic/dwarf_berserker_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/nordic/dwarf_berserker_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 短步快跑；攻击时双斧交替；狂暴时身体前倾。 |
| chinese | `azure_dragon` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, flight_or_glide, signature_multi_part_motion` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/chinese/azure_dragon_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/chinese/azure_dragon_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, flight_or_glide, signature_multi_part_motion` | 待机悬浮盘旋；飞行时身体分段波动；攻击时龙头前探喷吐龙息。 |
| chinese | `qilin_guardian` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/chinese/qilin_guardian_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/chinese/qilin_guardian_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 优雅行走；攻击时前蹄踏地；守护技能时头部稳定。 |
| chinese | `jiangshi` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/chinese/jiangshi_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/chinese/jiangshi_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 僵硬跳步；攻击时双臂前推；受击时头部轻晃。 |
| chinese | `nine_tailed_fox` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/chinese/nine_tailed_fox_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/chinese/nine_tailed_fox_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 轻快步态；攻击时尾部扇动；魅惑技能时尾巴展开。 |
| chinese | `stone_lion_guardian` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/chinese/stone_lion_guardian_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/chinese/stone_lion_guardian_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 低伏守卫；攻击时前爪压下；防御时身体稳定。 |
| atlantean | `crystal_leviathan` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/atlantean/crystal_leviathan_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/atlantean/crystal_leviathan_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | 待机时悬浮游弋；移动时分段身体横向波动；攻击时头部前探并触发棱镜潮汐冲击。 |
| atlantean | `orichalcum_golem` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/atlantean/orichalcum_golem_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/atlantean/orichalcum_golem_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death` | 沉重待机；行走时手臂低摆；攻击时巨拳蓄力下砸。 |
| atlantean | `siren_priestess` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/atlantean/siren_priestess_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/atlantean/siren_priestess_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | 悬浮漂游；施法时权杖抬起；尾巴缓慢摆动。 |
| atlantean | `tidal_guardian` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, swim_or_drift` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/atlantean/tidal_guardian_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/atlantean/tidal_guardian_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, swim_or_drift` | 缓慢横移；攻击时双钳夹击；防御时甲壳稳定。 |
| atlantean | `abyssal_ray` | `static_action_contract_passed_pending_motion_review` | `True` | `True` | `True` | `False` | `locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | `docs/preview_assets/creature_action_acceptance/motion_storyboards/atlantean/abyssal_ray_motion_storyboard.png` | `docs/preview_assets/creature_action_acceptance/motion_review_boards/atlantean/abyssal_ray_motion_review.png` | `idle, locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | 飞/游时双翼缓慢拍动；攻击时尾刺甩动。 |

## 验收规则

- 先按设计包确认动作意图，再看模型 `setAngles` 和实体技能/粒子证据。
- Storyboard 是不开游戏的动作分解图，只证明动作意图、关键部件和源码证据可审查。
- 静态通过后仍必须做动作 clip 或实机观察，重点看 idle、移动、攻击/特殊、受击/死亡。
- 飞行/滑游/骑乘单位必须额外确认飞行或骑乘动作不遮挡头、翼、尾和玩家坐点。
- 多头、多尾、蛇发、翼、尾刺、武器单位必须确认关键部件参与动作。

