FROM openjdk:17.0.2-jdk

RUN mkdir /app
USER 1000
COPY --chown=1000:1000 build/libs/crypto-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --chown=1000:1000 prices /app/prices
WORKDIR /app

EXPOSE 8081
ENTRYPOINT [ "java", "-jar", "./app.jar" ]