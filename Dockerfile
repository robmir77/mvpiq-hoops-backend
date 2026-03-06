FROM eclipse-temurin:17-jdk-jammy as build
WORKDIR /build
COPY . .
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /work

COPY --from=build /build/target/quarkus-app/lib/ /work/lib/
COPY --from=build /build/target/quarkus-app/*.jar /work/
COPY --from=build /build/target/quarkus-app/app/ /work/app/
COPY --from=build /build/target/quarkus-app/quarkus/ /work/quarkus/

EXPOSE 8080

CMD ["java","-jar","quarkus-run.jar"]