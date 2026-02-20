from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "RuleForge AI Assistant"
    debug: bool = False
    llm_api_key: str = ""
    llm_model: str = "claude-sonnet-4-5-20250929"
    management_service_url: str = "http://localhost:8082"

    class Config:
        env_prefix = "RULEFORGE_AI_"


settings = Settings()
