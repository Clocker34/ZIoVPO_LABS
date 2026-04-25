package ru.rkjrth.adboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.rkjrth", "ru.mfa"})
@ConfigurationPropertiesScan(basePackages = {"ru.rkjrth", "ru.mfa"})
public class AdboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdboardApplication.class, args);
    }
}
