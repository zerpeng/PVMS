package com.zzp.pvms.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 网关全局请求日志过滤器
 */
@Slf4j
@Component
public class LogGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI uri = exchange.getRequest().getURI();
        String method = exchange.getRequest().getMethod().name();
        
        log.info(">>> 网关收到请求: [{}] {}", method, uri.getPath());
        
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long endTime = System.currentTimeMillis();
            log.info("<<< 网关完成请求: {} {}, 耗时: {}ms", method, uri.getPath(), (endTime - startTime));
        }));
    }

    @Override
    public int getOrder() {
        return -1; // 优先级最高，最先执行
    }
}
