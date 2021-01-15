# Dockerizing order-import-poc

### Overview
Docker is a tool designed to make it easier to create, deploy, and run applications by using containers. Containers allow a developer to package up an application with all of the parts it needs, such as libraries and other dependencies, and deploy it as one package. 
### Setting up the Environment
The order-import-poc application requires several environment variables to be set up and these can be passed into the application using a .env file. An example of a .env file is found in the repositor as .env.example
```
baseOkapEndpoint="https://okapi-cornell-training.folio.ebsco.com/"
okapi_username="CHANGE ME"
okapi_password="CHANGE ME"
tenant="CHANGE ME"
permELocation="serv,remo"
permLocation="Mann"
fiscalYearCode="FY2021"
loanType="Can circulate"
textForElectronicResources="Avaialble to Snapshot Users"
noteType="General note"
materialType="Book"
```
- copy the .env.example file into a file called .env
- Edit the .env file to to change the username, password, and tenant values.
- Edit other properties as needed
### Building and Running the application
The docker-compose command is used to build the application image and deploy it to a container.  The command reads the docker-compose.yaml file and Dockerfile to build the image, read the environment variables in the .env file, and launch the application.  It can all be done with the following command:
```
docker-compose up --build
```
### Accessing the application
This Dockerfile is configured to run the application on the localhost on port 8080.  Enter the following URL into a browser:
```
http://localhost:8080/order-import-poc/import
```





