FROM eclipse-temurin:27-jdk
ARG JAR_FILE=target/eureka-server.jar
COPY ${JAR_FILE} eureka-server.jar
ENTRYPOINT ["java", "-jar", "eureka-server.jar"]