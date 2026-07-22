package io.agentforge.controlplane.github;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class GithubAuthorizedClientTokenResolver {

    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClients;

    public GithubAuthorizedClientTokenResolver(ObjectProvider<OAuth2AuthorizedClientService> authorizedClients) {
        this.authorizedClients = authorizedClients;
    }

    public String resolve(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth)) {
            throw new GithubAuthenticationRequiredException("Sign in with GitHub before accessing installations");
        }
        OAuth2AuthorizedClientService service = authorizedClients.getIfAvailable();
        if (service == null) {
            throw new GithubConfigurationException("GitHub OAuth client registration is not configured");
        }
        OAuth2AuthorizedClient client = service.loadAuthorizedClient(
                oauth.getAuthorizedClientRegistrationId(), oauth.getName());
        if (client == null || client.getAccessToken() == null) {
            throw new GithubAuthenticationRequiredException("The GitHub user access token is unavailable; sign in again");
        }
        return client.getAccessToken().getTokenValue();
    }
}

