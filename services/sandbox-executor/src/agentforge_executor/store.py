from __future__ import annotations

import asyncio
import hashlib
import os
from datetime import UTC, datetime
from pathlib import Path

from .models import Invocation, InvocationRequest, InvocationStatus
from .tools import ToolExecutor


class InvocationStore:
    def __init__(self, workspace: Path, executor: ToolExecutor) -> None:
        self.directory = workspace / ".agentforge" / "invocations"
        self.directory.mkdir(parents=True, exist_ok=True)
        self.executor = executor
        self.lock = asyncio.Lock()
        self.io_lock = asyncio.Lock()
        self.tasks: set[asyncio.Task[None]] = set()

    async def create(self, request: InvocationRequest) -> tuple[Invocation, bool]:
        async with self.lock:
            existing = await self.get_by_key(request.idempotency_key)
            if existing:
                if existing.tool != request.tool or existing.arguments != request.arguments:
                    raise ValueError("Idempotency key was already used with a different invocation")
                return existing, False

            now = datetime.now(UTC)
            invocation = Invocation(
                id=hashlib.sha256(request.idempotency_key.encode()).hexdigest()[:24],
                idempotency_key=request.idempotency_key,
                tool=request.tool,
                arguments=request.arguments,
                status=InvocationStatus.QUEUED,
                created_at=now,
            )
            await self._write(invocation)
            task = asyncio.create_task(self._execute(invocation))
            self.tasks.add(task)
            task.add_done_callback(self.tasks.discard)
            return invocation, True

    async def get(self, invocation_id: str) -> Invocation | None:
        path = self.directory / f"{invocation_id}.json"
        async with self.io_lock:
            if not path.is_file():
                return None
            data = await asyncio.to_thread(path.read_text, encoding="utf-8")
        return Invocation.model_validate_json(data)

    async def get_by_key(self, key: str) -> Invocation | None:
        invocation_id = hashlib.sha256(key.encode()).hexdigest()[:24]
        return await self.get(invocation_id)

    async def _execute(self, invocation: Invocation) -> None:
        invocation.status = InvocationStatus.RUNNING
        invocation.started_at = datetime.now(UTC)
        await self._write(invocation)
        try:
            result = await self.executor.execute(invocation.tool, invocation.arguments)
            invocation.exit_code = result.exit_code
            invocation.output = result.output
            invocation.status = InvocationStatus.SUCCEEDED if result.exit_code == 0 else InvocationStatus.FAILED
            if result.exit_code != 0:
                invocation.error = f"Tool exited with code {result.exit_code}"
        except TimeoutError:
            invocation.status = InvocationStatus.TIMED_OUT
            invocation.error = "Tool execution timed out"
        except Exception as exception:  # The API must return structured tool failure.
            invocation.status = InvocationStatus.FAILED
            invocation.error = str(exception)
        finally:
            invocation.finished_at = datetime.now(UTC)
            await self._write(invocation)

    async def _write(self, invocation: Invocation) -> None:
        target = self.directory / f"{invocation.id}.json"
        temporary = target.with_suffix(".tmp")
        data = invocation.model_dump_json(indent=2)
        async with self.io_lock:
            await asyncio.to_thread(temporary.write_text, data, encoding="utf-8")
            await asyncio.to_thread(os.replace, temporary, target)
