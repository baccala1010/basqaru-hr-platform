package kz.whosnext.hr.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> OPEN_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/confirm",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/oauth2/**",
            "/api/v1/contracts/*/verify",
            "/actuator/**",
            "/ws/**"
    );

    private static final List<OpenRoute> OPEN_METHOD_PATHS = List.of(
            new OpenRoute(HttpMethod.GET, "/api/v1/vacancies")
    );

    private static final List<String> OPEN_GET_PATTERNS = List.of(
            "/api/v1/vacancies/*"
    );

    private final SecretKey signingKey;

    public AuthenticationFilter(@Value("${app.jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");
        boolean openPath = isOpenPath(path, request.getMethod());

        if (!hasToken && openPath) {
            return chain.filter(exchange);
        }

        if (!hasToken) {
            return unauthorized(exchange, "Токен отсутствует");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.debug("JWT невалиден: {}", e.getMessage());
            if (openPath) {
                return chain.filter(exchange);
            }
            return unauthorized(exchange, "Невалидный токен");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isOpenPath(String path, HttpMethod method) {
        for (String pattern : OPEN_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        for (OpenRoute route : OPEN_METHOD_PATHS) {
            if (route.method() == method && path.equals(route.path())) {
                return true;
            }
        }
        if (method == HttpMethod.GET) {
            for (String pattern : OPEN_GET_PATTERNS) {
                if (PATH_MATCHER.match(pattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}".formatted(message);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private record OpenRoute(HttpMethod method, String path) {}
}

