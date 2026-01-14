# 使用预装 Maven 和 JDK21 的镜像
FROM maven:3.11-amazoncorretto-21

WORKDIR /app

# 只复制必要的源代码和配置文件
COPY pom.xml .
COPY src ./src

# 使用 Maven 执行打包
RUN mvn clean package -DskipTests

# 方式二，本地打包，包括拉取依赖等等：使用轻量级 JDK21 运行环境（alpine 100MB左右， slim 功能完整200MB）
# FROM openjdk:21-slim
# WORKDIR /app
# COPY target/pet-care-core-SNAPSHOT.jar app.jar
# 后续同下

# 暴露应用端口
EXPOSE 8080,8081

# 使用生产环境配置启动应用
CMD ["java", "-jar", "/app/target/pet-care-core-1.0-SNAPSHOT.jar", "--spring.profiles.active=sae"]