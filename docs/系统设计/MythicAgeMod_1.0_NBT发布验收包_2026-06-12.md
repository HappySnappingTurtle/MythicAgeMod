# MythicAgeMod 1.0 NBT 发布验收包

本文档由 `tools/nbt_release_acceptance_package.py audit` 生成，用于区分静态 NBT 合约完成度和真实 MC 存档重启验收。

## 总览

- 静态 NBT 合约通过：True
- 运行中实体 NBT 探针通过：True
- 真实 MC 重启周期通过：True
- prepare/verify 进程不同：True
- NBT 发布验收通过：True
- NBT release review board：`docs/preview_assets/nbt_roundtrip_validation/nbt_release_review_board.png`
- 阻塞项：`[]`

## 证据

- schema report：`docs/preview_assets/entity_validation/nbt_schema_report.json`
- target report：`docs/preview_assets/nbt_target_validation/nbt_target_report.json`
- source round-trip：`docs/preview_assets/nbt_roundtrip_validation/nbt_roundtrip_report.json`
- restart contract：`docs/preview_assets/nbt_roundtrip_validation/nbt_restart_contract_report.json`
- runtime entity：`run/visual_probe/entity_nbt_report.json`，exists=True，ok=True
- restart semantic：`run/visual_probe/nbt_restart_report.json`，exists=False，ok=False
- restart prepare：`run/visual_probe/nbt_restart_prepare_report.json`，exists=True，ok=True
- restart verify：`run/visual_probe/nbt_restart_verify_report.json`，exists=True，ok=True
- prepare process id：`85721`
- verify process id：`85947`

## 下一步

- 保持 tools/nbt_target_validation_pipeline.py audit 与本验收包通过。
- 新增长期状态前先更新 NBT 目标表，再补 schema、round-trip/restart probe 和默认值策略。
- 发布前保留 run/visual_probe/nbt_restart_prepare_report.json 与 run/visual_probe/nbt_restart_verify_report.json 作为真实重启证据。
- 重跑 tools/mythic_age_1_0_gap_audit.py audit。
