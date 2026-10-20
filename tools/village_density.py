#!/usr/bin/env python3
"""汇总密度验收报告（V0 §3.9、W3；T13b §8）。

用法：python3 tools/village_density.py <报告目录或文件...> [--max-chunks 1600] [--p90 600] [--p99-ms 50]
报告由开发命令 /mythicage dev villages density <种子> <半径区块> 写到 <存档>/mythic_age/density_<种子>.json。
"""
import argparse
import collections
import json
import sys
from pathlib import Path

DEFAULT_SEEDS = [1, 42, 1337, 20261020, 987654321, -5, 7777777, 31415926]


def pct(values, p):
    if not values:
        return None
    v = sorted(values)
    k = min(len(v) - 1, max(0, int(round(p / 100 * (len(v) - 1)))))
    return v[k]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("paths", nargs="+")
    ap.add_argument("--max-chunks", type=int, default=1600)
    ap.add_argument("--p90", type=int, default=600)
    ap.add_argument("--p99-ms", type=float, default=50.0)
    a = ap.parse_args()
    files = []
    for p in map(Path, a.paths):
        files += sorted(p.glob("density_*.json")) if p.is_dir() else [p]
    ok_all = True
    reasons_all = collections.Counter()
    for f in files:
        r = json.loads(f.read_text(encoding="utf-8"))
        s = r["summary"]
        cands = r["candidates"]
        ms = [c["ms"] for c in cands]
        dists = [d for d in r["edgeDistances"] if d >= 0]
        reasons = collections.Counter(c.get("reason", "") for c in cands if not c["ok"])
        reasons_all.update(reasons)
        p90 = pct(dists, 90)
        p99ms = pct(ms, 99)
        cpv = s["chunksPerVillage"]
        passed = cpv != -1 and cpv <= a.max_chunks and p90 is not None and p90 <= a.p90 and (p99ms or 0) <= a.p99_ms
        ok_all &= passed
        fail_rate = 1 - s["villages"] / max(1, s["candidates"])
        print(f"种子 {r['seed']}: 村庄 {s['villages']}/{s['candidates']}（失败 {fail_rate:.0%}），"
              f"每 {cpv} 区块 1 座，边缘距离 p50 {pct(dists, 50)} p90 {p90} p99 {pct(dists, 99)}，"
              f"单候选耗时 p50 {pct(ms, 50)} p99 {p99ms} ms → {'通过' if passed else '不通过'}")
    print("失败原因：", dict(reasons_all.most_common(8)))
    print("总体：", "通过" if ok_all else "不通过")
    return 0 if ok_all else 1


if __name__ == "__main__":
    sys.exit(main())
