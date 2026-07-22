from functools import lru_cache

from pydantic import AnyHttpUrl, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AGENTFORGE_", extra="ignore")

    service_name: str = "agentforge-runtime"
    control_plane_url: AnyHttpUrl = "http://localhost:8080"
    executor_url: AnyHttpUrl = "http://localhost:8082"
    executor_token: str = "development-only"
    checkpoint_database_url: str | None = None
    setup_checkpoint_schema: bool = False
    model: str | None = None
    max_grill_questions: int = Field(default=3, ge=1, le=6)


@lru_cache
def get_settings() -> Settings:
    return Settings()
