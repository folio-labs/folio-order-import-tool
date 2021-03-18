FROM maven:3.6.3-openjdk-15-slim as maven

COPY ./pom.xml ./pom.xml
COPY ./src ./src

RUN mvn dependency:go-offline -B
RUN mvn package -DskipTests=true

FROM jetty

WORKDIR $JETTY_BASE

COPY --from=maven target/order-import-poc-*.war ./webapps/order-import-poc.war
