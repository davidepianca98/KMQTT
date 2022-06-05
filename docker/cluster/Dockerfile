FROM openjdk:11-jre-slim

ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000
COPY build/libs/kmqtt-0.3.2.jar kmqtt.jar
EXPOSE 1883
EXPOSE 22222
EXPOSE 22223
ENTRYPOINT [ "java", "-server", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "kmqtt.jar" ]
