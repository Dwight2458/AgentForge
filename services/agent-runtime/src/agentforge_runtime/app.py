from contextlib import asynccontextmanager
from typing import AsyncIterator

from fastapi import FastAPI, Request
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver

from .config import get_settings
from .graph import build_execution_graph, initial_state, to_result
from .grill import generate_grill_questions
from .model_gateway import ModelRequest, ModelResponse, create_model_gateway
from .models import GrillRequest, GrillResponse, RunRequest, RunResult


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    settings = get_settings()
    model_gateway = create_model_gateway(settings)
    app.state.model_gateway = model_gateway
    try:
        if settings.checkpoint_database_url:
            async with AsyncPostgresSaver.from_conn_string(settings.checkpoint_database_url) as checkpointer:
                if settings.setup_checkpoint_schema:
                    await checkpointer.setup()
                app.state.execution_graph = build_execution_graph(checkpointer, model_gateway)
                yield
        else:
            app.state.execution_graph = build_execution_graph(InMemorySaver(), model_gateway)
            yield
    finally:
        await model_gateway.aclose()


app = FastAPI(
    title="AgentForge Agent Runtime",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/healthz")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/internal/v1/grill/questions", response_model=GrillResponse)
async def grill(request: GrillRequest) -> GrillResponse:
    return generate_grill_questions(request, get_settings().max_grill_questions)


@app.post("/internal/v1/runs/execute", response_model=RunResult)
async def execute_run(payload: RunRequest, request: Request) -> RunResult:
    config = {"configurable": {"thread_id": payload.run_id}}
    state = await request.app.state.execution_graph.ainvoke(initial_state(payload), config=config)
    return to_result(state)


@app.post("/internal/v1/model/complete", response_model=ModelResponse)
async def complete_model(payload: ModelRequest, request: Request) -> ModelResponse:
    return await request.app.state.model_gateway.complete(payload)
