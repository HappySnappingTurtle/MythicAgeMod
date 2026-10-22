// 实体审阅截图（检查清单 §五.1）：对每个姿势（绑定姿势或某动画某时刻）按若干机位截图，写到 out 目录。
// 用法：window.MA.reviewEntity({out: '<目录>', poses: [{name: 'bind'}, {name: 'idle', anim: 'idle', time: 0}], views?})
// 机位用 Blockbench 坐标（设计卡的前方 = Blockbench -Z、角色左 = Blockbench -X）。拼图用 Python 另做。
(function () {
    const MA = window.MA;
    const VIEWS = [
        { name: 'front34', position: [-80, 50, -100], target: [0, 17, 0], fov: 30 },
        { name: 'side', position: [-140, 20, 0], target: [0, 17, 0], fov: 30 },
        { name: 'back34', position: [80, 50, 100], target: [0, 17, 0], fov: 30 },
        { name: 'front', position: [0, 22, -140], target: [0, 17, 0], fov: 30 },
        { name: 'top', position: [0, 160, -0.5], target: [0, 15, 0], fov: 30 },
    ];
    MA.REVIEW_VIEWS = VIEWS;

    const setPose = (p) => {
        if (!p.anim) {
            Modes.options.edit.select();
            return;
        }
        Modes.options.animate.select();
        const anim = Animation.all.find((a) => a.name === p.anim);
        if (!anim) {
            throw new Error('没有动画 ' + p.anim);
        }
        anim.select();
        Timeline.setTime(p.time || 0);
        Animator.preview();
    };

    MA.reviewEntity = (cfg) => {
        const views = cfg.views || VIEWS;
        const files = [];
        for (const p of cfg.poses) {
            setPose(p);
            for (const v of views) {
                const rel = cfg.out + '/' + p.name + '_' + v.name + '.png';
                MA.shot(rel, v);
                files.push(rel);
            }
        }
        Modes.options.edit.select();
        return files;
    };
    return 'review loaded';
})();
