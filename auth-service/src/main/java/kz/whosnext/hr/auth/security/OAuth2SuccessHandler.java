package kz.whosnext.hr.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.whosnext.hr.auth.model.dto.response.AuthResponse;
import kz.whosnext.hr.auth.model.entity.User;
import kz.whosnext.hr.auth.model.enums.AuditAction;
import kz.whosnext.hr.auth.repository.UserRepository;
import kz.whosnext.hr.auth.service.AuditService;
import kz.whosnext.hr.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.oauth2.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String userId = oAuth2User.getAttribute("userId");

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow();

        AuthResponse tokens = authService.generateTokenPair(user);

        auditService.log(user.getId(), user.getEmail(), AuditAction.OAUTH2_LOGIN, true,
                "Google OAuth2 вход", request);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("access_token", tokens.accessToken())
                .queryParam("refresh_token", tokens.refreshToken())
                .build().toUriString();

        log.info("OAuth2 успешный вход: email={}", user.getEmail());
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

