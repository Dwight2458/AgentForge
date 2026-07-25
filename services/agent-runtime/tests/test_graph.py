import asyncio

from agentforge_runtime.graph import build_execution_graph, initial_state, to_result
from agentforge_runtime.model_gateway import FakeModelGateway
from agentforge_runtime.models import RequirementSpec, RunRequest


def request(failures: int, repairs: int = 3) -> RunRequest:
    return RunRequest(
        run_id=f"run-{failures}-{repairs}",
        requirement_spec=RequirementSpec(
            goal="Add payment status filtering",
            acceptance_criteria=["API accepts paymentStatus", "UI persists status in URL"],
            constraints=["Keep response shape compatible"],
            test_plan=["Run backend tests", "Run Playwright"],
        ),
        force_verification_failures=failures,
        max_repair_rounds=repairs,
    )


def test_succeeds_after_bounded_repair() -> None:
    graph = build_execution_graph()
    payload = request(failures=2)
    state = asyncio.run(graph.ainvoke(initial_state(payload)))
    result = to_result(state)

    assert result.status == "SUCCEEDED"
    assert result.repair_round == 2
    assert all(item.passed for item in result.evidence)


def test_fails_when_repair_budget_is_exhausted() -> None:
    graph = build_execution_graph()
    payload = request(failures=4, repairs=2)
    state = asyncio.run(graph.ainvoke(initial_state(payload)))
    result = to_result(state)

    assert result.status == "FAILED"
    assert result.repair_round == 2
    assert result.unresolved_issues == ["API accepts paymentStatus", "UI persists status in URL"]


def test_fake_model_drives_function_called_execution_plan() -> None:
    gateway = FakeModelGateway()
    graph = build_execution_graph(model_gateway=gateway)

    state = asyncio.run(graph.ainvoke(initial_state(request(failures=0))))
    result = to_result(state)

    assert len(gateway.requests) == 1
    assert gateway.requests[0].tool_choice == "submit_execution_plan"
    assert result.plan.steps[2].verification == ["Run backend tests", "Run Playwright"]
    assert result.model_calls == 1
    assert result.prompt_tokens > 0
    assert result.completion_tokens == 40
