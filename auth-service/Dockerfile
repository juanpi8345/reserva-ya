FROM eclipse-temurin:27-jdk
ARG JAR_FILE=target/auth-service.jar
COPY ${JAR_FILE} auth-service.jar
ENTRYPOINT ["java", "-jar", "auth-service.jar"]