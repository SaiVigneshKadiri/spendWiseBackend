FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Resolve dependencies in their own layer so source-only edits skip the download.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system spendwise && useradd --system --gid spendwise --no-create-home spendwise
COPY --from=build /workspace/target/spendwise-api-0.1.0.jar app.jar
USER spendwise
EXPOSE 8080
# Size the heap to the container limit; die fast on OOM so the orchestrator restarts the instance.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
