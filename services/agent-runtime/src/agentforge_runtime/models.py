from enum import StrEnum
from typing import Any

from pydantic import BaseModel, Field


class RunPhase(StrEnum):
    ANALYZE = "ANALYZE"
    PLAN = "PLAN"
    IMPLEMENT = "IMPLEMENT"
    VERIFY = "VERIFY"
    REPAIR = "REPAIR"
    DELIVER = "DELIVER"
    FAILED = "FAILED"


class RequirementSpec(BaseModel):
    goal: str = Field(min_length=1, max_length=8_000)
    acceptance_criteria: list[str] = Field(min_length=1)
    constraints: list[str] = Field(default_factory=list)
    test_plan: list[str] = Field(min_length=1)
    preview_required: bool = False


class PlanStep(BaseModel):
    id: str
    title: str
    agent: str
    depends_on: list[str] = Field(default_factory=list)
    verification: list[str] = Field(default_factory=list)


class ExecutionPlan(BaseModel):
    summary: str
    steps: list[PlanStep]


class Evidence(BaseModel):
    criterion: str
    passed: bool
    source: str
    detail: str


class RunRequest(BaseModel):
    run_id: str
    requirement_spec: RequirementSpec
    max_repair_rounds: int = Field(default=3, ge=0, le=10)
    force_verification_failures: int = Field(default=0, ge=0, le=10)


class RunResult(BaseModel):
    run_id: str
    status: str
    plan: ExecutionPlan
    evidence: list[Evidence]
    repair_round: int
    actions: list[str]
    unresolved_issues: list[str]
    model_calls: int = 0
    prompt_tokens: int = 0
    completion_tokens: int = 0


class GrillRequest(BaseModel):
    request: str = Field(min_length=1, max_length=8_000)
    known_context: dict[str, Any] = Field(default_factory=dict)


class GrillResponse(BaseModel):
    questions: list[str]
