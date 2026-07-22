from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AGENTFORGE_EXECUTOR_", extra="ignore")

    workspace: Path = Path("/workspace")
    token: str = "development-only"
    max_output_bytes: int = Field(default=1_000_000, ge=1_024)
    default_timeout_seconds: int = Field(default=300, ge=1, le=3_600)


@lru_cache
def get_settings() -> Settings:
    return Settings()

