# Dockerizing order-import-poc

### Overview
Docker is a tool designed to make it easier to create, deploy, and run applications by using containers. Containers allow a developer to package up an application with all of the parts it needs, such as libraries and other dependencies, and deploy it as one package. 

### Setting up the Environment
The order-import-poc application requires several environment variables to be set up and these can be passed into the application using a `.env` file. An example can be found in the repository root as `.env.example`.


1. Copy `.env.example` to `.env`
1. Edit the `.env` file to use appropriate values for:
   - `baseOkapEndpoint`
   - `okapi_username`
   - `okapi_password`
   - `tenant`
1. Edit other properties as needed

### Building and Running the application
The docker-compose command is used to build the application image and deploy it to a container locally.  The command reads the `docker-compose.yaml` file and `Dockerfile` to build the image, read the environment variables in the `.env` file, and launch the application.  It can all be done with the following command:

```sh
docker-compose up --build
```
### Accessing the application
This Dockerfile is configured to run the application on the localhost on port 8080.  Enter the following URL into a browser:

```sh
http://localhost:8080/order-import-poc/import
```
