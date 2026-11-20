FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -DskipTests clean package -pl web -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV TZ=Europe/Kyiv
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
COPY --from=build /build/web/target/web-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
