## 1st stage

FROM eclipse-temurin:21.0.2_13-jdk-jammy AS build

COPY . /src
WORKDIR /src
RUN ./gradlew installDist

## 2nd stage

FROM eclipse-temurin:21.0.2_13-jre-alpine

WORKDIR /app
ENTRYPOINT ["java", "-jar", "translated-oracle-jdk-doc-normalizer.jar"]

COPY --from=build /src/build/install/translated-oracle-jdk-doc-normalizer/lib/*.jar .
