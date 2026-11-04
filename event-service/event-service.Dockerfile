FROM eclipse-temurin:17 AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
