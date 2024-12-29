FROM openjdk:17
COPY target/account-service-0.0.1-SNAPSHOT.jar account-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/account-service.jar"]
