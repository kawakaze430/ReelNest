FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --create-home --uid 10001 appuser
COPY --from=build /src/target/reelnest-api-0.1.0-SNAPSHOT.jar /app/api.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/api.jar"]
