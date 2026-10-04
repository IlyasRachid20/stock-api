# Dashboard stage: build the React app into static files
FROM node:24-alpine AS dashboard
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# Build stage: compile and package the app with the Maven wrapper.
# The dashboard goes into static/, so the API serves it at / (one address, no CORS).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
COPY --from=dashboard /frontend/dist/ src/main/resources/static/
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
