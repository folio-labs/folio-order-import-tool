mvn install -DskipTests=true
rm -f logs/run.log
docker build -t order-import-poc .
