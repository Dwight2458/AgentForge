import asyncio
from pathlib import Path

import pytest

from agentforge_executor.tools import ToolError, ToolExecutor


def executor(tmp_path: Path) -> ToolExecutor:
    return ToolExecutor(tmp_path, max_output_bytes=10_000, default_timeout_seconds=5)


def test_file_tools_are_confined_to_workspace(tmp_path: Path) -> None:
    tool_executor = executor(tmp_path)
    asyncio.run(tool_executor.execute("filesystem.write", {"path": "src/demo.txt", "content": "hello"}))
    result = asyncio.run(tool_executor.execute("filesystem.read", {"path": "src/demo.txt"}))

    assert result.output == "hello"
    with pytest.raises(ToolError, match="escapes the workspace"):
        asyncio.run(tool_executor.execute("filesystem.read", {"path": "../secret.txt"}))


def test_shell_uses_argv_and_captures_output(tmp_path: Path) -> None:
    result = asyncio.run(
        executor(tmp_path).execute(
            "shell.execute",
            {"argv": ["python", "-c", "print('executor-ready')"]},
        )
    )

    assert result.exit_code == 0
    assert result.output.strip() == "executor-ready"

