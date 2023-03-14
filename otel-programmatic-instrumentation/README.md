# OTel programmatic instrumentation

Example of project with [Application Insights for Spring native](https://github.com/jeanbisutti/applicationinsights-spring-native).

This project allows having telemetry data with Spring native images on azure for:
* HTTP requests
* SQL requests
* Logback logs
* JVM metrics

So try this project, set a connection string in the application.properties file and execute the following command lines:

`mvn -Pnative spring-boot:build-image`

`docker run -p 8080:8080  otel-programmatic-instrumentation:1.0-SNAPSHOT`