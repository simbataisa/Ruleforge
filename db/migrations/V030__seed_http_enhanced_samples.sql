-- ============================================================================
-- V030: HTTP Enhanced Callable Action Sample Rules
--
-- Adds two TREE rules demonstrating advanced HTTP callable action features:
-- 1. User Lookup with GET & Query Params (GET, queryParams, path params, headers)
-- 2. Multi-Method API Integration (POST, PUT, DELETE with body, queryParams, headers)
--
-- UUID scheme: e4000000-0000-0000-0000-00000000000x (rules)
--              f4000000-0000-0000-0000-00000000000x (versions)
--              a4000000-0000-0000-0000-00000000000x (sample inputs)
-- ============================================================================

-- ============================================================================
-- Rule 1: User Lookup with GET & Query Params
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e4000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'User Lookup with GET & Query Params', 'user-lookup-get-query', 'TREE',
     'Demonstrates GET method with queryParams, path params in URL, and custom headers. Branches by userRole to call different API endpoints with role-specific query parameters and headers.',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f4000000-0000-0000-0000-000000000001',
     'e4000000-0000-0000-0000-000000000001', 1,
     '{
  "root": {
    "nodeType": "CONDITION",
    "nodeId": "role_check",
    "label": "User Role Check",
    "field": "userRole",
    "operator": "EQ",
    "branches": [
      {
        "value": "admin",
        "child": {
          "nodeType": "ACTION",
          "nodeId": "admin_lookup",
          "label": "Admin User Lookup",
          "outputs": {
            "lookupType": "ADMIN",
            "accessLevel": "full",
            "endpoint": "admin/users"
          },
          "callableActions": [
            {
              "actionId": "fetch_admin_user",
              "protocol": "http",
              "target": "https://api.example.com/admin/users/${userId}",
              "parameters": {
                "method": "GET",
                "headers": {
                  "Accept": "application/json",
                  "X-Request-ID": "${requestId}",
                  "X-Admin-Access": "true"
                },
                "queryParams": {
                  "includeInactive": "${includeInactive}",
                  "fields": "all"
                },
                "timeout": 10
              },
              "outputMapping": "userLookupResponse"
            }
          ]
        }
      }
    ],
    "defaultBranch": {
      "nodeType": "ACTION",
      "nodeId": "regular_lookup",
      "label": "Regular User Lookup",
      "outputs": {
        "lookupType": "REGULAR",
        "accessLevel": "basic",
        "endpoint": "users"
      },
      "callableActions": [
        {
          "actionId": "fetch_regular_user",
          "protocol": "http",
          "target": "https://api.example.com/users/${userId}",
          "parameters": {
            "method": "GET",
            "headers": {
              "Accept": "application/json",
              "X-Request-ID": "${requestId}"
            },
            "queryParams": {
              "fields": "basic"
            },
            "timeout": 10
          },
          "outputMapping": "userLookupResponse"
        }
      ]
    }
  },
  "scoringMode": false,
  "earlyTermination": true
}',
     '{"userId":"STRING","userRole":"STRING","requestId":"STRING","includeInactive":"BOOLEAN"}',
     '{"lookupType":"STRING","accessLevel":"STRING","endpoint":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'User lookup tree with GET callable actions demonstrating queryParams, path params in URL, and custom headers')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- Rule 2: Multi-Method API Integration
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e4000000-0000-0000-0000-000000000002',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Multi-Method API Integration', 'multi-method-api', 'TREE',
     'Demonstrates POST, PUT, and DELETE HTTP methods with headers, body, and query params combined. Branches by action field to route to create, update, or cancel order endpoints.',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f4000000-0000-0000-0000-000000000002',
     'e4000000-0000-0000-0000-000000000002', 1,
     '{
  "root": {
    "nodeType": "CONDITION",
    "nodeId": "action_check",
    "label": "Order Action Check",
    "field": "action",
    "operator": "EQ",
    "branches": [
      {
        "value": "create",
        "child": {
          "nodeType": "ACTION",
          "nodeId": "create_order",
          "label": "Create Order (POST)",
          "outputs": {
            "operation": "CREATE",
            "method": "POST",
            "status": "submitted"
          },
          "callableActions": [
            {
              "actionId": "post_create_order",
              "protocol": "http",
              "target": "https://api.example.com/orders",
              "parameters": {
                "method": "POST",
                "headers": {
                  "Content-Type": "application/json",
                  "Accept": "application/json",
                  "X-Priority": "${priority}"
                },
                "body": {
                  "customerName": "${customerName}",
                  "priority": "${priority}",
                  "source": "ruleforge"
                },
                "timeout": 15
              },
              "outputMapping": "apiResponse"
            }
          ]
        }
      },
      {
        "value": "update",
        "child": {
          "nodeType": "ACTION",
          "nodeId": "update_order",
          "label": "Update Order (PUT)",
          "outputs": {
            "operation": "UPDATE",
            "method": "PUT",
            "status": "updated"
          },
          "callableActions": [
            {
              "actionId": "put_update_order",
              "protocol": "http",
              "target": "https://api.example.com/orders/${orderId}",
              "parameters": {
                "method": "PUT",
                "headers": {
                  "Content-Type": "application/json",
                  "Accept": "application/json",
                  "X-Priority": "${priority}"
                },
                "queryParams": {
                  "notify": "true"
                },
                "body": {
                  "customerName": "${customerName}",
                  "priority": "${priority}",
                  "updatedBy": "ruleforge"
                },
                "timeout": 15
              },
              "outputMapping": "apiResponse"
            }
          ]
        }
      }
    ],
    "defaultBranch": {
      "nodeType": "ACTION",
      "nodeId": "cancel_order",
      "label": "Cancel Order (DELETE)",
      "outputs": {
        "operation": "CANCEL",
        "method": "DELETE",
        "status": "cancelled"
      },
      "callableActions": [
        {
          "actionId": "delete_cancel_order",
          "protocol": "http",
          "target": "https://api.example.com/orders/${orderId}",
          "parameters": {
            "method": "DELETE",
            "headers": {
              "Accept": "application/json",
              "X-Priority": "${priority}"
            },
            "queryParams": {
              "reason": "customer_request"
            },
            "timeout": 10
          },
          "outputMapping": "apiResponse"
        }
      ]
    }
  },
  "scoringMode": false,
  "earlyTermination": true
}',
     '{"orderId":"STRING","action":"STRING","customerName":"STRING","priority":"STRING"}',
     '{"operation":"STRING","method":"STRING","status":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Multi-method API tree with POST, PUT, DELETE callable actions demonstrating headers, body, and queryParams combined')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- Sample Inputs for User Lookup Rule
-- ============================================================================
INSERT INTO rule_sample_inputs (id, rule_id, name, description, input, sort_order, created_by) VALUES
    ('a4000000-0000-0000-0000-000000000001',
     'e4000000-0000-0000-0000-000000000001',
     'Admin Full Lookup',
     'Admin user requesting full user details including inactive accounts',
     '{"userId":"USR-42","userRole":"admin","requestId":"req-abc-001","includeInactive":true}',
     1, 'c0000000-0000-0000-0000-000000000001'),
    ('a4000000-0000-0000-0000-000000000002',
     'e4000000-0000-0000-0000-000000000001',
     'Admin Active Only',
     'Admin user requesting only active user details',
     '{"userId":"USR-99","userRole":"admin","requestId":"req-abc-002","includeInactive":false}',
     2, 'c0000000-0000-0000-0000-000000000001'),
    ('a4000000-0000-0000-0000-000000000003',
     'e4000000-0000-0000-0000-000000000001',
     'Regular User Lookup',
     'Non-admin user with basic access — limited fields returned',
     '{"userId":"USR-77","userRole":"viewer","requestId":"req-abc-003","includeInactive":false}',
     3, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- ============================================================================
-- Sample Inputs for Multi-Method API Rule
-- ============================================================================
INSERT INTO rule_sample_inputs (id, rule_id, name, description, input, sort_order, created_by) VALUES
    ('a4000000-0000-0000-0000-000000000004',
     'e4000000-0000-0000-0000-000000000002',
     'Create New Order',
     'Creates a new high-priority order via POST',
     '{"orderId":"","action":"create","customerName":"Jane Doe","priority":"high"}',
     1, 'c0000000-0000-0000-0000-000000000001'),
    ('a4000000-0000-0000-0000-000000000005',
     'e4000000-0000-0000-0000-000000000002',
     'Update Existing Order',
     'Updates an existing order via PUT with notification',
     '{"orderId":"ORD-2024-100","action":"update","customerName":"Jane Doe","priority":"medium"}',
     2, 'c0000000-0000-0000-0000-000000000001'),
    ('a4000000-0000-0000-0000-000000000006',
     'e4000000-0000-0000-0000-000000000002',
     'Cancel Order',
     'Cancels an order via DELETE with reason query parameter',
     '{"orderId":"ORD-2024-200","action":"cancel","customerName":"John Smith","priority":"low"}',
     3, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;
