# 第一阶段：构建阶段
FROM maven:3.9-eclipse-temurin-22 AS builder
WORKDIR /app
COPY pom.xml .
# 这步会缓存依赖，只有 pom.xml 变化时才重新下载
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# 第二阶段：运行阶段
FROM openjdk:22-jdk-slim
# 设置时区为 Asia/Shanghai
ENV TZ=Asia/Shanghai
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail http://localhost:8082/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
