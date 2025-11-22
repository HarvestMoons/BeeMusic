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
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
