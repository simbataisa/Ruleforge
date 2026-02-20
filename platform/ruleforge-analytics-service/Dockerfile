FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
