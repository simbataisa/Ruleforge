# API Reference

## Base URLs

| Access Method | URL |
|---------------|-----|
| Via Kong Gateway | `http://localhost:8000` |
| Auth Service (direct) | `http://localhost:8081` |
| Management Service (direct) | `http://localhost:8082` |
| Solver Service (direct) | `http://localhost:8083` |
| Event Service (direct) | `http://localhost:8084` |
| Analytics Service (direct) | `http://localhost:8085` |
| Scheduler Service (direct) | `http://localhost:8086` |
| Connector Service (direct) | `http://localhost:8087` |

## Authentication

All endpoints (except public ones) require one of the following:

- **JWT**: `Authorization: Bearer <token>`
- **API Key**: `X-API-Key: rf_slv_... or rf_mgt_...`

Public endpoints that do not require authentication: `/v1/auth/**`, `/v1/sso/saml/**`, `/v1/sso/oidc/**`, `/actuator/health`, `/internal/**`.

---

## Auth Service (port 8081)

### Authentication

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/auth/register` | RegisterRequest | AuthResponse | 201 Created. Creates user + default org + OWNER membership |
| POST | `/v1/auth/login` | LoginRequest | AuthResponse | Returns access token, refresh token, and user |
| POST | `/v1/auth/refresh` | RefreshRequest | AuthResponse | Exchanges refresh token for new token pair |

**RegisterRequest:**
```json
{
  "email": "string",
  "password": "string",
  "fullName": "string"
}
```

**LoginRequest:**
```json
{
  "email": "string",
  "password": "string"
}
```

**RefreshRequest:**
```json
{
  "refreshToken": "string"
}
```

**AuthResponse:**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "user": {
    "id": "uuid",
    "email": "string",
    "fullName": "string"
  }
}
```

### Users

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| GET | `/v1/users/me` | -- | UserDto | Returns the currently authenticated user |

### Organizations

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/organizations` | CreateOrganizationRequest | OrganizationDto | 201 Created |
| GET | `/v1/organizations` | -- | List\<OrganizationDto\> | Lists organizations the user belongs to |
| GET | `/v1/organizations/{orgId}` | -- | OrganizationDto | Get organization by ID |
| PUT | `/v1/organizations/{orgId}` | CreateOrganizationRequest | OrganizationDto | Update organization |

**CreateOrganizationRequest:**
```json
{
  "name": "string",
  "description": "string"
}
```

### Spaces

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/organizations/{orgId}/spaces` | CreateSpaceRequest | SpaceDto | 201 Created |
| GET | `/v1/organizations/{orgId}/spaces` | -- | List\<SpaceDto\> | List all spaces in the organization |
| GET | `/v1/organizations/{orgId}/spaces/{spaceId}` | -- | SpaceDto | Get space by ID |

**CreateSpaceRequest:**
```json
{
  "name": "string",
  "description": "string"
}
```

### Members

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/organizations/{orgId}/members` | AddMemberRequest | MemberDto | 201 Created |
| GET | `/v1/organizations/{orgId}/members` | -- | List\<MemberDto\> | List all members |
| PUT | `/v1/organizations/{orgId}/members/{memberId}/role` | UpdateRoleRequest | MemberDto | Change member role |
| DELETE | `/v1/organizations/{orgId}/members/{memberId}` | -- | -- | 204 No Content. Remove member |

**AddMemberRequest:**
```json
{
  "email": "string",
  "role": "OWNER | ADMIN | EDITOR | VIEWER | SOLVER"
}
```

**UpdateRoleRequest:**
```json
{
  "role": "OWNER | ADMIN | EDITOR | VIEWER | SOLVER"
}
```

### API Keys

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/spaces/{spaceId}/api-keys` | CreateApiKeyRequest | ApiKeyCreatedDto | 201 Created. Full key returned only once |
| GET | `/v1/spaces/{spaceId}/api-keys` | -- | List\<ApiKeyDto\> | Lists keys (prefix only, no full key) |
| DELETE | `/v1/api-keys/{keyId}` | -- | -- | 204 No Content. Permanently revokes the key |

**CreateApiKeyRequest:**
```json
{
  "name": "string",
  "keyType": "SOLVER | MANAGEMENT"
}
```

**ApiKeyCreatedDto:**
```json
{
  "id": "uuid",
  "name": "string",
  "keyType": "SOLVER | MANAGEMENT",
  "prefix": "string",
  "fullKey": "string"
}
```

### SSO Configuration

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/sso/config` | CreateSsoConfigRequest | SsoConfigDto | 201 Created |
| GET | `/v1/sso/config?orgId={id}` | -- | List\<SsoConfigDto\> | List SSO configs for an org |
| GET | `/v1/sso/config/{id}` | -- | SsoConfigDto | Get SSO config by ID |
| PUT | `/v1/sso/config/{id}` | UpdateSsoConfigRequest | SsoConfigDto | Update SSO config |
| DELETE | `/v1/sso/config/{id}` | -- | -- | 204 No Content |
| GET | `/v1/sso/saml/metadata` | -- | String | Stub. Returns SP metadata XML |
| POST | `/v1/sso/saml/acs` | -- | -- | Stub. Assertion Consumer Service |
| GET | `/v1/sso/oidc/callback` | -- | -- | Stub. Params: `code`, `state` |

**CreateSsoConfigRequest:**
```json
{
  "providerType": "SAML | OIDC",
  "metadataUrl": "string",
  "clientId": "string",
  "clientSecret": "string (optional, for OIDC)",
  "attributeMapping": { "email": "string", "fullName": "string", "role": "string" },
  "enabled": true
}
```

---

## Management Service (port 8082)

### Rules

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/spaces/{spaceId}/rules` | CreateRuleRequest | RuleDto | 201 Created |
| GET | `/v1/spaces/{spaceId}/rules` | -- | PageResponse\<RuleDto\> | Params: `folderId`, `status`, `page`, `size` |
| GET | `/v1/rules/{ruleId}` | -- | RuleDto | Get rule by ID |
| PUT | `/v1/rules/{ruleId}` | UpdateRuleRequest | RuleDto | Update rule |
| DELETE | `/v1/rules/{ruleId}` | -- | -- | Delete rule |
| POST | `/v1/rules/{ruleId}/lock` | -- | RuleDto | Acquire edit lock |
| DELETE | `/v1/rules/{ruleId}/lock` | -- | RuleDto | Release edit lock |
| POST | `/v1/rules/{ruleId}/validate` | -- | ValidationResultDto | Validate rule definition |
| POST | `/v1/rules/{ruleId}/test` | TestRuleRequest | TestResultDto | Test rule with sample input |

**CreateRuleRequest:**
```json
{
  "name": "string",
  "description": "string",
  "ruleType": "DECISION_TABLE | DECISION_TREE",
  "folderId": "uuid (optional)",
  "definition": { }
}
```

**TestRuleRequest:**
```json
{
  "input": { "key": "value" },
  "version": 1
}
```

### Versions

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/rules/{ruleId}/versions` | CreateVersionRequest | RuleVersionDto | 201 Created |
| GET | `/v1/rules/{ruleId}/versions` | -- | List\<RuleVersionDto\> | List all versions |
| GET | `/v1/rules/{ruleId}/versions/{version}` | -- | RuleVersionDto | Get specific version |
| POST | `/v1/rules/{ruleId}/versions/{version}/publish` | -- | RuleVersionDto | Publish version (makes it active) |
| POST | `/v1/rules/{ruleId}/versions/{version}/deprecate` | -- | RuleVersionDto | Deprecate version |

**CreateVersionRequest:**
```json
{
  "description": "string",
  "definition": { }
}
```

### Folders

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/spaces/{spaceId}/folders` | CreateFolderRequest | FolderDto | 201 Created |
| GET | `/v1/spaces/{spaceId}/folders` | -- | List\<FolderDto\> | Returns folder tree |
| GET | `/v1/folders/{folderId}` | -- | FolderDto | Get folder by ID |
| PUT | `/v1/folders/{folderId}` | UpdateFolderRequest | FolderDto | Update folder |
| DELETE | `/v1/folders/{folderId}` | -- | -- | Delete folder |

**CreateFolderRequest:**
```json
{
  "name": "string",
  "parentFolderId": "uuid (optional)"
}
```

### Tags

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/organizations/{orgId}/tags` | CreateTagRequest | TagDto | 201 Created |
| GET | `/v1/organizations/{orgId}/tags` | -- | List\<TagDto\> | List all tags |
| PUT | `/v1/tags/{tagId}` | CreateTagRequest | TagDto | Update tag |
| DELETE | `/v1/tags/{tagId}` | -- | -- | Delete tag |
| POST | `/v1/rules/{ruleId}/tags/{tagId}` | -- | -- | Add tag to rule |
| DELETE | `/v1/rules/{ruleId}/tags/{tagId}` | -- | -- | Remove tag from rule |

**CreateTagRequest:**
```json
{
  "name": "string",
  "color": "string (optional)"
}
```

### Import/Export

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/rules/import` | MultipartFile | ImportResult | Params: `file`, `spaceId`, `createdBy` |
| GET | `/v1/rules/{id}/export` | -- | byte[] (JSON file) | Params: `spaceId`, `format` |
| POST | `/v1/rules/export-bulk` | List\<UUID\> | byte[] (JSON file) | Param: `spaceId` |

---

## Solver Service (port 8083)

### Rule Execution

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/solve/{ruleIdOrAlias}` | SolveRequest | SolveResponse | Execute latest published version |
| POST | `/v1/solve/{ruleIdOrAlias}/{version}` | SolveRequest | SolveResponse | Execute specific version |
| POST | `/v1/solve/bulk/{ruleIdOrAlias}` | BulkSolveRequest | BulkSolveResponse | Batch execution (latest version) |
| POST | `/v1/solve/bulk/{ruleIdOrAlias}/{version}` | BulkSolveRequest | BulkSolveResponse | Batch execution (specific version) |

**Headers:**
- `X-Correlation-Id` (optional) -- Correlation ID for tracing requests across services

**SolveRequest:**
```json
{
  "input": {
    "age": 30,
    "income": 75000,
    "creditScore": 720
  },
  "options": {
    "trace": true,
    "executionMode": "FIRST_MATCH"
  }
}
```

**SolveResponse:**
```json
{
  "result": {
    "matched": true,
    "outputs": { "riskCategory": "LOW", "approved": true },
    "matchedRules": ["row-3"]
  },
  "trace": {
    "nodes": [ ]
  },
  "metadata": {
    "ruleId": "uuid",
    "version": 1,
    "executionTimeMs": 12,
    "correlationId": "string"
  }
}
```

**BulkSolveRequest:**
```json
{
  "inputs": [
    { "age": 30, "income": 75000 },
    { "age": 45, "income": 120000 }
  ],
  "options": {
    "trace": false
  }
}
```

**Caching:**
The solver uses a three-tier cache for compiled rules:
1. **L1**: Caffeine in-memory cache (per instance)
2. **L2**: Redis distributed cache (key pattern: `tenant:{orgId}:rule:{ruleId}:{version}:compiled`)
3. **L3**: PostgreSQL (source of truth)

---

## Event Service (port 8084)

### Audit Events

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| GET | `/v1/audit` | -- | PageResponse\<AuditEventDto\> | Params: `eventType`, `page`, `size` |
| GET | `/v1/audit/resources/{resourceType}/{resourceId}` | -- | PageResponse\<AuditEventDto\> | Params: `page`, `size` |

**AuditEventDto:**
```json
{
  "id": "uuid",
  "eventType": "RULE_CREATED | RULE_UPDATED | RULE_DELETED | RULE_PUBLISHED | ...",
  "resourceType": "RULE | FOLDER | TAG | USER | ...",
  "resourceId": "uuid",
  "userId": "uuid",
  "orgId": "uuid",
  "metadata": { },
  "createdAt": "2026-02-21T10:30:00Z"
}
```

### Webhooks

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/webhooks` | CreateWebhookRequest | WebhookDto | 201 Created |
| GET | `/v1/webhooks` | -- | List\<WebhookDto\> | List all webhooks |
| GET | `/v1/webhooks/{id}` | -- | WebhookDto | Get webhook by ID |
| PUT | `/v1/webhooks/{id}` | UpdateWebhookRequest | WebhookDto | Update webhook |
| DELETE | `/v1/webhooks/{id}` | -- | -- | Delete webhook |
| GET | `/v1/webhooks/{id}/deliveries` | -- | PageResponse\<WebhookDeliveryDto\> | Params: `page`, `size` |

**CreateWebhookRequest:**
```json
{
  "url": "https://example.com/webhook",
  "events": ["RULE_PUBLISHED", "RULE_DELETED"],
  "secret": "string (optional, for HMAC signature verification)",
  "enabled": true
}
```

**WebhookDeliveryDto:**
```json
{
  "id": "uuid",
  "webhookId": "uuid",
  "eventType": "string",
  "statusCode": 200,
  "success": true,
  "requestBody": "string",
  "responseBody": "string",
  "deliveredAt": "2026-02-21T10:30:00Z"
}
```

**Kafka Topics Consumed:**
- `rule.lifecycle` -- Rule CRUD and version events
- `rule.execution` -- Solve request/response events
- `user.activity` -- Login, registration, role changes
- `system.events` -- System-level events

---

## Analytics Service (port 8085) -- Phase 3

All analytics endpoints currently return **501 Not Implemented**. They will be available in Phase 3.

| Method | Endpoint | Response | Notes |
|--------|----------|----------|-------|
| GET | `/v1/analytics/decisions` | -- | 501 Not Implemented |
| GET | `/v1/analytics/decisions/aggregate` | -- | 501 Not Implemented |
| GET | `/v1/analytics/rules/{id}/stats` | -- | 501 Not Implemented |

---

## Scheduler Service (port 8086)

### Schedules

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/schedules` | CreateScheduleRequest | ScheduleDto | 201 Created |
| GET | `/v1/schedules?spaceId={id}` | -- | List\<ScheduleDto\> | Filter by space |
| GET | `/v1/schedules/{id}` | -- | ScheduleDto | Get schedule by ID |
| PUT | `/v1/schedules/{id}` | UpdateScheduleRequest | ScheduleDto | Update schedule |
| DELETE | `/v1/schedules/{id}` | -- | -- | 204 No Content |
| POST | `/v1/schedules/{id}/trigger` | -- | ScheduleRunDto | Manually trigger a scheduled run |
| GET | `/v1/schedules/{id}/runs` | -- | Page\<ScheduleRunDto\> | Paginated run history |

**CreateScheduleRequest:**
```json
{
  "name": "string",
  "ruleId": "uuid",
  "cronExpression": "0 0 * * *",
  "spaceId": "uuid",
  "inputSource": { "type": "S3", "bucket": "string", "key": "string" },
  "enabled": true
}
```

---

## Connector Service (port 8087)

### Connectors

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/v1/connectors` | CreateConnectorRequest | ConnectorDto | 201 Created |
| GET | `/v1/connectors?spaceId={id}` | -- | List\<ConnectorDto\> | Filter by space |
| GET | `/v1/connectors/{id}` | -- | ConnectorDto | Get connector by ID |
| PUT | `/v1/connectors/{id}` | UpdateConnectorRequest | ConnectorDto | Update connector |
| DELETE | `/v1/connectors/{id}` | -- | -- | 204 No Content |
| POST | `/v1/connectors/{id}/test` | -- | TestConnectionResponse | Test connectivity to external system |

**CreateConnectorRequest:**
```json
{
  "name": "string",
  "type": "DATABASE | HTTP | S3",
  "spaceId": "uuid",
  "config": {
    "url": "string",
    "username": "string",
    "password": "string"
  }
}
```

### Enrichment (Internal)

| Method | Endpoint | Body | Response | Notes |
|--------|----------|------|----------|-------|
| POST | `/internal/enrich` | EnrichRequest | EnrichResponse | Service-to-service only. Not exposed via Kong |

**EnrichRequest:**
```json
{
  "connectorId": "uuid",
  "query": "string",
  "parameters": { }
}
```

---

## Kong Gateway Routes

| Path Prefix | Backend Service | Port |
|-------------|----------------|------|
| `/v1/auth`, `/v1/users`, `/v1/organizations`, `/v1/spaces`, `/v1/api-keys` | auth-service | 8081 |
| `/v1/rules`, `/v1/folders`, `/v1/tags`, `/v1/webhooks` | management-service | 8082 |
| `/v1/solve` | solver-service | 8083 |
| `/v1/analytics` | analytics-service | 8085 |
| `/v1/ai` | ai-service | 8088 |
| `/v1/schedules`, `/v1/batch` | scheduler-service | 8086 |
| `/v1/connectors`, `/v1/connections` | connector-service | 8087 |

---

## Common Response Wrapper

All API responses are wrapped in `ApiResponse<T>`:

**Success:**
```json
{
  "success": true,
  "data": { },
  "error": null
}
```

**Error:**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ENTITY_NOT_FOUND",
    "message": "Rule not found"
  }
}
```

**Common error codes:**

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid request body or parameters |
| `UNAUTHORIZED` | 401 | Missing or invalid authentication |
| `FORBIDDEN` | 403 | Insufficient role/permissions |
| `ENTITY_NOT_FOUND` | 404 | Requested resource does not exist |
| `CONFLICT` | 409 | Resource conflict (e.g., duplicate name, locked rule) |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## Pagination

Paginated endpoints use Spring Data `Pageable` conventions:

**Query Parameters:**
- `page` -- Page number (0-based, default: 0)
- `size` -- Page size (default: 20)
- `sort` -- Sort field and direction (e.g., `createdAt,desc`)

**Response: `PageResponse<T>`**
```json
{
  "content": [ ],
  "totalElements": 150,
  "totalPages": 8,
  "page": 0,
  "size": 20
}
```
