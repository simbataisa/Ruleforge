rootProject.name = "ruleforge-platform"

includeBuild("../engine")

include(
    "ruleforge-common",
    "ruleforge-auth-service",
    "ruleforge-management-service",
    "ruleforge-solver-service",
    "ruleforge-event-service",
    "ruleforge-analytics-service",
    "ruleforge-scheduler-service",
    "ruleforge-connector-service"
)
