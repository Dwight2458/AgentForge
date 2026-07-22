package io.agentforge.controlplane.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.agentforge.controlplane.config.AgentForgeProperties;
import io.agentforge.controlplane.config.GithubProperties;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AgentForgeProperties platformProperties;
    private final GithubProperties githubProperties;

    public AuthController(AgentForgeProperties platformProperties, GithubProperties githubProperties) {
        this.platformProperties = platformProperties;
        this.githubProperties = githubProperties;
    }

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth && oauth.isAuthenticated()) {
            return new SessionResponse(
                    true,
                    "github",
                    attribute(oauth, "login"),
                    attribute(oauth, "name"),
                    attribute(oauth, "avatar_url"),
                    null,
                    githubProperties.installUrl());
        }
        return new SessionResponse(
                false,
                platformProperties.security().devAuth() ? "development" : "github",
                platformProperties.security().devAuth() ? "local-developer" : null,
                platformProperties.security().devAuth() ? "Local developer" : null,
                null,
                platformProperties.security().devAuth() ? null : "/oauth2/authorization/github",
                githubProperties.installUrl());
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletRequest request) {
        Object attribute = request.getAttribute(CsrfToken.class.getName());
        if (!(attribute instanceof CsrfToken token)) {
            return new CsrfResponse(false, null, null);
        }
        return new CsrfResponse(true, token.getHeaderName(), token.getToken());
    }

    private static String attribute(OAuth2AuthenticationToken oauth, String name) {
        Object value = oauth.getPrincipal().getAttributes().get(name);
        return value == null ? null : value.toString();
    }

    public record SessionResponse(
            boolean authenticated,
            String mode,
            String login,
            String name,
            String avatarUrl,
            String loginUrl,
            String installUrl) {}

    public record CsrfResponse(boolean enabled, String headerName, String token) {}
}

