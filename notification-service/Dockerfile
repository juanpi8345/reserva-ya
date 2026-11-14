FROM eclipse-temurin:27-jdk
ARG JAR_FILE=target/notification-service.jar
COPY ${JAR_FILE} notification-service.jar
ENTRYPOINT ["java", "-jar", "notification-service.jar"]