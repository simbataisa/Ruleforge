# API Contract Template

## Service Metadata
- **Service Name:** [Service Name]
- **API Version:** 1.0.0
- **Service Owner:** [Team/Owner Name]
- **Last Updated:** [ISO 8601 Date]
- **Status:** [Alpha | Beta | Stable | Deprecated]

---

## Service Overview

### Purpose
[Clear one-to-two sentence description of what this service does and its primary business function]

### Base URL
- **Development:** `https://api-dev.internal.bmad.io/v1`
- **Staging:** `https://api-staging.internal.bmad.io/v1`
- **Production:** `https://api.bmad.io/v1`

### Authentication Method
- **Type:** [Bearer Token | OAuth 2.0 | mTLS | API Key]
- **Header:** `Authorization: Bearer {token}`
- **Scope Requirements:** [List required scopes if OAuth]
- **Token Lifetime:** [Duration]
- **Refresh Mechanism:** [How to refresh/renew]

### Rate Limiting
- **Rate Limit Header:** `X-RateLimit-Limit: {limit}`
- **Remaining Header:** `X-RateLimit-Remaining: {remaining}`
- **Reset Header:** `X-RateLimit-Reset: {unix-timestamp}`
- **Default Limit:** 1000 requests per minute per API key
- **Burst Allowance:** 50 requests in 10-second window

---

## API Endpoints Summary

| HTTP Method | Path | Description | Auth Required | Deprecated |
|---|---|---|---|---|
| GET | `/health` | Service health check | No | No |
| POST | `/resources` | Create a new resource | Yes | No |
| GET | `/resources` | List all resources (paginated) | Yes | No |
| GET | `/resources/{id}` | Retrieve a specific resource | Yes | No |
| PUT | `/resources/{id}` | Update an existing resource | Yes | No |
| DELETE | `/resources/{id}` | Delete a resource | Yes | No |
| POST | `/resources/{id}/actions/process` | Perform custom action on resource | Yes | No |

---

## Detailed Endpoint Specifications

### 1. Health Check
**Endpoint:** `GET /health`
**Authentication:** Not required
**Rate Limited:** No

#### Request
```
GET /health HTTP/1.1
Host: api.bmad.io
```

#### Response - Success (200 OK)
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "timestamp": "2026-03-14T10:30:00Z",
  "uptime_seconds": 86400,
  "dependencies": {
    "database": "healthy",
    "cache": "healthy",
    "message_queue": "healthy"
  }
}
```

#### Response - Degraded (503 Service Unavailable)
```json
{
  "status": "degraded",
  "version": "1.0.0",
  "timestamp": "2026-03-14T10:30:00Z",
  "uptime_seconds": 86400,
  "dependencies": {
    "database": "healthy",
    "cache": "unhealthy",
    "message_queue": "healthy"
  },
  "message": "Cache service is unreachable"
}
```

---

### 2. Create Resource
**Endpoint:** `POST /resources`
**Authentication:** Required (Bearer token)
**Rate Limited:** Yes

#### Request Headers
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json
X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
X-Request-ID: 123e4567-e89b-12d3-a456-426614174000
```

#### Query Parameters
None

#### Path Parameters
None

#### Request Body
```json
{
  "name": "Resource Name",
  "description": "Detailed description of the resource",
  "type": "standard",
  "attributes": {
    "category": "invoices",
    "priority": "high"
  },
  "metadata": {
    "client_id": "client-123",
    "source": "api"
  }
}
```

#### Request Body Schema
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["name", "type"],
  "properties": {
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 255,
      "description": "Human-readable name for the resource"
    },
    "description": {
      "type": "string",
      "maxLength": 1000,
      "description": "Detailed description of the resource's purpose"
    },
    "type": {
      "type": "string",
      "enum": ["standard", "premium", "enterprise"],
      "description": "Classification of the resource tier"
    },
    "attributes": {
      "type": "object",
      "additionalProperties": true,
      "description": "Custom attributes as key-value pairs"
    },
    "metadata": {
      "type": "object",
      "additionalProperties": true,
      "description": "Additional metadata for tracking and correlation"
    }
  }
}
```

#### Response - Success (201 Created)
```json
{
  "id": "res-550e8400-e29b-41d4-a716-446655440000",
  "name": "Resource Name",
  "description": "Detailed description of the resource",
  "type": "standard",
  "status": "active",
  "created_at": "2026-03-14T10:30:00Z",
  "updated_at": "2026-03-14T10:30:00Z",
  "created_by": "user-123",
  "attributes": {
    "category": "invoices",
    "priority": "high"
  },
  "metadata": {
    "client_id": "client-123",
    "source": "api"
  },
  "_links": {
    "self": {
      "href": "/resources/res-550e8400-e29b-41d4-a716-446655440000"
    },
    "update": {
      "href": "/resources/res-550e8400-e29b-41d4-a716-446655440000",
      "method": "PUT"
    }
  }
}
```

#### Response Body Schema
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^res-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
      "description": "Unique resource identifier (UUID v4 prefixed with 'res-')"
    },
    "name": { "type": "string" },
    "type": { "type": "string" },
    "status": { "type": "string", "enum": ["active", "inactive", "archived"] },
    "created_at": { "type": "string", "format": "date-time" },
    "updated_at": { "type": "string", "format": "date-time" },
    "created_by": { "type": "string" }
  }
}
```

#### Error Responses

| HTTP Status | Error Code | Message | When |
|---|---|---|---|
| 400 | `INVALID_REQUEST_BODY` | Request body failed schema validation | Missing required fields or invalid field types |
| 400 | `INVALID_RESOURCE_TYPE` | Resource type '{type}' is not valid | Unsupported enum value for `type` field |
| 401 | `UNAUTHORIZED` | Authentication token is invalid or expired | Missing or malformed Authorization header |
| 403 | `FORBIDDEN` | User does not have permission to create resources | Insufficient scopes or quota exceeded |
| 409 | `RESOURCE_ALREADY_EXISTS` | Resource with idempotency key already exists (ID: {id}) | Duplicate idempotency key with different payload |
| 429 | `RATE_LIMIT_EXCEEDED` | Rate limit of 1000 requests/min exceeded | Too many requests in time window |
| 500 | `INTERNAL_SERVER_ERROR` | An unexpected error occurred. Please retry. (Trace ID: {traceId}) | Server-side processing error |
| 503 | `SERVICE_UNAVAILABLE` | Service is temporarily unavailable | Dependency failure or maintenance window |

---

### 3. List Resources
**Endpoint:** `GET /resources`
**Authentication:** Required
**Rate Limited:** Yes
**Pagination:** Supported (cursor-based)

#### Query Parameters
| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `limit` | integer | No | 20 | Number of results per page (1-100) |
| `cursor` | string | No | null | Opaque cursor for pagination. Use value from previous response's `next_cursor` |
| `filter[type]` | string | No | null | Filter by resource type (standard\|premium\|enterprise) |
| `filter[status]` | string | No | null | Filter by status (active\|inactive\|archived) |
| `sort` | string | No | `-created_at` | Sort field and direction: `name`, `-name`, `created_at`, `-created_at` |
| `search` | string | No | null | Full-text search across name and description |

#### Response - Success (200 OK)
```json
{
  "data": [
    {
      "id": "res-550e8400-e29b-41d4-a716-446655440000",
      "name": "Resource One",
      "type": "standard",
      "status": "active",
      "created_at": "2026-03-14T10:30:00Z",
      "updated_at": "2026-03-14T10:30:00Z"
    },
    {
      "id": "res-660e8400-e29b-41d4-a716-446655440111",
      "name": "Resource Two",
      "type": "premium",
      "status": "active",
      "created_at": "2026-03-13T15:45:00Z",
      "updated_at": "2026-03-13T15:45:00Z"
    }
  ],
  "pagination": {
    "limit": 20,
    "cursor": "eyJpZCI6ICJyZXMtNjYwZTg0MDAiLCAidHMiOiAxNzEwMzk3MzAwfQ==",
    "next_cursor": "eyJpZCI6ICJyZXMtNzcwZTg0MDAiLCAidHMiOiAxNzEwMzk2NzAwfQ==",
    "has_more": true,
    "total_count": 245
  },
  "meta": {
    "request_id": "req-123e4567-e89b-12d3-a456-426614174000",
    "timestamp": "2026-03-14T10:30:00Z"
  }
}
```

---

## Event Contracts (Async/Event-Driven Services)

### Events Published by This Service

| Event Name | Topic/Queue | Producer | Consumer(s) | Ordering |
|---|---|---|---|---|
| `resource.created` | `bmad.resources.events` | Resource Service | Notification Service, Analytics Service | Per resource ID |
| `resource.updated` | `bmad.resources.events` | Resource Service | Notification Service, Analytics Service | Per resource ID |
| `resource.deleted` | `bmad.resources.events` | Resource Service | Audit Service, Notification Service | Per resource ID |
| `resource.processed` | `bmad.resources.completion` | Resource Service | Downstream Workflow Service | Per resource ID, monotonic increasing by timestamp |

### Event Schema Example: `resource.created`
```json
{
  "event_type": "resource.created",
  "event_id": "evt-550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-14T10:30:00Z",
  "version": "1",
  "source": "resource-service",
  "source_version": "1.0.0",
  "correlation_id": "corr-123e4567-e89b-12d3-a456-426614174000",
  "causation_id": "req-123e4567-e89b-12d3-a456-426614174000",
  "data": {
    "resource_id": "res-550e8400-e29b-41d4-a716-446655440000",
    "name": "Resource Name",
    "type": "standard",
    "created_by": "user-123",
    "attributes": {
      "category": "invoices"
    }
  }
}
```

**Delivery Guarantees:**
- At-least-once delivery (duplicates possible; consumers must be idempotent using `event_id`)
- Ordering guaranteed per partition key (`resource_id`)
- Retention: 7 days in message broker
- Dead letter queue: `bmad.resources.dlq` for processing errors

---

## Data Models (Shared Schemas)

### Resource Object
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^res-[0-9a-f-]{36}$"
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 255
    },
    "type": {
      "type": "string",
      "enum": ["standard", "premium", "enterprise"]
    },
    "status": {
      "type": "string",
      "enum": ["active", "inactive", "archived"]
    },
    "created_at": {
      "type": "string",
      "format": "date-time"
    },
    "updated_at": {
      "type": "string",
      "format": "date-time"
    }
  },
  "required": ["id", "name", "type", "status", "created_at", "updated_at"]
}
```

### Error Response Envelope
```json
{
  "type": "object",
  "properties": {
    "error": {
      "type": "object",
      "properties": {
        "code": { "type": "string" },
        "message": { "type": "string" },
        "details": { "type": "object" },
        "trace_id": { "type": "string" }
      },
      "required": ["code", "message", "trace_id"]
    }
  }
}
```

---

## Changelog

### 1.0.0 (2026-03-14)
- Initial stable release
- Endpoints: Create, List, Get, Update, Delete
- Event contracts for resource lifecycle
- Support for cursor-based pagination
- Idempotency via X-Idempotency-Key header

### 0.2.0 (2026-03-01)
- Added `metadata` field to resource object
- Added filtering and sorting support to list endpoint
- Beta event contracts

### 0.1.0 (2026-02-15)
- Initial alpha release with basic CRUD operations
