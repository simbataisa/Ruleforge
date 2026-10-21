FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S ruleforge && adduser -S ruleforge -G ruleforge

WORKDIR /app

COPY --chown=ruleforge:ruleforge build/libs/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8081/actuator/health || exit 1

USER ruleforge

ENTRYPOINT ["java", "-XX:+UseCompactObjectHeaders", "-jar", "app.jar"]
