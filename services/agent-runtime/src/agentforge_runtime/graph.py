from __future__ import annotations

from typing import Annotated, Any, TypedDict

from langgraph.graph import END, START, StateGraph
from typing_extensions import NotRequired

from .models import Evidence, ExecutionPlan, PlanStep, RequirementSpec, RunPhase, RunRequest, RunResult


def append_items(left: list[str], right: list[str]) -> list[str]:
    return [*left, *right]


class AgentRunState(TypedDict):
    run_id: str
    requirement_spec: dict[str, Any]
    max_repair_rounds: int
    force_verification_failures: int
    phase: RunPhase
    plan: NotRequired[dict[str, Any]]
    evidence: NotRequired[list[dict[str, Any]]]
    repair_round: int
    actions: Annotated[list[str], append_items]
    unresolved_issues: NotRequired[list[str]]
    status: NotRequired[str]


def analyze(state: AgentRunState) -> dict[str, Any]:
    spec = RequirementSpec.model_validate(state["requirement_spec"])
    return {
        "phase": RunPhase.PLAN,
        "actions": [f"Analyzed requirement: {spec.goal}"],
    }


def plan(state: AgentRunState) -> dict[str, Any]:
    spec = RequirementSpec.model_validate(state["requirement_spec"])
    execution_plan = ExecutionPlan(
        summary=f"Implement and verify: {spec.goal}",
        steps=[
            PlanStep(id="analyze", title="Inspect repository", agent="RepositoryAnalyst"),
            PlanStep(
                id="implement",
                title="Implement requested behavior",
                agent="Developer",
                depends_on=["analyze"],
            ),
            PlanStep(
                id="verify",
                title="Run acceptance checks",
                agent="TestEngineer",
                depends_on=["implement"],
                verification=spec.test_plan,
            ),
        ],
    )
    return {
        "phase": RunPhase.IMPLEMENT,
        "plan": execution_plan.model_dump(mode="json"),
        "actions": ["Created dependency-aware execution plan"],
    }


def implement(state: AgentRunState) -> dict[str, Any]:
    action = "Executed implementation tools"
    if state["repair_round"]:
        action = f"Executed targeted repair round {state['repair_round']}"
    return {"phase": RunPhase.VERIFY, "actions": [action]}


def verify(state: AgentRunState) -> dict[str, Any]:
    spec = RequirementSpec.model_validate(state["requirement_spec"])
    should_fail = state["repair_round"] < state["force_verification_failures"]
    evidence = [
        Evidence(
            criterion=criterion,
            passed=not should_fail,
            source="verification-command",
            detail="Recorded command evidence" if not should_fail else "Verification command failed",
        ).model_dump(mode="json")
        for criterion in spec.acceptance_criteria
    ]
    return {
        "evidence": evidence,
        "actions": ["Mapped verification evidence to acceptance criteria"],
    }


def route_after_verify(state: AgentRunState) -> str:
    if all(item["passed"] for item in state["evidence"]):
        return "deliver"
    if state["repair_round"] < state["max_repair_rounds"]:
        return "repair"
    return "fail"


def repair(state: AgentRunState) -> dict[str, Any]:
    next_round = state["repair_round"] + 1
    return {
        "phase": RunPhase.REPAIR,
        "repair_round": next_round,
        "actions": [f"Diagnosed failed evidence for repair round {next_round}"],
    }


def deliver(state: AgentRunState) -> dict[str, Any]:
    return {
        "phase": RunPhase.DELIVER,
        "status": "SUCCEEDED",
        "unresolved_issues": [],
        "actions": ["Prepared verified delivery"],
    }


def fail(state: AgentRunState) -> dict[str, Any]:
    failed = [item["criterion"] for item in state["evidence"] if not item["passed"]]
    return {
        "phase": RunPhase.FAILED,
        "status": "FAILED",
        "unresolved_issues": failed,
        "actions": ["Repair budget exhausted; prepared failure patch"],
    }


def build_execution_graph(checkpointer: Any = None):
    builder = StateGraph(AgentRunState)
    builder.add_node("analyze", analyze)
    builder.add_node("plan", plan)
    builder.add_node("implement", implement)
    builder.add_node("verify", verify)
    builder.add_node("repair", repair)
    builder.add_node("deliver", deliver)
    builder.add_node("fail", fail)

    builder.add_edge(START, "analyze")
    builder.add_edge("analyze", "plan")
    builder.add_edge("plan", "implement")
    builder.add_edge("implement", "verify")
    builder.add_conditional_edges(
        "verify",
        route_after_verify,
        {"deliver": "deliver", "repair": "repair", "fail": "fail"},
    )
    builder.add_edge("repair", "implement")
    builder.add_edge("deliver", END)
    builder.add_edge("fail", END)
    return builder.compile(checkpointer=checkpointer)


def initial_state(request: RunRequest) -> AgentRunState:
    return {
        "run_id": request.run_id,
        "requirement_spec": request.requirement_spec.model_dump(mode="json"),
        "max_repair_rounds": request.max_repair_rounds,
        "force_verification_failures": request.force_verification_failures,
        "phase": RunPhase.ANALYZE,
        "repair_round": 0,
        "actions": [],
    }


def to_result(state: AgentRunState) -> RunResult:
    return RunResult(
        run_id=state["run_id"],
        status=state["status"],
        plan=ExecutionPlan.model_validate(state["plan"]),
        evidence=[Evidence.model_validate(item) for item in state["evidence"]],
        repair_round=state["repair_round"],
        actions=state["actions"],
        unresolved_issues=state["unresolved_issues"],
    )

