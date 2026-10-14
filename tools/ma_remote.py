#!/usr/bin/env python3
"""Mythic Age 实机遥控口外部工具（T2b，移植自 MCMagicBuild 的 tools/mb_remote.py）。

  python3 tools/ma_remote.py start [--fresh] [--world 名字] [--lang zh_cn] [--realtime] [--structures] [--foreground]
      后台启动 runClient -Pagent=remote，等待新的 ready.json（默认最多 6 分钟）。
      默认把游戏窗口隐藏，并把焦点还给之前在用的程序；--foreground 让窗口留在前台。
  python3 tools/ma_remote.py send '<指令>' ['<指令>' ...]     或    send -f 指令文件（每行一条，# 开头为注释）
      自动编号、追加到 commands.jsonl、等待全部结果、每条打印一行中文摘要；--raw 打印原始 JSON。
  python3 tools/ma_remote.py stop        发送 quit，等待客户端退出
  python3 tools/ma_remote.py status      客户端是否在运行、最近会话

指令简写：不以 { 开头的参数按「指令名 键=值 键=值」解析，例如
  send state  "key name=use action=down"  "wait ticks=30"  "capture name=hud"  "chat text=/time set noon"
值中的 [1,2,3]、数字、true/false 按 JSON 解析；text= 之后的内容整段原样保留（命令里常带等号）。
"""
import argparse
import json
import re
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RUN = ROOT / "neoforge" / "runs" / "client"
REMOTE = RUN / "agent_out" / "remote"
CMDS, RESULTS, READY = REMOTE / "commands.jsonl", REMOTE / "results.jsonl", REMOTE / "ready.json"
LOG = RUN / "agent_out" / "remote_client.log"
PIDFILE = RUN / "agent_out" / "remote_client.pid"


def read_json(p):
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except Exception:  # noqa: BLE001
        return None


def client_alive():
    """遥控模式的开发客户端：MDG 把客户端参数写在 clientRunVmArgs 文件里，
    遥控模式下 fml.modFolders 中还带有 agent 源集的输出目录。"""
    out = subprocess.run(["pgrep", "-f", "clientRunVmArgs"], capture_output=True, text=True).stdout.split()
    pids = []
    for pid in out:
        cmd = subprocess.run(["ps", "-o", "command=", "-p", pid], capture_output=True, text=True).stdout
        if "classes/java/agent" in cmd:
            pids.append(pid)
    return pids


def next_id():
    mx = 0
    for p in (CMDS, RESULTS):
        if p.exists():
            for line in p.read_text(encoding="utf-8", errors="ignore").splitlines():
                try:
                    mx = max(mx, int(json.loads(line).get("id", 0)))
                except Exception:  # noqa: BLE001
                    pass
    return mx + 1


def responsive(timeout=15):
    """发一条 state，看客户端在 timeout 秒内有没有回应。"""
    nid = next_id()
    with open(CMDS, "a", encoding="utf-8") as f:
        f.write(json.dumps({"id": nid, "cmd": "state"}) + "\n")
    deadline = time.time() + timeout
    while time.time() < deadline:
        if RESULTS.exists():
            for line in RESULTS.read_text(encoding="utf-8", errors="ignore").splitlines():
                try:
                    if json.loads(line).get("id") == nid:
                        return True
                except Exception:  # noqa: BLE001
                    continue
        time.sleep(0.5)
    return False


def kill_clients():
    """锁屏时启动的客户端可能卡在报错对话框上：进程还在却不处理指令，只能强制结束。"""
    for sig in ("-TERM", "-KILL"):
        pids = client_alive()
        if not pids:
            return
        subprocess.run(["kill", sig, *pids], capture_output=True)
        time.sleep(3)


# ── 不抢前台：游戏窗口出现后隐藏，并把焦点还给之前在用的程序（macOS） ──

def front_app():
    if sys.platform != "darwin":
        return None
    try:
        asn = subprocess.run(["lsappinfo", "front"], capture_output=True, text=True, timeout=5).stdout.strip()
        out = subprocess.run(["lsappinfo", "info", "-only", "name", asn], capture_output=True, text=True,
                             timeout=5).stdout
        m = re.search(r'"LSDisplayName"="([^"]*)"', out)
        return m.group(1) if m else None
    except Exception:  # noqa: BLE001
        return None


def is_game_app(name):
    return bool(name) and (name.lower().startswith("java") or "minecraft" in name.lower() or "openjdk" in name.lower())


def give_back(prev):
    if not prev or is_game_app(prev):
        return False
    cur = front_app()
    if cur and cur != prev and is_game_app(cur):
        subprocess.run(["open", "-a", prev], capture_output=True)
        return True
    return False


def hide_game():
    """等于对游戏进程按 ⌘H（macOS 自带接口，不需要额外授权）。"""
    if sys.platform != "darwin":
        return False
    hid = False
    for pid in client_alive():
        js = ('ObjC.import("AppKit"); var a = $.NSRunningApplication.runningApplicationWithProcessIdentifier(%s);'
              ' (a && !a.hidden) ? (a.hide, "hid") : "";' % int(pid))
        out = subprocess.run(["osascript", "-l", "JavaScript", "-e", js], capture_output=True, text=True).stdout
        hid = hid or "hid" in out
    return hid


def cmd_start(a):
    if client_alive():
        r = read_json(READY)
        if responsive():
            print(f"客户端已在运行（会话 {r and r.get('sessionId')}），直接 send 即可。")
            return 0
        print(f"客户端进程还在（会话 {r and r.get('sessionId')}）但 15 秒没有回应，强制结束后重新启动")
        kill_clients()
    old = (read_json(READY) or {}).get("sessionId")
    prev = None if a.foreground else front_app()
    props = ["-Pagent=remote", f"-PremoteWorld={a.world}", f"-PremoteLang={a.lang}"]
    if a.fresh:
        props.append("-PremoteFresh=true")
    if a.realtime:
        props.append("-PremoteRealtime=true")
    if a.structures:
        props.append("-PremoteStructures=true")
    LOG.parent.mkdir(parents=True, exist_ok=True)
    with open(LOG, "w") as log:
        proc = subprocess.Popen(["./gradlew", ":neoforge:runClient", *props], cwd=ROOT,
                                stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    PIDFILE.write_text(str(proc.pid))
    print(f"已在后台启动（gradle pid {proc.pid}），日志：{LOG}")
    deadline = time.time() + a.timeout
    gave = False
    while time.time() < deadline:
        if not a.foreground:
            gave = hide_game() or gave
            gave = give_back(prev) or gave
        r = read_json(READY)
        if r and r.get("sessionId") != old:
            # 进入世界时窗口会再抢一次焦点：再盯 20 秒
            for _ in range(40):
                if not a.foreground:
                    hide_game()
                    gave = give_back(prev) or gave
                time.sleep(0.5)
            if gave:
                print(f"已把焦点还给「{prev}」（游戏窗口在后台运行）")
            print("就绪：", json.dumps(r, ensure_ascii=False))
            return 0
        if proc.poll() is not None:
            print(f"gradle 进程已退出（{proc.returncode}），日志末尾：")
            print("\n".join(LOG.read_text(errors="ignore").splitlines()[-25:]))
            return 1
        time.sleep(0.5)
    print("等待 ready.json 超时")
    return 1


def parse_short(text):
    """'key name=use action=down' → dict。text= 之后整段原样保留；其他情况下最后一个键吞掉其后所有文本。"""
    parts = text.split(" ")
    d = {"cmd": parts[0]}
    rest = parts[1:]
    while rest:
        tok = rest.pop(0)
        if tok == "":
            continue
        if "=" not in tok:
            raise SystemExit(f"看不懂：{text}")
        k, v = tok.split("=", 1)
        # 参考项目踩过的坑：/kill @e[type=...] 曾被误拆成两个参数，只发出了 /kill
        if k == "text" or not any("=" in r for r in rest):
            v = " ".join([v, *rest]).strip()
            rest = []
        try:
            d[k] = json.loads(v)
        except Exception:  # noqa: BLE001
            d[k] = v
    return d


def fmt_item(it):
    if not it or it.get("id") == "minecraft:air":
        return "空手"
    s = it.get("name") or it["id"]
    if it.get("count", 1) != 1:
        s += f"×{it['count']}"
    return s


def fmt_hit(h):
    if not h:
        return "-"
    t = h.get("type")
    if t == "entity":
        return f"{h.get('name')}({h.get('health')}血,{h.get('distance', '?')}格)"
    if t == "block":
        return f"{h.get('id')}@{h.get('pos')}" + (f",{h['distance']}格" if "distance" in h else "")
    return "空"


def summarize(r, raw=False):
    if raw:
        return json.dumps(r, ensure_ascii=False)
    head = f"#{r['id']} {r['cmd']} {'✓' if r['ok'] else '✗ ' + str(r.get('error'))}"
    d = r.get("data") or {}
    extra = ""
    if r["cmd"] == "state" and r["ok"]:
        p = d.get("pos", {})
        ro = d.get("rot", {})
        near = "、".join(f"{n.get('name')}{'(' + str(n['health']) + '血)' if 'health' in n else ''}"
                        f"{n.get('distance', 0):.1f}格" for n in d.get("nearby", [])[:4]) or "无"
        scr = (d.get("screen") or {}).get("class") or "无界面"
        hotbar = "、".join(f"{s['slot']}:{fmt_item(s)}" for s in d.get("hotbar", [])) or "空"
        extra = (f" 位置({p.get('x', 0):.1f},{p.get('y', 0):.1f},{p.get('z', 0):.1f})"
                 f" 朝向({ro.get('yaw', 0):.0f},{ro.get('pitch', 0):.0f})"
                 f" {'飞行' if d.get('flying') else ('着地' if d.get('onGround') else '空中')}"
                 f" {d.get('gamemode')} 血{d.get('health')} 手:{fmt_item(d.get('mainHand'))} 快捷栏:{hotbar}"
                 f" 瞄准:{fmt_hit(d.get('aim'))} 界面:{scr} 附近:{near}"
                 f" mythic:{json.dumps(d.get('mythic'), ensure_ascii=False)}")
    elif r["cmd"] == "capture" and r["ok"]:
        extra = " → " + str(d.get("absolutePath") or REMOTE / d.get("path", ""))
    elif r["cmd"] == "screen" and r["ok"]:
        ws = "；".join(f"[{w['index']}]{w['class']}「{w.get('message', '')}」@({w['x']},{w['y']})"
                      for w in d.get("widgets", []))
        c = d.get("container")
        slots = ""
        if c:
            filled = [s for s in c.get("slots", []) if s.get("id") != "minecraft:air"]
            slots = (f" 容器{c.get('containerId')} 左上({c.get('guiLeft')},{c.get('guiTop')}) 非空格{len(filled)}: "
                     + "、".join(f"{s['index']}:{s.get('name') or s['id']}" for s in filled[:24])
                     + f" 光标:{fmt_item(c.get('carried'))}")
        cu = f" custom={json.dumps(d['custom'], ensure_ascii=False)}" if d.get("custom") else ""
        extra = f" {d.get('class')}「{d.get('title')}」 控件:{ws or '无'}{slots}{cu}"
    elif d:
        extra = " " + json.dumps(d, ensure_ascii=False)
    chat = r.get("chat") or []
    if chat:
        extra += " 聊天:" + " | ".join(chat)
    return head + extra


def cmd_send(a):
    if not client_alive():
        print("客户端没有在运行，先 start")
        return 1
    lines = []
    if a.file:
        lines = [l for l in Path(a.file).read_text(encoding="utf-8").splitlines()
                 if l.strip() and not l.lstrip().startswith("#")]
    lines += a.commands
    nid = next_id()
    ids, payload = [], []
    for line in lines:
        d = json.loads(line) if line.lstrip().startswith("{") else parse_short(line)
        if "id" not in d:
            d = {"id": nid, **d}
            nid += 1
        ids.append(d["id"])
        payload.append(json.dumps(d, ensure_ascii=False))
    with open(CMDS, "a", encoding="utf-8") as f:
        f.write("\n".join(payload) + "\n")
    want, got = set(ids), {}
    budget = a.timeout + sum((json.loads(p).get("ticks", 0) or 0) / 20 for p in payload)
    deadline = time.time() + budget
    while time.time() < deadline and want - set(got):
        if RESULTS.exists():
            for line in RESULTS.read_text(encoding="utf-8", errors="ignore").splitlines():
                try:
                    r = json.loads(line)
                except Exception:  # noqa: BLE001
                    continue
                if r.get("id") in want and r["id"] not in got:
                    got[r["id"]] = r
                    print(summarize(r, a.raw), flush=True)
        time.sleep(0.2)
    missing = want - set(got)
    if missing:
        print(f"超时未返回：{sorted(missing)}（客户端{'在' if client_alive() else '不在'}运行）")
        return 1
    return 0 if all(r["ok"] for r in got.values()) else 2


def cmd_stop(a):
    if not client_alive():
        print("客户端没有在运行")
        return 0
    with open(CMDS, "a", encoding="utf-8") as f:
        f.write(json.dumps({"id": next_id(), "cmd": "quit"}) + "\n")
    for _ in range(60):
        if not client_alive():
            print("已退出")
            return 0
        time.sleep(1)
    kill_clients()
    print("60 秒内没有退出，已强制结束" if not client_alive() else "60 秒内没有退出，强制结束也失败")
    return 1


def cmd_status(a):
    pids = client_alive()
    r = read_json(READY)
    print(f"客户端：{'运行中 pid ' + ','.join(pids) if pids else '未运行'}；"
          f"最近会话：{json.dumps(r, ensure_ascii=False) if r else '无'}")
    return 0


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="op", required=True)
    s = sub.add_parser("start")
    s.add_argument("--fresh", action="store_true")
    s.add_argument("--foreground", action="store_true", help="不隐藏游戏窗口、不归还焦点")
    s.add_argument("--realtime", action="store_true")
    s.add_argument("--structures", action="store_true", help="新建世界时生成结构")
    s.add_argument("--world", default="ma_autotest")
    s.add_argument("--lang", default="zh_cn")
    s.add_argument("--timeout", type=int, default=360)
    s.set_defaults(fn=cmd_start)
    s = sub.add_parser("send")
    s.add_argument("commands", nargs="*")
    s.add_argument("-f", "--file")
    s.add_argument("--raw", action="store_true")
    s.add_argument("--timeout", type=float, default=30)
    s.set_defaults(fn=cmd_send)
    sub.add_parser("stop").set_defaults(fn=cmd_stop)
    sub.add_parser("status").set_defaults(fn=cmd_status)
    a = ap.parse_args()
    sys.exit(a.fn(a))


if __name__ == "__main__":
    main()
