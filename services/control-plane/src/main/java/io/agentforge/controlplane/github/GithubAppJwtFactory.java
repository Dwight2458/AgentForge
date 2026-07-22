package io.agentforge.controlplane.github;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.agentforge.controlplane.config.GithubProperties;

@Component
public class GithubAppJwtFactory {

    private static final byte[] RSA_ALGORITHM_IDENTIFIER = new byte[] {
            0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86,
            (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00
    };

    private final GithubProperties properties;
    private final Clock clock;

    @Autowired
    public GithubAppJwtFactory(GithubProperties properties) {
        this(properties, Clock.systemUTC());
    }

    GithubAppJwtFactory(GithubProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String create() {
        if (!properties.appCredentialsConfigured()) {
            throw new GithubConfigurationException("GitHub App ID and private key are not configured");
        }

        try {
            Instant now = clock.instant();
            String header = encode("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
            String payload = encode("{\"iat\":" + now.minusSeconds(60).getEpochSecond()
                    + ",\"exp\":" + now.plusSeconds(540).getEpochSecond()
                    + ",\"iss\":" + properties.appId() + "}");
            String signingInput = header + "." + payload;

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(readPrivateKey(properties.privateKey()));
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        } catch (GithubConfigurationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GithubConfigurationException("Unable to create the GitHub App JWT", exception);
        }
    }

    private static PrivateKey readPrivateKey(String pem) throws Exception {
        boolean pkcs1 = pem.contains("BEGIN RSA PRIVATE KEY");
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        if (pkcs1) {
            keyBytes = wrapPkcs1AsPkcs8(keyBytes);
        }
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private static byte[] wrapPkcs1AsPkcs8(byte[] pkcs1) {
        byte[] version = new byte[] { 0x02, 0x01, 0x00 };
        byte[] privateKey = tlv(0x04, pkcs1);
        return tlv(0x30, concat(version, RSA_ALGORITHM_IDENTIFIER, privateKey));
    }

    private static byte[] tlv(int tag, byte[] value) {
        byte[] length = derLength(value.length);
        byte[] output = new byte[1 + length.length + value.length];
        output[0] = (byte) tag;
        System.arraycopy(length, 0, output, 1, length.length);
        System.arraycopy(value, 0, output, 1 + length.length, value.length);
        return output;
    }

    private static byte[] derLength(int length) {
        if (length < 128) {
            return new byte[] { (byte) length };
        }
        int byteCount = Integer.BYTES - Integer.numberOfLeadingZeros(length) / 8;
        byte[] encoded = new byte[byteCount + 1];
        encoded[0] = (byte) (0x80 | byteCount);
        for (int index = byteCount; index > 0; index--) {
            encoded[index] = (byte) (length & 0xff);
            length >>>= 8;
        }
        return encoded;
    }

    private static byte[] concat(byte[]... values) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] value : values) {
            output.writeBytes(value);
        }
        return output.toByteArray();
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
