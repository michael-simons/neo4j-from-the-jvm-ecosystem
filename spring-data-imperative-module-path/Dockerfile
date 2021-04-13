FROM adoptopenjdk:16-jdk as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
ARG APP_ONLY_JAR_FILE=target/*.jar.original
COPY ${JAR_FILE} full.jar
COPY ${APP_ONLY_JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar full.jar extract

FROM adoptopenjdk:16-jdk
WORKDIR application
COPY --from=builder application/dependencies/BOOT-INF/lib ./lib
COPY --from=builder application/application.jar ./
ENTRYPOINT ["java", "-p", ".:lib", "-m", "sdn.mp/org.neo4j.examples.jvm.spring.data.imperative.Application"]
