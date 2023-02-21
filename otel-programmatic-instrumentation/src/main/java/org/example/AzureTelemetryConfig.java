package org.example;

import com.azure.monitor.opentelemetry.exporter.AzureMonitorExporterBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
@EnableOpenTelemetry
public class AzureTelemetryConfig {

    private final AzureMonitorExporterBuilder azureMonitorExporterBuilder;

    public AzureTelemetryConfig() {
        ContextTagKeys aiInternalSdkVersion = ContextTagKeys.AI_INTERNAL_SDK_VERSION;
        System.out.println("aiInternalSdkVersion = " + aiInternalSdkVersion);
        String connectionString = "YOUR_CONNECTION_STRING";
        this.azureMonitorExporterBuilder = new AzureMonitorExporterBuilder().connectionString(connectionString);
    }

   @Bean
    public MetricExporter metricExporter() {
        return azureMonitorExporterBuilder.buildMetricExporter();
    }

    @Bean
    public SpanExporter spanExporter() {
        return azureMonitorExporterBuilder.buildTraceExporter();
    }

    @Bean
    public LogRecordExporter logRecordExporter() {
        return azureMonitorExporterBuilder.buildLogRecordExporter();
    }

    @Bean
    public SdkTracerProvider sdkTracerProvider(SpanExporter spanExporter) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
    }

   @Bean
   @Primary
    public OpenTelemetrySdk openTelemetrySdk(SdkTracerProvider sdkTracerProvider) {
       return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .buildAndRegisterGlobal();
    }

}
