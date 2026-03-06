FROM eclipse-temurin:17-jdk-jammy as build

WORKDIR /build
COPY . .

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

CMD ["java","-jar","app.jar"]