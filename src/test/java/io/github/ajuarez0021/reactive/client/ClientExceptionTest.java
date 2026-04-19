package io.github.ajuarez0021.reactive.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ClientExceptionTest {

    @Test
    void constructor_message_setsMessage() {
        ClientException ex = new ClientException("error message");

        assertThat(ex.getMessage()).isEqualTo("error message");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructor_messageAndCause_setsBoth() {
        RuntimeException cause = new RuntimeException("root cause");
        ClientException ex = new ClientException("error message", cause);

        assertThat(ex.getMessage()).isEqualTo("error message");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_cause_wrapsCause() {
        RuntimeException cause = new RuntimeException("root cause");
        ClientException ex = new ClientException(cause);

        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void isRuntimeException() {
        assertThat(new ClientException("x")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void isSerializable() {
        assertThatCode(() -> {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            new java.io.ObjectOutputStream(baos).writeObject(new ClientException("test"));
        }).doesNotThrowAnyException();
    }
}
