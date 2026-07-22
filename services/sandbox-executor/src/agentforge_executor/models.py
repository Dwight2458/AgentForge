from datetime import datetime
from enum import StrEnum
from typing import Any

from pydantic import BaseModel, Field


class InvocationStatus(StrEnum):
    QUEUED = "QUEUED"
    RUNNING = "RUNNING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    TIMED_OUT = "TIMED_OUT"


class InvocationRequest(BaseModel):
    idempotency_key: str = Field(min_length=8, max_length=200, pattern=r"^[a-zA-Z0-9._:-]+$")
    tool: str
    arguments: dict[str, Any] = Field(default_factory=dict)


class Invocation(BaseModel):
    id: str
    idempotency_key: str
    tool: str
    arguments: dict[str, Any]
    status: InvocationStatus
    exit_code: int | None = None
    output: str = ""
    error: str | None = None
    created_at: datetime
    started_at: datetime | None = None
    finished_at: datetime | None = None


class ToolResult(BaseModel):
    exit_code: int = 0
    output: str = ""

