# ---- 构建阶段：Maven + JDK 17 打包（服务器无需装 Java/Maven）----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先只拷贝 pom.xml 预下载依赖，形成缓存层：以后只改代码时这层不用重新下载，重建更快
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- 运行阶段：仅 JRE ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/fitness-training-agent-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
