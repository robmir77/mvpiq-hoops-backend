FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests


FROM eclipse-temurin:17-jdk

# installa ffmpeg
RUN apt-get update \
    && apt-get install -y ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /build/target/quarkus-app/ /app/

EXPOSE 8080

CMD ["java","-jar","/app/quarkus-run.jar"]