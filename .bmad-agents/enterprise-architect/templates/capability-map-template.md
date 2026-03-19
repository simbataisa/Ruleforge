# Enterprise Capability Map Template

**Organisation/Domain:** [Company Name / Business Unit]
**Map Version:** 1.0
**Date Created:** [YYYY-MM-DD]
**Last Updated:** [YYYY-MM-DD]
**Owner:** [Chief Architect / Business Architect Name]
**Status:** Draft | In Review | Published | Active
**Scope:** [Entire organisation | Specific division | Specific value stream]

---

## Introduction: What is a Capability Map?

A **capability map** is a hierarchical breakdown of the organisation's business capabilities—the things the organisation does to create value. It is independent of technology, organisational structure, and implementation.

**Key characteristics:**
- **Business-focused:** Describes what the business does, not how (technology is separate).
- **Hierarchical:** Capabilities grouped by business domain (e.g., Sales, Fulfillment, Finance).
- **Stable:** Capabilities change slowly; organisation changes rapidly.
- **Actionable:** Forms basis for roadmaps, technology investments, and governance.

**Uses of a capability map:**
- **Architecture planning:** Identify which capabilities need technology investment.
- **Portfolio analysis:** Understand which applications support which capabilities.
- **Gap analysis:** Find capabilities that are poorly supported or missing.
- **Redundancy identification:** Find overlapping capabilities that could be consolidated.
- **Roadmap development:** Prioritise capability improvements aligned with strategy.
- **Organisational design:** Align teams and accountability to capabilities.

---

## Capability Map Structure

The map is organised in a hierarchy:

**Level 1 — Business Domains** (highest level; e.g., Sales, Fulfillment, Finance)
**Level 2 — Capabilities** (key business functions within a domain; e.g., Order Management, Inventory Planning)
**Level 3 — Sub-capabilities** (specific activities within a capability; e.g., Order Placement, Order Tracking, Order Fulfillment)

Example structure:
```
SALES DOMAIN
├── Order Management (Capability)
│   ├── Order Placement (Sub-capability)
│   ├── Order Modification (Sub-capability)
│   ├── Order Cancellation (Sub-capability)
│   └── Order Tracking (Sub-capability)
├── Customer Management (Capability)
│   ├── Customer Onboarding
│   ├── Customer Profile Management
│   ├── Customer Communication
│   └── Customer Retention
└── Pricing & Promotions (Capability)
    ├── Pricing Strategy
    ├── Promotion Management
    └── Discount Management

FULFILLMENT DOMAIN
├── Inventory Management (Capability)
│   ├── Inventory Planning
│   ├── Inventory Tracking
│   ├── Stock Replenishment
│   └── Inventory Reconciliation
├── Warehouse Operations (Capability)
│   ├── Receiving & Put-Away
│   ├── Picking & Packing
│   ├── Shipping & Logistics
│   └── Returns Processing
└── Order Fulfillment (Capability)
    ├── Order Picking
    ├── Quality Check
    ├── Shipment Planning
    └── Delivery Coordination
```

---

## Capability Inventory

**This table lists all business capabilities. Fill in each row for every capability identified.**

| Capability ID | Domain | Capability Name | Level | Description | Supporting System(s) | Business Owner | Maturity (1-5) | Strategic Importance | Status |
|---|---|---|---|---|---|---|---|---|---|
| CAP-001 | Sales | Order Management | 2 | Manages complete order lifecycle from placement to fulfillment | Order Management System (OMS), SAP ERP | VP Sales | 4 | Core | Live |
| CAP-001-01 | Sales | Order Placement | 3 | Customer places order via web, mobile, or API | OMS, e-Commerce Platform | Director Order Ops | 4 | Core | Live |
| CAP-001-02 | Sales | Order Modification | 3 | Customer or support modifies order (address, items) before fulfillment | OMS, SAP ERP | Director Order Ops | 3 | Supporting | Live |
| CAP-001-03 | Sales | Order Cancellation | 3 | Customer or support cancels order; triggers refund and inventory release | OMS, Payment Service, Inventory System | Director Order Ops | 3 | Supporting | Live |
| CAP-001-04 | Sales | Order Tracking | 3 | Customer views real-time order status and shipment tracking | OMS, Fulfillment System, Shipping Integration | Director Order Ops | 4 | Core | Live |
| CAP-002 | Sales | Customer Management | 2 | Manages customer data, communication, and lifecycle | CRM System (Salesforce), Marketing Automation | VP Marketing | 3 | Core | Live |
| CAP-002-01 | Sales | Customer Onboarding | 3 | New customer registration and profile setup | CRM, Identity Provider | Manager CRM | 3 | Core | Live |
| CAP-002-02 | Sales | Customer Profile Management | 3 | Maintain customer data (contact, preferences, payment methods) | CRM, Data Lake | Manager CRM | 3 | Core | Live |
| CAP-002-03 | Sales | Customer Communication | 3 | Send transactional and marketing communications | Email Service, SMS Service, Push Notifications | Director Marketing | 2 | Supporting | Partial |
| CAP-002-04 | Sales | Customer Retention | 3 | Loyalty program, retention campaigns, churn analysis | CRM, Analytics Platform, Marketing Automation | Director Marketing | 2 | Supporting | Partial |
| CAP-003 | Sales | Pricing & Promotions | 2 | Set pricing strategy, manage discounts and promotional campaigns | Pricing Engine, OMS, SAP ERP | Director Pricing | 2 | Core | Partial |
| CAP-003-01 | Sales | Pricing Strategy | 3 | Determine pricing rules (cost-plus, competitive, dynamic) | Pricing Engine | Manager Pricing | 2 | Core | Partial |
| CAP-003-02 | Sales | Promotion Management | 3 | Create, schedule, and execute promotional campaigns | Marketing Automation, OMS | Manager Promotions | 2 | Supporting | Partial |
| CAP-003-03 | Sales | Discount Management | 3 | Apply discounts based on rules (bulk, loyalty, seasonal) | OMS, SAP ERP | Manager Pricing | 2 | Supporting | Partial |
| CAP-004 | Fulfillment | Inventory Management | 2 | Track, forecast, and optimise inventory levels | Inventory Management System, Data Lake | VP Operations | 3 | Core | Live |
| CAP-004-01 | Fulfillment | Inventory Planning | 3 | Forecast demand and set inventory targets | Inventory System, Demand Forecasting Tool | Manager Planning | 2 | Core | Partial |
| CAP-004-02 | Fulfillment | Inventory Tracking | 3 | Real-time visibility of inventory across locations | Inventory System, Warehouse Management System (WMS) | Manager Inventory | 4 | Core | Live |
| CAP-004-03 | Fulfillment | Stock Replenishment | 3 | Order and receive stock from suppliers | Inventory System, Procurement System | Manager Inventory | 3 | Core | Live |
| CAP-004-04 | Fulfillment | Inventory Reconciliation | 3 | Periodic counts and audits to ensure accuracy | Inventory System, WMS | Manager Inventory | 2 | Supporting | Partial |
| CAP-005 | Fulfillment | Warehouse Operations | 2 | Day-to-day warehouse operations (receiving, picking, shipping) | Warehouse Management System (WMS), OMS | VP Operations | 4 | Core | Live |
| CAP-005-01 | Fulfillment | Receiving & Put-Away | 3 | Receive goods from suppliers and store in warehouse | WMS, Inventory System | Manager Warehouse | 4 | Core | Live |
| CAP-005-02 | Fulfillment | Picking & Packing | 3 | Pick items from inventory and pack for shipment | WMS, OMS | Manager Warehouse | 4 | Core | Live |
| CAP-005-03 | Fulfillment | Shipping & Logistics | 3 | Arrange shipping, print labels, hand off to carriers | WMS, Shipping Integration, Carrier APIs | Manager Logistics | 3 | Core | Live |
| CAP-005-04 | Fulfillment | Returns Processing | 3 | Handle customer returns, restock, and refund processing | WMS, OMS, Inventory System | Manager Returns | 3 | Supporting | Live |
| CAP-006 | Fulfillment | Order Fulfillment | 2 | Execute order fulfillment from warehouse to customer | OMS, WMS, Inventory System, Shipping Integration | VP Fulfillment | 4 | Core | Live |
| CAP-006-01 | Fulfillment | Order Picking | 3 | Pick items from inventory for order | WMS, Inventory System | Manager Fulfillment | 4 | Core | Live |
| CAP-006-02 | Fulfillment | Quality Check | 3 | Inspect items before shipment for quality and accuracy | WMS | Manager Fulfillment | 3 | Supporting | Live |
| CAP-006-03 | Fulfillment | Shipment Planning | 3 | Consolidate orders, optimise shipments, select carriers | OMS, Shipping Optimization Tool, WMS | Manager Logistics | 3 | Core | Live |
| CAP-006-04 | Fulfillment | Delivery Coordination | 3 | Coordinate with carriers, track delivery, handle exceptions | Shipping Integration, Carrier APIs, OMS | Manager Logistics | 3 | Core | Live |
| CAP-007 | Finance | Financial Planning & Analysis | 2 | Budget planning, forecasting, variance analysis | SAP ERP, Hyperion Planning, Data Lake | VP Finance | 3 | Core | Live |
| CAP-007-01 | Finance | Budget Planning | 3 | Create annual budgets by cost center | SAP ERP, Hyperion Planning | Manager FP&A | 3 | Core | Live |
| CAP-007-02 | Finance | Forecasting | 3 | Forecast revenue and expenses | Hyperion Planning, Data Lake | Manager FP&A | 2 | Core | Partial |
| CAP-007-03 | Finance | Variance Analysis | 3 | Analyse actuals vs. budget and forecast | SAP ERP, Hyperion Planning, BI Tool | Manager FP&A | 3 | Core | Live |
| CAP-008 | Finance | Accounts Receivable | 2 | Invoice customers, collect payments, manage receivables | SAP ERP, Billing System, Payment Gateway | VP Finance | 4 | Core | Live |
| CAP-008-01 | Finance | Invoicing | 3 | Generate and send invoices to customers | SAP ERP, Billing System | Manager AR | 4 | Core | Live |
| CAP-008-02 | Finance | Payment Collection | 3 | Process customer payments via various methods | SAP ERP, Payment Gateway | Manager AR | 4 | Core | Live |
| CAP-008-03 | Finance | Collections Management | 3 | Follow up on overdue invoices; escalate if necessary | SAP ERP, Collections Tool | Manager Collections | 3 | Supporting | Live |
| CAP-009 | Finance | Accounts Payable | 2 | Process supplier invoices, manage payables | SAP ERP, Procurement System, Payment Gateway | VP Finance | 3 | Core | Live |
| CAP-009-01 | Finance | Invoice Processing | 3 | Receive, validate, and record supplier invoices | SAP ERP, Procurement System | Manager AP | 3 | Core | Live |
| CAP-009-02 | Finance | Payment Processing | 3 | Authorise and process payments to suppliers | SAP ERP, Payment Gateway | Manager AP | 4 | Core | Live |
| CAP-009-03 | Finance | Vendor Management | 3 | Maintain vendor data, terms, performance scorecards | SAP ERP, Procurement System | Manager Procurement | 2 | Supporting | Partial |
| CAP-010 | Support | Customer Support | 2 | Handle customer inquiries, issues, and complaints | Help Desk System (Zendesk), CRM, Knowledge Base | VP Support | 3 | Core | Live |
| CAP-010-01 | Support | Ticket Management | 3 | Create, assign, track, and resolve support tickets | Help Desk System, CRM | Manager Support Ops | 3 | Core | Live |
| CAP-010-02 | Support | Knowledge Management | 3 | Maintain FAQ, documentation, and troubleshooting guides | Knowledge Base, Help Desk System | Manager Support Ops | 2 | Supporting | Partial |
| CAP-010-03 | Support | Escalation Management | 3 | Escalate complex issues to senior support or engineering | Help Desk System, CRM | Manager Support | 3 | Supporting | Live |

---

## Capability Maturity Levels

**Maturity scale (1-5):**

| Level | Description | Example |
|-------|---|---|
| 1 | **Initial/Ad-hoc** | Manual processes; no automation; inconsistent execution | Customer onboarding done in spreadsheet |
| 2 | **Developing** | Some automation; partial process definition; manual workarounds | Basic CRM with manual data entry; no API integration |
| 3 | **Managed** | Well-defined processes; mostly automated; documented procedures | ERP system with defined workflows; some manual exceptions |
| 4 | **Optimised** | Fully automated; continuous improvement; aligned with strategy | Real-time order management with automated workflows and analytics |
| 5 | **Leading** | Best-in-class; continuous innovation; predictive capability | AI-driven demand forecasting; self-healing systems |

---

## Strategic Importance Classification

| Classification | Definition | Investment Level |
|---|---|---|
| **Core** | Critical to business differentiation and customer value | High investment; modernise and scale |
| **Supporting** | Necessary for operations but not differentiating | Moderate investment; automate and optimise |
| **Commodity** | Performed by many competitors identically; outsource or minimise | Low investment; consider outsourcing or SaaS |

---

## Capability Heat Map

**How to create a visual heat map:**

Use a 2D grid:
- **Y-axis:** Business domains
- **X-axis:** Capability maturity (1-5)
- **Colour coding:**
  - **Red:** Gaps or pain points (capability missing or immature)
  - **Amber:** Partial coverage (capability exists but needs improvement)
  - **Green:** Well-served (capability mature and aligned with strategy)

**Example heatmap (text representation):**

```
               Maturity 1   2      3      4      5
Sales          [Gap]   [🟨]   [🟩]   [🟩]   [ ]
Fulfillment    [ ]     [🟨]   [🟩]   [🟩]   [ ]
Finance        [🟨]    [🟨]   [🟩]   [🟩]   [ ]
Support        [Gap]   [🟥]   [🟨]   [ ]    [ ]
Marketing      [🟥]    [🟥]   [🟨]   [ ]    [ ]

Legend:
🟩 = Green (good maturity)
🟨 = Amber (partial)
🟥 = Red (gap)
```

**Interpretation:**
- Support: High gaps in Knowledge Management and Retention campaigns (red)
- Finance: Customer Communication and Promotion Management immature (amber)
- Fulfillment: Well-supported overall; some gaps in Inventory Planning

---

## Capability Gap Analysis

**Purpose:** Identify capabilities that are poorly supported or missing, and prioritise investments.

### Current State Assessment

For each capability, assess:

| Capability | Current State | Maturity | Gap Description | Business Impact | Priority |
|---|---|---|---|---|---|
| Customer Retention | Partially supported; manual campaigns only | 2 | No automated retention workflows; no churn prediction; reliant on spreadsheets | Losing customers to competitors; high churn rate (15% annually) | High |
| Inventory Forecasting | Limited; relies on historical data only | 2 | No demand forecasting tool; manual forecasting prone to errors | Stockouts and overstock; $2M annually in lost sales and holding costs | High |
| Customer Communication | Partial; email only; no SMS or push notifications | 2 | No SMS or push channel; no personalisation engine; no omnichannel support | Lower engagement; customers prefer SMS/push; competitive disadvantage | High |
| Pricing Strategy | Ad-hoc; no pricing engine | 2 | Manual pricing; no dynamic pricing; no A/B testing capability | Leaving money on table; $5M+ annual opportunity | High |
| Knowledge Management | Minimal; scattered documentation | 2 | Knowledge base incomplete; no search; hard for support team to help customers | High support costs; customer frustration; low CSAT | Medium |

### Target State Vision

Define where each capability should be in 12-24 months:

| Capability | Target Maturity | Target Approach | Expected Benefit |
|---|---|---|---|
| Customer Retention | 4 | Implement CDP; automated retention workflows; churn prediction ML model | Reduce churn to < 8%; +$3M revenue |
| Inventory Forecasting | 4 | Implement demand forecasting tool (AI-driven); integrate with inventory system | Improve forecast accuracy to 95%; save $1M annually |
| Customer Communication | 4 | Build omnichannel platform (email, SMS, push, in-app); personalisation engine | 25% increase in engagement; improved CSAT |
| Pricing Strategy | 4 | Implement pricing engine with dynamic pricing rules and A/B testing | Increase margins by 3-5%; +$5M revenue |
| Knowledge Management | 4 | Build modern knowledge base with AI search; integrate with help desk; gamification | 20% reduction in support time; improve CSAT |

---

## Application Landscape Mapping

**Purpose:** Show which applications/systems support which capabilities.

### Application to Capability Matrix

| Application | Owner | Type | Capabilities Supported | Maturity | Health | Notes |
|---|---|---|---|---|---|---|
| Order Management System (OMS) | Sales Team | Custom | Order Management, Order Fulfillment, Order Tracking | 4 | Good | Core system; high reliability; supports 99.95% SLA |
| SAP ERP | Finance Team | Commercial | Financial Planning & Analysis, Accounts Receivable, Accounts Payable, Inventory Management | 3 | Fair | Legacy; slow; requires modernisation; planned cloud migration in 2025 |
| Salesforce CRM | Marketing Team | SaaS | Customer Management, Customer Retention, Customer Communication (email only) | 3 | Good | Missing SMS/push; plan to integrate Twilio for SMS; ROI positive |
| Warehouse Management System (WMS) | Ops Team | Commercial | Warehouse Operations, Inventory Tracking, Fulfillment | 4 | Good | Recently upgraded; high maturity; real-time visibility |
| Zendesk Help Desk | Support Team | SaaS | Customer Support, Ticket Management | 3 | Good | Mostly good; knowledge base incomplete; plan phase 2 expansion |
| Hyperion Planning | Finance Team | Commercial | Financial Planning & Analysis, Forecasting | 2 | Poor | Slow; limited forecasting capability; plan to replace in 2024 |
| Demand Forecasting Tool | None | Gap | Inventory Forecasting | N/A | Missing | Capability gap; RFP in progress; target implementation Q3 2024 |
| Pricing Engine | None | Gap | Pricing Strategy | N/A | Missing | Capability gap; RFP in progress; expected benefit $5M annually |

### Redundancy and Consolidation Findings

| Redundancy | Systems | Recommendation | Priority |
|---|---|---|---|
| Customer data duplication | CRM (Salesforce), ERP (SAP), Help Desk (Zendesk) | Implement master customer data platform (CDP); single source of truth | High |
| Inventory data in three systems | Inventory System, WMS, ERP | Implement data sync; single inventory source; eliminate manual reconciliation | High |
| Reporting duplicated in two tools | SAP analytics, Power BI | Standardise on one BI tool (Power BI); retire SAP analytics module | Medium |
| Multiple payment processors | Stripe, PayPal, Square | Consolidate to one processor; save on integration and fee negotiation | Medium |

---

## Roadmap Implications

**Capability investments by year:**

| Year | Strategic Priority | Capability Investments | Estimated Budget |
|---|---|---|---|
| 2024 | Operational Excellence | Demand Forecasting Tool, SAP ERP Cloud Migration (Phase 1), Hyperion Replacement | $3.5M |
| 2025 | Customer Experience | Pricing Engine, Omnichannel Communication (SMS/Push), CDP Implementation, CRM Enhancements | $4.2M |
| 2026 | Growth & Innovation | Advanced Analytics & ML, Personalisation Engine, Inventory Optimisation (AI), Dynamic Pricing | $3.8M |
| 2027 | Sustainability | Supply Chain Visibility, Sustainable Packaging Capabilities, Carbon Tracking | $2.0M |

---

## Governance and Maintenance

**Who maintains this capability map:**

| Role | Responsibility |
|---|---|
| Chief Architect | Owns map; sets governance policies; approves capability definitions |
| Business Architects | Update capability definitions; maintain current state assessment |
| Line of Business Managers | Provide input on capability status and business needs |
| Application Portfolio Manager | Maintain application-to-capability mapping; identify redundancy |

**Review cadence:**
- Quarterly: Update capability status and maturity assessments.
- Biannually: Review gaps and roadmap alignment.
- Annually: Full map review; strategy alignment; publish updated version.

**Change control:**
- Changes to capability definitions require approval from Chief Architect.
- Changes to roadmap require steering committee approval.
- Version control: Track changes in version history table (below).

---

## Version History

| Version | Date | Author | Changes |
|---|---|---|---|
| 1.0 | [YYYY-MM-DD] | [Name] | Initial draft; 40 capabilities across 5 domains |
| 1.1 | [YYYY-MM-DD] | [Name] | Added Customer Communication capability; identified gaps in marketing stack |
| 2.0 | [YYYY-MM-DD] | [Name] | Added Supply Chain domain; revised roadmap based on 2025 strategy refresh |
| | | | |

---

## Approval and Sign-Off

| Role | Name | Date | Signature |
|---|---|---|---|
| Chief Architect | | | |
| VP Finance | | | |
| VP Operations | | | |
| VP Sales & Marketing | | | |

---

## Appendix: Capability Definitions (Detailed)

### CAP-001: Order Management
**Full name:** Order Management
**Domain:** Sales
**Level:** 2
**Purpose:** End-to-end management of customer orders from initial placement through delivery and post-delivery support.

**In scope:**
- Order placement via multiple channels (web, mobile, phone, API)
- Order validation and confirmation
- Order status tracking
- Order modification (address, quantities before shipment)
- Order cancellation and compensation
- Integration with payment and inventory systems

**Out of scope:**
- Customer refund processing (Finance domain)
- Returns processing (Fulfillment domain)
- Analytics on orders (Analytics capability)

**Key metrics:**
- Order placement time: < 2 minutes
- Order confirmation time: < 5 minutes
- Order tracking accuracy: 99%+
- Customer satisfaction: CSAT > 4.5/5

---

### CAP-002: Customer Management
**Full name:** Customer Management
**Domain:** Sales
**Level:** 2
**Purpose:** Manage complete customer lifecycle: acquisition, onboarding, communication, engagement, and retention.

**In scope:**
- Customer registration and profiling
- Customer data maintenance (contact info, preferences, payment methods)
- Customer segmentation
- Customer communication (transactional and marketing)
- Customer loyalty and retention programs
- Customer feedback and surveys

**Out of scope:**
- Billing and invoicing (Finance domain)
- Support ticket handling (Support domain)
- Analytics on customer behaviour (Analytics capability)

**Key metrics:**
- Customer onboarding time: < 24 hours
- Customer profile data quality: 95%+ complete
- Communication delivery rate: 98%+
- Customer lifetime value growth: 15% YoY

---

[Continue with detailed definitions for remaining capabilities as needed...]

