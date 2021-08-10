FROM openjdk:8-jdk-alpine
VOLUME /tmp

ADD target/metrics-client-1.0.jar  /usr/share/metrics/metrics.jar

ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -jar /usr/share/metrics/metrics.jar --server.port=8080 --server.servlet.contextPath=/metrics
