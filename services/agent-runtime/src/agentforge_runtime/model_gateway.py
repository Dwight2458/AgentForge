from __future__ import annotations

import json
from collections.abc import Sequence
from typing import Any, Literal, Protocol

import httpx
from pydantic import BaseModel, Field, ValidationError

from .config import Settings


class ModelMessage(BaseModel):
    role: Literal["system", "user", "assistant", "tool"]
    content: str | None = None
    tool_call_id: str | None = None


class FunctionTool(BaseModel):
    name: str = Field(min_length=1)
    description: str = ""
    parameters: dict[str, Any]


class ModelRequest(BaseModel):
    messages: list[ModelMessage] = Field(min_length=1)
    tools: list[FunctionTool] = Field(default_factory=list)
    tool_choice: str | None = None
    temperature: float | None = None


class ToolCall(BaseModel):
    id: str
    name: str
    arguments: dict[str, Any]


class ModelUsage(BaseModel):
    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0


class ModelResponse(BaseModel):
    content: str | None = None
    tool_calls: list[ToolCall] = Field(default_factory=list)
    finish_reason: str
    usage: ModelUsage = Field(default_factory=ModelUsage)


class ModelGatewayError(RuntimeError):
    pass


class ModelGateway(Protocol):
    async def complete(self, request: ModelRequest) -> ModelResponse: ...

    async def aclose(self) -> None: ...


class OpenAICompatibleModelGateway:
    def __init__(
        self,
        *,
        base_url: str,
        api_key: str,
        model: str,
        timeout_seconds: float,
        client: httpx.AsyncClient | None = None,
    ) -> None:
        if not api_key:
            raise ModelGatewayError("AGENTFORGE_MODEL_API_KEY is required for openai-compatible mode")
        self._url = f"{base_url.rstrip('/')}/chat/completions"
        self._model = model
        self._client = client or httpx.AsyncClient(timeout=timeout_seconds)
        self._owns_client = client is None
        self._headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }

    async def complete(self, request: ModelRequest) -> ModelResponse:
        body: dict[str, Any] = {
            "model": self._model,
            "messages": [
                message.model_dump(mode="json", exclude_none=True)
                for message in request.messages
            ],
        }
        if request.tools:
            body["tools"] = [
                {"type": "function", "function": tool.model_dump(mode="json")}
                for tool in request.tools
            ]
        if request.tool_choice:
            body["tool_choice"] = self._tool_choice(request.tool_choice)
        if request.temperature is not None:
            body["temperature"] = request.temperature

        try:
            response = await self._client.post(self._url, headers=self._headers, json=body)
            response.raise_for_status()
            payload = response.json()
            choice = payload["choices"][0]
            message = choice["message"]
            tool_calls = [
                ToolCall(
                    id=item["id"],
                    name=item["function"]["name"],
                    arguments=self._arguments(item["function"]["arguments"]),
                )
                for item in message.get("tool_calls", [])
            ]
            usage = ModelUsage.model_validate(payload.get("usage", {}))
            return ModelResponse(
                content=message.get("content"),
                tool_calls=tool_calls,
                finish_reason=choice["finish_reason"],
                usage=usage,
            )
        except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError, ValidationError) as exception:
            raise ModelGatewayError("OpenAI-compatible model request failed") from exception

    async def aclose(self) -> None:
        if self._owns_client:
            await self._client.aclose()

    @staticmethod
    def _tool_choice(value: str) -> str | dict[str, Any]:
        if value in {"auto", "none", "required"}:
            return value
        return {"type": "function", "function": {"name": value}}

    @staticmethod
    def _arguments(value: str | dict[str, Any]) -> dict[str, Any]:
        parsed = json.loads(value) if isinstance(value, str) else value
        if not isinstance(parsed, dict):
            raise ModelGatewayError("Tool-call arguments must be a JSON object")
        return parsed


class FakeModelGateway:
    def __init__(self, scripted_responses: Sequence[ModelResponse] | None = None) -> None:
        self.requests: list[ModelRequest] = []
        self._scripted_responses = list(scripted_responses or [])

    async def complete(self, request: ModelRequest) -> ModelResponse:
        self.requests.append(request.model_copy(deep=True))
        if self._scripted_responses:
            return self._scripted_responses.pop(0)
        if request.tool_choice == "submit_execution_plan":
            return self._execution_plan_response(request)
        last_content = request.messages[-1].content or ""
        return ModelResponse(
            content=f"fake:{last_content}",
            finish_reason="stop",
            usage=ModelUsage(
                prompt_tokens=max(1, len(last_content.split())),
                completion_tokens=1,
                total_tokens=max(2, len(last_content.split()) + 1),
            ),
        )

    async def aclose(self) -> None:
        return None

    def _execution_plan_response(self, request: ModelRequest) -> ModelResponse:
        raw_spec = request.messages[-1].content or "{}"
        try:
            spec = json.loads(raw_spec)
        except json.JSONDecodeError as exception:
            raise ModelGatewayError("Fake planner expected a JSON requirement spec") from exception
        goal = str(spec.get("goal", "requested behavior"))
        test_plan = list(spec.get("test_plan", []))
        arguments = {
            "summary": f"Implement and verify: {goal}",
            "steps": [
                {
                    "id": "analyze",
                    "title": "Inspect repository",
                    "agent": "RepositoryAnalyst",
                    "depends_on": [],
                    "verification": [],
                },
                {
                    "id": "implement",
                    "title": "Implement requested behavior",
                    "agent": "Developer",
                    "depends_on": ["analyze"],
                    "verification": [],
                },
                {
                    "id": "verify",
                    "title": "Run acceptance checks",
                    "agent": "TestEngineer",
                    "depends_on": ["implement"],
                    "verification": test_plan,
                },
            ],
        }
        prompt_tokens = max(1, len(raw_spec.split()))
        return ModelResponse(
            tool_calls=[
                ToolCall(
                    id=f"fake-plan-{len(self.requests)}",
                    name="submit_execution_plan",
                    arguments=arguments,
                )
            ],
            finish_reason="tool_calls",
            usage=ModelUsage(
                prompt_tokens=prompt_tokens,
                completion_tokens=40,
                total_tokens=prompt_tokens + 40,
            ),
        )


def create_model_gateway(settings: Settings) -> ModelGateway:
    if settings.model_provider == "fake":
        return FakeModelGateway()
    return OpenAICompatibleModelGateway(
        base_url=str(settings.model_base_url),
        api_key=settings.model_api_key,
        model=settings.model,
        timeout_seconds=settings.model_timeout_seconds,
    )
