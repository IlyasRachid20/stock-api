# Build stage: compile and package the app with the Maven wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# Run stage: only the JRE and the jar, running as a non-root user
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/stock-api-*.jar app.jar
USER app
EXPOSE 8080
# Fits small hosting plans (e.g. 512 MB): heap sized from the container's memory, and the
# serial garbage collector, which uses the least memory for a small app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC"
ENTRYPOINT ["java", "-jar", "app.jar"]
