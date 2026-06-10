FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .

COPY candidate-service/pom.xml candidate-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY employee-service/pom.xml employee-service/pom.xml
COPY document-service/pom.xml document-service/pom.xml

RUN mvn dependency:go-offline -B -q || true

COPY candidate-service/src candidate-service/src

RUN mvn -pl candidate-service package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache icu-libs tzdata && \
    addgroup -S appgroup && adduser -S appuser -G appgroup
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV TZ=Asia/Almaty
COPY --from=build /app/candidate-service/target/*.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", \
  "-Dfile.encoding=UTF-8", \
  "-Dstdout.encoding=UTF-8", \
  "-Dstderr.encoding=UTF-8", \
  "-Dsun.stdout.encoding=UTF-8", \
  "-Dsun.stderr.encoding=UTF-8", \
  "-jar", "app.jar"]
