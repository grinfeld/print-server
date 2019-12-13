FROM adoptopenjdk/openjdk11-openj9:alpine-jre

RUN apk --no-cache add curl

RUN apk --no-cache add bash

VOLUME /tmp

ADD build/libs/app.jar /

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $JAVA_ARGS -jar app.jar"]