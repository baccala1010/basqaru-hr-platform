package kz.whosnext.hr.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.whosnext.hr.auth.service.JwtService;
import kz.whosnext.hr.auth.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String jwt = extractJwt(request);
        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateWithJwt(jwt, request);
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateWithGatewayHeaders(request);
        }

        chain.doFilter(request, response);
    }

    private void authenticateWithJwt(String jwt, HttpServletRequest request) {
        try {
            if (!jwtService.isTokenValid(jwt)) return;

            String tokenType = jwtService.getTokenType(jwt);
            if (!"access".equals(tokenType)) return;

            String jti = jwtService.getTokenId(jwt);
            if (redisTokenService.isBlacklisted(jti)) {
                log.debug("Токен в blacklist: jti={}", jti);
                return;
            }

            Claims claims = jwtService.parseToken(jwt);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var auth = new UsernamePasswordAuthenticationToken(
                    UUID.fromString(userId), null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            log.debug("JWT аутентификация не удалась: {}", e.getMessage());
        }
    }

    private void authenticateWithGatewayHeaders(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        if (userId != null && role != null) {
            try {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var auth = new UsernamePasswordAuthenticationToken(
                        UUID.fromString(userId), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                log.debug("Gateway-header аутентификация не удалась: {}", e.getMessage());
            }
        }
    }

    private String extractJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}

