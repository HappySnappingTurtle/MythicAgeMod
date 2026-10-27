# 《神话手札》正文

Patchouli 书 `mythic_age:handbook` 的正文直接写在
`neoforge/src/main/resources/assets/mythic_age/patchouli_books/handbook/<语言>/`：

- `categories/<分类>.json`：分类（名字、说明、图标、排序）；
- `entries/<分类>/<条目>.json`：条目与页面。

三种语言（zh_cn、en_us、ja_jp）的文件名、分类、页数与锁页进度必须一致，`tools/check_content.py` 会检查。
图鉴条目按进度锁：整条锁「目击」`mythic_age:bestiary/<条目>/sighted`，后两页锁「交战」「击败」。
神话内容写出处，改编处写明「本模组的改编」（见 `docs/重构/开发/T24/T24 - 设计文档.md` §6.4）。
