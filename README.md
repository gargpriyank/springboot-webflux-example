# Sample Spring Boot application

- [Application overview](#application-overview)
- [How to start the application](#how-to-start-the-application)
- [How to test the application](#how-to-test-the-application)
- [How to build and push the docker image](#how-to-build-and-push-the-docker-image)
- [How to deploy the application using Jenkins pipeline](#how-to-deploy-the-application-using-jenkins-pipeline)

## Application overview

This sample spring boot webflux application uses Kafka APIs to send/receive JSON messages to/from IBM Event Streams (Kafka) topic. 
It saves the JSON message in IBM Databases for MongoDB. 

## How to start the application

1. Set the following environment variables to connect to IBM Databases for MongoDB.

```bash
export MONGO_DB_URL=<database_url starting with mongodb://>
export MONGO_DB_NAME=<database_name>
```

2. Download the certificate from IBM Databases for MongoDB instance dashboard, generate the JKS or use JDK `cacerts` and import the MongoDB 
certificate in JKS.

```bash
keytool -genkey -alias <fully_qualified_domain_name> -keyalg RSA -keystore <key_store_name>.jks -keysize 2048
or
cp <JDK_Cert_Path>/cacerts <key_store_name>.jks     # Use the existing JDK cacerts as JKS.
keytool -storepasswd -keystore keystore.jks     # change the JKS password.
keytool -importcert -trustcacerts -file <certificate_file_full_path> -keystore <key_store_name>.jks -storepass <key_store_password> \
-alias <certificate_specific_unique_name>
```

3. From the Event Streams for IBM Cloud instance dashboard, click Service Credentials and select or create a new one.

4. Copy the above created credentials content to the environment variable ES_KAFKA_SERVICE to connect to IBM Event Streams (Kafka). 
Set environment variable ES_KAFKA_TOPIC_NAME.

```bash
export ES_KAFKA_SERVICE='{
  "api_key": "...",
  "apikey": "...",
  "iam_apikey_description": "...",
  "iam_apikey_name": "...",
  "iam_role_crn": "...",
  "iam_serviceid_crn": "...",
  "instance_id": "...",
  "kafka_admin_url": "...",
  "kafka_brokers_sasl": [
    "...",
    "...",
  ],
  "kafka_http_url": "...",
  "password": "...",
  "user": "..."
}'

export ES_KAFKA_TOPIC_NAME=es-kafka-sample-topic
```

5. Install event-stream cli and initialize it to connect to event stream instance.

```bash
ibmcloud plugin install event-streams
ibmcloud es init
```

6. Build `.jar` file and start the application by passing JVM parameters as in below.

```bash
mvn clean install
java -Djavax.net.ssl.trustStore=<key_store_full_path>.jks -Djavax.net.ssl.trustStorePassword=<key_store_password> \
-jar <generated_jar_file_full_path>.jar
```

## How to test the application

The application saves and retrieves employee data and can be accessed through the endpoint `localhost:8080/employee`.
1. Send a POST request with following JSON to save employee data. The POST request sends the data to Kafka topic. 
Kafka consumer listens to the message and save it into MongoDB.

```bash
{
	"name": "..",
	"address": "...",
	"deptName": "..."
}
```

2. Send a GET request to retrieve all the employees.

## How to build and push the docker image

```bash
docker -t <registry_url>/<repo_name>/<image_name>:<image_version> build .
docker push <registry_url>/<repo_name>/<image_name>:<image_version>
```

## How to deploy the application using Jenkins pipeline

The deploy folder of this project contains the deployment artifacts Dockerfile, build-template.yaml, deploy-template.yaml and dev-pipeline.groovy.

#### Prerequisites

1. Install OpenShift CLI plugin to run `oc` commands.

2. Either generate a new JKS or copy `/jre/lib/security/cacerts` from JDk installation directory and rename it to `keystore.jks`.

3. Update the JKS password and import the SSL certificate of IBM Databases for MongoDB. 
IBM Event Streams (Kafka) will use the default JDK certificate.
```bash
keytool -storepasswd -keystore keystore.jks
keytool -importcert -trustcacerts -file mongodb.pem -keystore keystore.jks -alias mongodb -storepass <secure_password>
```

4. Create application secrets.

```bash
oc create secret generic springboot-webflux-example-cert-secret --from-file=keystore.jks
oc create secret generic springboot-webflux-example-secret \
--from-literal=jvm-secret="-Djavax.net.ssl.trustStore=/etc/keystore/cert/keystore.jks \
-Djavax.net.ssl.trustStorePassword=<secure_password>" --from-literal=mongo-db-url="<database_url starting with mongodb://>" \
--from-literal=mongo-db-name="<database_name>" --from-literal=es_kafka_topic_name="<kafka_topic_name>" \
--from-literal=es_kafka_service_name='<es_kafka_service_creds>'
```

5. This example stores the application image in IBM Container Registry. It uses OpenShift Jenkins as CI/CD tool. Create registry secret and link
with service account as in below.

```bash
oc create secret docker-registry <secret_name> --docker-server=de.icr.io --docker-username=<iam_key_name> --docker-password=<iam_key_password> 
-n <namespace>
oc secrets link builder <secret_name>
oc secrets link default <secret_name> --for=pull
```

#### Build and deployment files

1. **Dockerfile** builds the application image.

2. **build-template.yaml** template contains the build configurations and it builds the application image in OpenShift platform.

3. **deploy-template.yaml** template contains the deploy configurations and it deploys the application image in OpenShift platform.

4. **dev-pipeline.groovy** contains the groovy script for automated build and deployment of application in OpenShift platform. It uses the groovy
   functions of [Jenkins library](https://github.com/gargpriyank/jenkins-library.git). To start the Jenkins pipeline, add OpenShift service
   account credentials as a variable **os-jenkins-sa-token** in Jenkins credentials, create the Jenkins pipeline and add the **dev-pipeline.groovy** 
   script. Run the pipeline.
