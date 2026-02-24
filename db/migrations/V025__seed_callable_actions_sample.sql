-- ============================================================================
-- V025: Callable Actions Sample Rule
--
-- Adds a TREE rule that demonstrates callable actions (action-as-function).
-- The tree evaluates loan applications and uses log callable actions to
-- record decisions, demonstrating the callable actions feature end-to-end.
--
-- UUID scheme: e2000000-0000-0000-0000-000000000001 (rule)
--              f2000000-0000-0000-0000-000000000001 (version)
-- ============================================================================

INSERT INTO rules (id, space_id, folder_id, name, alias, type, description, status, latest_version, created_by) VALUES
    ('e2000000-0000-0000-0000-000000000001',
     'd0000000-0000-0000-0000-000000000001',
     'aa000000-0000-0000-0000-000000000001',
     'Loan Decision with Actions', 'loan-decision-actions', 'TREE',
     'Evaluates loan applications and executes callable actions (log) to record decisions. Demonstrates the callable actions feature with action-as-function.',
     'PUBLISHED', 1, 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT (alias) DO NOTHING;

INSERT INTO rule_versions (id, rule_id, version, definition, input_model, output_model, status, created_by, change_note) VALUES
    ('f2000000-0000-0000-0000-000000000001',
     'e2000000-0000-0000-0000-000000000001', 1,
     '{
  "root": {
    "nodeType": "CONDITION",
    "nodeId": "credit_check",
    "label": "Credit Score Check",
    "field": "creditScore",
    "operator": "GTE",
    "branches": [
      {
        "value": 700,
        "child": {
          "nodeType": "CONDITION",
          "nodeId": "income_check",
          "label": "Income Verification",
          "field": "annualIncome",
          "operator": "GTE",
          "branches": [
            {
              "value": 50000,
              "child": {
                "nodeType": "ACTION",
                "nodeId": "approve_prime",
                "label": "Approve - Prime Rate",
                "outputs": {
                  "decision": "APPROVED",
                  "riskTier": "prime",
                  "interestRate": "4.5",
                  "maxLoanAmount": "500000"
                },
                "callableActions": [
                  {
                    "actionId": "log_approval",
                    "protocol": "log",
                    "target": "Loan APPROVED for prime tier. Credit: ${creditScore}, Income: ${income}",
                    "parameters": {"creditScore": "creditScore", "income": "annualIncome"},
                    "outputMapping": null
                  }
                ]
              }
            }
          ],
          "defaultBranch": {
            "nodeType": "ACTION",
            "nodeId": "approve_standard",
            "label": "Approve - Standard Rate",
            "outputs": {
              "decision": "APPROVED",
              "riskTier": "standard",
              "interestRate": "6.5",
              "maxLoanAmount": "250000"
            },
            "callableActions": [
              {
                "actionId": "log_standard",
                "protocol": "log",
                "target": "Loan APPROVED for standard tier. Credit: ${creditScore}, Income: ${income}",
                "parameters": {"creditScore": "creditScore", "income": "annualIncome"},
                "outputMapping": null
              }
            ]
          }
        }
      }
    ],
    "defaultBranch": {
      "nodeType": "CONDITION",
      "nodeId": "low_credit_income",
      "label": "Low Credit - Income Check",
      "field": "annualIncome",
      "operator": "GTE",
      "branches": [
        {
          "value": 80000,
          "child": {
            "nodeType": "ACTION",
            "nodeId": "approve_subprime",
            "label": "Approve - Subprime Rate",
            "outputs": {
              "decision": "APPROVED",
              "riskTier": "subprime",
              "interestRate": "9.5",
              "maxLoanAmount": "100000"
            },
            "callableActions": [
              {
                "actionId": "log_subprime",
                "protocol": "log",
                "target": "Loan APPROVED subprime. High income compensates low credit: ${creditScore}",
                "parameters": {"creditScore": "creditScore"},
                "outputMapping": null
              }
            ]
          }
        }
      ],
      "defaultBranch": {
        "nodeType": "ACTION",
        "nodeId": "reject",
        "label": "Reject Application",
        "outputs": {
          "decision": "REJECTED",
          "riskTier": "unqualified",
          "interestRate": "N/A",
          "maxLoanAmount": "0",
          "reason": "Insufficient credit score and income"
        },
        "callableActions": [
          {
            "actionId": "log_rejection",
            "protocol": "log",
            "target": "Loan REJECTED. Credit: ${creditScore}, Income: ${income}",
            "parameters": {"creditScore": "creditScore", "income": "annualIncome"},
            "outputMapping": null
          }
        ]
      }
    }
  },
  "scoringMode": false,
  "earlyTermination": true
}',
     '{"creditScore":"INTEGER","annualIncome":"INTEGER"}',
     '{"decision":"STRING","riskTier":"STRING","interestRate":"STRING","maxLoanAmount":"STRING"}',
     'PUBLISHED', 'c0000000-0000-0000-0000-000000000001', 'Loan decision tree with callable log actions demonstrating action-as-function feature')
ON CONFLICT (rule_id, version) DO NOTHING;
