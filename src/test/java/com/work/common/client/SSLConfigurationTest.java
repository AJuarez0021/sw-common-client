package com.work.common.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.assertj.core.api.Assertions.*;

class SSLConfigurationTest {

    // ── configureWebClientInsecureSSL ─────────────────────────────────────────

    @Test
    void configureWebClientInsecureSSL_returnsNonNullClient() throws SSLException {
        HttpClient result = SSLConfiguration.configureWebClientInsecureSSL(HttpClient.create());
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClientInsecureSSL_returnsNewInstance() throws SSLException {
        HttpClient original = HttpClient.create();
        HttpClient result = SSLConfiguration.configureWebClientInsecureSSL(original);
        // Reactor Netty HttpClient is immutable — configure returns a new instance
        assertThat(result).isNotSameAs(original);
    }

    @Test
    void configureWebClientInsecureSSL_calledTwice_doesNotThrow() {
        assertThatCode(() -> {
            HttpClient first = SSLConfiguration.configureWebClientInsecureSSL(HttpClient.create());
            SSLConfiguration.configureWebClientInsecureSSL(first);
        }).doesNotThrowAnyException();
    }

    // ── configureWebClientSSL — empty/null paths (no keystore/truststore) ─────

    @Test
    void configureWebClientSSL_emptyPaths_returnsConfiguredClient() throws SSLException {
        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(), "", "", "", "");
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClientSSL_nullPaths_returnsConfiguredClient() throws SSLException {
        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(), null, null, null, null);
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClientSSL_nullKeystoreEmptyTruststore_returnsConfiguredClient() throws SSLException {
        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(), null, null, "", "");
        assertThat(result).isNotNull();
    }

    // ── configureWebClientSSL — with JKS keystore file ───────────────────────

    @Test
    void configureWebClientSSL_withJKSKeystore(@TempDir Path tempDir) throws Exception {
        File keystoreFile = createEmptyKeystore(tempDir, "keystore.jks", "JKS");

        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                keystoreFile.getAbsolutePath(), "password",
                "", "");
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClientSSL_withJKSTruststore(@TempDir Path tempDir) throws Exception {
        File truststoreFile = createEmptyKeystore(tempDir, "truststore.jks", "JKS");

        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                "", "",
                truststoreFile.getAbsolutePath(), "password");
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClientSSL_withJKSKeystoreAndTruststore(@TempDir Path tempDir) throws Exception {
        File keystoreFile = createEmptyKeystore(tempDir, "keystore.jks", "JKS");
        File truststoreFile = createEmptyKeystore(tempDir, "truststore.jks", "JKS");

        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                keystoreFile.getAbsolutePath(), "password",
                truststoreFile.getAbsolutePath(), "password");
        assertThat(result).isNotNull();
    }

    // ── configureWebClientSSL — PKCS12 / PFX keystores ───────────────────────

    @Test
    void configureWebClientSSL_withPKCS12Keystore(@TempDir Path tempDir) throws Exception {
        File keystoreFile = createEmptyKeystore(tempDir, "keystore.p12", "PKCS12");

        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                keystoreFile.getAbsolutePath(), "password",
                "", "");
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClientSSL_withPfxKeystore(@TempDir Path tempDir) throws Exception {
        File keystoreFile = createEmptyKeystore(tempDir, "keystore.pfx", "PKCS12");

        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                keystoreFile.getAbsolutePath(), "password",
                "", "");
        assertThat(result).isNotNull();
    }

    @Test
    void configureWebClientSSL_withPKCS12Truststore(@TempDir Path tempDir) throws Exception {
        File truststoreFile = createEmptyKeystore(tempDir, "truststore.p12", "PKCS12");

        HttpClient result = SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                "", "",
                truststoreFile.getAbsolutePath(), "password");
        assertThat(result).isNotNull();
    }

    // ── configureWebClientSSL — error cases ──────────────────────────────────

    @Test
    void configureWebClientSSL_keystoreFileNotFound_throwsSSLException() {
        assertThatThrownBy(() -> SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                "/nonexistent/path/keystore.jks", "password",
                "", ""))
                .isInstanceOf(SSLException.class);
    }

    @Test
    void configureWebClientSSL_truststoreFileNotFound_throwsSSLException() {
        assertThatThrownBy(() -> SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                "", "",
                "/nonexistent/path/truststore.jks", "password"))
                .isInstanceOf(SSLException.class);
    }

    @Test
    void configureWebClientSSL_keystoreClasspathResourceNotFound_throwsSSLException() {
        assertThatThrownBy(() -> SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                "classpath:nonexistent-keystore.jks", "password",
                "", ""))
                .isInstanceOf(SSLException.class);
    }

    @Test
    void configureWebClientSSL_truststoreClasspathResourceNotFound_throwsSSLException() {
        assertThatThrownBy(() -> SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                "", "",
                "classpath:nonexistent-truststore.jks", "password"))
                .isInstanceOf(SSLException.class);
    }

    @Test
    void configureWebClientSSL_wrongKeystorePassword_throwsSSLException(@TempDir Path tempDir) throws Exception {
        File keystoreFile = createEmptyKeystore(tempDir, "keystore.jks", "JKS");

        assertThatThrownBy(() -> SSLConfiguration.configureWebClientSSL(
                HttpClient.create(),
                keystoreFile.getAbsolutePath(), "wrongpassword",
                "", ""))
                .isInstanceOf(SSLException.class);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private File createEmptyKeystore(Path dir, String filename, String type) throws Exception {
        File file = dir.resolve(filename).toFile();
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(null, "password".toCharArray());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ks.store(fos, "password".toCharArray());
        }
        return file;
    }
}
