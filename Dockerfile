# Use Jetty image
FROM jetty
# copy WAR into image
COPY /target/order-import-poc-0.0.1-SNAPSHOT.war /var/lib/jetty/webapps/order-import-poc.war
# run application with this command line 
# ENTRYPOINT ["java","-jar","app.jar"]
