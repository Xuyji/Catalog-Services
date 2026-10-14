FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/catalog-api-1.0.0.jar app.jar
USER 1001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
