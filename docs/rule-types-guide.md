# RuleForge Rule Types Guide

This guide explains how to create rules and configure conditions for each rule type in RuleForge.

---

## Table of Contents

1. [TABLE — Decision Table](#1-table--decision-table)
2. [TREE — Decision Tree](#2-tree--decision-tree)
3. [SCORECARD — Weighted Scoring](#3-scorecard--weighted-scoring)
4. [ENSEMBLE — Model Aggregation](#4-ensemble--model-aggregation)
5. [DMN — Decision Model and Notation](#5-dmn--decision-model-and-notation)
6. [FLOW — Rule Flow / DAG Orchestrator](#6-flow--rule-flow--dag-orchestrator)
7. [SCRIPT — JavaScript Scripting](#7-script--javascript-scripting)
8. [CEP — Complex Event Processing](#8-cep--complex-event-processing)

---

## 1. TABLE — Decision Table

A decision table evaluates a matrix of conditions against input facts. Each row is an if-then rule: condition columns filter rows, and action columns define the outputs. A *hit policy* controls how many matched rows to return.

### When to use
Use TABLE when your logic can be expressed as a lookup matrix — for example, loan eligibility based on credit score ranges, or pricing tiers by region and customer type.

### Definition format

```json
{
  "hitPolicy": "FIRST",
  "columns": [
    { "name": "creditScore", "columnType": "CONDITION", "operator": "GTE", "dataType": "ANY", "index": 0 },
    { "name": "income",      "columnType": "CONDITION", "operator": "GTE", "dataType": "ANY", "index": 1 },
    { "name": "eligible",    "columnType": "ACTION",    "operator": "EQ",  "dataType": "ANY", "index": 2 },
    { "name": "maxLoan",     "columnType": "ACTION",    "operator": "EQ",  "dataType": "ANY", "index": 3 }
  ],
  "rows": [
    { "rowNumber": 1, "priority": 0, "conditions": [{"value": 750}, {"value": 50000}], "actions": [{"value": "true"}, {"value": 500000}] },
    { "rowNumber": 2, "priority": 0, "conditions": [{"value": 650}, {"value": 30000}], "actions": [{"value": "true"}, {"value": 200000}] },
    { "rowNumber": 3, "priority": 0, "conditions": [{"value": 0},   {"value": 0}],     "actions": [{"value": "false"},{"value": 0}] }
  ]
}
```

### Columns

| Field        | Required | Notes                                     |
|-------------|----------|-------------------------------------------|
| `name`       | YES      | Must match the fact path in the input     |
| `columnType` | YES      | `CONDITION` or `ACTION`                   |
| `operator`   | NO       | Defaults to `EQ` (see operators below)    |
| `dataType`   | NO       | Defaults to `ANY` (see data types below)  |
| `index`      | NO       | Positional index (auto-assigned if omitted)|

### Rows

Condition cells and action cells are **positional arrays** — each entry aligns with the corresponding condition/action column by index.

| Field        | Required | Notes                                      |
|-------------|----------|--------------------------------------------|
| `rowNumber`  | NO       | For ordering and tracing                   |
| `priority`   | NO       | Higher value = higher priority (default 0) |
| `conditions` | NO       | Array of `{value}` or `{value, secondValue}` |
| `actions`    | NO       | Array of `{value}`                         |

- **Wildcard cell**: omit or set both `value` and `secondValue` to `null` — matches any input value.
- **Range cell (BETWEEN)**: `{ "value": 100, "secondValue": 500 }`.
- **Numeric values**: use JSON numbers (not strings) for numeric comparisons — `750`, not `"750"`.

### Hit policies

| Policy      | Behavior                                                       |
|-------------|----------------------------------------------------------------|
| `FIRST`     | Return first matched row by priority then declaration order    |
| `ALL`       | Return all matched rows; merge outputs                         |
| `PRIORITY`  | Return only the highest-priority matched row                   |
| `AGGREGATE` | Sum/min/max/avg/count numeric outputs across all matches       |
| `RANKED`    | Return top-N matches ranked by `priority × weight`             |

### Operators

`EQ`, `NEQ`, `GT`, `GTE`, `LT`, `LTE`, `BETWEEN`, `IN`, `CONTAINS`, `REGEX`, `STARTS_WITH`, `IS_NULL`, `IS_NOT_NULL`

### Data types

`STRING`, `NUMBER`, `INTEGER`, `BOOLEAN`, `DATE`, `DATETIME`, `ARRAY`, `OBJECT`, `MONEY`, `ENUM`, `ANY`

### Testing

```json
{ "creditScore": 720, "income": 45000 }
```
Expected: matches row 2 → `{ "eligible": "true", "maxLoan": 200000 }`

---

## 2. TREE — Decision Tree

A decision tree is a recursive structure that traverses from a root node down to an action node. Each condition node tests a field against a threshold and routes to a child node (or a default fallback). Gate nodes evaluate arbitrary boolean RFL expressions. Action nodes emit the final output.

### When to use
Use TREE when logic is naturally hierarchical — for example, an insurance risk model that first checks age, then driving history, then claims history.

### Node types

#### CONDITION node
Tests one field against a value. Routes to the first matching branch, or to `defaultBranch` if none match.

```json
{
  "nodeType": "CONDITION",
  "nodeId": "n1",
  "field": "age",
  "operator": "LT",
  "branches": [
    {
      "value": 25,
      "child": { ... }
    }
  ],
  "defaultBranch": { ... }
}
```

| Field           | Required | Notes                                          |
|----------------|----------|------------------------------------------------|
| `field`         | YES      | Dot-path into input (e.g. `customer.age`)      |
| `operator`      | YES      | Comparison operator (see list below)           |
| `branches`      | NO       | Ordered list; first match wins                 |
| `branches[].value` | YES (per branch) | Threshold to compare the field against |
| `branches[].secondValue` | Conditional | Required for `BETWEEN` (upper bound) |
| `branches[].child` | YES  | Node to route to when branch matches           |
| `defaultBranch` | NO       | Fallback node when no branch matches           |

#### ACTION node
Emits key-value outputs and terminates the traversal.

```json
{
  "nodeType": "ACTION",
  "nodeId": "n3",
  "outputs": {
    "riskCategory": "HIGH",
    "premium": "1.5"
  }
}
```

#### GATE node
Evaluates a boolean RFL expression and routes to `trueChild` or `falseChild`.

```json
{
  "nodeType": "GATE",
  "nodeId": "g1",
  "expression": "input.age >= 18 && input.income > 30000",
  "trueChild": { ... },
  "falseChild": { ... }
}
```

RFL operators: `&&` (AND), `||` (OR), `!` (NOT), comparison operators, arithmetic.

### Top-level definition

```json
{
  "root": { ... root node ... },
  "scoringMode": false,
  "earlyTermination": true
}
```

| Field              | Required | Notes                                                        |
|-------------------|----------|--------------------------------------------------------------|
| `root`             | YES      | The root node of the tree                                    |
| `scoringMode`      | NO       | `true` = traverse all paths, accumulate SCORE node values    |
| `earlyTermination` | NO       | `true` (default) = stop at first ACTION node hit             |

### Operators

`EQ`, `NEQ`, `GT`, `GTE`, `LT`, `LTE`, `BETWEEN`, `IN`, `NOT_IN`, `CONTAINS`, `MATCHES`, `IS_NULL`, `IS_NOT_NULL`

### Condition chaining (AND semantics)

Connect a CONDITION node to another CONDITION node to express implicit AND: the child condition is only evaluated when the parent matches. For explicit multi-field boolean logic on the same level, use a GATE node.

### Using the visual editor

1. Drag **Condition** nodes onto the canvas and configure `field`, `operator`, and `value` in the Properties panel.
2. Draw edges between nodes. The **last edge** from a condition node is automatically labeled `default` and becomes `defaultBranch`.
3. For boolean expressions (AND/OR), add a **Gate** node and write an RFL expression.
4. Drag **Action** nodes as leaf nodes and add one or more outputs.
5. Click **Save** when done.

### Example

```json
{
  "root": {
    "nodeType": "CONDITION", "nodeId": "n1", "field": "age", "operator": "LT",
    "branches": [
      { "value": 25, "child": {
        "nodeType": "CONDITION", "nodeId": "n2", "field": "accidents", "operator": "GT",
        "branches": [
          { "value": 0, "child": { "nodeType": "ACTION", "nodeId": "n3", "outputs": { "riskCategory": "HIGH", "premium": "1.5" } } }
        ],
        "defaultBranch": { "nodeType": "ACTION", "nodeId": "n4", "outputs": { "riskCategory": "MEDIUM", "premium": "1.2" } }
      }}
    ],
    "defaultBranch": { "nodeType": "ACTION", "nodeId": "n5", "outputs": { "riskCategory": "LOW", "premium": "1.0" } }
  },
  "scoringMode": false,
  "earlyTermination": true
}
```

### Testing

```json
{ "age": 22, "accidents": 1 }
```
Expected: `{ "riskCategory": "HIGH", "premium": "1.5" }`

---

## 3. SCORECARD — Weighted Scoring

A scorecard assigns numeric points to input values across multiple categories and attributes, then aggregates them into a final score using an aggregation strategy. Useful for credit scoring, risk rating, or lead qualification.

### When to use
Use SCORECARD when you need to produce a numeric score from multiple weighted inputs — credit scores, insurance risk ratings, candidate evaluation.

### Definition format

```json
{
  "baseScore": 300,
  "strategy": "ADDITIVE",
  "minimumScore": 0.0,
  "maximumScore": 1000.0,
  "categories": [
    {
      "name": "Payment History",
      "weight": 0.35,
      "attributes": [
        {
          "name": "paymentHistory",
          "factPath": "paymentHistory",
          "defaultPoints": 0.0,
          "ranges": [
            { "min": 95, "max": 100, "points": 100.0, "label": "Excellent" },
            { "min": 80, "max": 94, "points": 70.0,  "label": "Good" },
            { "min": 60, "max": 79, "points": 40.0,  "label": "Fair" },
            { "min": 0,  "max": 59, "points": 10.0,  "label": "Poor" }
          ]
        }
      ]
    }
  ]
}
```

### Top-level fields

| Field          | Required | Notes                                               |
|---------------|----------|-----------------------------------------------------|
| `categories`   | YES      | List of categories (can be empty)                   |
| `baseScore`    | NO       | Added to the final aggregated score (default: 0)    |
| `strategy`     | NO       | Aggregation strategy (default: `ADDITIVE`)          |
| `minimumScore` | NO       | Clamps the final score from below (default: 0)      |
| `maximumScore` | NO       | Clamps the final score from above (default: 1000)   |

### Categories and attributes

| Field                       | Required | Notes                                              |
|----------------------------|----------|----------------------------------------------------|
| `categories[].name`         | NO       | Display name                                       |
| `categories[].weight`       | NO       | Used in `WEIGHTED_AVERAGE` (default: 1.0)          |
| `categories[].attributes`   | NO       | Multiple attributes per category are summed        |
| `attributes[].factPath`     | YES      | Dot-path into the input (e.g. `customer.score`)    |
| `attributes[].defaultPoints`| NO       | Points when no range matches (default: 0)          |
| `attributes[].ranges`       | NO       | Ordered list; first matching range wins            |

### Range fields

| Field              | Required | Notes                                              |
|-------------------|----------|----------------------------------------------------|
| `min`              | NO       | `null` = unbounded below                           |
| `max`              | NO       | `null` = unbounded above                           |
| `points`           | NO       | Points assigned when range matches (default: 0)    |
| `label`            | NO       | Appears in decision trace                          |
| `minInclusive`     | NO       | Include the min bound (default: `true`)            |
| `maxInclusive`     | NO       | Include the max bound (default: `false`)           |

Default bounds: `min <= value < max`. Set `maxInclusive: true` for inclusive upper bound.

### Aggregation strategies

| Strategy           | Description                                                  |
|-------------------|--------------------------------------------------------------|
| `ADDITIVE`         | `finalScore = baseScore + sum(all category scores)`          |
| `WEIGHTED_AVERAGE` | `finalScore = baseScore + weightedAverage(category scores)`  |
| `MULTIPLICATIVE`   | `finalScore = baseScore × product(1 + categoryScore)`        |

### Testing

```json
{ "paymentHistory": 90, "creditUtilization": 25, "creditAge": 8 }
```
Expected: a numeric score in the `[minimumScore, maximumScore]` range.

---

## 4. ENSEMBLE — Model Aggregation

An ensemble runs multiple member rules independently against the same input and then aggregates their outputs into a single result. Used to combine predictions from different models for improved accuracy.

### When to use
Use ENSEMBLE when you want to combine outputs from multiple rules — e.g. average three different risk models, or take a majority vote from several classifiers.

### Definition format

```json
{
  "strategy": "MAJORITY_VOTE",
  "outputField": "result",
  "members": [
    { "ruleId": "11111111-1111-1111-1111-111111111111", "alias": "credit-model", "weight": 0.6, "version": 0 },
    { "ruleId": "22222222-2222-2222-2222-222222222222", "alias": "income-model", "weight": 0.4, "version": 0 }
  ]
}
```

### Fields

| Field           | Required | Notes                                                      |
|----------------|----------|------------------------------------------------------------|
| `members`       | NO       | List of member rule references                             |
| `members[].ruleId` | YES (per member) | UUID of the member rule                       |
| `members[].alias`  | NO      | Display name for tracing (defaults to ruleId)             |
| `members[].weight` | NO      | Used in weighted strategies (default: 1.0)                |
| `members[].version`| NO      | Rule version to use; `0` = latest (default: 0)            |
| `strategy`      | NO       | Aggregation strategy (default: `MAJORITY_VOTE`)            |
| `outputField`   | NO       | Key in each member's output to aggregate (default: `result`)|

### Strategies

| Strategy          | Description                                                   |
|------------------|---------------------------------------------------------------|
| `MAJORITY_VOTE`   | Most common output value wins (equal weights)                 |
| `WEIGHTED_VOTE`   | Most common output value wins (weighted by member `weight`)   |
| `AVERAGE`         | Arithmetic mean of numeric outputs                            |
| `WEIGHTED_AVERAGE`| Weighted mean of numeric outputs                              |
| `FIRST_MATCH`     | First member that produced a result wins                      |
| `MAX`             | Maximum numeric value across all member outputs               |
| `MIN`             | Minimum numeric value across all member outputs               |

### Requirements

- Each member rule must be PUBLISHED and must output the key named in `outputField`.
- `outputField` must match exactly — it is case-sensitive.
- Use `version: 0` to always run the latest published version.

### Testing

```json
{ "creditScore": 720, "income": 50000 }
```
Expected: aggregated value from all member rules in the `result` (or your configured `outputField`) key.

---

## 5. DMN — Decision Model and Notation

DMN implements the OMG Decision Model and Notation (DMN 1.4) standard. A DMN definition contains one or more **Decision** elements, each backed by a decision table. Decisions can be chained — the output of one decision can be used as the input to another.

### When to use
Use DMN when your organisation already uses DMN notation, when you need standards compliance, or when decisions need to reference each other's outputs.

### Definition format

```json
{
  "name": "Vacation Days Model",
  "decisions": [
    {
      "id": "vacation-days",
      "name": "Vacation Days",
      "requiredInputs": ["yearsOfService", "role"],
      "requiredDecisions": [],
      "decisionTable": {
        "hitPolicy": "FIRST",
        "inputClauses": [
          { "name": "yearsOfService", "expression": "yearsOfService" },
          { "name": "role",           "expression": "role" }
        ],
        "outputClauses": [
          { "name": "vacationDays" }
        ],
        "rules": [
          { "ruleNumber": 1, "inputEntries": [">=0",  "Junior"],  "outputEntries": ["15"] },
          { "ruleNumber": 2, "inputEntries": [">=0",  "Senior"],  "outputEntries": ["20"] },
          { "ruleNumber": 3, "inputEntries": [">=5",  "Junior"],  "outputEntries": ["25"] },
          { "ruleNumber": 4, "inputEntries": [">=5",  "Manager"], "outputEntries": ["30"] }
        ]
      }
    }
  ]
}
```

### Decisions

| Field                 | Required | Notes                                              |
|----------------------|----------|----------------------------------------------------|
| `decisions`           | YES      | At least one decision required                     |
| `decisions[].id`      | NO       | Defaults to `"decision_N"`                         |
| `decisions[].decisionTable` | YES | Each decision must have a decision table      |
| `requiredInputs`      | NO       | List of input names this decision reads            |
| `requiredDecisions`   | NO       | IDs of upstream decisions whose output feeds here  |

### Decision table

| Field               | Required | Notes                                               |
|--------------------|----------|-----------------------------------------------------|
| `inputClauses`      | YES      | At least one; empty throws compile error            |
| `outputClauses`     | YES      | At least one; empty throws compile error            |
| `hitPolicy`         | NO       | Defaults to `UNIQUE`                                |
| `rules`             | NO       | List of rows; can be empty                          |

### Input/Output clauses

Input clauses can be a simple string (column name) or an object:
```json
{ "name": "yearsOfService", "expression": "yearsOfService" }
```
`expression` is an S-FEEL expression evaluated against the working memory.

Output clauses can be a string or:
```json
{ "name": "vacationDays", "defaultValue": "0" }
```

### Rules (rows)

| Field            | Required | Notes                                                      |
|-----------------|----------|------------------------------------------------------------|
| `inputEntries`   | NO       | S-FEEL expression per input clause; `""` = wildcard        |
| `outputEntries`  | NO       | S-FEEL output value per output clause                      |
| `annotation`     | NO       | Optional free-text comment                                 |

### S-FEEL input entry examples

| Entry         | Meaning                         |
|--------------|----------------------------------|
| `"Junior"`    | Equals "Junior"                  |
| `">=5"`       | Greater than or equal to 5       |
| `"[18..65]"`  | Range 18 inclusive to 65 inclusive |
| `""`          | Wildcard — matches any value     |

### Hit policies

| Policy        | Behavior                                                    |
|--------------|-------------------------------------------------------------|
| `UNIQUE`      | Exactly one rule may match; multiple matches = error        |
| `FIRST`       | Return first matched rule                                   |
| `PRIORITY`    | Return highest-priority matched rule                        |
| `ANY`         | Multiple rules may match; all must agree on outputs         |
| `COLLECT`     | Return all matched outputs as a list                        |
| `RULE_ORDER`  | Return all matches in declaration order                     |
| `OUTPUT_ORDER`| Return all matches ordered by output priority               |

### Chaining decisions

Set `requiredDecisions` to reference upstream decision IDs. The engine evaluates decisions in dependency order (topological sort). Cyclic references throw a compile error.

### Testing

```json
{ "yearsOfService": 6, "role": "Junior" }
```
Expected: `{ "vacationDays": "25" }`

---

## 6. FLOW — Rule Flow / DAG Orchestrator

A rule flow executes a directed acyclic graph (DAG) of nodes. Each node can be a condition, an action, a gate, a parallel fork/join, or a reference to another rule. Edges can carry RFL conditions that gate traversal.

### When to use
Use FLOW when you need to orchestrate a multi-step process — for example: validate input → check credit → compute premium → approve or reject. FLOW is the right choice when the decision depends on branching logic and multiple independent checks.

### Definition format

```json
{
  "startNodeId": "start",
  "maxIterations": 100,
  "nodes": [
    { "id": "start",   "type": "CONDITION", "expression": "input.amount > 0" },
    { "id": "approve", "type": "ACTION",    "outputs": { "status": "APPROVED" } },
    { "id": "reject",  "type": "ACTION",    "outputs": { "status": "REJECTED" } }
  ],
  "edges": [
    { "from": "start", "to": "approve", "condition": "input.amount > 0" },
    { "from": "start", "to": "reject",  "condition": "input.amount <= 0" }
  ]
}
```

### Top-level fields

| Field           | Required | Notes                                               |
|----------------|----------|-----------------------------------------------------|
| `nodes`         | NO       | List of flow nodes                                  |
| `edges`         | NO       | List of directed edges between nodes                |
| `startNodeId`   | NO       | ID of the first node to execute (defaults to nodes[0]) |
| `maxIterations` | NO       | Loop guard to prevent infinite cycles (default: 100)|

### Node types

| Type        | Extra fields          | Description                                      |
|------------|----------------------|--------------------------------------------------|
| `CONDITION` | `expression` (RFL)   | Evaluates a boolean expression; result stored in working memory |
| `ACTION`    | `outputs` (map)      | Writes key-value pairs to output                  |
| `GATE`      | `gateType` (`AND`/`OR`) | Combines multiple incoming branch results      |
| `FORK`      | —                    | Splits execution into parallel branches           |
| `JOIN`      | —                    | Waits for all forked branches to complete         |
| `SUB_RULE`  | `ruleId` (UUID)      | Delegates to another rule; result merged into working memory |

### Edges

| Field       | Required | Notes                                                       |
|------------|----------|-------------------------------------------------------------|
| `from`      | YES      | Source node ID                                              |
| `to`        | YES      | Target node ID                                              |
| `condition` | NO       | RFL expression; edge is taken only if `true` (default: `"true"`) |

### Example — multi-step approval

```json
{
  "startNodeId": "validate",
  "nodes": [
    { "id": "validate",  "type": "CONDITION", "expression": "input.amount > 0 && input.currency != null" },
    { "id": "credit",    "type": "SUB_RULE",  "ruleId": "credit-check-rule-uuid" },
    { "id": "approved",  "type": "ACTION",    "outputs": { "decision": "APPROVED" } },
    { "id": "rejected",  "type": "ACTION",    "outputs": { "decision": "REJECTED" } }
  ],
  "edges": [
    { "from": "validate", "to": "credit",   "condition": "true" },
    { "from": "credit",   "to": "approved", "condition": "creditScore >= 650" },
    { "from": "credit",   "to": "rejected", "condition": "creditScore < 650" }
  ]
}
```

### Testing

```json
{ "amount": 5000, "currency": "USD" }
```
Expected: `{ "decision": "APPROVED" }` (if `creditScore >= 650` from the sub-rule)

---

## 7. SCRIPT — JavaScript Scripting

A script rule executes arbitrary JavaScript code in a sandboxed GraalVM Polyglot environment. The script receives the input as a JavaScript object and must return an output object.

### When to use
Use SCRIPT for complex logic that can't easily be expressed in any other rule type — custom algorithms, mathematical formulas, string manipulation, or integration with external APIs (if the sandbox permits).

> **Note:** The GraalVM polyglot runtime is bundled as a dependency — any standard JDK 21+ (Temurin, Corretto, etc.) works. A GraalVM JDK is **not** required. The ScriptingEngine is registered in both the management and solver services.

### Definition format

```json
{
  "scriptBody": "function evaluate(input) {\n  var factor = Math.min(2.0, Math.max(0.5, input.demandIndex / 100));\n  return { finalPrice: Math.round(input.basePrice * factor * 100) / 100 };\n}",
  "language": "js",
  "timeout": 5000,
  "maxMemoryMb": 64
}
```

### Fields

| Field          | Required | Notes                                                      |
|---------------|----------|------------------------------------------------------------|
| `scriptBody`   | YES      | Non-empty JavaScript string                                |
| `language`     | NO       | Must be `"js"` (default: `"js"`). Do **not** use `"javascript"` — GraalVM registers the language as `js`. |
| `timeout`      | NO       | Execution timeout in milliseconds (default: 5000)          |
| `maxMemoryMb`  | NO       | Maximum memory in MB (default: 64)                         |

### Script conventions

- Define a function named **`evaluate`**. The engine looks up this exact name at runtime — other names (e.g. `execute`) will cause a runtime error.
- The function receives one argument: the input object (all facts from working memory).
- Return a plain JavaScript object — its keys become the output facts.

### Default template

```javascript
function evaluate(input) {
  // Write your rule logic here
  // Access inputs: input.fieldName
  return {
    result: true
  };
}
```

### Sandbox restrictions

- **No file system access** — no `readFile`, `Paths`, etc.
- **No network access** — no `fetch`, `XMLHttpRequest`, etc.
- GraalVM Polyglot sandbox is applied; only safe JS built-ins are available.
- Execution is interrupted after `timeout` milliseconds.

### Dynamic pricing example

```javascript
function evaluate(input) {
  var basePrice = input.basePrice;
  var demandFactor = Math.min(2.0, Math.max(0.5, input.demandIndex / 100));
  var finalPrice = basePrice * demandFactor;
  return {
    finalPrice: Math.round(finalPrice * 100) / 100,
    pricingTier: demandFactor > 1.5 ? "SURGE" : demandFactor < 0.8 ? "DISCOUNT" : "STANDARD"
  };
}
```

### Testing

```json
{ "basePrice": 100.0, "demandIndex": 150 }
```
Expected: `{ "finalPrice": 150.0, "pricingTier": "SURGE" }`

---

## 8. CEP — Complex Event Processing

CEP detects patterns across a stream of Kafka events over configurable time windows. It matches events against schemas and pattern logic, then emits results to an output topic.

### When to use
Use CEP for real-time stream processing — for example, detecting fraud (3+ transactions over $100 in 5 minutes), monitoring system alerts (absence of heartbeat events), or session analysis.

> **Note:** CEP supports two modes: (1) **In-memory evaluation** via the standard rule test API — useful for pattern detection against single events or small batches, and (2) **Kafka Streams streaming mode** for real-time continuous processing in a standalone deployment. The CepEngine is registered in both the management and solver services.

### Definition format

```json
{
  "patternType": "FREQUENCY",
  "inputTopic": "transactions",
  "outputTopic": "fraud-alerts",
  "eventSchemas": [
    {
      "eventType": "transaction",
      "fields": {
        "amount": "number",
        "country": "string"
      },
      "filterConditions": [
        { "field": "amount", "operator": "GT", "value": "100" }
      ]
    }
  ],
  "timeWindow": {
    "type": "TUMBLING",
    "duration": 300,
    "graceTime": 0
  },
  "actions": [
    { "actionType": "EMIT_EVENT", "parameters": {} }
  ]
}
```

### Top-level fields

| Field          | Required | Notes                                                    |
|---------------|----------|----------------------------------------------------------|
| `patternType`  | YES      | Pattern to detect (see below)                            |
| `eventSchemas` | NO       | Schemas for each event type to match against             |
| `timeWindow`   | NO       | Defaults to 5-minute tumbling window                     |
| `inputTopic`   | NO       | Kafka input topic (default: `"rule.execution"`)          |
| `outputTopic`  | NO       | Kafka output topic (default: `"cep.results"`)            |
| `actions`      | NO       | Actions to perform on match                              |
| `parameters`   | NO       | Additional pattern-specific parameters                   |

### Pattern types

| Type          | Description                                                     | Status    |
|--------------|-----------------------------------------------------------------|-----------|
| `SEQUENCE`    | Events must occur in a specific order within the window         | Supported |
| `AGGREGATION` | Aggregates values (sum/avg/min/max/count) over the window       | Supported |
| `FREQUENCY`   | Triggers when an event type occurs N or more times              | Supported |
| `ABSENCE`     | Triggers when an expected event does NOT appear in the window   | Not yet supported |
| `CONJUNCTION` | Any combination of events within the window (order-independent) | Not yet supported |

### Event schemas

| Field                | Required | Notes                                                  |
|---------------------|----------|--------------------------------------------------------|
| `eventType`          | YES      | Event type identifier; must not be blank               |
| `fields`             | NO       | Map of field name → type (`number`, `string`, etc.)    |
| `filterConditions`   | NO       | Pre-filter conditions; only matching events are counted |

### Time windows

| Field      | Required | Notes                                                       |
|-----------|----------|-------------------------------------------------------------|
| `type`     | NO       | `TUMBLING`, `SLIDING`, or `SESSION` (default: `TUMBLING`)   |
| `duration` | NO       | Window length in seconds (default: 300 = 5 minutes)         |
| `graceTime`| NO       | Late-arrival tolerance in seconds (default: 0)              |

### Action types

| Type          | Description                         |
|--------------|-------------------------------------|
| `EMIT_EVENT`  | Publish a result to `outputTopic`    |
| `ALERT`       | Trigger an alert                     |
| `INVOKE_RULE` | Execute another RuleForge rule       |

### Filter condition operators

`EQ`, `NEQ`, `GT`, `GTE`, `LT`, `LTE`

---

## Showcase Sample Rules (V024)

RuleForge ships with comprehensive sample rules for every rule type, seeded via the `V024__comprehensive_sample_rules.sql` migration. These demonstrate real-world use cases with production-grade complexity.

| Rule Type | Name | ID | Use Case |
|-----------|------|----|----------|
| TABLE | Employee Benefits Eligibility | `e1000000-...-01` | 12-row matrix with department, tenure, performance, and employee type columns |
| TREE | Support Ticket Routing | `e1000000-...-02` | 3-level tree: priority → customer tier → issue category |
| SCORECARD | Vendor Risk Assessment | `e1000000-...-03` | 5 weighted categories (financial, compliance, delivery, security, tenure) |
| SCRIPT | Tax Calculation Engine | `e1000000-...-04` | 40-line JS with 7 progressive tax brackets, state taxes, and deductions |
| FLOW | Loan Application Pipeline | `e1000000-...-05` | 10-node DAG: validation → credit check → debt ratio → manual review → decision |
| DMN | Shipping Rate Calculator | `e1000000-...-06` | 12 S-FEEL rules across weight, zone, and priority dimensions |
| ENSEMBLE | Customer Churn Prediction | `e1000000-...-07` | 3 member rules with weighted average aggregation |
| CEP | Login Anomaly Detector | `e1000000-...-08` | SEQUENCE pattern for failed login attempts within a time window |

### Testing showcase rules

All showcase rules are testable via the management service test endpoint:

```bash
# Example: Test the Tax Calculation Engine (SCRIPT)
curl -X POST http://localhost:8082/v1/rules/e1000000-0000-0000-0000-000000000004/test \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"input":{"grossIncome":85000,"filingStatus":"single","dependents":0,"deductions":12950,"stateCode":"CA"}}'
```

Expected output:
```json
{
  "federalTax": 10541,
  "stateTax": 6547.2,
  "effectiveRate": 20.1,
  "taxBracket": "22%",
  "totalDeductions": 14600
}
```

The smoke test suite (`scripts/smoke-test.sh`) validates all 8 showcase rules with multiple test scenarios.

---

## Quick Reference

### Choosing a rule type

| Scenario                                          | Recommended type |
|--------------------------------------------------|-----------------|
| Lookup table / tiered pricing / eligibility matrix | TABLE           |
| Sequential multi-field decision hierarchy          | TREE            |
| Credit scoring / risk rating / numeric scoring     | SCORECARD       |
| Combine multiple model outputs                     | ENSEMBLE        |
| DMN-standard compliance / chained decisions        | DMN             |
| Multi-step orchestration / process automation      | FLOW            |
| Custom algorithm / formula                         | SCRIPT          |
| Real-time stream pattern detection                 | CEP             |

### Common operators

| Operator     | Meaning                            |
|-------------|-----------------------------------|
| `EQ`         | equals                             |
| `NEQ`        | not equals                         |
| `GT`         | greater than                       |
| `GTE`        | greater than or equal              |
| `LT`         | less than                          |
| `LTE`        | less than or equal                 |
| `BETWEEN`    | value is in range [min, max]       |
| `IN`         | value is in a list                 |
| `CONTAINS`   | string contains substring          |
| `MATCHES`    | regex match                        |
| `IS_NULL`    | value is absent                    |
| `IS_NOT_NULL`| value is present                   |

### Fact path notation

All rule types use dot-notation to reference fields in the input:

| Example path      | Meaning                                    |
|------------------|--------------------------------------------|
| `age`             | Top-level `age` field                      |
| `customer.age`    | Nested `age` field inside `customer` object|
| `order.items[0]`  | First element of `items` array             |
