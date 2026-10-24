FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=secret,id=java_cacerts --mount=type=secret,id=maven_settings \
    if [ -f /run/secrets/java_cacerts ]; then export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/run/secrets/java_cacerts"; fi; \
    if [ -f /run/secrets/maven_settings ]; then export MAVEN_ARGS="-s /run/secrets/maven_settings"; fi; \
    mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=secret,id=java_cacerts --mount=type=secret,id=maven_settings \
    if [ -f /run/secrets/java_cacerts ]; then export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/run/secrets/java_cacerts"; fi; \
    if [ -f /run/secrets/maven_settings ]; then export MAVEN_ARGS="-s /run/secrets/maven_settings"; fi; \
    mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --create-home --uid 10001 appuser
COPY --from=build /src/target/reelnest-api-0.1.0-SNAPSHOT.jar /app/api.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/api.jar"]
