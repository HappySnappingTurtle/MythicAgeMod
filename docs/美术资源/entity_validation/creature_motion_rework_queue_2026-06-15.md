# MythicAgeMod 1.0 生物动作/动画重做队列（2026-06-15）

本文档由 `tools/creature_motion_rework_queue.py audit` 生成。它把动作验收包和美术重做队列合并，防止在未签收或畸形模型上做最终动作捕获。

## 总览

- 队列通过：`False`
- 生物数量：30
- 待捕获任务：138
- 因美术/设计阻断的动作任务：15
- 阻断生物：`["greek:cyclops", "greek:hydra", "greek:pegasus"]`
- 可捕获但最终验收等待美术：123
- 最低帧预算：1020
- 任务状态统计：`{"blocked_by_art_or_design": 15, "capture_allowed_but_final_acceptance_waits_for_art": 123}`
- 动作模式统计：`{"attack_or_special": 30, "death": 30, "flight_or_glide": 7, "hurt": 30, "locomotion": 30, "riding_fly": 1, "signature_multi_part_motion": 6, "swim_or_drift": 4}`
- 文明统计：`{"atlantean": 27, "chinese": 23, "egyptian": 21, "greek": 46, "nordic": 21}`
- 美术优先级统计：`{"P0-DESIGN-BLOCKER": 11, "P0-GREEK-REVIEW": 31, "P0-USER-ACCEPTANCE": 4, "P1-CIV-ART-UPGRADE": 92}`

## 关键结论

- 现有动作静态合同通过不等于 1.0 动作完成；当前仍缺 138 个 MC 连续帧动作模式。
- `pegasus` 和 `hydra` 的动作任务必须等设计/模型重建后才可作为最终证据。
- 其他生物可以保留捕获队列，但最终动作验收必须等视觉/模型人工结论稳定。

## 按生物汇总

| 文明 | 生物 | 美术依赖 | 缺失模式 | 队列数 | 阻断数 | 最低帧 |
| --- | --- | --- | --- | --- | --- | --- |
| atlantean | `abyssal_ray` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | 6 | 0 | 48 |
| atlantean | `crystal_leviathan` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | 6 | 0 | 48 |
| atlantean | `orichalcum_golem` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| atlantean | `siren_priestess` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, flight_or_glide, swim_or_drift` | 6 | 0 | 48 |
| atlantean | `tidal_guardian` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, swim_or_drift` | 5 | 0 | 38 |
| chinese | `azure_dragon` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, flight_or_glide, signature_multi_part_motion` | 6 | 0 | 48 |
| chinese | `jiangshi` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| chinese | `nine_tailed_fox` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 5 | 0 | 38 |
| chinese | `qilin_guardian` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| chinese | `stone_lion_guardian` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| egyptian | `anubis_warrior` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| egyptian | `mummy_soldier` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| egyptian | `scarab_guardian` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| egyptian | `sphinx_sentinel` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| egyptian | `sun_falcon` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, flight_or_glide` | 5 | 0 | 38 |
| greek | `centaur` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| greek | `chimera` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 5 | 0 | 38 |
| greek | `colossus` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| greek | `cyclops` | `blocked_until_user_visual_acceptance_or_rework` | `locomotion, attack_or_special, hurt, death` | 4 | 4 | 28 |
| greek | `hydra` | `blocked_until_design_and_model_rebuild` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 5 | 5 | 38 |
| greek | `manticore` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 5 | 0 | 38 |
| greek | `medusa` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, signature_multi_part_motion` | 5 | 0 | 38 |
| greek | `minotaur` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| greek | `nemean_lion` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| greek | `pegasus` | `blocked_until_design_and_model_rebuild` | `locomotion, attack_or_special, hurt, death, flight_or_glide, riding_fly` | 6 | 6 | 48 |
| nordic | `draugr_warrior` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| nordic | `dwarf_berserker` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| nordic | `fenrir_wolf` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| nordic | `frost_giant` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death` | 4 | 0 | 28 |
| nordic | `valkyrie` | `wait_for_visual_review_before_final_motion_acceptance` | `locomotion, attack_or_special, hurt, death, flight_or_glide` | 5 | 0 | 38 |

## 队列任务

### blocked_by_art_or_design

| 优先级 | 文明 | 生物 | 模式 | 视角 | 帧数 | 依赖说明 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | greek | `cyclops` | `locomotion` | `side` | 8 | 当前视觉修订仍需用户接受；动作最终验收等视觉结论稳定。 |
| 1 | greek | `hydra` | `locomotion` | `side` | 8 | 九头蛇所有动作必须等 V6 九头 socket、颈根、头颈重叠和低伏长身规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 1 | greek | `pegasus` | `locomotion` | `side` | 8 | 天马所有动作必须等 V6 原版成年马式马体、非大头头颈、翼根和骑乘空间规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 2 | greek | `cyclops` | `attack_or_special` | `three_quarter` | 8 | 当前视觉修订仍需用户接受；动作最终验收等视觉结论稳定。 |
| 2 | greek | `hydra` | `attack_or_special` | `three_quarter` | 8 | 九头蛇所有动作必须等 V6 九头 socket、颈根、头颈重叠和低伏长身规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 2 | greek | `pegasus` | `attack_or_special` | `three_quarter` | 8 | 天马所有动作必须等 V6 原版成年马式马体、非大头头颈、翼根和骑乘空间规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 3 | greek | `pegasus` | `flight_or_glide` | `three_quarter` | 10 | 天马所有动作必须等 V6 原版成年马式马体、非大头头颈、翼根和骑乘空间规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 4 | greek | `pegasus` | `riding_fly` | `three_quarter` | 10 | 天马所有动作必须等 V6 原版成年马式马体、非大头头颈、翼根和骑乘空间规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 6 | greek | `hydra` | `signature_multi_part_motion` | `three_quarter` | 10 | 九头蛇所有动作必须等 V6 九头 socket、颈根、头颈重叠和低伏长身规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 7 | greek | `cyclops` | `hurt` | `front` | 6 | 当前视觉修订仍需用户接受；动作最终验收等视觉结论稳定。 |
| 7 | greek | `hydra` | `hurt` | `front` | 6 | 九头蛇所有动作必须等 V6 九头 socket、颈根、头颈重叠和低伏长身规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 7 | greek | `pegasus` | `hurt` | `front` | 6 | 天马所有动作必须等 V6 原版成年马式马体、非大头头颈、翼根和骑乘空间规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 8 | greek | `cyclops` | `death` | `front` | 6 | 当前视觉修订仍需用户接受；动作最终验收等视觉结论稳定。 |
| 8 | greek | `hydra` | `death` | `front` | 6 | 九头蛇所有动作必须等 V6 九头 socket、颈根、头颈重叠和低伏长身规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |
| 8 | greek | `pegasus` | `death` | `front` | 6 | 天马所有动作必须等 V6 原版成年马式马体、非大头头颈、翼根和骑乘空间规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。 |

### capture_allowed_but_final_acceptance_waits_for_art

| 优先级 | 文明 | 生物 | 模式 | 视角 | 帧数 | 依赖说明 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | atlantean | `abyssal_ray` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | atlantean | `crystal_leviathan` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | atlantean | `orichalcum_golem` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | atlantean | `siren_priestess` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | atlantean | `tidal_guardian` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | chinese | `azure_dragon` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | chinese | `jiangshi` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | chinese | `nine_tailed_fox` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | chinese | `qilin_guardian` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | chinese | `stone_lion_guardian` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | egyptian | `anubis_warrior` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | egyptian | `mummy_soldier` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | egyptian | `scarab_guardian` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | egyptian | `sphinx_sentinel` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | egyptian | `sun_falcon` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | greek | `centaur` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | greek | `chimera` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | greek | `colossus` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | greek | `manticore` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | greek | `medusa` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | greek | `minotaur` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | greek | `nemean_lion` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | nordic | `draugr_warrior` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | nordic | `dwarf_berserker` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | nordic | `fenrir_wolf` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | nordic | `frost_giant` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 1 | nordic | `valkyrie` | `locomotion` | `side` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | atlantean | `abyssal_ray` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | atlantean | `crystal_leviathan` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | atlantean | `orichalcum_golem` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | atlantean | `siren_priestess` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | atlantean | `tidal_guardian` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | chinese | `azure_dragon` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | chinese | `jiangshi` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | chinese | `nine_tailed_fox` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | chinese | `qilin_guardian` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | chinese | `stone_lion_guardian` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | egyptian | `anubis_warrior` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | egyptian | `mummy_soldier` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | egyptian | `scarab_guardian` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | egyptian | `sphinx_sentinel` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | egyptian | `sun_falcon` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | greek | `centaur` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | greek | `chimera` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | greek | `colossus` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | greek | `manticore` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | greek | `medusa` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | greek | `minotaur` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | greek | `nemean_lion` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | nordic | `draugr_warrior` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | nordic | `dwarf_berserker` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | nordic | `fenrir_wolf` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | nordic | `frost_giant` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 2 | nordic | `valkyrie` | `attack_or_special` | `three_quarter` | 8 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 3 | atlantean | `abyssal_ray` | `flight_or_glide` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 3 | atlantean | `crystal_leviathan` | `flight_or_glide` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 3 | atlantean | `siren_priestess` | `flight_or_glide` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 3 | chinese | `azure_dragon` | `flight_or_glide` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 3 | egyptian | `sun_falcon` | `flight_or_glide` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 3 | nordic | `valkyrie` | `flight_or_glide` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 5 | atlantean | `abyssal_ray` | `swim_or_drift` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 5 | atlantean | `crystal_leviathan` | `swim_or_drift` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 5 | atlantean | `siren_priestess` | `swim_or_drift` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 5 | atlantean | `tidal_guardian` | `swim_or_drift` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 6 | chinese | `azure_dragon` | `signature_multi_part_motion` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 6 | chinese | `nine_tailed_fox` | `signature_multi_part_motion` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 6 | greek | `chimera` | `signature_multi_part_motion` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 6 | greek | `manticore` | `signature_multi_part_motion` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 6 | greek | `medusa` | `signature_multi_part_motion` | `three_quarter` | 10 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | atlantean | `abyssal_ray` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | atlantean | `crystal_leviathan` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | atlantean | `orichalcum_golem` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | atlantean | `siren_priestess` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | atlantean | `tidal_guardian` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | chinese | `azure_dragon` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | chinese | `jiangshi` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | chinese | `nine_tailed_fox` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | chinese | `qilin_guardian` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | chinese | `stone_lion_guardian` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | egyptian | `anubis_warrior` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | egyptian | `mummy_soldier` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | egyptian | `scarab_guardian` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | egyptian | `sphinx_sentinel` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | egyptian | `sun_falcon` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | greek | `centaur` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | greek | `chimera` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | greek | `colossus` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | greek | `manticore` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | greek | `medusa` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | greek | `minotaur` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | greek | `nemean_lion` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | nordic | `draugr_warrior` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | nordic | `dwarf_berserker` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | nordic | `fenrir_wolf` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | nordic | `frost_giant` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 7 | nordic | `valkyrie` | `hurt` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | atlantean | `abyssal_ray` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | atlantean | `crystal_leviathan` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | atlantean | `orichalcum_golem` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | atlantean | `siren_priestess` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | atlantean | `tidal_guardian` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | chinese | `azure_dragon` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | chinese | `jiangshi` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | chinese | `nine_tailed_fox` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | chinese | `qilin_guardian` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | chinese | `stone_lion_guardian` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | egyptian | `anubis_warrior` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | egyptian | `mummy_soldier` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | egyptian | `scarab_guardian` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | egyptian | `sphinx_sentinel` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | egyptian | `sun_falcon` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | greek | `centaur` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | greek | `chimera` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | greek | `colossus` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | greek | `manticore` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | greek | `medusa` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | greek | `minotaur` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | greek | `nemean_lion` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | nordic | `draugr_warrior` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | nordic | `dwarf_berserker` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | nordic | `fenrir_wolf` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | nordic | `frost_giant` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |
| 8 | nordic | `valkyrie` | `death` | `front` | 6 | 可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。 |

## 退出规则

- 先完成美术/模型设计 gate，再把动作连续帧作为最终验收。
- 每个动作模式必须有无菜单 GUI 的连续帧、manifest 记录和人工 motion review note。
- 天马必须分别验证自主飞行和骑乘飞行，且玩家不遮挡头颈/翼根。
- 九头蛇必须在 idle/移动/攻击/受击/死亡中保持每条颈和头的实体连接。
