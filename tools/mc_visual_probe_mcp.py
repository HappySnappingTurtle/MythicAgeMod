#!/usr/bin/env python3
"""MCP server for local Minecraft visual probe screenshots."""

from __future__ import annotations

import json
import os
import base64
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
PREVIEW_DIR = ROOT / "docs/preview_assets/greek_creatures"
MANIFEST_PATH = ROOT / "docs/美术资源/greek_creatures/visual_manifest.json"
ENTITY_MANIFEST_PATH = ROOT / "docs/美术资源/entity_validation/entity_manifest.json"
REPORT_PATH = PREVIEW_DIR / "visual_report.json"
ENTITY_CAPTURE_DIR = ROOT / "docs/preview_assets/entity_validation/minecraft"
MOTION_SEQUENCE_DIR = ROOT / "docs/preview_assets/creature_action_acceptance/minecraft_sequences"
MOTION_SEQUENCE_MANIFEST_PATH = MOTION_SEQUENCE_DIR / "motion_sequence_manifest.json"
RUNTIME_NBT_REPORT_PATH = ROOT / "run/visual_probe/entity_nbt_report.json"
RUNTIME_NBT_RESTART_REPORT_PATH = ROOT / "run/visual_probe/nbt_restart_report.json"
RUNTIME_NBT_RESTART_PREPARE_REPORT_PATH = ROOT / "run/visual_probe/nbt_restart_prepare_report.json"
RUNTIME_NBT_RESTART_VERIFY_REPORT_PATH = ROOT / "run/visual_probe/nbt_restart_verify_report.json"
RUNTIME_GOD_POWER_PLAYTEST_REPORT_PATH = ROOT / "run/visual_probe/god_power_playtest_report.json"
LOG_DIR = ROOT / "run/logs"
RUN_CLIENT_LOG = ROOT / "run/visual_probe_mcp_runclient.log"
VIEWS = ("front", "side", "three_quarter")
DEFAULT_GUI_CLEAR_TICKS = 6

RUN_CLIENT_PROCESS: subprocess.Popen[str] | None = None


def project_path(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(ROOT))
    except ValueError:
        return str(path.resolve())


def text_result(payload: Any, is_error: bool = False) -> dict[str, Any]:
    return {
        "content": [{"type": "text", "text": json.dumps(payload, indent=2, ensure_ascii=False)}],
        "isError": is_error,
    }


def java_home() -> str | None:
    configured = os.environ.get("JAVA_HOME")
    if configured:
        return configured
    try:
        return subprocess.check_output(["/usr/libexec/java_home", "-v", "17"], text=True).strip()
    except Exception:
        return None


def probe_url(port: int, path: str) -> str:
    return f"http://127.0.0.1:{port}{path}"


def http_json(method: str, url: str, payload: dict[str, Any] | None = None, timeout: float = 10.0) -> dict[str, Any]:
    data = None
    headers = {}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def load_manifest() -> dict[str, Any]:
    return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))


def load_entity_manifest() -> dict[str, Any]:
    return json.loads(ENTITY_MANIFEST_PATH.read_text(encoding="utf-8"))


def load_report() -> dict[str, Any]:
    if REPORT_PATH.exists():
        return json.loads(REPORT_PATH.read_text(encoding="utf-8"))
    return {
        "version": 1,
        "workflow": "design_first_blockbench_then_minecraft",
        "views": list(VIEWS),
        "creatures": [],
    }


def save_report(report: dict[str, Any]) -> None:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def update_report_capture(creature_id: str, view: str, destination: Path, source: Path) -> None:
    report = load_report()
    for creature in report.get("creatures", []):
        if creature.get("id") == creature_id:
            minecraft = creature.setdefault("artifacts", {}).setdefault("minecraft_screenshots", {})
            minecraft[view] = project_path(destination)
            checks = creature.setdefault("checks", {})
            all_ready = all((PREVIEW_DIR / creature_id / "minecraft" / f"{item}.png").exists() for item in VIEWS)
            checks["minecraft_screenshots_present"] = all_ready
            checks["matches_design_package"] = "pending_visual_review" if all_ready else "pending_mc_capture"
            creature["status"] = "mc_captured_pending_visual_review" if all_ready else "partial_mc_capture_pending"
            creature.setdefault("capture_sources", {})[view] = str(source)
            break
    save_report(report)


def launch_client(args: dict[str, Any]) -> dict[str, Any]:
    global RUN_CLIENT_PROCESS
    if RUN_CLIENT_PROCESS is not None and RUN_CLIENT_PROCESS.poll() is None:
        return {"ok": True, "already_running": True, "pid": RUN_CLIENT_PROCESS.pid, "log": project_path(RUN_CLIENT_LOG)}

    port = int(args.get("port", 47620))
    world_name = str(args.get("world_name") or "")
    home = str(args.get("java_home") or java_home() or "")
    env = os.environ.copy()
    if home:
        env["JAVA_HOME"] = home
        env["PATH"] = str(Path(home) / "bin") + os.pathsep + env.get("PATH", "")
    java_tool_options = env.get("JAVA_TOOL_OPTIONS", "")
    probe_options = f"-Dmythic.visualProbe=true -Dmythic.visualProbePort={port}"
    if world_name:
        encoded_world = base64.b64encode(world_name.encode("utf-8")).decode("ascii")
        probe_options += f" -Dmythic.visualProbeWorldBase64={encoded_world}"
    env["JAVA_TOOL_OPTIONS"] = (java_tool_options + " " + probe_options).strip()
    RUN_CLIENT_LOG.parent.mkdir(parents=True, exist_ok=True)
    log_file = RUN_CLIENT_LOG.open("w", encoding="utf-8")
    command = ["./gradlew", "runClient"]
    RUN_CLIENT_PROCESS = subprocess.Popen(
        command,
        cwd=ROOT,
        env=env,
        stdout=log_file,
        stderr=subprocess.STDOUT,
        text=True,
    )
    return {
        "ok": True,
        "pid": RUN_CLIENT_PROCESS.pid,
        "port": port,
        "log": project_path(RUN_CLIENT_LOG),
        "world_name": world_name,
        "note": "Open or create a local cheats-enabled visual test world, then call wait_ready and capture tools.",
    }


def wait_ready(args: dict[str, Any]) -> dict[str, Any]:
    port = int(args.get("port", 47620))
    timeout_seconds = float(args.get("timeout_seconds", 120))
    deadline = time.time() + timeout_seconds
    last_error = ""
    last_payload: dict[str, Any] | None = None
    while time.time() < deadline:
        try:
            last_payload = http_json("GET", probe_url(port, "/health"), timeout=3.0)
            if args.get("require_world", True):
                if last_payload.get("in_world") and last_payload.get("player_present"):
                    return {"ok": True, "health": last_payload}
            else:
                return {"ok": True, "health": last_payload}
        except Exception as exc:
            last_error = str(exc)
        time.sleep(2)
    return {"ok": False, "error": last_error or "not_ready", "last_health": last_payload}


def copy_capture(source: Path, creature_id: str, view: str) -> Path:
    if args_scope_is_entity_validation(creature_id):
        destination = ENTITY_CAPTURE_DIR / creature_id / f"{view}.png"
    else:
        destination = PREVIEW_DIR / creature_id / "minecraft" / f"{view}.png"
    destination.parent.mkdir(parents=True, exist_ok=True)
    deadline = time.time() + 10
    while time.time() < deadline:
        if source.exists() and source.stat().st_size > 0:
            shutil.copy2(source, destination)
            return destination
        time.sleep(0.25)
    raise FileNotFoundError(f"Screenshot was not written in time: {source}")
    return destination


def copy_sequence_frame(source: Path, creature_id: str, view: str, motion_mode: str, index: int) -> Path:
    destination = MOTION_SEQUENCE_DIR / creature_id / motion_mode / view / f"frame_{index:02d}.png"
    destination.parent.mkdir(parents=True, exist_ok=True)
    deadline = time.time() + 10
    while time.time() < deadline:
        if source.exists() and source.stat().st_size > 0:
            shutil.copy2(source, destination)
            return destination
        time.sleep(0.25)
    raise FileNotFoundError(f"Sequence frame was not written in time: {source}")


def load_motion_sequence_manifest() -> dict[str, Any]:
    if MOTION_SEQUENCE_MANIFEST_PATH.exists():
        return json.loads(MOTION_SEQUENCE_MANIFEST_PATH.read_text(encoding="utf-8"))
    return {
        "version": 1,
        "scope": "minecraft_motion_sequence_capture",
        "captures": [],
    }


def save_motion_sequence_manifest(manifest: dict[str, Any]) -> None:
    MOTION_SEQUENCE_MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    MOTION_SEQUENCE_MANIFEST_PATH.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def update_motion_sequence_manifest(record: dict[str, Any]) -> None:
    manifest = load_motion_sequence_manifest()
    captures = [
        item for item in manifest.get("captures", [])
        if not (
            item.get("creature_id") == record.get("creature_id")
            and item.get("view") == record.get("view")
            and item.get("motion_mode") == record.get("motion_mode")
        )
    ]
    captures.append(record)
    manifest["captures"] = sorted(
        captures,
        key=lambda item: (str(item.get("creature_id")), str(item.get("motion_mode")), str(item.get("view"))),
    )
    manifest["capture_count"] = len(manifest["captures"])
    manifest["ok"] = all(item.get("ok") for item in manifest["captures"])
    save_motion_sequence_manifest(manifest)


def args_scope_is_entity_validation(creature_id: str) -> bool:
    try:
        manifest = load_manifest()
        return creature_id not in {item["id"] for item in manifest.get("creatures", [])}
    except Exception:
        return True


def capture_entity(args: dict[str, Any]) -> dict[str, Any]:
    port = int(args.get("port", 47620))
    creature_id = str(args["creature_id"])
    entity_id = str(args.get("entity_id") or f"mythic_age:{creature_id}")
    view = str(args.get("view", "front"))
    if view not in VIEWS:
        return {"ok": False, "error": f"Unknown view: {view}"}
    payload = {
        "creature_id": creature_id,
        "entity_id": entity_id,
        "view": view,
        "delay_ticks": int(args.get("delay_ticks", 50)),
        "gui_clear_ticks": int(args.get("gui_clear_ticks", DEFAULT_GUI_CLEAR_TICKS)),
    }
    result = http_json("POST", probe_url(port, "/capture"), payload=payload, timeout=25.0)
    if not result.get("ok"):
        return result
    if result.get("screen_clear") is not True:
        return {
            "ok": False,
            "error": "capture_rejected_screen_not_confirmed_clear",
            "creature_id": creature_id,
            "view": view,
            "source_result": result,
        }
    source = Path(result["screenshot"])
    destination = copy_capture(source, creature_id, view)
    update_report_capture(creature_id, view, destination, source)
    result["copied_to"] = project_path(destination)
    result["visual_report"] = project_path(REPORT_PATH)
    return result


def capture_batch(args: dict[str, Any]) -> dict[str, Any]:
    manifest = load_entity_manifest() if args.get("scope") == "all_entities" else load_manifest()
    requested = args.get("creature_ids")
    creature_ids = set(requested) if requested else None
    visual_gate_filter = set(args.get("visual_gate_filter", []))
    results = []
    entity_items = manifest.get("entities", manifest.get("creatures", []))
    for creature in entity_items:
        creature_id = creature["id"]
        if creature_ids is not None and creature_id not in creature_ids:
            continue
        if visual_gate_filter and creature.get("visual_gate") not in visual_gate_filter:
            continue
        for view in VIEWS:
            try:
                results.append(capture_entity({
                    "port": args.get("port", 47620),
                    "creature_id": creature_id,
                    "entity_id": creature["entity_id"],
                    "view": view,
                    "delay_ticks": args.get("delay_ticks", 50),
                    "gui_clear_ticks": args.get("gui_clear_ticks", DEFAULT_GUI_CLEAR_TICKS),
                }))
            except (urllib.error.URLError, TimeoutError, OSError) as exc:
                results.append({"ok": False, "creature_id": creature_id, "view": view, "error": str(exc)})
    return {"ok": all(item.get("ok") for item in results), "results": results, "visual_report": project_path(REPORT_PATH)}


def capture_motion_sequence(args: dict[str, Any]) -> dict[str, Any]:
    port = int(args.get("port", 47620))
    creature_id = str(args["creature_id"])
    entity_id = str(args.get("entity_id") or f"mythic_age:{creature_id}")
    view = str(args.get("view", "three_quarter"))
    motion_mode = str(args.get("motion_mode", "idle"))
    if view not in VIEWS:
        return {"ok": False, "error": f"Unknown view: {view}"}
    payload = {
        "creature_id": creature_id,
        "entity_id": entity_id,
        "view": view,
        "motion_mode": motion_mode,
        "delay_ticks": int(args.get("delay_ticks", 50)),
        "gui_clear_ticks": int(args.get("gui_clear_ticks", DEFAULT_GUI_CLEAR_TICKS)),
        "frame_count": int(args.get("frame_count", 8)),
        "frame_interval_ticks": int(args.get("frame_interval_ticks", 8)),
    }
    if args.get("summon_nbt"):
        payload["summon_nbt"] = str(args["summon_nbt"])
    timeout = max(25.0, (payload["delay_ticks"] + payload["frame_count"] * payload["frame_interval_ticks"]) / 20.0 + 15.0)
    result = http_json("POST", probe_url(port, "/capture_sequence"), payload=payload, timeout=timeout)
    if not result.get("ok"):
        return result
    if result.get("screen_clear") is not True:
        return {
            "ok": False,
            "error": "capture_rejected_screen_not_confirmed_clear",
            "creature_id": creature_id,
            "view": view,
            "source_result": result,
        }
    copied = []
    for index, raw_path in enumerate(result.get("screenshots", [])):
        destination = copy_sequence_frame(Path(raw_path), creature_id, view, motion_mode, index)
        copied.append(project_path(destination))
    record = {
        "ok": bool(copied),
        "creature_id": creature_id,
        "entity_id": entity_id,
        "view": view,
        "motion_mode": motion_mode,
        "frame_count": len(copied),
        "frame_interval_ticks": payload["frame_interval_ticks"],
        "gui_clear_ticks": payload["gui_clear_ticks"],
        "screen_clear": result.get("screen_clear"),
        "screen_was_open": result.get("screen_was_open"),
        "closed_screen": result.get("closed_screen"),
        "frames": copied,
    }
    update_motion_sequence_manifest(record)
    return {
        "ok": record["ok"],
        "record": record,
        "manifest": project_path(MOTION_SEQUENCE_MANIFEST_PATH),
        "source": result,
    }


def run_runtime_nbt_validation(args: dict[str, Any]) -> dict[str, Any]:
    timeout_seconds = float(args.get("timeout_seconds", 12))
    port = int(args.get("port", 47620))
    command = "mythicage_validate nbt"
    if RUNTIME_NBT_REPORT_PATH.exists():
        RUNTIME_NBT_REPORT_PATH.unlink()
    try:
        source = http_json("POST", probe_url(port, "/validate_nbt"), {}, timeout=timeout_seconds)
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise
        source = http_json("POST", probe_url(port, "/command"), {"command": command}, timeout=timeout_seconds)
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        if RUNTIME_NBT_REPORT_PATH.exists() and RUNTIME_NBT_REPORT_PATH.stat().st_size > 0:
            report = json.loads(RUNTIME_NBT_REPORT_PATH.read_text(encoding="utf-8"))
            return {
                "ok": bool(report.get("failed", 1) == 0),
                "command": source,
                "report_path": project_path(RUNTIME_NBT_REPORT_PATH),
                "report": report,
            }
        time.sleep(0.25)
    return {
        "ok": False,
        "error": "runtime_nbt_report_not_written",
        "command": source,
        "expected_report": project_path(RUNTIME_NBT_REPORT_PATH),
    }


def run_runtime_nbt_restart_validation(args: dict[str, Any]) -> dict[str, Any]:
    timeout_seconds = float(args.get("timeout_seconds", 12))
    port = int(args.get("port", 47620))
    command = "mythicage_validate nbt_restart"
    if RUNTIME_NBT_RESTART_REPORT_PATH.exists():
        RUNTIME_NBT_RESTART_REPORT_PATH.unlink()
    try:
        source = http_json("POST", probe_url(port, "/validate_nbt_restart"), {}, timeout=timeout_seconds)
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise
        source = http_json("POST", probe_url(port, "/command"), {"command": command}, timeout=timeout_seconds)
    return wait_for_runtime_report(source, RUNTIME_NBT_RESTART_REPORT_PATH, timeout_seconds)


def run_runtime_nbt_restart_prepare(args: dict[str, Any]) -> dict[str, Any]:
    timeout_seconds = float(args.get("timeout_seconds", 20))
    port = int(args.get("port", 47620))
    if RUNTIME_NBT_RESTART_PREPARE_REPORT_PATH.exists():
        RUNTIME_NBT_RESTART_PREPARE_REPORT_PATH.unlink()
    try:
        source = http_json("POST", probe_url(port, "/validate_nbt_restart_prepare"), {}, timeout=timeout_seconds)
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise
        source = http_json(
            "POST",
            probe_url(port, "/command"),
            {"command": "mythicage_validate nbt_restart_prepare"},
            timeout=timeout_seconds,
        )
    return wait_for_runtime_report(source, RUNTIME_NBT_RESTART_PREPARE_REPORT_PATH, timeout_seconds)


def run_runtime_nbt_restart_verify(args: dict[str, Any]) -> dict[str, Any]:
    timeout_seconds = float(args.get("timeout_seconds", 20))
    port = int(args.get("port", 47620))
    if RUNTIME_NBT_RESTART_VERIFY_REPORT_PATH.exists():
        RUNTIME_NBT_RESTART_VERIFY_REPORT_PATH.unlink()
    try:
        source = http_json("POST", probe_url(port, "/validate_nbt_restart_verify"), {}, timeout=timeout_seconds)
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise
        source = http_json(
            "POST",
            probe_url(port, "/command"),
            {"command": "mythicage_validate nbt_restart_verify"},
            timeout=timeout_seconds,
        )
    return wait_for_runtime_report(source, RUNTIME_NBT_RESTART_VERIFY_REPORT_PATH, timeout_seconds)


def run_runtime_god_power_playtest(args: dict[str, Any]) -> dict[str, Any]:
    timeout_seconds = float(args.get("timeout_seconds", 120))
    port = int(args.get("port", 47620))
    if RUNTIME_GOD_POWER_PLAYTEST_REPORT_PATH.exists():
        RUNTIME_GOD_POWER_PLAYTEST_REPORT_PATH.unlink()
    try:
        source = http_json("POST", probe_url(port, "/validate_god_power_playtest"), {}, timeout=timeout_seconds)
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise
        source = http_json(
            "POST",
            probe_url(port, "/command"),
            {"command": "mythicage_validate god_power_playtest"},
            timeout=timeout_seconds,
        )
    return wait_for_runtime_report(source, RUNTIME_GOD_POWER_PLAYTEST_REPORT_PATH, timeout_seconds)


def wait_for_runtime_report(source: dict[str, Any], report_path: Path, timeout_seconds: float) -> dict[str, Any]:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        if report_path.exists() and report_path.stat().st_size > 0:
            report = json.loads(report_path.read_text(encoding="utf-8"))
            return {
                "ok": bool(report.get("failed", 1) == 0),
                "command": source,
                "report_path": project_path(report_path),
                "report": report,
            }
        time.sleep(0.25)
    return {
        "ok": False,
        "error": "runtime_nbt_report_not_written",
        "command": source,
        "expected_report": project_path(report_path),
    }


def read_latest_log(args: dict[str, Any]) -> dict[str, Any]:
    log_path = Path(args.get("path") or LOG_DIR / "latest.log")
    if not log_path.exists():
        log_path = RUN_CLIENT_LOG
    if not log_path.exists():
        return {"ok": False, "error": "No log file found"}
    lines = log_path.read_text(encoding="utf-8", errors="replace").splitlines()
    tail = int(args.get("tail", 160))
    return {"ok": True, "path": project_path(log_path), "tail": "\n".join(lines[-tail:])}


def shutdown_probe(args: dict[str, Any]) -> dict[str, Any]:
    port = int(args.get("port", 47620))
    try:
        return http_json("POST", probe_url(port, "/shutdown"), payload={}, timeout=5.0)
    except Exception as exc:
        return {"ok": False, "error": str(exc)}


def quit_client(args: dict[str, Any]) -> dict[str, Any]:
    global RUN_CLIENT_PROCESS
    port = int(args.get("port", 47620))
    timeout_seconds = float(args.get("timeout_seconds", 60))
    result: dict[str, Any]
    try:
        result = http_json("POST", probe_url(port, "/quit_client"), payload={}, timeout=5.0)
    except Exception as exc:
        result = {"ok": False, "error": str(exc)}
    process = RUN_CLIENT_PROCESS
    if process is None:
        return {
            "ok": bool(result.get("ok")),
            "probe": result,
            "process_known": False,
        }
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        code = process.poll()
        if code is not None:
            RUN_CLIENT_PROCESS = None
            return {
                "ok": bool(result.get("ok")),
                "probe": result,
                "process_known": True,
                "pid": process.pid,
                "returncode": code,
            }
        time.sleep(0.5)
    return {
        "ok": False,
        "probe": result,
        "process_known": True,
        "pid": process.pid,
        "error": "client_process_did_not_exit_before_timeout",
    }


def run_true_nbt_restart_cycle(args: dict[str, Any]) -> dict[str, Any]:
    port = int(args.get("port", 47620))
    world_name = str(args.get("world_name") or "MythicAgeNbtRestartProbe")
    launch_timeout = float(args.get("launch_timeout_seconds", 180))
    probe_timeout = float(args.get("probe_timeout_seconds", 30))
    quit_timeout = float(args.get("quit_timeout_seconds", 90))
    restart_pause = float(args.get("restart_pause_seconds", 3))
    steps: list[dict[str, Any]] = []

    first_launch = launch_client({"port": port, "world_name": world_name})
    steps.append({"step": "launch_prepare_client", "result": first_launch})
    ready = wait_ready({"port": port, "timeout_seconds": launch_timeout, "require_world": True})
    steps.append({"step": "wait_prepare_world", "result": ready})
    if not ready.get("ok"):
        return {"ok": False, "world_name": world_name, "steps": steps}

    prepare = run_runtime_nbt_restart_prepare({"port": port, "timeout_seconds": probe_timeout})
    steps.append({"step": "prepare", "result": prepare})
    if not prepare.get("ok"):
        return {"ok": False, "world_name": world_name, "steps": steps}

    quit_result = quit_client({"port": port, "timeout_seconds": quit_timeout})
    steps.append({"step": "quit_prepare_client", "result": quit_result})
    if not quit_result.get("ok"):
        return {"ok": False, "world_name": world_name, "steps": steps}
    time.sleep(restart_pause)

    second_launch = launch_client({"port": port, "world_name": world_name})
    steps.append({"step": "launch_verify_client", "result": second_launch})
    ready_after_restart = wait_ready({"port": port, "timeout_seconds": launch_timeout, "require_world": True})
    steps.append({"step": "wait_verify_world", "result": ready_after_restart})
    if not ready_after_restart.get("ok"):
        return {"ok": False, "world_name": world_name, "steps": steps}

    verify = run_runtime_nbt_restart_verify({"port": port, "timeout_seconds": probe_timeout})
    steps.append({"step": "verify", "result": verify})
    prepare_report = prepare.get("report", {}) if isinstance(prepare.get("report"), dict) else {}
    verify_report = verify.get("report", {}) if isinstance(verify.get("report"), dict) else {}
    prepare_process_id = prepare_report.get("process_id")
    verify_process_id = verify_report.get("process_id")
    different_process = (
        prepare_process_id is not None
        and verify_process_id is not None
        and prepare_process_id != verify_process_id
    )
    return {
        "ok": bool(verify.get("ok")) and different_process,
        "world_name": world_name,
        "prepare_report": project_path(RUNTIME_NBT_RESTART_PREPARE_REPORT_PATH),
        "verify_report": project_path(RUNTIME_NBT_RESTART_VERIFY_REPORT_PATH),
        "prepare_process_id": prepare_process_id,
        "verify_process_id": verify_process_id,
        "different_process": different_process,
        "steps": steps,
        "error": None if different_process else "prepare_and_verify_process_ids_not_distinct",
    }


def call_tool(name: str, args: dict[str, Any]) -> dict[str, Any]:
    try:
        if name == "launch_client":
            payload = launch_client(args)
        elif name == "wait_ready":
            payload = wait_ready(args)
        elif name == "capture_entity":
            payload = capture_entity(args)
        elif name == "capture_batch":
            payload = capture_batch(args)
        elif name == "capture_motion_sequence":
            payload = capture_motion_sequence(args)
        elif name == "run_runtime_nbt_validation":
            payload = run_runtime_nbt_validation(args)
        elif name == "run_runtime_nbt_restart_validation":
            payload = run_runtime_nbt_restart_validation(args)
        elif name == "run_runtime_nbt_restart_prepare":
            payload = run_runtime_nbt_restart_prepare(args)
        elif name == "run_runtime_nbt_restart_verify":
            payload = run_runtime_nbt_restart_verify(args)
        elif name == "run_runtime_god_power_playtest":
            payload = run_runtime_god_power_playtest(args)
        elif name == "read_latest_log":
            payload = read_latest_log(args)
        elif name == "shutdown_probe":
            payload = shutdown_probe(args)
        elif name == "quit_client":
            payload = quit_client(args)
        elif name == "run_true_nbt_restart_cycle":
            payload = run_true_nbt_restart_cycle(args)
        else:
            payload = {"ok": False, "error": f"Unknown tool: {name}"}
    except Exception as exc:
        payload = {"ok": False, "error": str(exc)}
    return text_result(payload, not payload.get("ok", False))


TOOLS = [
    {
        "name": "launch_client",
        "description": "Launch ./gradlew runClient with the localhost visual probe enabled.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "java_home": {"type": "string"},
                "world_name": {"type": "string"},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "wait_ready",
        "description": "Wait until the visual probe is reachable, optionally requiring a world and player.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "timeout_seconds": {"type": "number", "default": 120},
                "require_world": {"type": "boolean", "default": True},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "capture_entity",
        "description": "Capture one creature and one view from the local Minecraft visual probe.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "creature_id": {"type": "string"},
                "entity_id": {"type": "string"},
                "view": {"type": "string", "enum": list(VIEWS)},
                "delay_ticks": {"type": "integer", "default": 50},
                "gui_clear_ticks": {"type": "integer", "default": DEFAULT_GUI_CLEAR_TICKS},
            },
            "required": ["creature_id", "view"],
            "additionalProperties": False,
        },
    },
    {
        "name": "capture_batch",
        "description": "Capture all three Minecraft views for all or selected entities. Use scope=all_entities to use the full entity validation manifest.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "scope": {"type": "string", "enum": ["greek_creatures", "all_entities"], "default": "greek_creatures"},
                "creature_ids": {"type": "array", "items": {"type": "string"}},
                "visual_gate_filter": {"type": "array", "items": {"type": "string"}},
                "delay_ticks": {"type": "integer", "default": 50},
                "gui_clear_ticks": {"type": "integer", "default": DEFAULT_GUI_CLEAR_TICKS},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "capture_motion_sequence",
        "description": "Capture a menu-safe same-camera Minecraft screenshot sequence for one creature motion review.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "creature_id": {"type": "string"},
                "entity_id": {"type": "string"},
                "view": {"type": "string", "enum": list(VIEWS), "default": "three_quarter"},
                "motion_mode": {"type": "string", "default": "idle"},
                "delay_ticks": {"type": "integer", "default": 50},
                "gui_clear_ticks": {"type": "integer", "default": DEFAULT_GUI_CLEAR_TICKS},
                "frame_count": {"type": "integer", "default": 8},
                "frame_interval_ticks": {"type": "integer", "default": 8},
                "summon_nbt": {"type": "string"},
            },
            "required": ["creature_id"],
            "additionalProperties": False,
        },
    },
    {
        "name": "run_runtime_nbt_validation",
        "description": "Run the in-game /mythicage_validate nbt command and read the generated runtime NBT report.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "timeout_seconds": {"type": "number", "default": 12},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "run_runtime_nbt_restart_validation",
        "description": "Run the in-game /mythicage_validate nbt_restart semantic round-trip command and read the generated report.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "timeout_seconds": {"type": "number", "default": 12},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "run_runtime_nbt_restart_prepare",
        "description": "Prepare fixed world objects for a true save/restart NBT cycle, flush the world save, and read the prepare report.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "timeout_seconds": {"type": "number", "default": 20},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "run_runtime_nbt_restart_verify",
        "description": "After restarting the same world, verify fixed NBT probe objects and read the true restart-cycle report.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "timeout_seconds": {"type": "number", "default": 20},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "run_runtime_god_power_playtest",
        "description": "Run the in-game runtime smoke playtest for all 52 non-temple god power spellbooks and read the generated report.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "timeout_seconds": {"type": "number", "default": 120},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "read_latest_log",
        "description": "Read the latest Minecraft or MCP runClient log tail.",
        "inputSchema": {
            "type": "object",
            "properties": {"path": {"type": "string"}, "tail": {"type": "integer", "default": 160}},
            "additionalProperties": False,
        },
    },
    {
        "name": "shutdown_probe",
        "description": "Stop the localhost visual probe HTTP server inside the running client.",
        "inputSchema": {
            "type": "object",
            "properties": {"port": {"type": "integer", "default": 47620}},
            "additionalProperties": False,
        },
    },
    {
        "name": "quit_client",
        "description": "Ask the running local Minecraft client to quit, then wait for the runClient process to exit.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "timeout_seconds": {"type": "number", "default": 60},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "run_true_nbt_restart_cycle",
        "description": "Run prepare, quit the client, restart the same world, run verify, and require distinct process ids in the two NBT reports.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "port": {"type": "integer", "default": 47620},
                "world_name": {"type": "string", "default": "MythicAgeNbtRestartProbe"},
                "launch_timeout_seconds": {"type": "number", "default": 180},
                "probe_timeout_seconds": {"type": "number", "default": 30},
                "quit_timeout_seconds": {"type": "number", "default": 90},
                "restart_pause_seconds": {"type": "number", "default": 3},
            },
            "additionalProperties": False,
        },
    },
]


def handle(message: dict[str, Any]) -> dict[str, Any] | None:
    if "id" not in message:
        return None
    method = message.get("method")
    if method == "initialize":
        result = {
            "protocolVersion": "2024-11-05",
            "capabilities": {"tools": {}},
            "serverInfo": {"name": "mythic-age-mc-visual-probe", "version": "1.0.0"},
        }
    elif method == "tools/list":
        result = {"tools": TOOLS}
    elif method == "tools/call":
        params = message.get("params", {})
        result = call_tool(str(params.get("name", "")), params.get("arguments", {}) or {})
    else:
        return {
            "jsonrpc": "2.0",
            "id": message["id"],
            "error": {"code": -32601, "message": f"Unknown method: {method}"},
        }
    return {"jsonrpc": "2.0", "id": message["id"], "result": result}


def main() -> int:
    for line in sys.stdin:
        if not line.strip():
            continue
        try:
            response = handle(json.loads(line))
        except Exception as exc:
            response = {
                "jsonrpc": "2.0",
                "id": None,
                "error": {"code": -32603, "message": str(exc)},
            }
        if response is not None:
            print(json.dumps(response, ensure_ascii=False), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
