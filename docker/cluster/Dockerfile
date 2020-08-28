FROM openjdk:11-jre-slim

ADD build/libs/KMQTT-0.2.3.jar KMQTT.jar
EXPOSE 1883
ENTRYPOINT [ "java", "-server", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "KMQTT.jar" ]
