// 方块模型审阅截图：把仓库里的 Java 方块模型（含贴图）载入 Blockbench 并按几个角度截图。
// 分两次 risky_eval 调用（同一次调用里刚载入的模型还没建好网格，截出来是空的）：
//   1) window.MA_PREVIEW = {model: '<相对路径>', out: '<截图目录>'}; window.MA.run('tools/blockbench/preview_block.js')
//   2) window.MA.previewShots()
(function () {
    const MA = window.MA;
    const cfg = window.MA_PREVIEW;
    const file = MA.abs(cfg.model);
    const name = 'preview_' + MA.path.basename(file, '.json');
    for (const p of ModelProject.all.slice()) {
        if (p.name === name) {
            p.close(true);
        }
    }
    newProject(Formats.java_block);
    Project.name = name;
    Codecs.java_block.parse(JSON.parse(MA.fs.readFileSync(file, 'utf8')), file);
    Canvas.updateAll();
    const project = Project;
    MA.previewShots = () => {
        // 截图前切回预览工程（中间若切到了别的工程，画面会是别的模型）
        if (Project !== project) {
            project.select();
        }
        const views = cfg.views || [
            { name: 'front', position: [8, 14, -34], target: [8, 7, 8] },
            { name: 'corner', position: [-18, 24, -18], target: [8, 6, 8] },
            { name: 'top', position: [8, 40, -10], target: [8, 6, 8] },
        ];
        return views.map((v) => {
            MA.shot(cfg.out + '/' + v.name + '.png', { position: v.position, target: v.target, fov: 30 });
            return v.name;
        });
    };
    return Cube.all.length;
})();
