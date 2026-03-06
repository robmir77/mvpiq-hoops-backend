FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY target/quarkus-app/ /app/

EXPOSE 8080

CMD ["java","-jar","quarkus-run.jar"]