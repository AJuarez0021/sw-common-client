package com.work.common.client;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.KeyStore;


/**
 * The Class SSLConfiguration.
 */
@Slf4j
public final class SSLConfiguration {
	
	/**
	 * Instantiates a new SSL configuration.
	 */
	private SSLConfiguration() {
		
	}
    /**
     * Configures SSL/TLS for WebClient using Netty's SslContext.
     *
     * @param httpClient the Reactor Netty HTTP client
     * @param keystorePath the keystore path
     * @param keystorePassword the keystore password
     * @param truststorePath the truststore path
     * @param truststorePassword the truststore password
     * @return the configured HTTP client
     * @throws SSLException if SSL configuration fails
     */
    public static reactor.netty.http.client.HttpClient configureWebClientSSL(
            reactor.netty.http.client.HttpClient httpClient,
            String keystorePath,
            String keystorePassword,
            String truststorePath,
            String truststorePassword) throws SSLException {

        try {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

            if (keystorePath != null && !keystorePath.isEmpty()) {
                KeyManager[] keyManagers = createKeyManagers(keystorePath, keystorePassword);
                if (keyManagers != null && keyManagers.length > 0) {
                    sslContextBuilder.keyManager(keyManagers[0]);
                }
            }

            if (truststorePath != null && !truststorePath.isEmpty()) {
                TrustManager[] trustManagers = createTrustManagers(truststorePath, truststorePassword);
                if (trustManagers != null && trustManagers.length > 0) {
                    sslContextBuilder.trustManager(trustManagers[0]);
                }
            }

            SslContext sslContext = sslContextBuilder.build();

            return httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException
                | CertificateException | UnrecoverableKeyException
                | IllegalArgumentException e) {
            throw new SSLException("Error configuring SSL for WebClient", e);
        }
    }
    /**
     * Configures insecure SSL for WebClient (development only).
     *
     * @param httpClient the Reactor Netty HTTP client
     * @return the configured HTTP client
     * @throws SSLException The ssl exception
     */
    public static reactor.netty.http.client.HttpClient configureWebClientInsecureSSL(
            reactor.netty.http.client.HttpClient httpClient) throws SSLException {

        log.warn("Insecure SSL configured: certificate and hostname validation disabled."
                + " Use only in development environments.");

        SslContext nettySslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        return httpClient.secure(sslSpec -> sslSpec
                .sslContext(nettySslContext)
                .handlerConfigurator(handler -> {
                    SSLEngine engine = handler.engine();
                    SSLParameters params = engine.getSSLParameters();
                    params.setEndpointIdentificationAlgorithm(null);
                    engine.setSSLParameters(params);
                }));
    }

    /**
     * Creates the key managers.
     *
     * @param keystorePath the keystore path
     * @param password the password
     * @return the key manager[]
     * @throws KeyStoreException the exception
     */
    private static KeyManager[] createKeyManagers(String keystorePath, String password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(getKeystoreType(keystorePath));

        try (InputStream keystoreStream = getResourceStream(keystorePath)) {
            keyStore.load(keystoreStream, password.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());

        return kmf.getKeyManagers();
    }

    /**
     * Creates the trust managers.
     *
     * @param truststorePath the truststore path
     * @param password the password
     * @return the trust manager[]
     * @throws KeyStoreException the exception
     */
    private static TrustManager[] createTrustManagers(String truststorePath, String password)
            throws KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException {
        KeyStore trustStore = KeyStore.getInstance(getKeystoreType(truststorePath));

        try (InputStream truststoreStream = getResourceStream(truststorePath)) {
            trustStore.load(truststoreStream, password.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        return tmf.getTrustManagers();
    }

    /**
     * Gets the keystore type.
     *
     * @param path the path
     * @return the keystore type
     */
    private static String getKeystoreType(String path) {
        if (path.endsWith(".p12") || path.endsWith(".pfx")) {
            return "PKCS12";
        }
        return "JKS";
    }

    /**
     * Gets the resource stream.
     *
     * @param path the path
     * @return the resource stream
     * @throws IOException the exception
     */
    private static InputStream getResourceStream(String path) throws IOException{
        if (path.startsWith("classpath:")) {
            String resourcePath = path.substring("classpath:".length());
            InputStream stream = SSLConfiguration.class.getClassLoader()
                    .getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return stream;
        }
        return new FileInputStream(path);
    }
}
