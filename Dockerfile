FROM maven:3.6.3-openjdk-17 AS MAVEN_BUILD

COPY ./ ./

RUN mvn clean package

FROM jdk-

CMD ["java", "-jar", "target/StreamPostsBot-0.0.1-SNAPSHOT.jar"]