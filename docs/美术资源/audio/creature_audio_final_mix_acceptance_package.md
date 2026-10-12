# MythicAgeMod 1.0 生物音效最终混音验收包

本文档由 `tools/creature_audio_final_mix_acceptance_package.py audit` 生成。它只聚合文件元数据、音效方向和人工听感待办，不会把程序化首版音效标记为最终资源。

## 总览

- 生物数量：30
- 事件数量：149
- OGG 元数据/时长检查通过：True
- OGG 容器指标事件覆盖：149 / 149
- 最终混音验收通过：False
- 人工听感验收通过：False
- 响度/频谱工具可用：False
- 最终混音工具链阻塞：True
- 需要替换占位/程序首版音效的生物数：0
- 已生成最终混音候选、待听感验收的生物数：21
- 逐事件人工试听队列：149
- 人工试听批次：15
- 人工试听素材总时长：140.172 秒
- 人工试听队列按优先级：`{"1": 50, "2": 29, "3": 9, "4": 22, "5": 30, "6": 9}`
- 最终混音候选逐事件试听队列：104
- 需替换资产逐事件试听队列：0
- 程序化首版生物数：0
- 技术修复占位生物数：0
- 需要人工听感验收的生物数：30
- 最终混音 review board：30 / 30
- Review board 总览图：`docs/preview_assets/audio_validation/final_mix_review_board_contact_sheet.png`
- 本地试听验收页面：`docs/preview_assets/audio_validation/final_mix_audition_index.html`
- 可试听事件数：149 / 149
- 状态统计：`{"authored_or_existing_pending_listening": 9, "final_mix_candidate_pending_listening": 21}`

## 音频工具链状态

- 内置容器解析：`built_in_ogg_vorbis_page_parser`
- 容器指标可用：`True`
- 解码工具可用：`False`
- 响度指标可用：`False`
- 频谱指标可用：`False`
- 缺失工具：`ffprobe, ffmpeg, soxi, ogginfo`
- 缺口说明：Built-in OGG metadata parsing is available, but final loudness, peak, spectral balance, and decoded waveform checks require ffmpeg/ffprobe or equivalent audio tooling/MCP.

## 事件听审标准

| 事件 | 听审标准 |
| --- | --- |
| `special` | 必须能一听辨认该生物的神话身份和特殊能力。；与特殊动作峰值同步，不能抢掉玩家命中/受击反馈。；低频、尾音和粒子阶段不能在混战中糊成噪声。 |
| `attack` | 起手、命中或挥击峰值必须清楚。；音量不能盖过玩家受击、死亡或关键 UI 提示。；同类普通攻击之间要有身份差异，不能全部像同一个方块击打声。 |
| `charge` | 蓄力阶段要逐渐增强，峰值前不能误判为已经释放。；循环或尾音不能出现明显点击、断裂或过长拖尾。 |
| `death` | 死亡音效要明显区别 hurt，长度和体量感符合生物尺寸。；尾音不能拖慢战斗节奏，也不能被环境噪声完全吞掉。 |
| `fly` | 翼拍、气流或滑翔质感必须能支撑飞行动作。；循环感不能太机械，且不能遮住骑乘/移动反馈。 |
| `hurt` | 必须短、清楚、可重复播放，不能像死亡或特殊技能。；受击音量要能被听见，但不能刺耳或抢过攻击音。 |
| `ambient` | 体现文明和生物身份，但不能太频繁或太响。；在待机和群体场景中不能造成持续噪声疲劳。 |

## 人工试听批次计划

批次按事件重要性排序；special/attack/charge 优先，因为它们最容易影响战斗读秒、动作峰值和技能识别。

| 批次 | 优先级 | 事件 | 任务数 | 候选数 | 替换项 | 总时长秒 | 生物 |
| --- | ---: | --- | ---: | ---: | ---: | ---: | --- |
| P1-attack-1 | 1 | `attack` | 18 | 18 | 0 | 11.178 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P1-attack-2 | 1 | `attack` | 2 | 2 | 0 | 1.242 | nordic:frost_giant, nordic:valkyrie |
| P1-charge-1 | 1 | `charge` | 1 | 1 | 0 | 0.961 | greek:minotaur |
| P1-special-1 | 1 | `special` | 18 | 15 | 0 | 18.321 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P1-special-2 | 1 | `special` | 11 | 5 | 0 | 9.833 | greek:cyclops, greek:hydra, greek:manticore, greek:medusa, greek:nemean_lion, greek:pegasus, nordic:draugr_warrior, nordic:dwarf_berserker, nordic:fenrir_wolf, nordic:frost_gian... |
| P2-attack-1 | 2 | `attack` | 8 | 0 | 0 | 2.903 | greek:centaur, greek:chimera, greek:colossus, greek:cyclops, greek:hydra, greek:manticore, greek:medusa, greek:nemean_lion |
| P2-death-1 | 2 | `death` | 20 | 20 | 0 | 35.62 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P2-death-2 | 2 | `death` | 1 | 1 | 0 | 1.781 | nordic:valkyrie |
| P3-death-1 | 3 | `death` | 9 | 0 | 0 | 8.919 | greek:centaur, greek:chimera, greek:colossus, greek:cyclops, greek:hydra, greek:manticore, greek:medusa, greek:nemean_lion, greek:pegasus |
| P4-fly-1 | 4 | `fly` | 1 | 0 | 0 | 0.46 | greek:pegasus |
| P4-hurt-1 | 4 | `hurt` | 18 | 18 | 0 | 8.64 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P4-hurt-2 | 4 | `hurt` | 3 | 3 | 0 | 1.44 | nordic:fenrir_wolf, nordic:frost_giant, nordic:valkyrie |
| P5-ambient-1 | 5 | `ambient` | 21 | 21 | 0 | 28.371 | atlantean:abyssal_ray, atlantean:crystal_leviathan, atlantean:orichalcum_golem, atlantean:siren_priestess, atlantean:tidal_guardian, chinese:azure_dragon, chinese:jiangshi, chin... |
| P5-hurt-1 | 5 | `hurt` | 9 | 0 | 0 | 3.388 | greek:centaur, greek:chimera, greek:colossus, greek:cyclops, greek:hydra, greek:manticore, greek:medusa, greek:nemean_lion, greek:pegasus |
| P6-ambient-1 | 6 | `ambient` | 9 | 0 | 0 | 7.115 | greek:centaur, greek:chimera, greek:colossus, greek:cyclops, greek:hydra, greek:manticore, greek:medusa, greek:nemean_lion, greek:pegasus |

## 逐事件试听队列摘要

- 全量试听队列按事件：`{"ambient": 30, "attack": 28, "charge": 1, "death": 30, "fly": 1, "hurt": 30, "special": 29}`
- 全量试听队列按文明：`{"atlantean": 25, "chinese": 25, "egyptian": 25, "greek": 49, "nordic": 25}`
- 最终混音候选队列按事件：`{"ambient": 21, "attack": 20, "charge": 1, "death": 21, "hurt": 21, "special": 20}`

| 优先级 | 文明 | 生物 | 事件 | 资产状态 | 时长 | 路径 | 关注点 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | atlantean | `abyssal_ray` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/abyssal_ray/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `abyssal_ray` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/abyssal_ray/special.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `crystal_leviathan` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/crystal_leviathan/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `crystal_leviathan` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/crystal_leviathan/special.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `orichalcum_golem` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/orichalcum_golem/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `orichalcum_golem` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/orichalcum_golem/special.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `siren_priestess` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/siren_priestess/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `siren_priestess` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/siren_priestess/special.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `tidal_guardian` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/tidal_guardian/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | atlantean | `tidal_guardian` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/tidal_guardian/special.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `azure_dragon` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/azure_dragon/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `azure_dragon` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/azure_dragon/special.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `jiangshi` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/jiangshi/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `jiangshi` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/jiangshi/special.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `nine_tailed_fox` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/nine_tailed_fox/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `nine_tailed_fox` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/nine_tailed_fox/special.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `qilin_guardian` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/qilin_guardian/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `qilin_guardian` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/qilin_guardian/special.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `stone_lion_guardian` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/stone_lion_guardian/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | chinese | `stone_lion_guardian` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/stone_lion_guardian/special.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `anubis_warrior` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/anubis_warrior/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `anubis_warrior` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/anubis_warrior/special.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `mummy_soldier` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/mummy_soldier/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `mummy_soldier` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/mummy_soldier/special.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `scarab_guardian` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/scarab_guardian/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `scarab_guardian` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/scarab_guardian/special.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `sphinx_sentinel` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/sphinx_sentinel/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `sphinx_sentinel` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/sphinx_sentinel/special.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `sun_falcon` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/sun_falcon/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | egyptian | `sun_falcon` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/sun_falcon/special.ogg` | `candidate_loudness_and_identity` |
| 1 | greek | `centaur` | `special` | `authored_or_existing` | 0.361s | `src/main/resources/assets/mythic_age/sounds/entity/centaur/special.ogg` | `authored_asset_regression_listening` |
| 1 | greek | `chimera` | `special` | `authored_or_existing` | 0.580s | `src/main/resources/assets/mythic_age/sounds/entity/chimera/special.ogg` | `authored_asset_regression_listening` |
| 1 | greek | `colossus` | `special` | `authored_or_existing` | 0.580s | `src/main/resources/assets/mythic_age/sounds/entity/colossus/special.ogg` | `authored_asset_regression_listening` |
| 1 | greek | `cyclops` | `special` | `authored_or_existing` | 0.701s | `src/main/resources/assets/mythic_age/sounds/entity/cyclops/special.ogg` | `authored_asset_regression_listening` |
| 1 | greek | `hydra` | `special` | `authored_or_existing` | 0.901s | `src/main/resources/assets/mythic_age/sounds/entity/hydra/special.ogg` | `authored_asset_regression_listening` |
| 1 | greek | `manticore` | `special` | `authored_or_existing` | 0.580s | `src/main/resources/assets/mythic_age/sounds/entity/manticore/special.ogg` | `authored_asset_regression_listening` |
| 1 | greek | `medusa` | `special` | `authored_or_existing` | 0.850s | `src/main/resources/assets/mythic_age/sounds/entity/medusa/special.ogg` | `authored_asset_regression_listening` |
| 1 | greek | `minotaur` | `charge` | `final_mix_candidate_pending_listening` | 0.961s | `src/main/resources/assets/mythic_age/sounds/entity/minotaur/charge.ogg` | `candidate_loudness_and_identity` |
| 1 | greek | `nemean_lion` | `special` | `authored_or_existing` | 0.580s | `src/main/resources/assets/mythic_age/sounds/entity/nemean_lion/special.ogg` | `authored_asset_regression_listening` |
| 1 | greek | `pegasus` | `special` | `authored_or_existing` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/pegasus/special.ogg` | `authored_asset_regression_listening` |
| 1 | nordic | `draugr_warrior` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/draugr_warrior/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `draugr_warrior` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/draugr_warrior/special.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `dwarf_berserker` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/dwarf_berserker/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `dwarf_berserker` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/dwarf_berserker/special.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `fenrir_wolf` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/fenrir_wolf/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `fenrir_wolf` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/fenrir_wolf/special.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `frost_giant` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/frost_giant/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `frost_giant` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/frost_giant/special.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `valkyrie` | `attack` | `final_mix_candidate_pending_listening` | 0.621s | `src/main/resources/assets/mythic_age/sounds/entity/valkyrie/attack.ogg` | `candidate_loudness_and_identity` |
| 1 | nordic | `valkyrie` | `special` | `final_mix_candidate_pending_listening` | 1.120s | `src/main/resources/assets/mythic_age/sounds/entity/valkyrie/special.ogg` | `candidate_loudness_and_identity` |
| 2 | atlantean | `abyssal_ray` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/abyssal_ray/death.ogg` | `candidate_loudness_and_identity` |
| 2 | atlantean | `crystal_leviathan` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/crystal_leviathan/death.ogg` | `candidate_loudness_and_identity` |
| 2 | atlantean | `orichalcum_golem` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/orichalcum_golem/death.ogg` | `candidate_loudness_and_identity` |
| 2 | atlantean | `siren_priestess` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/siren_priestess/death.ogg` | `candidate_loudness_and_identity` |
| 2 | atlantean | `tidal_guardian` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/tidal_guardian/death.ogg` | `candidate_loudness_and_identity` |
| 2 | chinese | `azure_dragon` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/azure_dragon/death.ogg` | `candidate_loudness_and_identity` |
| 2 | chinese | `jiangshi` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/jiangshi/death.ogg` | `candidate_loudness_and_identity` |
| 2 | chinese | `nine_tailed_fox` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/nine_tailed_fox/death.ogg` | `candidate_loudness_and_identity` |
| 2 | chinese | `qilin_guardian` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/qilin_guardian/death.ogg` | `candidate_loudness_and_identity` |
| 2 | chinese | `stone_lion_guardian` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/stone_lion_guardian/death.ogg` | `candidate_loudness_and_identity` |
| 2 | egyptian | `anubis_warrior` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/anubis_warrior/death.ogg` | `candidate_loudness_and_identity` |
| 2 | egyptian | `mummy_soldier` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/mummy_soldier/death.ogg` | `candidate_loudness_and_identity` |
| 2 | egyptian | `scarab_guardian` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/scarab_guardian/death.ogg` | `candidate_loudness_and_identity` |
| 2 | egyptian | `sphinx_sentinel` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/sphinx_sentinel/death.ogg` | `candidate_loudness_and_identity` |
| 2 | egyptian | `sun_falcon` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/sun_falcon/death.ogg` | `candidate_loudness_and_identity` |
| 2 | greek | `centaur` | `attack` | `authored_or_existing` | 0.241s | `src/main/resources/assets/mythic_age/sounds/entity/centaur/attack.ogg` | `authored_asset_regression_listening` |
| 2 | greek | `chimera` | `attack` | `authored_or_existing` | 0.380s | `src/main/resources/assets/mythic_age/sounds/entity/chimera/attack.ogg` | `authored_asset_regression_listening` |
| 2 | greek | `colossus` | `attack` | `authored_or_existing` | 0.380s | `src/main/resources/assets/mythic_age/sounds/entity/colossus/attack.ogg` | `authored_asset_regression_listening` |
| 2 | greek | `cyclops` | `attack` | `authored_or_existing` | 0.421s | `src/main/resources/assets/mythic_age/sounds/entity/cyclops/attack.ogg` | `authored_asset_regression_listening` |
| 2 | greek | `hydra` | `attack` | `authored_or_existing` | 0.421s | `src/main/resources/assets/mythic_age/sounds/entity/hydra/attack.ogg` | `authored_asset_regression_listening` |
| 2 | greek | `manticore` | `attack` | `authored_or_existing` | 0.380s | `src/main/resources/assets/mythic_age/sounds/entity/manticore/attack.ogg` | `authored_asset_regression_listening` |
| 2 | greek | `medusa` | `attack` | `authored_or_existing` | 0.300s | `src/main/resources/assets/mythic_age/sounds/entity/medusa/attack.ogg` | `authored_asset_regression_listening` |
| 2 | greek | `minotaur` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/minotaur/death.ogg` | `candidate_loudness_and_identity` |
| 2 | greek | `nemean_lion` | `attack` | `authored_or_existing` | 0.380s | `src/main/resources/assets/mythic_age/sounds/entity/nemean_lion/attack.ogg` | `authored_asset_regression_listening` |
| 2 | nordic | `draugr_warrior` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/draugr_warrior/death.ogg` | `candidate_loudness_and_identity` |
| 2 | nordic | `dwarf_berserker` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/dwarf_berserker/death.ogg` | `candidate_loudness_and_identity` |
| 2 | nordic | `fenrir_wolf` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/fenrir_wolf/death.ogg` | `candidate_loudness_and_identity` |
| 2 | nordic | `frost_giant` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/frost_giant/death.ogg` | `candidate_loudness_and_identity` |
| 2 | nordic | `valkyrie` | `death` | `final_mix_candidate_pending_listening` | 1.781s | `src/main/resources/assets/mythic_age/sounds/entity/valkyrie/death.ogg` | `candidate_loudness_and_identity` |
| 3 | greek | `centaur` | `death` | `authored_or_existing` | 0.901s | `src/main/resources/assets/mythic_age/sounds/entity/centaur/death.ogg` | `authored_asset_regression_listening` |

## 单体清单

| 文明 | 生物 | 资产状态 | OGG 元数据 | 需替换 | Review board | Audition anchor | 事件数 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| greek | `minotaur` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/minotaur_final_mix_review.png` | `#greek-minotaur` | 4 |
| greek | `cyclops` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/cyclops_final_mix_review.png` | `#greek-cyclops` | 5 |
| greek | `centaur` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/centaur_final_mix_review.png` | `#greek-centaur` | 5 |
| greek | `hydra` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/hydra_final_mix_review.png` | `#greek-hydra` | 5 |
| greek | `medusa` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/medusa_final_mix_review.png` | `#greek-medusa` | 5 |
| greek | `nemean_lion` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/nemean_lion_final_mix_review.png` | `#greek-nemean_lion` | 5 |
| greek | `manticore` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/manticore_final_mix_review.png` | `#greek-manticore` | 5 |
| greek | `chimera` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/chimera_final_mix_review.png` | `#greek-chimera` | 5 |
| greek | `colossus` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/colossus_final_mix_review.png` | `#greek-colossus` | 5 |
| greek | `pegasus` | `authored_or_existing` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/greek/pegasus_final_mix_review.png` | `#greek-pegasus` | 5 |
| egyptian | `anubis_warrior` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/egyptian/anubis_warrior_final_mix_review.png` | `#egyptian-anubis_warrior` | 5 |
| egyptian | `scarab_guardian` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/egyptian/scarab_guardian_final_mix_review.png` | `#egyptian-scarab_guardian` | 5 |
| egyptian | `mummy_soldier` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/egyptian/mummy_soldier_final_mix_review.png` | `#egyptian-mummy_soldier` | 5 |
| egyptian | `sphinx_sentinel` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/egyptian/sphinx_sentinel_final_mix_review.png` | `#egyptian-sphinx_sentinel` | 5 |
| egyptian | `sun_falcon` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/egyptian/sun_falcon_final_mix_review.png` | `#egyptian-sun_falcon` | 5 |
| nordic | `frost_giant` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/nordic/frost_giant_final_mix_review.png` | `#nordic-frost_giant` | 5 |
| nordic | `draugr_warrior` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/nordic/draugr_warrior_final_mix_review.png` | `#nordic-draugr_warrior` | 5 |
| nordic | `valkyrie` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/nordic/valkyrie_final_mix_review.png` | `#nordic-valkyrie` | 5 |
| nordic | `fenrir_wolf` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/nordic/fenrir_wolf_final_mix_review.png` | `#nordic-fenrir_wolf` | 5 |
| nordic | `dwarf_berserker` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/nordic/dwarf_berserker_final_mix_review.png` | `#nordic-dwarf_berserker` | 5 |
| chinese | `azure_dragon` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/chinese/azure_dragon_final_mix_review.png` | `#chinese-azure_dragon` | 5 |
| chinese | `qilin_guardian` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/chinese/qilin_guardian_final_mix_review.png` | `#chinese-qilin_guardian` | 5 |
| chinese | `jiangshi` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/chinese/jiangshi_final_mix_review.png` | `#chinese-jiangshi` | 5 |
| chinese | `nine_tailed_fox` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/chinese/nine_tailed_fox_final_mix_review.png` | `#chinese-nine_tailed_fox` | 5 |
| chinese | `stone_lion_guardian` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/chinese/stone_lion_guardian_final_mix_review.png` | `#chinese-stone_lion_guardian` | 5 |
| atlantean | `crystal_leviathan` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/atlantean/crystal_leviathan_final_mix_review.png` | `#atlantean-crystal_leviathan` | 5 |
| atlantean | `orichalcum_golem` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/atlantean/orichalcum_golem_final_mix_review.png` | `#atlantean-orichalcum_golem` | 5 |
| atlantean | `siren_priestess` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/atlantean/siren_priestess_final_mix_review.png` | `#atlantean-siren_priestess` | 5 |
| atlantean | `tidal_guardian` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/atlantean/tidal_guardian_final_mix_review.png` | `#atlantean-tidal_guardian` | 5 |
| atlantean | `abyssal_ray` | `final_mix_candidate_pending_listening` | `True` | `False` | `docs/preview_assets/audio_validation/final_mix_review_boards/atlantean/abyssal_ray_final_mix_review.png` | `#atlantean-abyssal_ray` | 5 |

## 验收规则

- `replacement_required=true` 的生物不能进入 1.0 最终音频，只能作为技术占位或听感参考。
- `final_mix_candidate_pending_listening` 表示已经替换掉占位/首版资源，但仍需要人工听感、响度和战斗可读性验收。
- 当前环境没有本地响度/频谱解码工具，响度、峰值、频谱平衡和听感必须通过外部音频工具、音频 MCP 或人工试听补证。
- 最终混音通过前，每个生物至少检查 ambient、hurt、death、attack/special 的身份辨识、战斗可读性和与动作峰值的同步。

