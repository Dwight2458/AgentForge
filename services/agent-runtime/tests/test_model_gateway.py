import json

import httpx
import pytest

from agentforge_runtime.model_gateway import (
    FakeModelGateway,
    FunctionTool,
    ModelMessage,
    ModelRequest,
    OpenAICompatibleModelGateway,
)


def planning_request() -> ModelRequest:
    return ModelRequest(
        messages=[
            ModelMessage(
                role="user",
                content=json.dumps(
                    {
                        "goal": "Add payment filtering",
                        "test_plan": ["pytest", "playwright test"],
                    }
                ),
            )
        ],
        tools=[
            FunctionTool(
                name="submit_execution_plan",
                description="Submit a plan",
                parameters={"type": "object", "properties": {}},
            )
        ],
        tool_choice="submit_execution_plan",
        temperature=0,
    )


@pytest.mark.asyncio
async def test_fake_gateway_returns_deterministic_function_call() -> None:
    first = await FakeModelGateway().complete(planning_request())
    second = await FakeModelGateway().complete(planning_request())

    assert first == second
    assert first.finish_reason == "tool_calls"
    assert first.tool_calls[0].name == "submit_execution_plan"
    assert first.tool_calls[0].arguments["steps"][2]["verification"] == [
        "pytest",
        "playwright test",
    ]


@pytest.mark.asyncio
async def test_openai_compatible_gateway_maps_chat_completion_contract() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["authorization"] = request.headers["Authorization"]
        captured["body"] = json.loads(request.content)
        return httpx.Response(
            200,
            json={
                "id": "chatcmpl-test",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": None,
                            "tool_calls": [
                                {
                                    "id": "call-1",
                                    "type": "function",
                                    "function": {
                                        "name": "submit_execution_plan",
                                        "arguments": json.dumps(
                                            {
                                                "summary": "Plan",
                                                "steps": [],
                                            }
                                        ),
                                    },
                                }
                            ],
                        },
                        "finish_reason": "tool_calls",
                    }
                ],
                "usage": {
                    "prompt_tokens": 12,
                    "completion_tokens": 7,
                    "total_tokens": 19,
                },
            },
        )

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        gateway = OpenAICompatibleModelGateway(
            base_url="https://models.example.test/v1/",
            api_key="test-key",
            model="test-model",
            timeout_seconds=5,
            client=client,
        )
        response = await gateway.complete(planning_request())

    assert captured["authorization"] == "Bearer test-key"
    body = captured["body"]
    assert isinstance(body, dict)
    assert body["model"] == "test-model"
    assert body["tool_choice"] == {
        "type": "function",
        "function": {"name": "submit_execution_plan"},
    }
    assert body["tools"][0]["function"]["name"] == "submit_execution_plan"
    assert response.tool_calls[0].arguments == {"summary": "Plan", "steps": []}
    assert response.usage.total_tokens == 19
