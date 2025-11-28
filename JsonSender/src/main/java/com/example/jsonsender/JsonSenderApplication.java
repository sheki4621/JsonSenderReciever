package com.example.jsonsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import com.example.jsonsender.config.AppConfig;

@SpringBootApplication
@ComponentScan(basePackages = { "com.example.jsonsender", "com.example.jsoncommon" })
@EnableConfigurationProperties(AppConfig.class)
public class JsonSenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(JsonSenderApplication.class, args);
    }

}
