package com.example.jsonreceiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.example.jsonreceiver", "com.example.jsoncommon" })
public class JsonReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(JsonReceiverApplication.class, args);
    }

}
