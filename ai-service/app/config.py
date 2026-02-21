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

    class Config:
        env_prefix = "RULEFORGE_AI_"


settings = Settings()
