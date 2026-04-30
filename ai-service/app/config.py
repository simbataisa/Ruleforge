import os

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "RuleForge AI Assistant"
    debug: bool = False
    anthropic_api_key: str = os.environ.get("ANTHROPIC_API_KEY", "")
    anthropic_model: str = os.environ.get("ANTHROPIC_MODEL", "claude-sonnet-4-6")
    management_service_url: str = os.environ.get(
        "MANAGEMENT_SERVICE_URL", "http://localhost:8082"
    )
    cors_allowed_origins: str = os.environ.get(
        "CORS_ALLOWED_ORIGINS", "http://localhost:3000,http://127.0.0.1:3000"
    )
    cors_allow_credentials: bool = True

    @property
    def cors_origins(self) -> list[str]:
        origins = [
            origin.strip()
            for origin in self.cors_allowed_origins.split(",")
            if origin.strip()
        ]
        if self.cors_allow_credentials and "*" in origins:
            raise ValueError(
                "Wildcard CORS origins cannot be used when credentials are enabled"
            )
        return origins

    class Config:
        env_prefix = "RULEFORGE_AI_"


settings = Settings()
