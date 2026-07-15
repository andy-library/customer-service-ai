package com.enterprise.csai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CustomerServiceAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceAiApplication.class, args);
    }
}
