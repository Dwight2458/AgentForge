import asyncio
from pathlib import Path

import pytest

from agentforge_executor.models import InvocationRequest, InvocationStatus
from agentforge_executor.store import InvocationStore
from agentforge_executor.tools import ToolExecutor


async def wait_for_result(store: InvocationStore, invocation_id: str):
    for _ in range(100):
        invocation = await store.get(invocation_id)
        if invocation and invocation.status not in {InvocationStatus.QUEUED, InvocationStatus.RUNNING}:
            return invocation
        await asyncio.sleep(0.02)
    raise AssertionError("Invocation did not finish")


def test_idempotency_key_returns_same_completed_invocation(tmp_path: Path) -> None:
    async def scenario():
        store = InvocationStore(tmp_path, ToolExecutor(tmp_path, 10_000, 5))
        request = InvocationRequest(
            idempotency_key="run-1:step-1:tool-1",
            tool="filesystem.write",
            arguments={"path": "result.txt", "content": "once"},
        )
        created, is_new = await store.create(request)
        completed = await wait_for_result(store, created.id)
        replayed, replay_is_new = await store.create(request)
        return is_new, completed, replay_is_new, replayed

    is_new, completed, replay_is_new, replayed = asyncio.run(scenario())
    assert is_new is True
    assert replay_is_new is False
    assert replayed.id == completed.id
    assert replayed.status == InvocationStatus.SUCCEEDED


def test_idempotency_key_rejects_different_payload(tmp_path: Path) -> None:
    async def scenario():
        store = InvocationStore(tmp_path, ToolExecutor(tmp_path, 10_000, 5))
        first = InvocationRequest(
            idempotency_key="run-1:step-1:tool-2",
            tool="filesystem.write",
            arguments={"path": "result.txt", "content": "first"},
        )
        await store.create(first)
        await store.create(first.model_copy(update={"arguments": {"path": "result.txt", "content": "second"}}))

    with pytest.raises(ValueError, match="different invocation"):
        asyncio.run(scenario())
