package com.example.jsonsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.jsonsender.config.AppConfig;

@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
public class JsonSenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(JsonSenderApplication.class, args);
    }

}
