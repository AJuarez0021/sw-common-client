package io.github.ajuarez0021.reactive.client;

import io.github.ajuarez0021.reactive.client.autoconfigure.RestHttpClient;
import io.github.ajuarez0021.reactive.client.autoconfigure.SSLConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.*;

class HttpClientConfigurerTest {

    // ── Test configurations ───────────────────────────────────────────────────

    /** http:// + enabled=false → SSL skipped entirely */
    @RestHttpClient(url = "http://example.com", name = "http-insecure",
            ssl = @SSLConfig(enabled = false))
    interface HttpInsecureClient {}

    /** http:// + enabled=true → SSL skipped entirely (HTTP does not use TLS) */
    @RestHttpClient(url = "http://example.com", name = "http-secure",
            ssl = @SSLConfig(enabled = true))
    interface HttpSecureClient {}

    /** https:// + enabled=true, empty stores → system CA trust (proper HTTPS) */
    @RestHttpClient(url = "https://api.example.com", name = "https-system-ca",
            ssl = @SSLConfig(enabled = true))
    interface HttpsSystemCaClient {}

    /** https:// + enabled=false → trust-all (dev only) */
    @RestHttpClient(url = "https://api.example.com", name = "https-insecure",
            ssl = @SSLConfig(enabled = false))
    interface HttpsInsecureClient {}

    /** https:// + enabled=true + explicit empty paths → system CA trust */
    @RestHttpClient(url = "https://secure.example.com", name = "https-empty-stores",
            ssl = @SSLConfig(enabled = true, keystorePath = "", truststorePath = ""))
    interface HttpsEmptyStoresClient {}

    @RestHttpClient(url = "http://example.com", name = "with-ttl",
            connectionTimeToLive = 60000,
            ssl = @SSLConfig(enabled = false))
    interface TtlClient {}

    @RestHttpClient(url = "http://example.com", name = "zero-ttl",
            connectionTimeToLive = 0,
            ssl = @SSLConfig(enabled = false))
    interface ZeroTtlClient {}

    @RestHttpClient(url = "http://example.com", name = "custom-timeouts",
            connectTimeout = 3000,
            readTimeout = 10000,
            maxConnections = 50,
            ssl = @SSLConfig(enabled = false))
    interface CustomTimeoutClient {}

    // ── HTTP (no SSL configured) ──────────────────────────────────────────────

    @Test
    void configureWebClient_httpUrl_insecureFlag_returnWebClient() {
        RestHttpClient config = HttpInsecureClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_httpUrl_secureFlag_returnsWebClient_withoutSSL() {
        // SSL is skipped for http:// regardless of the enabled flag
        RestHttpClient config = HttpSecureClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_httpUrl_badKeystorePath_doesNotThrow() {
        // For http:// URLs SSL is never configured, so a bad keystore path is irrelevant
        @RestHttpClient(url = "http://example.com", name = "http-bad-ssl",
                ssl = @SSLConfig(enabled = true,
                        keystorePath = "/nonexistent/keystore.jks",
                        keystorePassword = "secret"))
        interface HttpBadSSLClient {}

        RestHttpClient config = HttpBadSSLClient.class.getAnnotation(RestHttpClient.class);
        assertThatNoException().isThrownBy(() -> HttpClientConfigurer.configureWebClient(config));
    }

    // ── HTTPS (SSL configured) ────────────────────────────────────────────────

    @Test
    void configureWebClient_httpsUrl_secureFlag_emptyStores_returnsWebClient() {
        // Uses system CA trust store — the most common production scenario
        RestHttpClient config = HttpsSystemCaClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_httpsUrl_insecureFlag_returnsWebClient() {
        // trust-all — valid for development, logs a warning
        RestHttpClient config = HttpsInsecureClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_httpsUrl_explicitEmptyStores_returnsWebClient() {
        RestHttpClient config = HttpsEmptyStoresClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_httpsUrl_invalidKeystorePath_throwsClientException() {
        // For https:// URLs SSL IS configured, so a bad keystore path must throw
        @RestHttpClient(url = "https://secure.example.com", name = "https-bad-ssl",
                ssl = @SSLConfig(enabled = true,
                        keystorePath = "/nonexistent/keystore.jks",
                        keystorePassword = "secret"))
        interface HttpsBadSSLClient {}

        RestHttpClient config = HttpsBadSSLClient.class.getAnnotation(RestHttpClient.class);
        assertThatThrownBy(() -> HttpClientConfigurer.configureWebClient(config))
                .isInstanceOf(ClientException.class)
                .hasMessageContaining("Error configurando WebClient");
    }

    // ── Connection pool ───────────────────────────────────────────────────────

    @Test
    void configureWebClient_withConnectionTimeToLive_returnsWebClient() {
        RestHttpClient config = TtlClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_withZeroConnectionTimeToLive_usesDefaultIdleTime() {
        RestHttpClient config = ZeroTtlClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_withCustomTimeouts_returnsWebClient() {
        RestHttpClient config = CustomTimeoutClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_returnsDistinctInstancePerCall() {
        RestHttpClient config = HttpInsecureClient.class.getAnnotation(RestHttpClient.class);
        WebClient first = HttpClientConfigurer.configureWebClient(config);
        WebClient second = HttpClientConfigurer.configureWebClient(config);
        assertThat(first).isNotSameAs(second);
    }
}
