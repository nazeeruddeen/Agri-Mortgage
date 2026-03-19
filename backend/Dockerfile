FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/target/agri-mortgage-loan-system-1.0.0.jar /app/app.jar

EXPOSE 8011

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
