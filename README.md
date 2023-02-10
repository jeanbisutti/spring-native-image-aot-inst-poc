# Spring native & AOT instrumentation (POC)

The goal of this project is to try to add ahead-of-time (AOT) instrumentation to a Spring native image.

## To test this project

We are going to apply the following transformations:

_Spring Boot jar with Spring AOT processing_ =>  _AOT instrumentation_ => _native image_

First, clone this project.

Execute `mvn install`.

After, you need a Spring Boot 3 application. You can use [this one](./spring-boot-app-demo) in this project.

Execute `cd spring-boot-app-demo` and then `mvn -Pnative native:compile`, that will generate a jar with the [Spring ahead-of-time processing](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/native-image.html#native-image.introducing-graalvm-native-images.understanding-aot-processing) applied.

Then, apply ahead-of-time instrumentation. execute [this test](./spring-boot-app-demo/src/test/java/test/ApplyAotInstrumentation.java).

Finally, transform the jar containing the Spring AOT processing and the AOT instrumentation to a native image:

`pack build --builder paketobuildpacks/builder:tiny --path target/spring-boot-3-app-instrumented.jar --env BP_NATIVE_IMAGE=true image-native-aot`

It will fail with the unlinkat `/workspace/META-INF/maven:` permission denied error. To do to make it works: add the `drwxr-xr-x` Unix file attribute for JAR directories and `-rw-r--r--`  for the JAR files.

