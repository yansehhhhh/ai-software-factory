package com.aifactory.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String serviceName() {
        return "ai-software-factory-server";
    }
}
