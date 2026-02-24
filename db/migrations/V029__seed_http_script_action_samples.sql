-- ============================================================================
-- V029: HTTP and Script Callable Action Sample Rules
--
-- Adds two TREE rules demonstrating HTTP and Script callable actions:
-- 1. Order Fulfillment with HTTP actions (POST with auth, headers, body expressions)
-- 2. Risk Scoring with Script actions (evaluate function returning computed values)
--
-- UUID scheme: e3000000-0000-0000-0000-00000000000x (rules)
--              f3000000-0000-0000-0000-00000000000x (versions)
--              g3000000-0000-0000-0000-00000000000x (sample inputs)
-- ============================================================================

-- ============================================================================
-- Rule 1: Order Fulfillment with HTTP Callable Actions
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e3000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Order Fulfillment with HTTP Actions', 'order-fulfillment-http', 'TREE',
     'Evaluates order details and uses HTTP callable actions to notify fulfillment services. Demonstrates POST with bearer auth, headers, and ${expression} resolution in URL and body.',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f3000000-0000-0000-0000-000000000001',
     'e3000000-0000-0000-0000-000000000001', 1,
     '{
  "root": {
    "nodeType": "CONDITION",
    "nodeId": "order_total_check",
    "label": "Order Total Check",
    "field": "orderTotal",
    "operator": "GTE",
    "branches": [
      {
        "value": 1000,
        "child": {
          "nodeType": "CONDITION",
          "nodeId": "priority_check",
          "label": "Priority Customer?",
          "field": "isPriority",
          "operator": "EQ",
          "branches": [
            {
              "value": true,
              "child": {
                "nodeType": "ACTION",
                "nodeId": "priority_express",
                "label": "Priority Express Fulfillment",
                "outputs": {
                  "fulfillmentType": "EXPRESS",
                  "shippingClass": "overnight",
                  "estimatedDays": "1"
                },
                "callableActions": [
                  {
                    "actionId": "notify_warehouse",
                    "protocol": "http",
                    "target": "https://api.warehouse.example.com/v1/orders/${orderId}/fulfill",
                    "parameters": {
                      "method": "POST",
                      "authorization": {
                        "type": "bearer",
                        "token": "warehouse-api-token-abc123"
                      },
                      "headers": {
                        "X-Priority": "HIGH",
                        "X-Correlation-ID": "rf-${orderId}"
                      },
                      "body": {
                        "orderId": "${orderId}",
                        "customerName": "${customerName}",
                        "total": "${orderTotal}",
                        "shippingClass": "overnight",
                        "priority": true
                      },
                      "timeout": 15
                    },
                    "outputMapping": "warehouseResponse"
                  },
                  {
                    "actionId": "notify_customer",
                    "protocol": "http",
                    "target": "https://api.notifications.example.com/v1/send",
                    "parameters": {
                      "method": "POST",
                      "authorization": {
                        "type": "api_key",
                        "header": "X-API-Key",
                        "value": "notif-key-xyz789"
                      },
                      "body": {
                        "to": "${customerEmail}",
                        "template": "express_shipping",
                        "vars": {
                          "name": "${customerName}",
                          "orderId": "${orderId}",
                          "eta": "1 business day"
                        }
                      }
                    },
                    "outputMapping": "notificationResponse"
                  }
                ]
              }
            }
          ],
          "defaultBranch": {
            "nodeType": "ACTION",
            "nodeId": "standard_high_value",
            "label": "Standard High-Value Fulfillment",
            "outputs": {
              "fulfillmentType": "STANDARD",
              "shippingClass": "priority",
              "estimatedDays": "3"
            },
            "callableActions": [
              {
                "actionId": "notify_warehouse_std",
                "protocol": "http",
                "target": "https://api.warehouse.example.com/v1/orders/${orderId}/fulfill",
                "parameters": {
                  "method": "POST",
                  "authorization": {
                    "type": "bearer",
                    "token": "warehouse-api-token-abc123"
                  },
                  "body": {
                    "orderId": "${orderId}",
                    "customerName": "${customerName}",
                    "total": "${orderTotal}",
                    "shippingClass": "priority",
                    "priority": false
                  }
                },
                "outputMapping": "warehouseResponse"
              }
            ]
          }
        }
      }
    ],
    "defaultBranch": {
      "nodeType": "ACTION",
      "nodeId": "economy_fulfillment",
      "label": "Economy Fulfillment",
      "outputs": {
        "fulfillmentType": "ECONOMY",
        "shippingClass": "ground",
        "estimatedDays": "7"
      },
      "callableActions": [
        {
          "actionId": "log_economy",
          "protocol": "log",
          "target": "Economy order ${orderId} for ${customerName}, total: ${orderTotal}",
          "parameters": {"orderId": "orderId", "customerName": "customerName", "orderTotal": "orderTotal"},
          "outputMapping": null
        }
      ]
    }
  },
  "scoringMode": false,
  "earlyTermination": true
}',
     '{"orderId":"STRING","customerName":"STRING","customerEmail":"STRING","orderTotal":"NUMBER","isPriority":"BOOLEAN"}',
     '{"fulfillmentType":"STRING","shippingClass":"STRING","estimatedDays":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Order fulfillment tree with HTTP callable actions demonstrating bearer auth, API key auth, headers, and ${expression} resolution')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- Rule 2: Risk Scoring with Script Callable Actions
-- ============================================================================
INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e3000000-0000-0000-0000-000000000002',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Risk Scoring with Script Actions', 'risk-scoring-script', 'TREE',
     'Evaluates applicant risk using decision tree conditions and Script callable actions to compute weighted risk scores. Demonstrates evaluate function with params and return values.',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f3000000-0000-0000-0000-000000000002',
     'e3000000-0000-0000-0000-000000000002', 1,
     '{
  "root": {
    "nodeType": "CONDITION",
    "nodeId": "age_check",
    "label": "Applicant Age Check",
    "field": "age",
    "operator": "GTE",
    "branches": [
      {
        "value": 25,
        "child": {
          "nodeType": "CONDITION",
          "nodeId": "employment_check",
          "label": "Employment Status",
          "field": "employmentYears",
          "operator": "GTE",
          "branches": [
            {
              "value": 3,
              "child": {
                "nodeType": "ACTION",
                "nodeId": "low_risk",
                "label": "Low Risk — Compute Score",
                "outputs": {
                  "riskCategory": "LOW",
                  "approved": "true"
                },
                "callableActions": [
                  {
                    "actionId": "compute_risk_score",
                    "protocol": "script",
                    "target": "function evaluate(params) { var base = 100; var ageFactor = Math.min(params.age / 10, 5) * 10; var empFactor = Math.min(params.employmentYears, 10) * 5; var incomeFactor = Math.min(params.annualIncome / 10000, 10) * 3; var score = base - ageFactor - empFactor - incomeFactor; return { riskScore: Math.max(score, 5), confidence: 0.92, factors: ''age:'' + ageFactor + '',emp:'' + empFactor + '',income:'' + incomeFactor }; }",
                    "parameters": {
                      "age": 30,
                      "employmentYears": 5,
                      "annualIncome": 75000
                    },
                    "outputMapping": "riskAnalysis"
                  }
                ]
              }
            }
          ],
          "defaultBranch": {
            "nodeType": "ACTION",
            "nodeId": "medium_risk",
            "label": "Medium Risk — Compute Score",
            "outputs": {
              "riskCategory": "MEDIUM",
              "approved": "true"
            },
            "callableActions": [
              {
                "actionId": "compute_medium_score",
                "protocol": "script",
                "target": "function evaluate(params) { var score = 50 + (params.age > 30 ? -5 : 5) + (params.employmentYears * 3); return { riskScore: Math.min(score, 80), confidence: 0.75 }; }",
                "parameters": {
                  "age": 28,
                  "employmentYears": 1,
                  "annualIncome": 45000
                },
                "outputMapping": "riskAnalysis"
              }
            ]
          }
        }
      }
    ],
    "defaultBranch": {
      "nodeType": "ACTION",
      "nodeId": "high_risk",
      "label": "High Risk — Flag for Review",
      "outputs": {
        "riskCategory": "HIGH",
        "approved": "false",
        "reason": "Age below threshold"
      },
      "callableActions": [
        {
          "actionId": "void_audit_script",
          "protocol": "script",
          "target": "function evaluate(params) { var timestamp = new Date().toISOString(); /* void script — side-effect only, no return value */ }",
          "parameters": {
            "applicantAge": 22,
            "action": "FLAGGED_FOR_REVIEW"
          },
          "outputMapping": null
        }
      ]
    }
  },
  "scoringMode": false,
  "earlyTermination": true
}',
     '{"age":"INTEGER","employmentYears":"INTEGER","annualIncome":"INTEGER"}',
     '{"riskCategory":"STRING","approved":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Risk scoring tree with Script callable actions demonstrating evaluate function, return values, and void scripts')
ON CONFLICT (rule_id, version) DO NOTHING;

-- ============================================================================
-- Sample Inputs for HTTP Action Rule
-- ============================================================================
INSERT INTO rule_sample_inputs (id, rule_id, name, description, input, sort_order, created_by) VALUES
    ('a3000000-0000-0000-0000-000000000001',
     'e3000000-0000-0000-0000-000000000001',
     'Priority Express Order',
     'High-value order from priority customer — triggers express fulfillment with HTTP notifications',
     '{"orderId":"ORD-2024-001","customerName":"Alice Johnson","customerEmail":"alice@example.com","orderTotal":2500,"isPriority":true}',
     1, 'c0000000-0000-0000-0000-000000000001'),
    ('a3000000-0000-0000-0000-000000000002',
     'e3000000-0000-0000-0000-000000000001',
     'Standard High-Value Order',
     'High-value order from regular customer — triggers standard priority fulfillment',
     '{"orderId":"ORD-2024-002","customerName":"Bob Smith","customerEmail":"bob@example.com","orderTotal":1500,"isPriority":false}',
     2, 'c0000000-0000-0000-0000-000000000001'),
    ('a3000000-0000-0000-0000-000000000003',
     'e3000000-0000-0000-0000-000000000001',
     'Economy Order',
     'Low-value order — economy fulfillment with log action only',
     '{"orderId":"ORD-2024-003","customerName":"Carol White","customerEmail":"carol@example.com","orderTotal":49.99,"isPriority":false}',
     3, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;

-- ============================================================================
-- Sample Inputs for Script Action Rule
-- ============================================================================
INSERT INTO rule_sample_inputs (id, rule_id, name, description, input, sort_order, created_by) VALUES
    ('a3000000-0000-0000-0000-000000000004',
     'e3000000-0000-0000-0000-000000000002',
     'Low Risk Applicant',
     'Experienced applicant with high income — low risk score computed via script',
     '{"age":35,"employmentYears":8,"annualIncome":95000}',
     1, 'c0000000-0000-0000-0000-000000000001'),
    ('a3000000-0000-0000-0000-000000000005',
     'e3000000-0000-0000-0000-000000000002',
     'Medium Risk Applicant',
     'Young professional with short employment history — medium risk',
     '{"age":28,"employmentYears":1,"annualIncome":45000}',
     2, 'c0000000-0000-0000-0000-000000000001'),
    ('a3000000-0000-0000-0000-000000000006',
     'e3000000-0000-0000-0000-000000000002',
     'High Risk — Under Age Threshold',
     'Young applicant below age threshold — flagged for review via void script',
     '{"age":22,"employmentYears":0,"annualIncome":28000}',
     3, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (rule_id, name) DO NOTHING;
