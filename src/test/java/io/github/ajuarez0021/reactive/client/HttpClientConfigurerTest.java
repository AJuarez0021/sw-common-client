package io.github.ajuarez0021.reactive.client;

import io.github.ajuarez0021.reactive.client.autoconfigure.RestHttpClient;
import io.github.ajuarez0021.reactive.client.autoconfigure.SSLConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.*;

class HttpClientConfigurerTest {

    // ── Test configurations ───────────────────────────────────────────────────

    @RestHttpClient(url = "http://example.com", name = "insecure",
            ssl = @SSLConfig(enabled = false))
    interface InsecureClient {}

    @RestHttpClient(url = "http://example.com", name = "secure",
            ssl = @SSLConfig(enabled = true))
    interface SecureClient {}

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

    @RestHttpClient(url = "https://secure.example.com", name = "secure-empty-stores",
            ssl = @SSLConfig(enabled = true, keystorePath = "", truststorePath = ""))
    interface SecureEmptyStoresClient {}

    // ── configureWebClient ────────────────────────────────────────────────────

    @Test
    void configureWebClient_insecureSSL_returnsWebClient() {
        RestHttpClient config = InsecureClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_secureSSL_emptyPaths_returnsWebClient() {
        RestHttpClient config = SecureClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_withConnectionTimeToLive_returnsWebClient() {
        RestHttpClient config = TtlClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_withZeroConnectionTimeToLive_usesDefaultIdleTime() {
        RestHttpClient config = ZeroTtlClient.class.getAnnotation(RestHttpClient.class);
        // connectionTimeToLive <= 0 triggers the default Duration.ofMinutes(2) branch
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
    void configureWebClient_secureWithEmptyStores_returnsWebClient() {
        RestHttpClient config = SecureEmptyStoresClient.class.getAnnotation(RestHttpClient.class);
        WebClient result = HttpClientConfigurer.configureWebClient(config);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClient_returnsDistinctInstancePerCall() {
        RestHttpClient config = InsecureClient.class.getAnnotation(RestHttpClient.class);
        WebClient first = HttpClientConfigurer.configureWebClient(config);
        WebClient second = HttpClientConfigurer.configureWebClient(config);
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void configureWebClient_invalidSSLKeystorePath_throwsClientException() {
        // When SSL is enabled with a non-existent keystore file, configureWebClient
        // must catch the SSLException and wrap it in a ClientException
        @RestHttpClient(url = "http://example.com", name = "bad-ssl",
                ssl = @SSLConfig(enabled = true,
                        keystorePath = "/nonexistent/keystore.jks",
                        keystorePassword = "secret"))
        interface BadSSLClient {}

        RestHttpClient config = BadSSLClient.class.getAnnotation(RestHttpClient.class);
        assertThatThrownBy(() -> HttpClientConfigurer.configureWebClient(config))
                .isInstanceOf(ClientException.class)
                .hasMessageContaining("Error configurando WebClient");
    }
}
