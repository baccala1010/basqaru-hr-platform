package kz.whosnext.hr.auth.security;

import kz.whosnext.hr.auth.model.entity.User;
import kz.whosnext.hr.auth.model.enums.OAuthProvider;
import kz.whosnext.hr.auth.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthService authService;

    public CustomOAuth2UserService(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String firstName = (String) attributes.get("given_name");
        String lastName = (String) attributes.get("family_name");
        String providerId = (String) attributes.get("sub");

        User user = authService.processOAuth2User(email, firstName, lastName, providerId, OAuthProvider.GOOGLE);

        log.info("OAuth2 пользователь обработан: email={}, userId={}", email, user.getId());

        var updatedAttributes = new HashMap<>(attributes);
        updatedAttributes.put("userId", user.getId().toString());
        updatedAttributes.put("role", user.getRole().name());

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                updatedAttributes,
                "email"
        );
    }
}

