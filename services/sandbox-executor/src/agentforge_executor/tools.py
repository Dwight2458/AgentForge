from __future__ import annotations

import asyncio
from pathlib import Path
from typing import Any

from .models import ToolResult


class ToolError(RuntimeError):
    pass


class ToolExecutor:
    def __init__(self, workspace: Path, max_output_bytes: int, default_timeout_seconds: int) -> None:
        self.workspace = workspace.resolve()
        self.workspace.mkdir(parents=True, exist_ok=True)
        self.max_output_bytes = max_output_bytes
        self.default_timeout_seconds = default_timeout_seconds

    async def execute(self, tool: str, arguments: dict[str, Any]) -> ToolResult:
        handlers = {
            "filesystem.read": self._read,
            "filesystem.write": self._write,
            "shell.execute": self._shell,
            "git.status": self._git_status,
            "git.diff": self._git_diff,
            "test.run": self._test_run,
        }
        handler = handlers.get(tool)
        if handler is None:
            raise ToolError(f"Unsupported tool: {tool}")
        return await handler(arguments)

    async def _read(self, arguments: dict[str, Any]) -> ToolResult:
        path = self._resolve(arguments.get("path"))
        if not path.is_file():
            raise ToolError(f"File does not exist: {path.relative_to(self.workspace)}")
        data = await asyncio.to_thread(path.read_bytes)
        return ToolResult(output=self._decode(data))

    async def _write(self, arguments: dict[str, Any]) -> ToolResult:
        path = self._resolve(arguments.get("path"))
        content = arguments.get("content")
        if not isinstance(content, str):
            raise ToolError("filesystem.write requires string argument 'content'")
        await asyncio.to_thread(path.parent.mkdir, parents=True, exist_ok=True)
        await asyncio.to_thread(path.write_text, content, encoding="utf-8")
        return ToolResult(output=f"Wrote {len(content.encode('utf-8'))} bytes to {path.relative_to(self.workspace)}")

    async def _shell(self, arguments: dict[str, Any]) -> ToolResult:
        argv = arguments.get("argv")
        if not isinstance(argv, list) or not argv or not all(isinstance(item, str) and item for item in argv):
            raise ToolError("shell.execute requires a non-empty string array argument 'argv'")
        cwd = self._resolve(arguments.get("cwd", "."))
        if not cwd.is_dir():
            raise ToolError(f"Working directory does not exist: {cwd.relative_to(self.workspace)}")
        timeout = int(arguments.get("timeout_seconds", self.default_timeout_seconds))
        timeout = max(1, min(timeout, 3_600))

        process = await asyncio.create_subprocess_exec(
            *argv,
            cwd=cwd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
        )
        try:
            output, _ = await asyncio.wait_for(process.communicate(), timeout=timeout)
        except TimeoutError:
            process.kill()
            await process.wait()
            raise
        return ToolResult(exit_code=process.returncode or 0, output=self._decode(output))

    async def _git_status(self, arguments: dict[str, Any]) -> ToolResult:
        return await self._shell({"argv": ["git", "status", "--short", "--branch"], **arguments})

    async def _git_diff(self, arguments: dict[str, Any]) -> ToolResult:
        return await self._shell({"argv": ["git", "diff", "--no-ext-diff", "--"], **arguments})

    async def _test_run(self, arguments: dict[str, Any]) -> ToolResult:
        argv = arguments.get("argv")
        if not argv:
            raise ToolError("test.run requires the project-approved test command in 'argv'")
        return await self._shell(arguments)

    def _resolve(self, raw_path: Any) -> Path:
        if not isinstance(raw_path, str) or not raw_path:
            raise ToolError("A non-empty path is required")
        candidate = (self.workspace / raw_path).resolve()
        if not candidate.is_relative_to(self.workspace):
            raise ToolError("Path escapes the workspace")
        return candidate

    def _decode(self, data: bytes) -> str:
        clipped = data[: self.max_output_bytes]
        output = clipped.decode("utf-8", errors="replace")
        if len(data) > self.max_output_bytes:
            output += f"\n[output truncated after {self.max_output_bytes} bytes]"
        return output

