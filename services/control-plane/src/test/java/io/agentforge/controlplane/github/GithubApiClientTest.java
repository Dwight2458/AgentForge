package io.agentforge.controlplane.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.agentforge.controlplane.config.GithubProperties;

class GithubApiClientTest {

    private HttpServer server;
    private GithubApiClient client;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> apiVersion = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/user/installations/42/repositories", exchange -> respond(exchange, """
                {"total_count":1,"repositories":[{
                  "id":77,
                  "full_name":"dwight2458/AgentForge-DemoMall",
                  "name":"AgentForge-DemoMall",
                  "description":"Agent demo application",
                  "default_branch":"main",
                  "clone_url":"https://github.com/dwight2458/AgentForge-DemoMall.git",
                  "private":false,
                  "html_url":"https://github.com/dwight2458/AgentForge-DemoMall"
                }]}
                """));
        server.createContext("/user/installations", exchange -> respond(exchange, """
                {"total_count":1,"installations":[{
                  "id":42,
                  "account":{"login":"dwight2458","avatar_url":"https://avatars.example/user.png"},
                  "repository_selection":"selected",
                  "html_url":"https://github.com/settings/installations/42"
                }]}
                """));
        server.start();

        GithubProperties properties = new GithubProperties(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                "2026-03-10",
                "agentforge",
                0,
                "");
        client = new GithubApiClient(
                new ObjectMapper(), properties, new GithubAppJwtFactory(properties));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void mapsInstallationsAndRepositoriesUsingTheUserToken() {
        var installations = client.listInstallations("user-token");
        var repositories = client.listRepositories("user-token", 42);

        assertThat(installations).singleElement().satisfies(installation -> {
            assertThat(installation.id()).isEqualTo(42);
            assertThat(installation.accountLogin()).isEqualTo("dwight2458");
        });
        assertThat(repositories).singleElement().satisfies(repository -> {
            assertThat(repository.installationId()).isEqualTo(42);
            assertThat(repository.fullName()).isEqualTo("dwight2458/AgentForge-DemoMall");
            assertThat(repository.defaultBranch()).isEqualTo("main");
        });
        assertThat(authorization).hasValue("Bearer user-token");
        assertThat(apiVersion).hasValue("2026-03-10");
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        apiVersion.set(exchange.getRequestHeaders().getFirst("X-GitHub-Api-Version"));
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
