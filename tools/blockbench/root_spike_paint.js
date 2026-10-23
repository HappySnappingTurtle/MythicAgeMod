// 根刺贴图（T15 阶段 ⑤）：裂纹薄板只在顶面画几道从中心向外分叉的裂缝，其余像素透明；树根是竖向树皮纹，尖端浅。
(function () {
    const MA = window.MA;
    function paint(ctx, nets) {
        for (const [g, net] of Object.entries(nets)) {
            for (const [face, r] of Object.entries(net.faces)) {
                if (g === 'crack') {
                    ctx.clearRect(r[0], r[1], r[2], r[3]);
                    if (face !== 'top') continue;
                    const cx = r[0] + r[2] / 2;
                    const cy = r[1] + r[3] / 2;
                    ctx.fillStyle = '#1e160d';
                    // 七道主裂缝，每道走折线并在中途分一叉
                    for (let k = 0; k < 7; k++) {
                        let a = (k / 7) * Math.PI * 2 + 0.3;
                        let x = cx;
                        let y = cy;
                        for (let s = 0; s < 22; s++) {
                            a += (Math.sin(k * 7.1 + s * 1.7) * 0.35);
                            x += Math.cos(a) * 1.0;
                            y += Math.sin(a) * 1.0;
                            ctx.fillRect(Math.round(x), Math.round(y), 1, 1);
                            if (s === 10) {
                                let bx = x;
                                let by = y;
                                for (let t = 0; t < 7; t++) {
                                    bx += Math.cos(a + 0.8);
                                    by += Math.sin(a + 0.8);
                                    ctx.fillRect(Math.round(bx), Math.round(by), 1, 1);
                                }
                            }
                        }
                    }
                    ctx.fillStyle = '#3a2a18';
                    ctx.fillRect(Math.round(cx) - 2, Math.round(cy) - 2, 4, 4);
                } else {
                    const img = ctx.getImageData(r[0], r[1], r[2], r[3]);
                    for (let v = 0; v < r[3]; v++) {
                        for (let u = 0; u < r[2]; u++) {
                            const i = (v * r[2] + u) * 4;
                            const f = (u % 3 === 0 ? 0.72 : 1.05) * (0.92 + ((u * 31 + v * 17) % 7) * 0.02);
                            for (let c = 0; c < 3; c++) img.data[i + c] = Math.min(255, img.data[i + c] * f);
                        }
                    }
                    ctx.putImageData(img, r[0], r[1]);
                }
            }
        }
    }
    return { paint };
})();
