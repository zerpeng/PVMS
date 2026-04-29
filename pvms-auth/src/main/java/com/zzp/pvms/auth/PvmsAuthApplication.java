package com.zzp.pvms.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * PVMS 认证与用户中心
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.zzp.pvms.auth.mapper")
public class PvmsAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(PvmsAuthApplication.class, args);
    }
}
