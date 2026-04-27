package com.aifactory;

import com.aifactory.config.ClaudeRunnerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClaudeRunnerConfig.class)
public class AiFactoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFactoryApplication.class, args);
    }
}
