// T20 阶段 ③ 士兵审阅：按兵种与兵装档显隐（同 ResidentGeoModel 的规则：男装、kit_<兵种>_<档|any>_*），
// 每个动作取若干时刻 × 机位截图。用法：
//   MA.soldierShow('hoplite', 'full', true)   显示盾矛卫兵完整兵装（第三个参数：显示武器）
//   MA.reviewSoldier({out, kit: ['hoplite', 'full'], items: [{anim, times: [0, 0.5]}], views: ['front34', 'side']})
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/review_entity.js');
    const tagOf = (c) => {
        for (let g = c.parent; g && g.name; g = g.parent) {
            if (/^(carry_|tool_|outfit_|kit_)/.test(g.name)) return g.name;
        }
        return '';
    };
    MA.soldierShow = (prof, tier, weapons) => {
        for (const c of Cube.all) {
            const t = tagOf(c);
            let vis = true;
            if (t.startsWith('outfit_male')) vis = true;
            else if (t.startsWith('outfit_female')) vis = false;
            else if (t.startsWith('carry_') || t.startsWith('tool_')) vis = false;
            else if (t.startsWith('kit_')) {
                const [, p, tr] = t.split('_');
                const weapon = /spear|shield/.test(t);
                vis = !!prof && p === prof && (tr === 'any' || tr === tier) && (!weapon || weapons);
            }
            c.visibility = vis;
            if (c.mesh) c.mesh.visible = vis;
        }
        return Cube.all.filter((c) => c.visibility).length;
    };
    MA.reviewSoldier = (cfg) => {
        // 截图前取消选中，免得 Blockbench 的枢轴图标画进图里
        if (typeof unselectAllElements === 'function') unselectAllElements();
        if (Group.first_selected) Group.first_selected.unselect();
        MA.soldierShow(cfg.kit[0], cfg.kit[1], cfg.weapons !== false);
        const views = MA.REVIEW_VIEWS.filter((v) => (cfg.views || ['front34', 'side']).includes(v.name));
        const files = [];
        for (const item of cfg.items) {
            const anim = item.anim ? Animation.all.find((a) => a.name === item.anim) : null;
            for (const f of item.times || [0]) {
                if (anim) {
                    Modes.options.animate.select();
                    anim.select();
                    Timeline.setTime(Math.round(anim.length * f * 20) / 20);
                    Animator.preview();
                } else {
                    Modes.options.edit.select();
                }
                for (const v of views) {
                    const rel = cfg.out + '/' + (item.label || item.anim || 'bind') + '_' + f.toFixed(2) + '_' + v.name + '.png';
                    MA.shot(rel, v);
                    files.push(rel);
                }
            }
        }
        return files;
    };
    return 'soldier review loaded';
})();
