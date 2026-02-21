# RuleForge Java SDK

A Java 21 client SDK for the RuleForge BRMS REST API.

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.ruleforge</groupId>
    <artifactId>sdk-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

### Initialize the client

```java
import io.ruleforge.sdk.RuleForgeClient;

RuleForgeClient client = new RuleForgeClient("https://api.ruleforge.io", "your-api-key");
```

### Solve a rule

```java
import java.util.Map;

Map<String, Object> input = Map.of(
    "age", 30,
    "income", 75000,
    "creditScore", 720
);

Map<String, Object> result = client.solver().solve("loan-eligibility", input);
System.out.println(result);
```

### Solve in bulk

```java
import java.util.List;
import java.util.Map;

List<Map<String, Object>> inputs = List.of(
    Map.of("age", 25, "income", 50000),
    Map.of("age", 45, "income", 120000)
);

List<Map<String, Object>> results = client.solver().solveBulk("loan-eligibility", inputs);
```

### List rules in a space

```java
Map<String, Object> rules = client.rules().listRules("your-space-id");
```

### Get a specific rule

```java
Map<String, Object> rule = client.rules().getRule("your-rule-id");
```

## Authentication

The SDK supports two authentication modes:

- **API Key** (default): Pass your API key to the constructor. It will be sent as `X-API-Key` header.
- **JWT**: Use `new RuleForgeClient(baseUrl, null, jwtToken)` and it will be sent as `Authorization: Bearer <token>`.
