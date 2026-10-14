FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/charts-api-1.0.0-all.jar app.jar
COPY config.yml config.yml
USER 1001
EXPOSE 8080 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar", "server", "/app/config.yml"]
