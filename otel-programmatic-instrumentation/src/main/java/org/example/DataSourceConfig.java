package org.example;

import io.opentelemetry.instrumentation.jdbc.datasource.OpenTelemetryDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:db");
        dataSource.setUsername("username");
        dataSource.setPassword("pwd!");
        return new OpenTelemetryDataSource(dataSource);
    }

}