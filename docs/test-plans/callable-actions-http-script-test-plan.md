# Test Plan: HTTP and Script Callable Actions

**Version:** 1.0
**Date:** 2026-02-24
**Author:** QE / Tester
**Status:** Final
**Scope:** `HttpActionExecutor`, `ScriptActionExecutor`, frontend `CallableActionsEditor` (HTTP/Script tabs), V029 seed data, solver integration

---

## 1. Scope and Objectives

This plan covers verification of two new callable action protocols added to the RuleForge callable action system:

| Protocol | Executor Class | Transport |
|----------|---------------|-----------|
| `http` | `HttpActionExecutor` | `java.net.http.HttpClient` (JDK 11+) |
| `script` | `ScriptActionExecutor` | GraalVM polyglot JS (via reflection) |

**Objectives:**
- Confirm both executors handle happy-path and error cases correctly
- Verify `${factPath}` expression resolution in HTTP URL and body
- Verify void/null script functions return empty map (not `_result: "undefined"`)
- Confirm protocol-specific UI renders correctly in the tree editor properties panel
- Confirm serialization round-trips preserve all HTTP/script fields
- Confirm smoke-level integration: tree rules with callable actions execute end-to-end

**Out of scope:** Performance benchmarking, load testing, GraalVM polyglot language interop beyond JS.

---

## 2. Test Environment

| Component | Version / Location |
|-----------|-------------------|
| Engine | `engine/ruleforge-core` — `HttpActionExecutor`, `ScriptActionExecutor` |
| Platform | `platform/ruleforge-management-service` (port 8082), `platform/ruleforge-solver-service` (port 8083) |
| Frontend | `frontend/` (Vite dev server, port 3000) |
| DB | PostgreSQL 16 with V029 migration applied |
| Test runner — Engine | JUnit 5 + AssertJ (`cd engine && ./gradlew test`) |
| Test runner — Frontend | Vitest (`cd frontend && npm test`) |
| HTTP mock server | WireMock (in-process) for `HttpActionExecutor` unit tests |

### Prerequisites

```bash
# Apply V029 migration
docker exec -i docker-postgres-1 psql -U ruleforge -d ruleforge \
  < db/migrations/V029__seed_http_script_action_samples.sql

# Start services
./scripts/start-local.sh --skip-build

# Verify health
./scripts/smoke-test.sh --quick
```

---

## 3. Engine Unit Tests

### 3.1 HttpActionExecutor

**File to create:** `engine/ruleforge-core/src/test/java/io/ruleforge/core/action/HttpActionExecutorTest.java`

Use WireMock (`com.github.tomakehurst:wiremock-jre8:2.35.x`) or WireMock standalone to stub HTTP endpoints. Use `SimpleEvaluationContext` with a `WorkingMemory` populated from a test map.

#### TC-HTTP-01: GET request — JSON response

```
Setup:    WireMock stub GET /api/items → 200 { "count": 3, "status": "ok" }
Input:    target = "http://localhost:{port}/api/items"
          params = { "method": "GET" }
          wm     = {}
Expected: result = { "count": 3, "status": "ok" }
```

**Pass criteria:** Map keys `count` and `status` present with correct values.

#### TC-HTTP-02: POST request with JSON body

```
Setup:    WireMock stub POST /api/notify → 200 { "sent": true }
Input:    target = "http://localhost:{port}/api/notify"
          params = { "method": "POST", "body": { "message": "hello" } }
Expected: result = { "sent": true }
          Request body received by WireMock == {"message":"hello"}
```

#### TC-HTTP-03: PUT request

```
Setup:    WireMock stub PUT /api/resource/1 → 200 { "updated": true }
Input:    method = "PUT", body = { "value": 99 }
Expected: result = { "updated": true }
```

#### TC-HTTP-04: DELETE request — no body sent

```
Setup:    WireMock stub DELETE /api/resource/1 → 204 (empty body)
Input:    method = "DELETE"
Expected: result = { "_response": "", "_statusCode": 204 }
          WireMock request has no body
```

#### TC-HTTP-05: Bearer authorization header

```
Setup:    WireMock stub POST /secure → verify header Authorization = "Bearer tok123" → 200 { "ok": true }
Input:    params.authorization = { "type": "bearer", "token": "tok123" }
Expected: result = { "ok": true }
          If auth header missing: WireMock returns 401 → test should confirm header was sent
```

#### TC-HTTP-06: API key authorization header — default header name

```
Input:    params.authorization = { "type": "api_key", "value": "mykey" }
          (no "header" key — defaults to "X-API-Key")
Expected: Request header X-API-Key = "mykey" received by WireMock
```

#### TC-HTTP-07: API key authorization header — custom header name

```
Input:    params.authorization = { "type": "api_key", "header": "X-Custom-Auth", "value": "secret" }
Expected: Request header X-Custom-Auth = "secret"
          Header X-API-Key NOT present
```

#### TC-HTTP-08: Custom headers map

```
Input:    params.headers = { "X-Tenant": "acme", "X-Correlation-ID": "abc123" }
Expected: Both headers present in WireMock received request
```

#### TC-HTTP-09: `${factPath}` expression in URL

```
Input:    target = "http://localhost:{port}/orders/${orderId}"
          wm     = { orderId: "ORD-42" }
Expected: WireMock receives GET/POST to /orders/ORD-42
          URL resolved before request is sent
```

#### TC-HTTP-10: `${factPath}` expression in body string values

```
Input:    target = "http://localhost:{port}/api"
          params.body = { "id": "${orderId}", "customer": "${name}" }
          wm = { orderId: "ORD-99", name: "Alice" }
Expected: Request body == {"id":"ORD-99","customer":"Alice"}
```

#### TC-HTTP-11: `${factPath}` resolves to empty string for missing fact

```
Input:    target = "http://localhost:{port}/api/${unknownFact}"
          wm = {} (no facts)
Expected: URL resolved to "http://localhost:{port}/api/" (empty substitution, no exception)
```

#### TC-HTTP-12: Non-JSON response — graceful wrapping

```
Setup:    WireMock stub → 200 "plain text response"
Expected: result = { "_response": "plain text response", "_statusCode": 200 }
          No exception thrown
```

#### TC-HTTP-13: HTTP error status — non-JSON body

```
Setup:    WireMock stub → 500 "Internal Server Error"
Expected: result = { "_response": "Internal Server Error", "_statusCode": 500 }
          No exception thrown (error is surfaced in result, not thrown)
```

#### TC-HTTP-14: Connection refused / network error

```
Setup:    target points to a closed port (e.g., localhost:19999)
Expected: result = { "_error": "<message about connection refused>" }
          No exception propagated out of execute()
```

#### TC-HTTP-15: Custom timeout parameter

```
Setup:    WireMock stub with delay > 2s
          params.timeout = 1
Expected: result = { "_error": "<timeout message>" }
          Elapsed time < 3s
```

#### TC-HTTP-16: Bearer token with `${factPath}` resolution

```
Input:    params.authorization = { "type": "bearer", "token": "${apiToken}" }
          wm = { apiToken: "runtime-token-xyz" }
Expected: Authorization header = "Bearer runtime-token-xyz"
```

#### TC-HTTP-17: protocol() returns "http"

```
Expected: new HttpActionExecutor().protocol() == "http"
```

---

### 3.2 ScriptActionExecutor

**File to create:** `engine/ruleforge-core/src/test/java/io/ruleforge/core/action/ScriptActionExecutorTest.java`

Note: These tests require GraalVM JS on the classpath. If running on a standard JDK without GraalVM polyglot, TC-SCRIPT-01 through TC-SCRIPT-09 will be skipped via `@DisabledIfSystemProperty` or `Assumptions.assumeTrue(isGraalVmAvailable())`. TC-SCRIPT-10 and TC-SCRIPT-11 test the no-GraalVM path explicitly.

#### TC-SCRIPT-01: Valid evaluate function returning map

```javascript
function evaluate(params) {
  return { score: 42, approved: true };
}
```
```
Input:    params = {}
Expected: result = { "score": 42.0, "approved": true }
```

#### TC-SCRIPT-02: Params passed to evaluate function

```javascript
function evaluate(params) {
  return { doubled: params.value * 2 };
}
```
```
Input:    params = { "value": 21 }
Expected: result = { "doubled": 42.0 }
```

#### TC-SCRIPT-03: Void function — no return value — returns empty map

```javascript
function evaluate(params) {
  var x = params.value; // side-effect only, no return
}
```
```
Expected: result = Collections.emptyMap() (not { "_result": "undefined" })
          result.isEmpty() == true
```

#### TC-SCRIPT-04: Explicit null return — returns empty map

```javascript
function evaluate(params) {
  return null;
}
```
```
Expected: result.isEmpty() == true
```

#### TC-SCRIPT-05: String return value

```javascript
function evaluate(params) {
  return "hello";
}
```
```
Expected: result = { "_result": "hello" }
```

#### TC-SCRIPT-06: Syntax error in script

```javascript
function evaluate(params) {
  return {{{ broken syntax
}
```
```
Expected: result = { "_error": "<message containing 'Script execution error' or similar>" }
          No exception propagated
```

#### TC-SCRIPT-07: Runtime error in evaluate function

```javascript
function evaluate(params) {
  throw new Error("intentional runtime error");
}
```
```
Expected: result = { "_error": "<message containing 'intentional runtime error'>" }
```

#### TC-SCRIPT-08: Script defines no evaluate function

```javascript
var x = 1 + 1;
```
```
Expected: result = { "_error": "<message containing 'evaluate'>" }
          Error is descriptive (not NPE/stack trace)
```

#### TC-SCRIPT-09: Timeout enforcement

```javascript
function evaluate(params) {
  while (true) {} // infinite loop
}
```
```
Input:    params = { "timeout": 1 }
Expected: result = { "_error": "Script execution timed out after 1 seconds" }
          Elapsed wall time < 5s
```

#### TC-SCRIPT-10: Empty/blank script body

```
Input:    target = ""  (or "   ")
Expected: result = { "_error": "Script action target (script body) must not be empty" }
          Fast-path check, no GraalVM invocation
```

#### TC-SCRIPT-11: GraalVM not available — descriptive error

```
Setup:    Use reflection to test the ClassNotFoundException path
          OR run on a JVM without graalvm-polyglot jar
Expected: result = { "_error": "<message containing 'GraalVM polyglot runtime not available'>" }
```

#### TC-SCRIPT-12: protocol() returns "script"

```
Expected: new ScriptActionExecutor().protocol() == "script"
```

#### TC-SCRIPT-13: Default timeout is 5 seconds (not configurable override)

```
Setup:    Script with 3s delay, no "timeout" in params
Expected: Script completes (not timed out at default 5s)
```

#### TC-SCRIPT-14: Custom timeout param respected

```
Input:    params = { "timeout": 2 }
          Script sleeps 3s
Expected: Timed out at 2s, not 5s default
```

---

### 3.3 ActionExecutorRegistry

**File to create:** `engine/ruleforge-core/src/test/java/io/ruleforge/core/engine/ActionExecutorRegistryTest.java`

#### TC-REG-01: Register and retrieve executor by protocol

```
registry.register(new HttpActionExecutor());
Expected: registry.get("http") instanceof HttpActionExecutor
          registry.hasExecutor("http") == true
```

#### TC-REG-02: Unknown protocol returns null

```
Expected: registry.get("unknown") == null
          registry.hasExecutor("unknown") == false
```

#### TC-REG-03: Last registration wins for same protocol

```
registry.register(executorA);  // protocol = "http"
registry.register(executorB);  // protocol = "http"
Expected: registry.get("http") == executorB
```

#### TC-REG-04: Chained registration

```
registry.register(new HttpActionExecutor())
        .register(new ScriptActionExecutor())
        .register(new LogActionExecutor());
Expected: All three protocols registered
```

---

### 3.4 Integration — TreeTraverser with Callable Actions

**File to create:** `engine/ruleforge-tree/src/test/java/io/ruleforge/tree/TreeCallableActionIntegrationTest.java`

These tests compile a TREE rule definition containing action nodes with `callableActions`, then evaluate with a `WorkingMemory` populated from test data. A mock `ActionExecutor` is registered to capture calls without making real HTTP requests.

#### TC-INT-01: Action node executes callable action and merges result into output

```
Rule:    TREE with one condition (age >= 18) → ACTION node
         ACTION outputs = { "approved": "true" }
         callableActions = [{ protocol: "mock", target: "ping", params: {} }]
Mock executor returns: { "score": 95 }

Input:   age = 25
Expected: result.output["approved"] == "true"
          result.output["score"] == 95   (merged from callable action)
```

#### TC-INT-02: outputMapping namespaces action result

```
callableActions = [{ protocol: "mock", target: "...", outputMapping: "httpResult", params: {} }]
Mock executor returns: { "status": "ok" }

Expected: result.output["httpResult"]["status"] == "ok"
          Key "status" NOT at top level of output
```

#### TC-INT-03: Multiple callable actions execute in order, results merged

```
callableActions = [
  { protocol: "mock1", target: "a", outputMapping: null },   → returns { "a": 1 }
  { protocol: "mock2", target: "b", outputMapping: null },   → returns { "b": 2 }
]
Expected: result.output["a"] == 1
          result.output["b"] == 2
```

#### TC-INT-04: Callable action error (`_error` key) does not abort rule evaluation

```
callableActions = [{ protocol: "mock", returns: { "_error": "failed" } }]
ACTION outputs = { "decision": "approved" }

Expected: result.output["decision"] == "approved"
          result.output["_error"] == "failed" (or under outputMapping if set)
          No exception thrown from evaluate()
```

#### TC-INT-05: Null result from executor treated as empty map

```
Mock executor returns: null
Expected: No NullPointerException; rule output contains ACTION outputs unchanged
```

#### TC-INT-06: Unknown protocol — action skipped, no exception

```
callableActions = [{ protocol: "nonexistent", target: "...", params: {} }]
No executor registered for "nonexistent"

Expected: Rule evaluation succeeds; callable action result absent from output
          (or logged warning, not exception)
```

---

## 4. Frontend Unit Tests

**File to create or extend:** `frontend/src/stores/tree-editor.store.test.ts` and `frontend/src/features/tree-editor/components/PropertiesPanel.test.tsx`

### 4.1 Tree Editor Store — Callable Actions

Extend `tree-editor.store.test.ts`:

#### TC-FE-STORE-01: addActionNode includes empty callableActions array

```typescript
getState().addActionNode({ x: 0, y: 0 });
const node = getState().nodes[0];
expect(node.data.callableActions ?? []).toEqual([]);
```

#### TC-FE-STORE-02: updateNode merges callableActions without losing outputs

```typescript
getState().addActionNode({ x: 0, y: 0 });
const id = getState().nodes[0].id;
const action: CallableAction = {
  id: 'ca_1', protocol: 'http', target: 'https://example.com', parameters: { method: 'GET' }
};
getState().updateNode(id, { callableActions: [action] });
const node = getState().nodes.find(n => n.id === id)!;
expect((node.data as ActionNodeData).callableActions).toHaveLength(1);
expect((node.data as ActionNodeData).callableActions![0].protocol).toBe('http');
```

#### TC-FE-STORE-03: CallableAction.parameters type is Record<string, unknown>

```typescript
// Verify parameters can hold nested objects (authorization, headers)
const action: CallableAction = {
  id: 'ca_2',
  protocol: 'http',
  target: 'https://api.example.com',
  parameters: {
    method: 'POST',
    authorization: { type: 'bearer', token: 'tok123' },
    headers: { 'X-Foo': 'bar' },
    body: { orderId: '${orderId}' },
  },
};
// TypeScript compile check: no type error expected
expect(action.parameters.authorization).toBeDefined();
```

#### TC-FE-STORE-04: toDefinition serializes callableActions to engine format

```typescript
getState().addActionNode({ x: 0, y: 0 });
const id = getState().nodes[0].id;
getState().updateNode(id, {
  callableActions: [{
    id: 'ca_1',
    protocol: 'http',
    target: 'https://api.example.com/notify',
    parameters: { method: 'POST', authorization: { type: 'bearer', token: 'tok' } },
    outputMapping: 'apiResult',
  }]
});
const def = getState().toDefinition();
// The definition's action node should contain callableActions array
const root = def.root as Record<string, unknown>;
const actions = root.callableActions as unknown[];
expect(actions).toHaveLength(1);
expect((actions[0] as Record<string, unknown>).protocol).toBe('http');
expect((actions[0] as Record<string, unknown>).outputMapping).toBe('apiResult');
```

#### TC-FE-STORE-05: loadFromDefinition restores callableActions

```typescript
// Use the V029 order fulfillment rule definition shape
const definition = {
  root: {
    nodeType: 'ACTION',
    nodeId: 'action_1',
    label: 'Test Action',
    outputs: { decision: 'approved' },
    callableActions: [{
      actionId: 'ca_1',
      protocol: 'http',
      target: 'https://api.example.com',
      parameters: { method: 'GET' },
      outputMapping: null,
    }],
  },
  scoringMode: false,
  earlyTermination: true,
};
getState().loadFromDefinition(definition);
const actionNode = getState().nodes.find(n => n.type === 'actionNode');
expect(actionNode).toBeDefined();
const data = actionNode!.data as ActionNodeData;
expect(data.callableActions).toHaveLength(1);
expect(data.callableActions![0].protocol).toBe('http');
```

#### TC-FE-STORE-06: Round-trip — toDefinition then loadFromDefinition preserves all HTTP fields

```typescript
// Build a tree with an HTTP callable action including auth, headers, body, outputMapping
// Call toDefinition(), then reset(), then loadFromDefinition(result)
// Verify all fields survive the round-trip
const originalAction: CallableAction = {
  id: 'ca_1', protocol: 'http', target: 'https://api.example.com/endpoint',
  parameters: {
    method: 'PUT',
    authorization: { type: 'api_key', header: 'X-Key', value: 'secret' },
    headers: { 'Accept': 'application/json' },
    body: { field: '${fact}' },
    timeout: 30,
  },
  outputMapping: 'myResult',
};
// ... populate store, round-trip, verify
```

---

### 4.2 PropertiesPanel Component Tests

**File to create:** `frontend/src/features/tree-editor/components/PropertiesPanel.test.tsx`

Use `@testing-library/react` + `vitest`.

#### TC-FE-UI-01: HTTP protocol selected — shows method selector, URL, headers, auth, body editor

```tsx
// Render PropertiesPanel with an action node selected that has an HTTP callable action
// Assert:
expect(screen.getByText('Method')).toBeInTheDocument();
expect(screen.getByText('URL')).toBeInTheDocument();
expect(screen.getByText('Headers')).toBeInTheDocument();
expect(screen.getByText('Authorization')).toBeInTheDocument();
expect(screen.getByText('Request Body (JSON)')).toBeInTheDocument();
```

#### TC-FE-UI-02: HTTP method selector has 4 options

```tsx
const select = screen.getByRole('combobox', { name: /method/i });
const options = within(select).getAllByRole('option');
expect(options.map(o => o.textContent)).toEqual(['GET', 'POST', 'PUT', 'DELETE']);
```

#### TC-FE-UI-03: GET method — body editor hidden

```tsx
// Set method to GET
// Assert: "Request Body (JSON)" label not in document
expect(screen.queryByText('Request Body (JSON)')).not.toBeInTheDocument();
```

#### TC-FE-UI-04: POST method — body editor visible

```tsx
// Set method to POST
// Assert: body editor section present
expect(screen.getByText('Request Body (JSON)')).toBeInTheDocument();
```

#### TC-FE-UI-05: Bearer auth — token field appears as password input

```tsx
// Select Bearer Token in auth dropdown
// Assert: password input for token visible with placeholder "Bearer token"
const tokenInput = screen.getByPlaceholderText('Bearer token');
expect(tokenInput).toHaveAttribute('type', 'password');
```

#### TC-FE-UI-06: API key auth — header name + value fields appear

```tsx
// Select API Key in auth dropdown
// Assert: header name input (placeholder "Header name") and value (password input) present
expect(screen.getByPlaceholderText('Header name')).toBeInTheDocument();
expect(screen.getByPlaceholderText('API key value')).toBeInTheDocument();
```

#### TC-FE-UI-07: None auth — no token/key fields

```tsx
// Auth type = "none"
// Assert: neither "Bearer token" nor "API key value" placeholders in DOM
expect(screen.queryByPlaceholderText('Bearer token')).not.toBeInTheDocument();
expect(screen.queryByPlaceholderText('API key value')).not.toBeInTheDocument();
```

#### TC-FE-UI-08: Script protocol selected — shows Monaco JS editor and hint panel

```tsx
// Action with protocol = 'script'
expect(screen.getByText('Script')).toBeInTheDocument();
// Hint panel visible:
expect(screen.getByText(/evaluate/i)).toBeInTheDocument();
expect(screen.getByText(/sandboxed/i)).toBeInTheDocument();
```

#### TC-FE-UI-09: Script editor pre-populated with DEFAULT_SCRIPT_TEMPLATE for new action

```tsx
// New action with protocol = 'script', target = DEFAULT_SCRIPT_TEMPLATE
// Assert Monaco editor value contains "function evaluate(params)"
// (Monaco value can be checked via the onChange prop or data-testid)
```

#### TC-FE-UI-10: Adding a header row updates parameters.headers

```tsx
// Fill in "Header name" = "X-Test" and "Header value" = "hello"
// Click add button
// Assert: store's callable action parameters.headers["X-Test"] == "hello"
```

#### TC-FE-UI-11: Deleting a header removes it from parameters.headers

```tsx
// Start with headers = { "X-Test": "hello" }
// Click the trash icon next to "X-Test"
// Assert: parameters.headers no longer contains "X-Test"
```

#### TC-FE-UI-12: outputMapping field saves to CallableAction.outputMapping

```tsx
// Type "myResult" in the Output Mapping field
// Assert: store's callable action outputMapping == "myResult"
```

#### TC-FE-UI-13: Switching protocol resets parameters

```tsx
// Start with protocol = 'http', parameters = { method: 'GET', headers: {...} }
// Switch to protocol = 'log'
// Assert: parameters == {} (reset on switch)
```

#### TC-FE-UI-14: Adding new callable action — script protocol pre-fills target with template

```tsx
// Show new form, select "Script", click "Add Action"
// Assert: new action has target == DEFAULT_SCRIPT_TEMPLATE
//         new action has parameters == {}
```

#### TC-FE-UI-15: Inline validation — invalid JSON in body shows Monaco error markers

```
Manual / visual test (Monaco language service handles this automatically).
Verify: opening the body editor and typing malformed JSON shows a red squiggly underline.
```

---

## 5. API-Level Tests (Integration)

Run with services live (`./scripts/start-local.sh`). Use `curl` or an API client (Postman / httpie). Authenticate as `admin@ruleforge.io` / `password123`.

### 5.1 Create a TREE Rule with HTTP Callable Action

#### TC-API-HTTP-01: Create rule with HTTP callable action via management API

```bash
POST /v1/rules
{
  "spaceId": "<space-id>",
  "name": "HTTP Action Test",
  "type": "TREE",
  "definition": {
    "root": {
      "nodeType": "ACTION",
      "nodeId": "n1",
      "label": "Notify",
      "outputs": { "result": "notified" },
      "callableActions": [{
        "actionId": "ca_1",
        "protocol": "http",
        "target": "https://httpbin.org/post",
        "parameters": {
          "method": "POST",
          "body": { "msg": "hello" }
        },
        "outputMapping": "httpResponse"
      }]
    },
    "scoringMode": false,
    "earlyTermination": true
  }
}

Expected: 201 Created, rule returned with id
```

#### TC-API-HTTP-02: Fetch the created rule — definition preserved

```bash
GET /v1/rules/{ruleId}
Expected: 200, definition.root.callableActions[0].protocol == "http"
          All parameters intact (method, body, outputMapping)
```

#### TC-API-HTTP-03: Update rule — add authorization to existing HTTP action

```bash
PUT /v1/rules/{ruleId}/versions/{version}
(update parameters to add authorization: { type: "bearer", token: "tok" })
Expected: 200, updated definition returned with authorization preserved
```

### 5.2 Create a TREE Rule with Script Callable Action

#### TC-API-SCRIPT-01: Create rule with script callable action

```bash
POST /v1/rules
{
  "definition": {
    "root": {
      "nodeType": "ACTION",
      "nodeId": "n1",
      "label": "Compute",
      "outputs": {},
      "callableActions": [{
        "actionId": "ca_s1",
        "protocol": "script",
        "target": "function evaluate(params) { return { doubled: params.x * 2 }; }",
        "parameters": { "x": 5 },
        "outputMapping": null
      }]
    }
  }
}
Expected: 201, definition stored with script target intact
```

### 5.3 Solver Execution with Callable Actions

#### TC-API-SOLVER-01: Execute V029 HTTP rule — Economy path (log action only, no HTTP call)

```bash
POST /v1/solve
{
  "ruleId": "e3000000-0000-0000-0000-000000000001",
  "input": { "orderId": "ORD-TEST", "customerName": "Test", "customerEmail": "test@example.com", "orderTotal": 49.99, "isPriority": false }
}
Expected: 200
          output.fulfillmentType == "ECONOMY"
          output.shippingClass == "ground"
          (no HTTP outbound call attempted — log action only)
```

#### TC-API-SOLVER-02: Execute V029 script rule — Low Risk path

```bash
POST /v1/solve
{
  "ruleId": "e3000000-0000-0000-0000-000000000002",
  "input": { "age": 35, "employmentYears": 8, "annualIncome": 95000 }
}
Expected: 200
          output.riskCategory == "LOW"
          output.approved == "true"
          output.riskAnalysis.riskScore (a number)
          output.riskAnalysis.confidence (a number)
```

#### TC-API-SOLVER-03: Execute V029 script rule — High Risk path (void script)

```bash
POST /v1/solve
{
  "ruleId": "e3000000-0000-0000-0000-000000000002",
  "input": { "age": 22, "employmentYears": 0, "annualIncome": 28000 }
}
Expected: 200
          output.riskCategory == "HIGH"
          output.approved == "false"
          (void_audit_script produced no output keys — no "_result": "undefined" in output)
```

#### TC-API-SOLVER-04: Sample inputs for V029 HTTP rule load correctly

```bash
GET /v1/rules/e3000000-0000-0000-0000-000000000001/samples
Expected: 200, array of 3 samples:
          - "Priority Express Order"
          - "Standard High-Value Order"
          - "Economy Order"
```

#### TC-API-SOLVER-05: Sample inputs for V029 script rule load correctly

```bash
GET /v1/rules/e3000000-0000-0000-0000-000000000002/samples
Expected: 200, array of 3 samples:
          - "Low Risk Applicant"
          - "Medium Risk Applicant"
          - "High Risk — Under Age Threshold"
```

---

## 6. Frontend Smoke Tests (Manual)

Run against the local dev stack. Log in as `admin@ruleforge.io`.

### 6.1 CallableActionsEditor — HTTP Protocol

| Step | Expected |
|------|----------|
| Open a TREE rule in the editor | Tree editor loads |
| Click an action node | PropertiesPanel shows "Callable Actions" section |
| Click "Add" → select "HTTP" → click "Add Action" | New HTTP action appears expanded |
| Method dropdown shows GET/POST/PUT/DELETE | All 4 options present |
| Select GET | Body editor disappears |
| Select POST | Body editor (Monaco JSON) appears |
| Expand "Headers" → add key "X-Test" value "hello" → click + | Header row appears with key/value |
| Delete header | Row removed |
| Auth: select "Bearer Token" | Token field (password type) appears |
| Auth: select "API Key" | Header name + key value fields appear |
| Auth: select "None" | Token/key fields gone |
| Fill URL with `${orderId}` | Accepted without error |
| Fill body with `{"id":"${orderId}"}` | Accepted without error |
| Save rule | Save succeeds; reload shows all fields intact |

### 6.2 CallableActionsEditor — Script Protocol

| Step | Expected |
|------|----------|
| Click "Add" → select "Script" → click "Add Action" | Monaco JS editor appears with template pre-filled |
| Template contains `function evaluate(params)` | Confirmed |
| Hint panel shows: evaluate, params, sandboxed | Confirmed |
| Edit the script | Changes accepted |
| Add parameters key "x" value "42" | Parameter row appears |
| Set outputMapping to "result" | Field saves |
| Switch protocol to HTTP | Parameters reset (no old script params) |
| Save rule | Serialization succeeds |

### 6.3 Test Bench with Callable Actions

| Step | Expected |
|------|----------|
| Open V029 script rule "Risk Scoring with Script Actions" | TestBench loads |
| Load sample "Low Risk Applicant" | JSON input populated |
| Click "Run Test" | Result panel shows riskCategory, approved, riskAnalysis keys |
| `riskAnalysis.riskScore` is a number | Confirmed |
| No `_result: "undefined"` in output | Confirmed |
| Open V029 HTTP rule "Order Fulfillment with HTTP Actions" | TestBench loads |
| Load sample "Economy Order" | JSON input populated |
| Click "Run Test" | Result shows fulfillmentType: ECONOMY; no network error (log action only) |

### 6.4 Regression — V025 Log Actions Still Work

| Step | Expected |
|------|----------|
| Open a rule using `log` protocol callable actions | Renders correctly using GenericActionFields |
| Execute rule via TestBench | No regression; log callable action result in output |

---

## 7. Smoke Test Script Verification

```bash
./scripts/smoke-test.sh --verbose
```

Expected:
- Health checks pass for management-service and solver-service
- At minimum one rule creation + solve call succeeds
- No 500 errors in logs

---

## 8. Known Findings and Exclusions

| ID | Finding | Severity | Status |
|----|---------|----------|--------|
| F-001 | `resolveBodyExpressions` does not recurse into List/array values | LOW | Deferred to follow-up |
| F-002 | Script param JSON injected via `JSON.parse('...')` string replacement — brittle with special characters | MEDIUM | Deferred to follow-up |
| F-003 | US-7 inline validation relies on Monaco built-in diagnostics, no custom validation overlay | LOW | Accepted as-is |

**F-001 Reproduction:**
```java
// Body with array value — ${fact} inside array NOT resolved
params.put("body", Map.of("ids", List.of("${userId}", "static")));
// wm.assertFact("userId", "U-42", FactType.STRING);
// Expected: ids = ["U-42", "static"] — Actual: ids = ["${userId}", "static"]
```

**F-002 Reproduction:**
```java
// Param with single quote breaks JSON.parse injection
params.put("name", "O'Brien");
// script: function evaluate(params) { return { name: params.name }; }
// → JSON.parse('{"name":"O\'Brien"}') — may cause SyntaxError in JS
```

---

## 9. Test Coverage Summary

| Area | Tests | Automated | Manual |
|------|-------|-----------|--------|
| HttpActionExecutor | 17 | 17 | 0 |
| ScriptActionExecutor | 14 | 14 | 0 |
| ActionExecutorRegistry | 4 | 4 | 0 |
| TreeTraverser integration | 6 | 6 | 0 |
| Frontend store (Vitest) | 6 | 6 | 0 |
| PropertiesPanel UI (Vitest) | 15 | 14 | 1 (TC-FE-UI-15) |
| API integration | 9 | 0 | 9 |
| Frontend smoke | 28 steps | 0 | 28 |
| **Total** | **99** | **61** | **38** |

---

## 10. Exit Criteria

All of the following must be true before the feature is considered done:

- [ ] All automated engine unit tests pass (`./gradlew test`)
- [ ] All automated frontend unit tests pass (`npm test`)
- [ ] TC-API-SOLVER-02 confirmed: script rule with Low Risk input returns `riskAnalysis.riskScore`
- [ ] TC-API-SOLVER-03 confirmed: void script produces no `_result: "undefined"` in output
- [ ] TC-HTTP-12 confirmed: non-JSON response wrapped as `{ _response, _statusCode }`
- [ ] Frontend smoke tests for HTTP and Script editors pass
- [ ] No regressions in log-protocol callable actions
- [ ] F-001 and F-002 tracked in backlog with reproduction cases
