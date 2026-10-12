#!/usr/bin/env python3
"""Run a small Minecraft motion-sequence capture batch through the visual probe.

This wrapper keeps the Python parent process alive while Gradle runClient is
running. That avoids losing the spawned client before the probe has finished
capturing frames.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MCP_PATH = ROOT / "tools/mc_visual_probe_mcp.py"
REPORT_PATH = ROOT / "docs/preview_assets/creature_action_acceptance/minecraft_sequences/motion_sequence_probe_run.json"

DEFAULT_CREATURES = ["hydra", "pegasus", "cyclops"]


def load_probe_module() -> Any:
    spec = importlib.util.spec_from_file_location("mc_visual_probe_mcp", MCP_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load {MCP_PATH}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def write_report(payload: dict[str, Any]) -> None:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def run(args: argparse.Namespace) -> dict[str, Any]:
    probe = load_probe_module()
    steps: list[dict[str, Any]] = []
    captures: list[dict[str, Any]] = []
    payload: dict[str, Any] = {
        "ok": False,
        "world_name": args.world_name,
        "port": args.port,
        "creatures": args.creatures,
        "steps": steps,
        "captures": captures,
        "manifest": "docs/preview_assets/creature_action_acceptance/minecraft_sequences/motion_sequence_manifest.json",
    }

    launch = probe.launch_client({"port": args.port, "world_name": args.world_name})
    steps.append({"step": "launch_client", "result": launch})
    if not launch.get("ok"):
        payload["error"] = "launch_client_failed"
        write_report(payload)
        return payload

    ready = probe.wait_ready({"port": args.port, "timeout_seconds": args.launch_timeout, "require_world": True})
    steps.append({"step": "wait_ready", "result": ready})
    if not ready.get("ok"):
        payload["error"] = "wait_ready_failed"
        try:
            steps.append({"step": "latest_log", "result": probe.read_latest_log({"tail": 160})})
        finally:
            try:
                steps.append({"step": "quit_client", "result": probe.quit_client({"port": args.port, "timeout_seconds": args.quit_timeout})})
            except Exception as exc:  # noqa: BLE001 - preserve diagnostic payload
                steps.append({"step": "quit_client", "result": {"ok": False, "error": str(exc)}})
        write_report(payload)
        return payload

    for creature_id in args.creatures:
        result = probe.capture_motion_sequence({
            "port": args.port,
            "creature_id": creature_id,
            "view": args.view,
            "motion_mode": args.motion_mode,
            "delay_ticks": args.delay_ticks,
            "gui_clear_ticks": args.gui_clear_ticks,
            "frame_count": args.frame_count,
            "frame_interval_ticks": args.frame_interval_ticks,
        })
        captures.append(result)
        steps.append({
            "step": "capture_motion_sequence",
            "creature_id": creature_id,
            "ok": bool(result.get("ok")),
            "manifest": result.get("manifest"),
        })
        write_report(payload)
        if not result.get("ok") and args.stop_on_failure:
            break

    if args.quit_client:
        steps.append({"step": "quit_client", "result": probe.quit_client({"port": args.port, "timeout_seconds": args.quit_timeout})})

    payload["ok"] = bool(captures) and all(item.get("ok") for item in captures)
    payload["capture_count"] = len(captures)
    payload["successful_capture_count"] = sum(1 for item in captures if item.get("ok"))
    write_report(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--world-name", default="新的世界")
    parser.add_argument("--port", type=int, default=47620)
    parser.add_argument("--creatures", nargs="+", default=DEFAULT_CREATURES)
    parser.add_argument("--view", choices=["front", "side", "three_quarter"], default="three_quarter")
    parser.add_argument("--motion-mode", default="idle")
    parser.add_argument("--delay-ticks", type=int, default=60)
    parser.add_argument("--gui-clear-ticks", type=int, default=8)
    parser.add_argument("--frame-count", type=int, default=6)
    parser.add_argument("--frame-interval-ticks", type=int, default=8)
    parser.add_argument("--launch-timeout", type=float, default=180)
    parser.add_argument("--quit-timeout", type=float, default=90)
    parser.add_argument("--stop-on-failure", action="store_true")
    parser.add_argument("--keep-client", dest="quit_client", action="store_false")
    parser.set_defaults(quit_client=True)
    args = parser.parse_args()

    payload = run(args)
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    return 0 if payload.get("ok") else 1


if __name__ == "__main__":
    sys.exit(main())
