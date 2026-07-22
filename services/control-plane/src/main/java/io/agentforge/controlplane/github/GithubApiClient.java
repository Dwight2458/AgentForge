package io.agentforge.controlplane.github;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.agentforge.controlplane.config.GithubProperties;

@Service
public class GithubApiClient {

    private static final int PAGE_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final GithubProperties properties;
    private final GithubAppJwtFactory jwtFactory;
    private final HttpClient httpClient;

    @Autowired
    public GithubApiClient(
            ObjectMapper objectMapper,
            GithubProperties properties,
            GithubAppJwtFactory jwtFactory) {
        this(objectMapper, properties, jwtFactory, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    GithubApiClient(
            ObjectMapper objectMapper,
            GithubProperties properties,
            GithubAppJwtFactory jwtFactory,
            HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.jwtFactory = jwtFactory;
        this.httpClient = httpClient;
    }

    public List<Installation> listInstallations(String userAccessToken) {
        JsonNode body = send("GET", "/user/installations?per_page=100", userAccessToken, null);
        List<Installation> installations = new ArrayList<>();
        for (JsonNode node : body.path("installations")) {
            JsonNode account = node.path("account");
            installations.add(new Installation(
                    node.path("id").asLong(),
                    account.path("login").asText(),
                    account.path("avatar_url").asText(null),
                    node.path("repository_selection").asText(),
                    node.path("html_url").asText(null)));
        }
        return List.copyOf(installations);
    }

    public List<Repository> listRepositories(String userAccessToken, long installationId) {
        List<Repository> repositories = new ArrayList<>();
        for (int page = 1; page <= 100; page++) {
            String path = "/user/installations/" + installationId
                    + "/repositories?per_page=" + PAGE_SIZE + "&page=" + page;
            JsonNode body = send("GET", path, userAccessToken, null);
            JsonNode pageRepositories = body.path("repositories");
            for (JsonNode node : pageRepositories) {
                repositories.add(toRepository(node, installationId));
            }
            if (pageRepositories.size() < PAGE_SIZE) {
                break;
            }
        }
        return List.copyOf(repositories);
    }

    public InstallationToken createInstallationToken(long installationId, String repositoryFullName) {
        String[] repositoryParts = repositoryFullName.split("/", 2);
        if (repositoryParts.length != 2 || repositoryParts[1].isBlank()) {
            throw new IllegalArgumentException("Repository must use owner/name format");
        }
        String repositoryName = repositoryParts[1];
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(java.util.Map.of("repositories", List.of(repositoryName)));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize the installation token request", exception);
        }
        JsonNode response = send(
                "POST",
                "/app/installations/" + installationId + "/access_tokens",
                jwtFactory.create(),
                requestBody);
        return new InstallationToken(
                response.path("token").asText(),
                Instant.parse(response.path("expires_at").asText()),
                installationId,
                repositoryFullName);
    }

    private Repository toRepository(JsonNode node, long installationId) {
        return new Repository(
                node.path("id").asLong(),
                installationId,
                node.path("full_name").asText(),
                node.path("name").asText(),
                node.path("description").isNull() ? null : node.path("description").asText(null),
                node.path("default_branch").asText("main"),
                node.path("clone_url").asText(),
                node.path("private").asBoolean(),
                node.path("html_url").asText());
    }

    private JsonNode send(String method, String path, String bearerToken, String requestBody) {
        try {
            HttpRequest.BodyPublisher publisher = requestBody == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(requestBody);
            HttpRequest request = HttpRequest.newBuilder(resolve(path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("X-GitHub-Api-Version", properties.apiVersion())
                    .header("User-Agent", "AgentForge-Control-Plane")
                    .header("Content-Type", "application/json")
                    .method(method, publisher)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = response.body().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = body.path("message").asText("GitHub API request failed");
                throw new GithubApiException(response.statusCode(), message);
            }
            return body;
        } catch (GithubApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GithubApiException("GitHub API request was interrupted", exception);
        } catch (IOException exception) {
            throw new GithubApiException("Unable to call the GitHub API", exception);
        }
    }

    private URI resolve(String path) {
        String encodedPath = path.replace(" ", URLEncoder.encode(" ", StandardCharsets.UTF_8));
        return properties.apiBaseUrl().resolve(encodedPath);
    }

    public record Installation(
            long id,
            String accountLogin,
            String accountAvatarUrl,
            String repositorySelection,
            String htmlUrl) {}

    public record Repository(
            long id,
            long installationId,
            String fullName,
            String name,
            String description,
            String defaultBranch,
            String cloneUrl,
            boolean privateRepository,
            String htmlUrl) {}

    public record InstallationToken(
            String token,
            Instant expiresAt,
            long installationId,
            String repositoryFullName) {}
}
