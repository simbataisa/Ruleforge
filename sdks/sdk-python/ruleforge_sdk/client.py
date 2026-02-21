"""RuleForge Python SDK client."""

from __future__ import annotations

from typing import Optional

import httpx


class RuleForgeClient:
    """Synchronous client for the RuleForge BRMS API.

    Supports API key and JWT token authentication.

    Example::

        client = RuleForgeClient(base_url="https://api.ruleforge.io", api_key="key")
        result = client.solve("loan-eligibility", {"age": 30, "income": 75000})
    """

    def __init__(
        self,
        base_url: str,
        api_key: Optional[str] = None,
        jwt_token: Optional[str] = None,
        timeout: float = 30.0,
    ) -> None:
        """
        Initialize the RuleForge client.

        Args:
            base_url:  Base URL of the RuleForge API (e.g. ``https://api.ruleforge.io``).
            api_key:   API key for ``X-API-Key`` header authentication.
            jwt_token: JWT token for ``Authorization: Bearer`` header authentication.
            timeout:   HTTP request timeout in seconds (default 30).
        """
        if not base_url:
            raise ValueError("base_url must not be empty")

        self._base_url = base_url.rstrip("/")
        self._api_key = api_key
        self._jwt_token = jwt_token
        self._http = httpx.Client(
            base_url=self._base_url,
            headers=self._headers(),
            timeout=timeout,
        )

    def _headers(self) -> dict[str, str]:
        """Build authentication headers."""
        headers: dict[str, str] = {
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        if self._api_key:
            headers["X-API-Key"] = self._api_key
        elif self._jwt_token:
            headers["Authorization"] = f"Bearer {self._jwt_token}"
        return headers

    def solve(self, rule_id_or_alias: str, input: dict) -> dict:
        """
        Execute a single rule against the provided input facts.

        Args:
            rule_id_or_alias: The rule UUID or its unique alias.
            input:            Key/value map of input facts.

        Returns:
            Solver response dict with keys ``output``, ``matchedRules``, ``executionTimeMs``.

        Raises:
            httpx.HTTPStatusError: On non-2xx HTTP response.
            httpx.RequestError:    On connection/network errors.
        """
        response = self._http.post(
            f"/v1/solve/{rule_id_or_alias}",
            json={"input": input},
        )
        response.raise_for_status()
        return response.json()

    def solve_bulk(self, rule_id_or_alias: str, inputs: list[dict]) -> list[dict]:
        """
        Execute a rule against multiple input records in a single request.

        Args:
            rule_id_or_alias: The rule UUID or its unique alias.
            inputs:           List of input fact maps.

        Returns:
            List of solver response dicts.

        Raises:
            httpx.HTTPStatusError: On non-2xx HTTP response.
            httpx.RequestError:    On connection/network errors.
        """
        response = self._http.post(
            f"/v1/solve/bulk/{rule_id_or_alias}",
            json={"inputs": inputs},
        )
        response.raise_for_status()
        return response.json()

    def list_rules(self, space_id: str) -> list:
        """
        List all rules in a given space.

        Args:
            space_id: The UUID of the space.

        Returns:
            List of rule dicts from the paginated response.

        Raises:
            httpx.HTTPStatusError: On non-2xx HTTP response.
        """
        response = self._http.get(f"/v1/spaces/{space_id}/rules")
        response.raise_for_status()
        data = response.json()
        # Unwrap the ApiResponse envelope: data -> content list
        if isinstance(data, dict):
            payload = data.get("data", data)
            if isinstance(payload, dict):
                return payload.get("content", [])
        return data if isinstance(data, list) else []

    def get_rule(self, rule_id: str) -> dict:
        """
        Retrieve a single rule by its UUID.

        Args:
            rule_id: The rule UUID.

        Returns:
            Rule details dict.

        Raises:
            httpx.HTTPStatusError: On non-2xx HTTP response.
        """
        response = self._http.get(f"/v1/rules/{rule_id}")
        response.raise_for_status()
        data = response.json()
        # Unwrap ApiResponse envelope if present
        if isinstance(data, dict) and "data" in data:
            return data["data"]
        return data

    # --- Analytics ---

    def get_metrics(self) -> dict:
        """Retrieve aggregated metrics for the current organization."""
        response = self._http.get("/v1/analytics/metrics")
        response.raise_for_status()
        return response.json()

    def get_rule_stats(self, rule_id: str) -> dict:
        """Retrieve execution statistics for a specific rule."""
        response = self._http.get(f"/v1/analytics/rules/{rule_id}/stats")
        response.raise_for_status()
        return response.json()

    def get_decision_logs(
        self,
        rule_id: Optional[str] = None,
        page: int = 0,
        size: int = 20,
    ) -> list[dict]:
        """Retrieve decision log entries, optionally filtered by rule."""
        params: dict = {"page": page, "size": size}
        if rule_id:
            params["ruleId"] = rule_id
        response = self._http.get("/v1/analytics/decision-logs", params=params)
        response.raise_for_status()
        return response.json()

    # --- Schedules ---

    def list_schedules(self) -> list[dict]:
        """List all schedules for the current organization."""
        response = self._http.get("/v1/schedules")
        response.raise_for_status()
        return response.json()

    def create_schedule(self, schedule: dict) -> dict:
        """Create a new schedule."""
        response = self._http.post("/v1/schedules", json=schedule)
        response.raise_for_status()
        return response.json()

    def delete_schedule(self, schedule_id: str) -> None:
        """Delete a schedule by its UUID."""
        response = self._http.delete(f"/v1/schedules/{schedule_id}")
        response.raise_for_status()

    # --- AI ---

    def explain_rule(self, rule_id: str) -> dict:
        """Return a natural-language explanation of a rule."""
        response = self._http.post("/v1/ai/explain", json={"ruleId": rule_id})
        response.raise_for_status()
        return response.json()

    def generate_rule(self, prompt: str, rule_type: str) -> dict:
        """Generate a rule definition from a natural-language prompt."""
        response = self._http.post(
            "/v1/ai/generate-rule",
            json={"prompt": prompt, "ruleType": rule_type},
        )
        response.raise_for_status()
        return response.json()

    def generate_test_data(self, rule_id: str, count: int = 5) -> dict:
        """Generate test data suitable for exercising a rule."""
        response = self._http.post(
            "/v1/ai/generate-test-data",
            json={"ruleId": rule_id, "count": count},
        )
        response.raise_for_status()
        return response.json()

    def close(self) -> None:
        """Close the underlying HTTP client and release connections."""
        self._http.close()

    def __enter__(self) -> "RuleForgeClient":
        return self

    def __exit__(self, *args: object) -> None:
        self.close()
