FROM eclipse-temurin:20-jdk-alpine

RUN apk update && \
    apk add tzdata && \
    addgroup -g 1002 -S java && \
    adduser -u 1002 -s /bin/sh -S -G java java && \
    mkdir -p /app /config && \
    chown java /app /config && \
    cp /usr/share/zoneinfo/Europe/Helsinki /etc/localtime

USER java
COPY target/tiira-watcher.jar /app/tiira-watcher.jar
WORKDIR /config
VOLUME /config

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/tiira-watcher.jar"]
# Default argument, overrideable
CMD ["server"]
