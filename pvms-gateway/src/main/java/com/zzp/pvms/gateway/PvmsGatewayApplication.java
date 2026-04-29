package com.zzp.pvms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * PVMS 网关中心
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PvmsGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PvmsGatewayApplication.class, args);
    }
}
