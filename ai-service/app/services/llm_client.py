import asyncio
import json
import logging
from typing import Any

from anthropic import AsyncAnthropic

from app.config import settings

logger = logging.getLogger(__name__)


class LLMClient:
    """Wrapper around AsyncAnthropic with retry logic and token tracking."""

    def __init__(self) -> None:
        self._client: AsyncAnthropic | None = None
        self.total_input_tokens: int = 0
        self.total_output_tokens: int = 0

    @property
    def client(self) -> AsyncAnthropic:
        if self._client is None:
            self._client = AsyncAnthropic(api_key=settings.anthropic_api_key)
        return self._client

    @property
    def is_configured(self) -> bool:
        return bool(settings.anthropic_api_key)

    async def call(
        self,
        system_prompt: str,
        user_message: str,
        max_tokens: int = 4096,
    ) -> str:
        """Send a regular completion request. Returns the text response."""
        response = await self._call_with_retry(
            system=system_prompt,
            messages=[{"role": "user", "content": user_message}],
            max_tokens=max_tokens,
        )
        self._track_usage(response)
        return response.content[0].text

    async def call_with_tools(
        self,
        system_prompt: str,
        user_message: str,
        tools: list[dict[str, Any]],
        max_tokens: int = 4096,
    ) -> dict[str, Any]:
        """Send a tool_use request for structured output. Returns the tool input dict."""
        response = await self._call_with_retry(
            system=system_prompt,
            messages=[{"role": "user", "content": user_message}],
            max_tokens=max_tokens,
            tools=tools,
        )
        self._track_usage(response)

        for block in response.content:
            if block.type == "tool_use":
                return block.input

        # Fallback: try to parse text as JSON
        text = response.content[0].text if response.content else ""
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return {"raw_text": text}

    async def _call_with_retry(self, **kwargs: Any) -> Any:
        """Call the Anthropic API with exponential backoff retry (3 attempts)."""
        last_error: Exception | None = None
        for attempt in range(3):
            try:
                return await self.client.messages.create(
                    model=settings.anthropic_model,
                    **kwargs,
                )
            except Exception as e:
                last_error = e
                if attempt < 2:
                    wait = 2**attempt
                    logger.warning(
                        "Anthropic API call failed (attempt %d/3): %s. Retrying in %ds...",
                        attempt + 1,
                        str(e),
                        wait,
                    )
                    await asyncio.sleep(wait)
        raise last_error  # type: ignore[misc]

    def _track_usage(self, response: Any) -> None:
        if hasattr(response, "usage"):
            self.total_input_tokens += response.usage.input_tokens
            self.total_output_tokens += response.usage.output_tokens


llm_client = LLMClient()
