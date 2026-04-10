package com.work.common.client;


import com.work.common.autoconfigure.RestHttpClient;
import com.work.common.autoconfigure.SSLConfig;
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

    /**
     * Instantiates a new http client configurer.
     */
    private HttpClientConfigurer() {

    }
    



    /**
     * Configura un WebClient con Reactor Netty HTTP client.
     * Connection pool configuration:
     * - maxConnections: Total connections in pool
     * - connectionTimeToLive: Maximum time a connection can stay in pool
     *
     * @param config the config
     * @return the web client
     */
    public static WebClient configureWebClient(RestHttpClient config) {
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

            return WebClient.builder()
                    .baseUrl(config.url())
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

        } catch (Exception e) {
            throw new ClientException("Error configurando WebClient", e);
        }
    }
}
