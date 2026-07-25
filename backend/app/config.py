"""Environment-based configuration for the focused competition backend."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    app_name: str = "KinVoice"
    environment: str = "development"
    docs_enabled: bool = True
    allowed_origins: str = "http://localhost:3000,http://localhost:5173,http://127.0.0.1:4173"
    app_access_token: str = ""

    llm_api_key: str = ""
    llm_api_base: str = ""
    llm_model: str = ""
    llm_timeout_seconds: float = 35.0

    log_level: str = "INFO"

    @property
    def llm_configured(self) -> bool:
        values = (self.llm_api_key, self.llm_api_base, self.llm_model)
        return all(value.strip() and "your-" not in value.lower() for value in values)


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
