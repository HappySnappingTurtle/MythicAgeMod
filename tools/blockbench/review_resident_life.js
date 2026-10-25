// T19 阶段 ⑤b 居民生活与节庆动作审阅：按动作显示对应的道具与男 / 女装（同 ResidentGeoModel 的显隐规则），
// 每个动作取 3 个时刻 × 2 个机位截图，写到 out 目录；拼图用 Python。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/review_entity.js');
    const SHOW = {
        water_fill: ['tool_hydria'], water_carry_head: ['carry_hydria'], carry_basket_head: ['carry_kanoun'],
        torch_run: ['carry_torch'], aulos_play: ['carry_aulos'], perform: ['tool_mask'], grave_tend: ['carry_ribbon'],
        eat: ['tool_bread'], libation_home: ['tool_cup'], fish_cast: ['tool_net'], fish_reel: ['tool_net'],
        carry_walk: ['carry_wine_jar'],
    };
    const FEMALE = new Set(['water_fill', 'water_carry_head', 'carry_basket_head', 'dance', 'grave_tend', 'wash', 'sit_watch']);
    const tag = (c) => {
        for (let g = c.parent; g && g.name; g = g.parent) {
            if (/^(carry_|tool_|outfit_)/.test(g.name)) return g.name;
        }
        return '';
    };
    const apply = (anim, extra) => {
        const show = new Set((SHOW[anim] || []).concat(extra || []));
        const female = FEMALE.has(anim);
        for (const c of Cube.all) {
            const t = tag(c);
            let vis = true;
            if (t.startsWith('outfit_male')) vis = !female;
            else if (t.startsWith('outfit_female')) vis = female;
            else if (t.startsWith('carry_') || t.startsWith('tool_')) vis = show.has(t);
            c.visibility = vis;
            if (c.mesh) c.mesh.visible = vis;
        }
    };
    MA.reviewResidentLife = (cfg) => {
        const V = MA.REVIEW_VIEWS;
        const views = [V[0], V[1]];
        const files = [];
        for (const item of cfg.items) {
            const name = item.anim;
            const anim = Animation.all.find((a) => a.name === name);
            apply(name, item.extra);
            const tag2 = item.label || name;
            for (const f of [0, 0.33, 0.66]) {
                const t = Math.round(anim.length * f * 20) / 20;
                Modes.options.animate.select();
                anim.select();
                Timeline.setTime(t);
                Animator.preview();
                for (const v of views) {
                    const rel = cfg.out + '/' + tag2 + '_' + f.toFixed(2) + '_' + v.name + '.png';
                    MA.shot(rel, v);
                    files.push(rel);
                }
            }
        }
        apply('', []);
        Modes.options.edit.select();
        return files.length;
    };
    return 'resident life review loaded';
})();
