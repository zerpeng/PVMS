package com.zzp.pvms.gateway.filter;

import com.zzp.pvms.common.constant.AuthConstants;
import com.zzp.pvms.common.constant.ServiceConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

/**
 * 网关统一鉴权过滤器，负责在请求进入业务服务前校验 JWT。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 不需要登录即可访问的接口白名单。
     */
    private static final Set<String> WHITELIST = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/captcha",
            "/api/auth/debug"
    );

    /**
     * JWT 签名密钥，需要与 pvms-auth 生成 Token 时使用的密钥保持一致。
     */
    @Value("${jwt.secret:pvms-secret-key-for-jwt-authentication-2026-zzp}")
    private String secret;

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public AuthGlobalFilter(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 放行浏览器 CORS 预检请求，避免前端跨域请求在正式鉴权前被拦截。
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        // 放行登录、注册、验证码等公开接口。
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(ServiceConstants.AUTHORIZATION_HEADER);
        if (!hasBearerToken(authorization)) {
            return unauthorized(exchange, "未登录或登录已过期");
        }

        String token = authorization.substring(ServiceConstants.TOKEN_PREFIX.length());
        try {
            Claims claims = parseToken(token);
            if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
                return unauthorized(exchange, "登录已过期，请重新登录");
            }

            // JWT 签名和过期时间通过后，再检查 Redis 黑名单，确保退出后的 token 不能继续访问。
            return isTokenBlacklisted(token)
                    .flatMap(blacklisted -> {
                        if (Boolean.TRUE.equals(blacklisted)) {
                            return unauthorized(exchange, "登录已退出，请重新登录");
                        }

                        // Token 校验通过后，把用户名透传给下游服务，方便后续接口做用户级数据隔离。
                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(builder -> builder
                                        .header("X-User-Name", claims.getSubject())
                                        .header("X-User-Roles", String.valueOf(claims.get("roles")))
                                        .header("X-User-Role", String.valueOf(claims.get("roleKey")))
                                        .header("X-Super-Admin", String.valueOf(claims.get("superAdmin")))
                                        .header("X-Visitor", String.valueOf(claims.get("visitor")))
                                        .header("X-Menu-Keys", String.valueOf(claims.get("menuKeys")))
                                        .header("X-Directory-Ids", String.valueOf(claims.get("directoryIds")))
                                        .header("X-Album-Ids", String.valueOf(claims.get("albumIds")))
                                        .header("X-Tag-Ids", String.valueOf(claims.get("tagIds"))))
                                .build();
                        return chain.filter(mutatedExchange);
                    });
        } catch (Exception e) {
            log.warn("网关鉴权失败: path={}, reason={}", path, e.getMessage());
            return unauthorized(exchange, "登录已过期，请重新登录");
        }
    }

    /**
     * 判断当前请求是否属于公开白名单。
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST.contains(path);
    }

    /**
     * 判断请求头中是否携带 Bearer Token。
     */
    private boolean hasBearerToken(String authorization) {
        return authorization != null && authorization.startsWith(ServiceConstants.TOKEN_PREFIX);
    }

    /**
     * 解析并校验 JWT 签名，签名错误或格式错误会抛出异常。
     */
    private Claims parseToken(String token) {
        SecretKey signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
    }

    /**
     * 查询 Redis 黑名单，判断 token 是否已在退出登录时失效。
     */
    private Mono<Boolean> isTokenBlacklisted(String token) {
        return reactiveStringRedisTemplate.hasKey(AuthConstants.JWT_BLACKLIST_PREFIX + token);
    }

    /**
     * 返回统一的 401 JSON 响应，避免未登录请求继续进入业务服务。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
