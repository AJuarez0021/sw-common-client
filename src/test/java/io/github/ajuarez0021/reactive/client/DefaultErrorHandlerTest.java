package io.github.ajuarez0021.reactive.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.*;

class DefaultErrorHandlerTest {

    private DefaultErrorHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DefaultErrorHandler();
    }

    // --- handleError ---

    @Test
    void handleError_4xxClientError_throwsHttpClientErrorException() {
        assertThatThrownBy(() -> handler.handleError(HttpStatus.BAD_REQUEST, "sensitive-token-xyz", null))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageNotContaining("sensitive-token-xyz")
                .hasMessageContaining("400");
    }

    @Test
    void handleError_404_throwsHttpClientErrorException() {
        assertThatThrownBy(() -> handler.handleError(HttpStatus.NOT_FOUND, "not found body", null))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageNotContaining("not found body")
                .hasMessageContaining("404");
    }

    @Test
    void handleError_5xxServerError_throwsHttpClientErrorException() {
        assertThatThrownBy(() -> handler.handleError(HttpStatus.INTERNAL_SERVER_ERROR, "stack trace data", null))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageNotContaining("stack trace data")
                .hasMessageContaining("500");
    }

    @Test
    void handleError_503_throwsHttpClientErrorException() {
        assertThatThrownBy(() -> handler.handleError(HttpStatus.SERVICE_UNAVAILABLE, "pii-data", null))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageNotContaining("pii-data")
                .hasMessageContaining("503");
    }

    @Test
    void handleError_bodyIsNeverLeakedInExceptionMessage() {
        String sensitiveBody = "Bearer eyJhbGciOiJSUzI1NiJ9.secret.payload";
        assertThatThrownBy(() -> handler.handleError(HttpStatus.UNAUTHORIZED, sensitiveBody, null))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageNotContaining(sensitiveBody);
    }

    @Test
    void handleError_nullBody_doesNotThrowNullPointer() {
        assertThatThrownBy(() -> handler.handleError(HttpStatus.BAD_REQUEST, null, null))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("400");
    }

    @Test
    void handleError_2xxSuccess_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> handler.handleError(HttpStatus.OK, "ok", null));
    }

    @Test
    void handleError_3xx_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> handler.handleError(HttpStatus.MOVED_PERMANENTLY, "redirect", null));
    }

    // --- shouldRetry ---

    @Test
    void shouldRetry_502_returnsTrue() {
        assertThat(handler.shouldRetry(HttpStatus.BAD_GATEWAY)).isTrue();
    }

    @Test
    void shouldRetry_503_returnsTrue() {
        assertThat(handler.shouldRetry(HttpStatus.SERVICE_UNAVAILABLE)).isTrue();
    }

    @Test
    void shouldRetry_504_returnsTrue() {
        assertThat(handler.shouldRetry(HttpStatus.GATEWAY_TIMEOUT)).isTrue();
    }

    @Test
    void shouldRetry_500_returnsFalse() {
        assertThat(handler.shouldRetry(HttpStatus.INTERNAL_SERVER_ERROR)).isFalse();
    }

    @Test
    void shouldRetry_404_returnsFalse() {
        assertThat(handler.shouldRetry(HttpStatus.NOT_FOUND)).isFalse();
    }

    @Test
    void shouldRetry_200_returnsFalse() {
        assertThat(handler.shouldRetry(HttpStatus.OK)).isFalse();
    }

    // --- shouldRecordAsFailure ---

    @Test
    void shouldRecordAsFailure_500_returnsTrue() {
        assertThat(handler.shouldRecordAsFailure(HttpStatus.INTERNAL_SERVER_ERROR)).isTrue();
    }

    @Test
    void shouldRecordAsFailure_502_returnsTrue() {
        assertThat(handler.shouldRecordAsFailure(HttpStatus.BAD_GATEWAY)).isTrue();
    }

    @Test
    void shouldRecordAsFailure_503_returnsTrue() {
        assertThat(handler.shouldRecordAsFailure(HttpStatus.SERVICE_UNAVAILABLE)).isTrue();
    }

    @Test
    void shouldRecordAsFailure_400_returnsFalse() {
        assertThat(handler.shouldRecordAsFailure(HttpStatus.BAD_REQUEST)).isFalse();
    }

    @Test
    void shouldRecordAsFailure_404_returnsFalse() {
        assertThat(handler.shouldRecordAsFailure(HttpStatus.NOT_FOUND)).isFalse();
    }

    @Test
    void shouldRecordAsFailure_200_returnsFalse() {
        assertThat(handler.shouldRecordAsFailure(HttpStatus.OK)).isFalse();
    }
}
