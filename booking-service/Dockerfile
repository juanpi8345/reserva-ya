FROM eclipse-temurin:27-jdk
ARG JAR_FILE=target/booking-service.jar
COPY ${JAR_FILE} booking-service.jar
ENTRYPOINT ["java", "-jar", "booking-service.jar"]