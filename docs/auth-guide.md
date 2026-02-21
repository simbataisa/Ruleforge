# Authentication & Authorization Guide

## Overview

RuleForge provides three authentication mechanisms: **JWT tokens**, **API Keys**, and **SSO (SAML 2.0 / OIDC)**. Access control is enforced through Role-Based Access Control (RBAC) with five roles, and data isolation is achieved via multi-tenancy using the hierarchy: Organization > Space > Folder > Rule.

All authentication is handled by the **auth-service** (port 8081), with requests routed through the Kong API Gateway (port 8000).

---

## JWT Authentication

RuleForge uses JSON Web Tokens for user authentication. Tokens are signed using **HMAC-SHA512** via the JJWT library.

- **Access tokens**: 1 hour TTL
- **Refresh tokens**: 7 days TTL
- **Claims**: `sub` (userId), `orgId`, `email`, `role`, `type` (access or refresh)
- **Header format**: `Authorization: Bearer <token>`

### Register

Creates a new user account along with a default organization and an OWNER membership.

**POST** `/v1/auth/register`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecureP@ssw0rd",
  "fullName": "Jane Smith"
}
```

**Example:**
```bash
curl -X POST http://localhost:8000/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecureP@ssw0rd",
    "fullName": "Jane Smith"
  }'
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "fullName": "Jane Smith"
    }
  },
  "error": null
}
```

### Login

Authenticates an existing user and returns a token pair.

**POST** `/v1/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecureP@ssw0rd"
}
```

**Example:**
```bash
curl -X POST http://localhost:8000/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecureP@ssw0rd"
  }'
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "fullName": "Jane Smith"
    }
  },
  "error": null
}
```

### Refresh Token

Exchanges a valid refresh token for a new access/refresh token pair. Use this to obtain a new access token before or after the current one expires.

**POST** `/v1/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Example:**
```bash
curl -X POST http://localhost:8000/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...<new>",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...<new>",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "fullName": "Jane Smith"
    }
  },
  "error": null
}
```

---

## API Key Authentication

API keys provide programmatic access to RuleForge without requiring a user login. They are ideal for service-to-service communication and CI/CD pipelines.

- **Two types**:
  - `SOLVER` -- prefixed with `rf_slv_` followed by 32 random characters. Grants access to the solve endpoint only.
  - `MANAGEMENT` -- prefixed with `rf_mgt_` followed by 32 random characters. Grants access to rule management endpoints.
- **Header format**: `X-API-Key: <full-key>`
- **Security**: The full key is SHA-256 hashed before storage in the database. Only the prefix is stored in plaintext for lookup purposes.
- **Scope**: Each API key is scoped to a specific organization and space.

### Create API Key

Creates a new API key for a space. The full key is returned **only once** in the response and is never stored or retrievable again.

**POST** `/v1/spaces/{spaceId}/api-keys`

**Request Body:**
```json
{
  "name": "Production Solver Key",
  "keyType": "SOLVER"
}
```

**Example:**
```bash
curl -X POST http://localhost:8000/v1/spaces/abc123/api-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{
    "name": "Production Solver Key",
    "keyType": "SOLVER"
  }'
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "key-uuid-here",
    "name": "Production Solver Key",
    "keyType": "SOLVER",
    "prefix": "rf_slv_a1b2c3d4",
    "fullKey": "rf_slv_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
  },
  "error": null
}
```

> **Important:** Copy and securely store the `fullKey` value immediately. It will not be shown again.

### List API Keys

Returns all API keys for a space. The full key is not included -- only the prefix, type, and permissions are shown.

**GET** `/v1/spaces/{spaceId}/api-keys`

**Example:**
```bash
curl http://localhost:8000/v1/spaces/abc123/api-keys \
  -H "Authorization: Bearer <access-token>"
```

### Revoke API Key

Permanently revokes an API key. This action cannot be undone.

**DELETE** `/v1/api-keys/{keyId}`

**Example:**
```bash
curl -X DELETE http://localhost:8000/v1/api-keys/key-uuid-here \
  -H "Authorization: Bearer <access-token>"
```

**Response:** 204 No Content

---

## SSO Configuration

RuleForge supports Single Sign-On via **SAML 2.0** and **OIDC** providers. SSO configurations are managed per organization with a unique constraint: only one enabled configuration is allowed per (organization, provider type) combination.

SSO configuration entity fields:
- `providerType` -- `SAML` or `OIDC`
- `metadataUrl` -- IdP metadata URL
- `clientId` -- Application client ID at the IdP
- `clientSecretEncrypted` -- Encrypted client secret (for OIDC)
- `attributeMapping` -- JSONB mapping of IdP attributes to RuleForge user fields
- `enabled` -- Boolean flag to activate/deactivate the configuration

CRUD endpoints are available at `/v1/sso/config`.

### Setting Up SAML 2.0

1. **Create SSO configuration:**

```bash
curl -X POST http://localhost:8000/v1/sso/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{
    "providerType": "SAML",
    "metadataUrl": "https://idp.example.com/saml/metadata",
    "clientId": "ruleforge-sp",
    "enabled": true,
    "attributeMapping": {
      "email": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
      "fullName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name",
      "role": "http://custom.org/claims/role"
    }
  }'
```

2. **Attribute mapping** maps IdP SAML assertion attributes to RuleForge user fields:

```json
{
  "email": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
  "fullName": "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name",
  "role": "http://custom.org/claims/role"
}
```

3. **SP metadata endpoint** (stub -- implementation coming in Phase 2):

   `GET /v1/sso/saml/metadata` -- Returns the Service Provider metadata XML for configuring your IdP.

4. **Assertion Consumer Service endpoint** (stub):

   `POST /v1/sso/saml/acs` -- Receives SAML assertions from the IdP after authentication.

### Setting Up OIDC

1. **Create SSO configuration:**

```bash
curl -X POST http://localhost:8000/v1/sso/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{
    "providerType": "OIDC",
    "metadataUrl": "https://idp.example.com/.well-known/openid-configuration",
    "clientId": "ruleforge-client-id",
    "clientSecret": "your-client-secret",
    "enabled": true,
    "attributeMapping": {
      "email": "email",
      "fullName": "name"
    }
  }'
```

2. **Configure callback URI** in your Identity Provider:

   `https://<ruleforge-host>/v1/sso/oidc/callback`

3. **OIDC callback endpoint** (stub -- implementation coming in Phase 2):

   `GET /v1/sso/oidc/callback?code=...&state=...` -- Handles the authorization code exchange after the user authenticates with the IdP.

### Future SSO Implementation

Full SSO support will be added in Phase 2 with:

- **Spring Security SAML2 Service Provider** for SAML 2.0 flows
- **Spring Security OAuth2 Client** for OIDC flows
- Complete flow: redirect to IdP --> callback --> user provisioning (auto-create or match existing) --> JWT generation

---

## RBAC Roles

RuleForge uses five roles to control access. Roles are assigned per organization membership.

| Role | Rules | Members | Spaces | API Keys | Solve |
|------|-------|---------|--------|----------|-------|
| **OWNER** | Full | Full | Full | Full | Yes |
| **ADMIN** | Full | Manage | Manage | Manage | Yes |
| **EDITOR** | Create/Edit | View | View | -- | Yes |
| **VIEWER** | View | View | View | -- | Yes |
| **SOLVER** | -- | -- | -- | -- | Yes |

**Role descriptions:**

- **OWNER** -- Full control over the organization including all rules, members, spaces, and API keys. Automatically assigned when a user creates an organization.
- **ADMIN** -- Can manage rules, members, spaces, and API keys but cannot delete the organization or transfer ownership.
- **EDITOR** -- Can create and edit rules and execute them. Read-only access to members and spaces.
- **VIEWER** -- Read-only access to rules, members, and spaces. Can execute rules.
- **SOLVER** -- Can only execute rules via the solve endpoint. No access to management features. Designed for API key-based service accounts.

---

## Multi-Tenancy

### Hierarchy

RuleForge enforces data isolation through a four-level hierarchy:

```
Organization
  └── Space
        └── Folder
              └── Rule
```

- **Organization** -- The top-level tenant. All data belongs to an organization.
- **Space** -- A logical workspace within an organization (e.g., "Production", "Staging", "Team A").
- **Folder** -- Organizes rules within a space into a tree structure (supports nesting via parentFolderId).
- **Rule** -- A decision table, decision tree, or other rule type. Always belongs to a space and optionally to a folder.

### PostgreSQL Row-Level Security (RLS)

All tenant tables use PostgreSQL Row-Level Security to enforce data isolation at the database level:

- RLS policies are enabled via migration **V020**.
- The application sets a session variable `app.current_org_id` before each database request.
- Policies ensure that queries only return rows belonging to the current organization.

Example policy:
```sql
CREATE POLICY org_isolation ON rules
  USING (org_id = current_setting('app.current_org_id')::uuid);
```

This applies to SELECT, INSERT, UPDATE, and DELETE operations, guaranteeing that no cross-tenant data leakage is possible even if application-level checks are bypassed.

### TenantContext

The `TenantContext` provides tenant information to all service-layer code:

- **Populated from JWT claims**: `orgId`, `userId`, `role`
- **Populated from API key lookup**: `orgId`, `spaceId`
- **Implementation**: ThreadLocal-based, cleared after each request completes
- **Usage**: `TenantContext.get()` returns the current tenant context in any service method

```java
// Example usage in a service
TenantContext ctx = TenantContext.get();
UUID orgId = ctx.getOrgId();
UUID userId = ctx.getUserId();
String role = ctx.getRole();
```

---

## Security Configuration

The auth-service security filter chain is configured as follows:

- **CSRF**: Disabled (stateless REST API)
- **Session management**: `STATELESS` (no server-side sessions)
- **Filter chain order**:
  1. `JwtAuthenticationFilter` -- Extracts and validates JWT from the `Authorization` header
  2. `ApiKeyAuthenticationFilter` -- Extracts and validates API key from the `X-API-Key` header
- **Public endpoints** (no authentication required):
  - `/v1/auth/**` -- Registration, login, token refresh
  - `/v1/sso/saml/**` -- SAML metadata and ACS
  - `/v1/sso/oidc/**` -- OIDC callback
  - `/actuator/health` -- Health check
  - `/internal/**` -- Service-to-service calls
- **All other endpoints** require a valid JWT or API key

---

## Production Considerations

- **JWT secret**: Use environment variables (e.g., `RULEFORGE_JWT_SECRET`) to configure the signing key. Never hardcode secrets in `application.yml`.
- **Secrets management**: Use a dedicated secrets manager such as AWS Secrets Manager, HashiCorp Vault, or Azure Key Vault for production deployments.
- **API key rotation**: Rotate API keys periodically. Create a new key, update consuming services, then revoke the old key.
- **TLS/HTTPS**: Enable TLS termination at the Kong API Gateway or your load balancer. All traffic between clients and RuleForge should be encrypted.
- **Password policies**: Enforce strong password requirements for user registration (minimum length, complexity rules).
- **Rate limiting**: Configure rate limiting at the Kong gateway to prevent brute-force attacks on authentication endpoints.
- **Audit logging**: All authentication events (login, logout, key creation, key revocation) are published to Kafka and stored in the audit trail via the event-service.
