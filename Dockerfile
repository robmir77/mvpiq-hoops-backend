FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=build /build/target/*-runner.jar app.jar

EXPOSE 8080

CMD ["java","-jar","app.jar"]