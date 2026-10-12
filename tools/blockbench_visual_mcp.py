#!/usr/bin/env python3
"""Small MCP server for the Greek creature Blockbench visual gate."""

from __future__ import annotations

import contextlib
import io
import json
import sys
from pathlib import Path
from typing import Any

import greek_creature_visual_pipeline as pipeline


ROOT = Path(__file__).resolve().parents[1]


def as_project_path(path: Path) -> str:
    return str(path.resolve().relative_to(ROOT))


def text_result(payload: Any, is_error: bool = False) -> dict[str, Any]:
    return {
        "content": [{"type": "text", "text": json.dumps(payload, indent=2, ensure_ascii=False)}],
        "isError": is_error,
    }


def call_tool(name: str, args: dict[str, Any]) -> dict[str, Any]:
    if name == "generate_design_gate_assets":
        with contextlib.redirect_stdout(io.StringIO()) as output:
            pipeline.generate_all()
            code = pipeline.audit()
        return text_result({
            "ok": code == 0,
            "output": output.getvalue(),
            "manifest": as_project_path(pipeline.MANIFEST_PATH),
            "visual_report": as_project_path(pipeline.REPORT_PATH),
            "texture_fit_report": as_project_path(pipeline.TEXTURE_FIT_REPORT_PATH),
            "contact_sheet": as_project_path(pipeline.CONTACT_SHEET_PATH),
            "design_dir": as_project_path(pipeline.DESIGN_DIR),
            "blockbench_dir": as_project_path(pipeline.BLOCKBENCH_DIR),
            "preview_dir": as_project_path(pipeline.PREVIEW_DIR),
        }, code != 0)
    if name == "audit_design_gate":
        with contextlib.redirect_stdout(io.StringIO()) as output:
            code = pipeline.audit()
        return text_result({"ok": code == 0, "output": output.getvalue()}, code != 0)
    if name == "list_creatures":
        creatures = [
            {
                "id": creature["id"],
                "entity_id": creature["entity_id"],
                "design_doc": as_project_path(pipeline.DESIGN_DIR / f"{creature['id']}.md"),
                "bbmodel": as_project_path(pipeline.BLOCKBENCH_DIR / f"{creature['id']}.bbmodel"),
                "uv_template": as_project_path(pipeline.PREVIEW_DIR / creature["id"] / "uv_template.png"),
                "texture_fit_preview": as_project_path(pipeline.PREVIEW_DIR / creature["id"] / "texture_fit.png"),
                "blockbench_previews": {
                    view: as_project_path(pipeline.PREVIEW_DIR / creature["id"] / "blockbench" / f"{view}.png")
                    for view in pipeline.VIEWS
                },
            }
            for creature in pipeline.CREATURES
        ]
        return text_result({"creatures": creatures})
    if name == "get_creature_design":
        creature_id = str(args.get("creature_id", ""))
        path = pipeline.DESIGN_DIR / f"{creature_id}.md"
        if not path.exists():
            return text_result({"ok": False, "error": f"Unknown creature or missing design doc: {creature_id}"}, True)
        return {
            "content": [{"type": "text", "text": path.read_text(encoding="utf-8")}],
            "isError": False,
        }
    return text_result({"ok": False, "error": f"Unknown tool: {name}"}, True)


TOOLS = [
    {
        "name": "generate_design_gate_assets",
        "description": "Generate the 9 Greek creature design docs, bbmodels, UV templates, textured Blockbench previews, texture-fit overlays, and reports.",
        "inputSchema": {"type": "object", "properties": {}, "additionalProperties": False},
    },
    {
        "name": "audit_design_gate",
        "description": "Audit that each creature has a complete design package, bbmodel, UV template, and Blockbench previews.",
        "inputSchema": {"type": "object", "properties": {}, "additionalProperties": False},
    },
    {
        "name": "list_creatures",
        "description": "List creature design artifacts and preview paths.",
        "inputSchema": {"type": "object", "properties": {}, "additionalProperties": False},
    },
    {
        "name": "get_creature_design",
        "description": "Read one creature design package.",
        "inputSchema": {
            "type": "object",
            "properties": {"creature_id": {"type": "string"}},
            "required": ["creature_id"],
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
            "serverInfo": {"name": "mythic-age-blockbench-visual-gate", "version": "1.0.0"},
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
