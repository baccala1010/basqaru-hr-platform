package kz.whosnext.hr.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_METHODS = Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
    );

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            String origin = request.getHeaders().getOrigin();
            if (origin != null && isAllowedOrigin(origin)) {
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        String.join(", ", ALLOWED_METHODS));
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        "Authorization, Content-Type, Accept, X-Requested-With, " +
                        "X-User-Id, X-User-Role, X-User-Email, Cache-Control");
                headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        "Authorization, X-User-Id, X-User-Role");
                headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
                headers.set(HttpHeaders.VARY, "Origin");
            }

            if (request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                return response.setComplete();
            }

            return chain.filter(exchange);
        };
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        // Allow all localhost and local network origins
        return origin.matches("https?://localhost(:\\d+)?") ||
               origin.matches("https?://127\\.0\\.0\\.1(:\\d+)?") ||
               origin.matches("https?://10\\.\\d+\\.\\d+\\.\\d+(:\\d+)?") ||
               origin.matches("https?://192\\.168\\.\\d+\\.\\d+(:\\d+)?") ||
               origin.matches("https?://172\\.(1[6-9]|2\\d|3[01])\\.\\d+\\.\\d+(:\\d+)?");
    }
}
