package io.agentforge.controlplane.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.agentforge.controlplane.config.GithubProperties;

class GithubAppJwtFactoryTest {

    @Test
    void createsAValidShortLivedRs256Jwt() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String privateKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                        .encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        GithubProperties properties = new GithubProperties(
                URI.create("https://api.github.com"), "2026-03-10", "agentforge", 12345, privateKey);
        Instant now = Instant.parse("2026-07-22T08:00:00Z");

        String jwt = new GithubAppJwtFactory(properties, Clock.fixed(now, ZoneOffset.UTC)).create();
        String[] parts = jwt.split("\\.");

        assertThat(parts).hasSize(3);
        ObjectMapper objectMapper = new ObjectMapper();
        var payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payload.path("iss").asLong()).isEqualTo(12345);
        assertThat(payload.path("iat").asLong()).isEqualTo(now.minusSeconds(60).getEpochSecond());
        assertThat(payload.path("exp").asLong()).isEqualTo(now.plusSeconds(540).getEpochSecond());

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
    }
}

