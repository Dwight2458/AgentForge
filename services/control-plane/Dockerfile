FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY services/control-plane/pom.xml services/control-plane/pom.xml
RUN mvn -q -pl services/control-plane -am dependency:go-offline
COPY services/control-plane/src services/control-plane/src
RUN mvn -q -pl services/control-plane -am package -DskipTests

FROM eclipse-temurin:21.0.8_9-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home agentforge
COPY --from=build --chown=agentforge:agentforge /workspace/services/control-plane/target/*.jar /app/control-plane.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/control-plane.jar"]

