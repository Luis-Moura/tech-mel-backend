package com.tech_mel.tech_mel.infrastructure.security.oauth2;

import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepositoryPort userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String sub = oAuth2User.getAttribute("sub");

        if (email == null) {
            throw new OAuth2AuthenticationException("Email não encontrado do OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            if (user.getAuthProvider() != User.AuthProvider.GOOGLE) {
                throw new OAuth2AuthenticationException(
                        "Você já possui uma conta registrada com " + user.getAuthProvider() + ". Por favor, use essa forma de login."
                );
            }
            user = updateExistingUser(user, name, sub);
        } else {
            user = registerNewUser(email, name, sub);
        }

        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("userId", user.getId().toString());

        return new DefaultOAuth2User(
                authorities,
                attributes,
                "sub"
        );
    }

    private User registerNewUser(String email, String name, String sub) {
        User user = User.builder()
                .email(email)
                .name(name)
                .password("")
                .providerId(sub)
                .authProvider(User.AuthProvider.GOOGLE)
                .emailVerified(true)
                .enabled(true)
                .role(User.Role.COMMON)
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User user, String name, String sub) {
        user.setName(name);
        user.setProviderId(sub);
        return userRepository.save(user);
    }
}