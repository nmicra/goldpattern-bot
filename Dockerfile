FROM adoptopenjdk/openjdk11:alpine-jre
COPY build/libs/goldpattern.jar /app.jar

EXPOSE 8080
ENV TZ=Asia/Jerusalem
VOLUME /work
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx100m","-jar","/app.jar"]