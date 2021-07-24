FROM adoptopenjdk:11-jre-hotspot as builder
WORKDIR target
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM adoptopenjdk:11-jre-hotspot
WORKDIR target
COPY --from=builder target/dependencies/ ./
COPY --from=builder target/snapshot-dependencies/ ./
COPY --from=builder target/resources/ ./
COPY --from=builder target/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]