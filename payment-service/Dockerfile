FROM eclipse-temurin:27-jdk
ARG JAR_FILE=target/payment-service.jar
COPY ${JAR_FILE} payment-service.jar
ENTRYPOINT ["java", "-jar", "payment-service.jar"]