# OTel programmatic instrumentation

To get instrumentation with Spring native images, the following components could be used together:
* [OTel Spring Boot starter](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/spring/starters/spring-boot-starter/README.md)
* [Programmatic instrumentation libraries](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#libraries--frameworks)
* [Azure Monitor OTel exporter](https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/monitor/azure-monitor-opentelemetry-exporter)

Execute the following command lines to try this project:

`mvn -Pnative spring-boot:build-image`

`docker run -p 8080:8080  otel-programmatic-instrumentation:1.0-SNAPSHOT`

If you would like to try the used configuration in another SPring Boot 3 project, you need tp:
* Add the (io.opentelemetry.instrumentation/opentelemetry-spring-boot-starter/1.23.0-alpha-SNAPSHOT), (com.azure/azure-monitor-opentelemetry-exporter/1.0.0-beta.7), (io.opentelemetry.instrumentatio/opentelemetry-jdbc/1.23.0-alpha-SNAPSHOT) dependencies
* Add the [AzureTelemetryConfig](./src/main/java/org/example/AzureTelemetryConfig.java) class, _set your Azure connection in the constructor_
* Configure [OpenTelemetry JDBC](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/jdbc/library)
* Add [this native-image.properties file](./src/main/resources/META-INF/native-image/native-image.properties) in the `resources/META-INF/native.image` folder

_[An issue has to be fixed today about the telemetry data export with GraalVM native images](https://github.com/Azure/azure-sdk-for-java/issues/33646)._
