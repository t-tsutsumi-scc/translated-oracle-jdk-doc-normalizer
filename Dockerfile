## 1st stage

FROM eclipse-temurin:21.0.9_10-jdk-jammy AS build

COPY . /src
WORKDIR /src
RUN ./gradlew installDist

## 2nd stage

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
ENTRYPOINT ["java", "-jar", "translated-oracle-jdk-doc-normalizer.jar"]

COPY --from=build /src/build/install/translated-oracle-jdk-doc-normalizer/lib/*.jar .
