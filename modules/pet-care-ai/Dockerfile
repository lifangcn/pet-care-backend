# 使用预装 Maven 和 JDK21 的镜像
FROM maven:3.9-amazoncorretto-21
WORKDIR /app

# 只复制必要的源代码和配置文件
COPY pom.xml .
COPY src ./src

# 使用 Maven 执行打包
RUN mvn clean package -DskipTests

# 暴露应用端口
EXPOSE 8082

# 使用生产环境配置启动应用（profile 由 docker-compose 环境变量覆盖）
ENTRYPOINT ["java", "-jar", "/app/target/pet-care-ai-1.0-SNAPSHOT.jar"]