FROM gradle:8.6-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar -x test && \
    mv build/libs/*.jar build/libs/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/app.jar .
CMD ["java", "-jar", "app.jar"]