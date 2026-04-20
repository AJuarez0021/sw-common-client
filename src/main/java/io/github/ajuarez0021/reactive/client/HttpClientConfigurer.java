package io.github.ajuarez0021.reactive.client;

import io.github.ajuarez0021.reactive.client.autoconfigure.RestHttpClient;
import io.github.ajuarez0021.reactive.client.autoconfigure.SSLConfig;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.ConnectionProvider;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * The Class HttpClientConfigurer.
 */
public final class HttpClientConfigurer {

    private HttpClientConfigurer() {
    }

    /**
     * Configures a WebClient using the URL declared in the annotation.
     * Delegates to {@link #configureWebClient(RestHttpClient, String)}.
     *
     * @param config the client annotation config
     * @return configured WebClient
     */
    public static WebClient configureWebClient(RestHttpClient config) {
        return configureWebClient(config, config.url());
    }

    /**
     * Configures a WebClient with Reactor Netty, connection pooling and SSL.
     *
     * SSL is only configured when the resolved base URL uses the {@code https://} scheme:
     * <ul>
     *   <li>{@code https://} + {@code ssl.enabled=true}  → system CAs or mTLS (secure)</li>
     *   <li>{@code https://} + {@code ssl.enabled=false} → trust-all (development only)</li>
     *   <li>{@code http://}  (any ssl setting)           → no SSL configuration applied</li>
     * </ul>
     *
     * @param config the client annotation config
     * @param resolvedBaseUrl the base URL with property placeholders already resolved
     * @return configured WebClient
     */
    public static WebClient configureWebClient(RestHttpClient config, String resolvedBaseUrl) {
        try {
            ConnectionProvider connectionProvider = ConnectionProvider.builder("http-pool")
                    .maxConnections(config.maxConnections())
                    .maxIdleTime(config.connectionTimeToLive() > 0
                            ? Duration.ofMillis(config.connectionTimeToLive())
                            : Duration.ofMinutes(2))
                    .maxLifeTime(config.connectionTimeToLive() > 0
                            ? Duration.ofMillis(config.connectionTimeToLive())
                            : Duration.ZERO)
                    .pendingAcquireMaxCount(500)
                    .pendingAcquireTimeout(Duration.ofMillis(config.connectTimeout()))
                    .build();

            reactor.netty.http.client.HttpClient httpClient =
                    reactor.netty.http.client.HttpClient.create(connectionProvider)
                    .responseTimeout(Duration.ofMillis(config.readTimeout()))
                    .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                            (int) config.connectTimeout())
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(config.readTimeout(),
                                    TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(config.readTimeout(),
                                    TimeUnit.MILLISECONDS))
                    );

            // SSL is only meaningful for HTTPS endpoints. Plain HTTP requests must not
            // be routed through an SSL context — doing so adds overhead and, when
            // ssl.enabled=false, configures trust-all unnecessarily for non-TLS traffic.
            boolean isHttps = resolvedBaseUrl != null
                    && resolvedBaseUrl.toLowerCase().startsWith("https://");

            if (isHttps) {
                SSLConfig sslConfig = config.ssl();
                if (sslConfig.enabled()) {
                    httpClient = SSLConfiguration.configureWebClientSSL(
                            httpClient,
                            sslConfig.keystorePath(),
                            sslConfig.keystorePassword(),
                            sslConfig.truststorePath(),
                            sslConfig.truststorePassword()
                    );
                } else {
                    httpClient = SSLConfiguration.configureWebClientInsecureSSL(httpClient);
                }
            }

            return WebClient.builder()
                    .baseUrl(resolvedBaseUrl)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (Exception e) {
            throw new ClientException("Error configurando WebClient", e);
        }
    }
}
