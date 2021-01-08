rm -f cid
docker run --cidfile=cid -d --rm -p 8080:8080 --name=order-import-poc order-import-poc
