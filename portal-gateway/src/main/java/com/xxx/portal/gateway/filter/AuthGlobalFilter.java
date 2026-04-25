package com.xxx.portal.gateway.filter;

import com.xxx.portal.common.utils.JwtUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证全局过滤器
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 白名单：不需要认证的路径 */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/verify",
            "/api/auth/test-login",
            "/api/auth/logout"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 从 Header 获取 token
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || !JwtUtils.verify(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 验证通过，将用户信息透传给下游
        String empNo = JwtUtils.parseEmpNo(token);
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-EmpNo", empNo)
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isWhiteListed(String path) {
        for (String prefix : WHITE_LIST) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
