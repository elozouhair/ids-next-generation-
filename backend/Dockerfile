FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ids-backend-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
