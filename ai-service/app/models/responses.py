from pydantic import BaseModel, Field


class GenerateRuleResponse(BaseModel):
    rule_definition: dict = Field(..., description="Generated rule JSON definition")
    explanation: str = Field(default="", description="Explanation of the generated rule")


class ExplainRuleResponse(BaseModel):
    explanation: str = Field(..., description="Markdown explanation of the rule")
    summary: str = Field(default="", description="One-line summary")


class OptimizeSuggestion(BaseModel):
    type: str = Field(..., description="Suggestion type: consolidation, removal, reorder")
    description: str = Field(..., description="What to change")
    impact: str = Field(default="", description="Expected impact")


class OptimizeRuleResponse(BaseModel):
    suggestions: list[OptimizeSuggestion] = Field(default_factory=list)
    analysis: str = Field(default="", description="Overall analysis")


class TestCase(BaseModel):
    inputs: dict = Field(..., description="Input facts for the test case")
    expected_description: str = Field(default="", description="What this test case verifies")


class GenerateTestDataResponse(BaseModel):
    test_cases: list[TestCase] = Field(default_factory=list)


class Conflict(BaseModel):
    rule_id_a: str
    rule_id_b: str
    conflict_type: str = Field(..., description="Type: identical, overlapping, contradictory")
    description: str = Field(..., description="Explanation of the conflict")


class DetectConflictsResponse(BaseModel):
    conflicts: list[Conflict] = Field(default_factory=list)
    summary: str = Field(default="", description="Overall conflict analysis")


class NaturalLanguageQueryResponse(BaseModel):
    answer: str = Field(..., description="Natural language answer")
    relevant_rule_ids: list[str] = Field(default_factory=list)
