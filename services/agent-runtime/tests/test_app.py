from fastapi.testclient import TestClient

from agentforge_runtime.app import app


def test_health_and_execution_endpoint() -> None:
    with TestClient(app) as client:
        assert client.get("/healthz").json() == {"status": "ok"}
        response = client.post(
            "/internal/v1/runs/execute",
            json={
                "run_id": "run-api-test",
                "requirement_spec": {
                    "goal": "Change code",
                    "acceptance_criteria": ["Tests pass"],
                    "constraints": [],
                    "test_plan": ["pytest"],
                },
            },
        )

    assert response.status_code == 200
    assert response.json()["status"] == "SUCCEEDED"
