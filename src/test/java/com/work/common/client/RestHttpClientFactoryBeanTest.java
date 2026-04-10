package com.work.common.client;

import com.work.common.autoconfigure.RestHttpClient;
import com.work.common.autoconfigure.SSLConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestHttpClientFactoryBeanTest {

    @RestHttpClient(url = "http://example.com", name = "factory-test",
            ssl = @SSLConfig(enabled = false))
    interface SampleClient {
        @GetMapping("/ping")
        String ping();
    }

    @RestHttpClient(url = "http://example.com", name = "",
            ssl = @SSLConfig(enabled = false))
    interface NoNameClient {
        @GetMapping("/ping")
        String ping();
    }

    // ── constructor ───────────────────────────────────────────────────────────

    @Test
    void constructor_nullInterface_throwsNullPointerException() {
        assertThatThrownBy(() ->
                new RestHttpClientFactoryBean<>(null, new Resilience4jConfiguration()))
                .isInstanceOf(NullPointerException.class);
    }

    // ── getObject ─────────────────────────────────────────────────────────────

    @Test
    void getObject_returnsProxyImplementingClientInterface() throws Exception {
        RestHttpClientFactoryBean<SampleClient> factory =
                new RestHttpClientFactoryBean<>(SampleClient.class, new Resilience4jConfiguration());

        SampleClient client = factory.getObject();

        assertThat(client).isNotNull();
        assertThat(client).isInstanceOf(SampleClient.class);
    }

    @Test
    void getObject_calledTwice_returnsDifferentProxyInstances() throws Exception {
        RestHttpClientFactoryBean<SampleClient> factory =
                new RestHttpClientFactoryBean<>(SampleClient.class, new Resilience4jConfiguration());

        Object first = factory.getObject();
        Object second = factory.getObject();

        assertThat(first).isNotSameAs(second);
    }

    // ── getObjectType ─────────────────────────────────────────────────────────

    @Test
    void getObjectType_returnsClientInterface() {
        RestHttpClientFactoryBean<SampleClient> factory =
                new RestHttpClientFactoryBean<>(SampleClient.class, new Resilience4jConfiguration());

        assertThat(factory.getObjectType()).isEqualTo(SampleClient.class);
    }

    // ── isSingleton ───────────────────────────────────────────────────────────

    @Test
    void isSingleton_returnsTrue() {
        RestHttpClientFactoryBean<SampleClient> factory =
                new RestHttpClientFactoryBean<>(SampleClient.class, new Resilience4jConfiguration());

        assertThat(factory.isSingleton()).isTrue();
    }

    // ── setApplicationContext / getApplicationContext ─────────────────────────

    @Test
    void setApplicationContext_storesTheContext() {
        RestHttpClientFactoryBean<SampleClient> factory =
                new RestHttpClientFactoryBean<>(SampleClient.class, new Resilience4jConfiguration());
        ApplicationContext ctx = mock(ApplicationContext.class);

        factory.setApplicationContext(ctx);

        assertThat(factory.getApplicationContext()).isSameAs(ctx);
    }

    @Test
    void getApplicationContext_beforeSet_returnsNull() {
        RestHttpClientFactoryBean<SampleClient> factory =
                new RestHttpClientFactoryBean<>(SampleClient.class, new Resilience4jConfiguration());

        assertThat(factory.getApplicationContext()).isNull();
    }
}
