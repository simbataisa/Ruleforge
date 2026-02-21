from pydantic import BaseModel, Field


class GenerateRuleRequest(BaseModel):
    description: str = Field(..., description="Natural language description of the rule")
    rule_type: str = Field(..., description="Rule type: 'decision_table' or 'decision_tree'")
    space_id: str = Field(..., description="Space ID to create the rule in")


class ExplainRuleRequest(BaseModel):
    rule_id: str = Field(..., description="Rule ID to explain")
    rule_definition: dict = Field(..., description="Full rule JSON definition")


class OptimizeRuleRequest(BaseModel):
    rule_id: str = Field(..., description="Rule ID to optimize")
    decision_logs: list[dict] = Field(default_factory=list, description="Recent decision logs")


class GenerateTestDataRequest(BaseModel):
    rule_id: str = Field(..., description="Rule ID to generate test data for")
    rule_definition: dict = Field(..., description="Full rule JSON definition")
    count: int = Field(default=10, ge=1, le=100, description="Number of test cases to generate")


class DetectConflictsRequest(BaseModel):
    space_id: str = Field(..., description="Space ID to check for conflicts")
    rule_ids: list[str] = Field(..., description="List of rule IDs to compare")


class NaturalLanguageQueryRequest(BaseModel):
    query: str = Field(..., description="Natural language query about rules")
    space_id: str = Field(..., description="Space ID to search in")
