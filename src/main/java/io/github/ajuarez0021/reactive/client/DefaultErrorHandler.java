package io.github.ajuarez0021.reactive.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

/**
 * The Class DefaultErrorHandler.
 */
@Slf4j
public class DefaultErrorHandler implements ErrorHandler {

    private static final int MAX_LOGGED_BODY_LENGTH = 200;

    /**
     * Handle error.
     * The response body is intentionally excluded from the exception message to prevent
     * leaking sensitive data (tokens, PII, internal stack traces) into logs and error
     * propagation chains. The truncated body is available at DEBUG level for diagnostics.
     *
     * @param statusCode the status code
     * @param responseBody the response body
     * @param exception the exception
     */
    @Override
    public void handleError(HttpStatusCode statusCode, String responseBody, Exception exception) {
        log.debug("HTTP {} response body: {}", statusCode.value(), sanitize(responseBody));
        if (statusCode.is4xxClientError()) {
            throw new HttpClientErrorException(statusCode, "Client error " + statusCode.value());
        } else if (statusCode.is5xxServerError()) {
            throw new HttpClientErrorException(statusCode, "Server error " + statusCode.value());
        }
    }

    /**
     * Should retry.
     *
     * @param statusCode the status code
     * @return true, if successful
     */
    @Override
    public boolean shouldRetry(HttpStatusCode statusCode) {
        return statusCode.value() == 502
                || statusCode.value() == 503
                || statusCode.value() == 504;
    }

    /**
     * Should record as failure.
     *
     * @param statusCode the status code
     * @return true, if successful
     */
    @Override
    public boolean shouldRecordAsFailure(HttpStatusCode statusCode) {
        return statusCode.is5xxServerError();
    }

    private static String sanitize(String body) {
        if (body == null || body.isEmpty()) {
            return "(empty)";
        }
        return body.length() > MAX_LOGGED_BODY_LENGTH
                ? body.substring(0, MAX_LOGGED_BODY_LENGTH) + "...[truncated]"
                : body;
    }
}
