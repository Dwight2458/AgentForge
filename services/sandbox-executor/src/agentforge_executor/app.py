import asyncio
import hmac
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, HTTPException, Request, status
from fastapi.responses import StreamingResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .config import Settings, get_settings
from .models import Invocation, InvocationRequest, InvocationStatus
from .store import InvocationStore
from .tools import ToolExecutor


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    settings = get_settings()
    executor = ToolExecutor(
        workspace=settings.workspace,
        max_output_bytes=settings.max_output_bytes,
        default_timeout_seconds=settings.default_timeout_seconds,
    )
    app.state.store = InvocationStore(settings.workspace.resolve(), executor)
    yield


app = FastAPI(title="AgentForge Sandbox Executor", version="0.1.0", lifespan=lifespan)
bearer = HTTPBearer(auto_error=False)


def authorize(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    settings: Settings = Depends(get_settings),
) -> None:
    if credentials is None or not hmac.compare_digest(credentials.credentials, settings.token):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid run token")


def store(request: Request) -> InvocationStore:
    return request.app.state.store


@app.get("/healthz")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post(
    "/internal/v1/invocations",
    response_model=Invocation,
    status_code=status.HTTP_202_ACCEPTED,
    dependencies=[Depends(authorize)],
)
async def create_invocation(payload: InvocationRequest, invocation_store: InvocationStore = Depends(store)) -> Invocation:
    try:
        invocation, _ = await invocation_store.create(payload)
        return invocation
    except ValueError as exception:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exception)) from exception


@app.get(
    "/internal/v1/invocations/{invocation_id}",
    response_model=Invocation,
    dependencies=[Depends(authorize)],
)
async def get_invocation(invocation_id: str, invocation_store: InvocationStore = Depends(store)) -> Invocation:
    invocation = await invocation_store.get(invocation_id)
    if invocation is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Invocation not found")
    return invocation


@app.get(
    "/internal/v1/invocations/{invocation_id}/events",
    dependencies=[Depends(authorize)],
)
async def stream_invocation(invocation_id: str, invocation_store: InvocationStore = Depends(store)) -> StreamingResponse:
    async def events() -> AsyncIterator[str]:
        last_payload = ""
        while True:
            invocation = await invocation_store.get(invocation_id)
            if invocation is None:
                yield "event: error\ndata: {\"detail\":\"Invocation not found\"}\n\n"
                return
            payload = invocation.model_dump_json()
            if payload != last_payload:
                yield f"event: snapshot\ndata: {payload}\n\n"
                last_payload = payload
            if invocation.status in {
                InvocationStatus.SUCCEEDED,
                InvocationStatus.FAILED,
                InvocationStatus.TIMED_OUT,
            }:
                return
            await asyncio.sleep(0.2)

    return StreamingResponse(events(), media_type="text/event-stream")
