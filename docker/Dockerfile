FROM adoptopenjdk/openjdk11-openj9:alpine-jre

RUN apk --no-cache add curl

RUN apk --no-cache add bash

VOLUME /tmp

ADD build/libs/redirect-to-stream-0.9.jar /app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $JAVA_ARGS -Dio.netty.tryReflectionSetAccessible=true -jar app.jar"]