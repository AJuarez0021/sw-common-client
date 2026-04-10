package com.work.common.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import com.work.common.autoconfigure.RestHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Class RestHttpClientInvocationHandler.
 */
@Slf4j
public class RestHttpClientInvocationHandler implements InvocationHandler {

    /** The mappingHandlerMap. */
    private final Map<Class<? extends Annotation>, MappingHandler> mappingHandlerMap =
            Map.of(
                    GetMapping.class, new MappingHandler(HttpMethod.GET, (metadata, method) -> {
                        GetMapping m = method.getAnnotation(GetMapping.class);
                        metadata.path = resolvePath(m.value(), m.path());
                        extractStaticParams(m.params(), metadata);
                        metadata.headers = extractHeaders(m.headers());
                        metadata.consumes = m.consumes();
                        metadata.produces = m.produces();
                    }),
                    PostMapping.class, new MappingHandler(HttpMethod.POST, (metadata, method) -> {
                        PostMapping m = method.getAnnotation(PostMapping.class);
                        metadata.path = resolvePath(m.value(), m.path());
                        extractStaticParams(m.params(), metadata);
                        metadata.headers = extractHeaders(m.headers());
                        metadata.consumes = m.consumes();
                        metadata.produces = m.produces();
                    }),
                    PutMapping.class, new MappingHandler(HttpMethod.PUT, (metadata, method) -> {
                        PutMapping m = method.getAnnotation(PutMapping.class);
                        metadata.path = resolvePath(m.value(), m.path());
                        extractStaticParams(m.params(), metadata);
                        metadata.headers = extractHeaders(m.headers());
                        metadata.consumes = m.consumes();
                        metadata.produces = m.produces();
                    }),
                    DeleteMapping.class, new MappingHandler(HttpMethod.DELETE, (metadata, method) -> {
                        DeleteMapping m = method.getAnnotation(DeleteMapping.class);
                        metadata.path = resolvePath(m.value(), m.path());
                        extractStaticParams(m.params(), metadata);
                        metadata.headers = extractHeaders(m.headers());
                        metadata.consumes = m.consumes();
                        metadata.produces = m.produces();
                    }),
                    PatchMapping.class, new MappingHandler(HttpMethod.PATCH, (metadata, method) -> {
                        PatchMapping m = method.getAnnotation(PatchMapping.class);
                        metadata.path = resolvePath(m.value(), m.path());
                        extractStaticParams(m.params(), metadata);
                        metadata.headers = extractHeaders(m.headers());
                        metadata.consumes = m.consumes();
                        metadata.produces = m.produces();
                    }),
                    RequestMapping.class, new MappingHandler(null, (metadata, method) -> {
                        RequestMapping m = method.getAnnotation(RequestMapping.class);
                        metadata.path = resolvePath(m.value(), m.path());
                        metadata.httpMethod = resolveRequestMethod(m.method());
                        extractStaticParams(m.params(), metadata);
                        metadata.headers = extractHeaders(m.headers());
                        metadata.consumes = m.consumes();
                        metadata.produces = m.produces();
                    })
            );

    /** The http client. */
    private final Object httpClient;
    
    /** The client config. */
    private final RestHttpClient clientConfig;
    
    /** The error handler. */
    private final ErrorHandler errorHandler;
    
    /** The circuit breaker. */
    private final CircuitBreaker circuitBreaker;
    
    /** The retry. */
    private final Retry retry;
    
    /** The rate limiter. */
    private final RateLimiter rateLimiter;
    
    /** The time limiter. */
    private final TimeLimiter timeLimiter;

    /**
     * Instantiates a new rest http client invocation handler.
     *
     * @param httpClient the http client
     * @param clientConfig the client config
     * @param errorHandler the error handler
     * @param clientInterface the client interface
     * @param resilience4jConfig the resilience 4 j config
     */
    public RestHttpClientInvocationHandler(Object httpClient,
                                           RestHttpClient clientConfig,
                                           ErrorHandler errorHandler,
                                           Class<?> clientInterface,
                                           Resilience4jConfiguration resilience4jConfig) {
        this.httpClient = httpClient;
        this.clientConfig = clientConfig;
        this.errorHandler = errorHandler;

        String clientName = clientConfig.name().isEmpty() 
        		? clientInterface.getSimpleName() 
        		: clientConfig.name();
                

        this.retry = resilience4jConfig.createRetry(clientName, clientConfig, errorHandler);


        this.circuitBreaker = clientConfig.circuitBreaker().enabled()
                ? resilience4jConfig.createCircuitBreaker(clientName, clientConfig, errorHandler)
                : null;


        this.rateLimiter = clientConfig.rateLimiter().enabled()
                ? resilience4jConfig.createRateLimiter(clientName, clientConfig)
                : null;


        this.timeLimiter = clientConfig.timeLimiter().enabled()
                ? resilience4jConfig.createTimeLimiter(clientName, clientConfig)
                : null;
    }

    /**
     * Invoke.
     * Always returns a {@link Mono} — the library is fully reactive end-to-end.
     * Interface methods must declare {@code Mono<T>} as their return type.
     *
     * @param proxy the proxy
     * @param method the method
     * @param args the args
     * @return a {@link Mono} that emits the response body or signals an error
     * @throws Throwable the throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        RequestMetadata metadata = extractMetadata(method, args);
        return buildReactivePipeline(metadata, method);
    }

    /**
     * Builds the reactive pipeline: applies Resilience operators
     * (Retry → CircuitBreaker → RateLimiter → TimeLimiter) around the base request Mono.
     *
     * @param metadata the request metadata
     * @param method the invoked method
     * @return a Mono that emits the response body or signals an error
     */
    private Mono<Object> buildReactivePipeline(RequestMetadata metadata, Method method) {
        Mono<Object> mono = buildRequestMono(metadata, method)
                .transformDeferred(RetryOperator.of(retry));

        if (circuitBreaker != null) {
            mono = mono.transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
        }
        if (rateLimiter != null) {
            mono = mono.transformDeferred(RateLimiterOperator.of(rateLimiter));
        }
        if (timeLimiter != null) {
            mono = mono.transformDeferred(TimeLimiterOperator.of(timeLimiter));
        }

        // Ensure any surviving WebClientResponseException is wrapped as ClientException
        return mono.onErrorMap(WebClientResponseException.class, ClientException::new);
    }

    /**
     * Builds the base request Mono, handling {@link WebClientResponseException} reactively:
     * errors that should be recorded as circuit-breaker failures are propagated as-is so the
     * {@link CircuitBreakerOperator} can account for them; all others go through
     * {@link ErrorHandler#handleError} and are wrapped as {@link ClientException}.
     *
     * @param metadata the request metadata
     * @param method the invoked method
     * @return a cold Mono that executes the HTTP call on each subscription
     */
    private Mono<Object> buildRequestMono(RequestMetadata metadata, Method method) {
        if (!(httpClient instanceof WebClient)) {
            return Mono.error(new ClientException("Unsupported HTTP client type: "
                    + httpClient.getClass().getName()));
        }
        return Mono.defer(() -> executeWithWebClient(metadata, method))
                .onErrorResume(WebClientResponseException.class, e -> {
                    HttpStatusCode status = HttpStatusCode.valueOf(e.getStatusCode().value());
                    // Propagate WebClientResponseException unchanged so that:
                    // - RetryOperator can act on it when shouldRetry is true
                    // - CircuitBreakerOperator can record it as a failure when shouldRecordAsFailure is true
                    if (errorHandler.shouldRetry(status)
                            || (circuitBreaker != null && errorHandler.shouldRecordAsFailure(status))) {
                        return Mono.error(e);
                    }
                    try {
                        errorHandler.handleError(status, e.getResponseBodyAsString(), e);
                    } catch (Exception handlerEx) {
                        return Mono.error(new ClientException(handlerEx));
                    }
                    return Mono.error(new ClientException(e));
                });
    }

    /**
     * Executes the HTTP request using WebClient and returns a reactive {@link Mono}.
     * No blocking occurs here; the caller decides whether to subscribe reactively or block.
     *
     * @param metadata the request metadata
     * @param method the invoked method
     * @return a Mono that emits the deserialized response body
     */
    private Mono<Object> executeWithWebClient(RequestMetadata metadata, Method method) {
        if (metadata.httpMethod == null) {
            throw new ClientException("No HTTP method resolved for '" + method.getName()
                    + "'. Ensure the method is annotated with a mapping annotation"
                    + " (e.g. @GetMapping or @RequestMapping(method = RequestMethod.GET)).");
        }

        WebClient client = (WebClient) httpClient;
        String url = buildUrl(metadata);

        WebClient.RequestBodySpec requestSpec = client.method(
                        convertToSpringHttpMethod(metadata.httpMethod))
                .uri(url);

        metadata.headers.forEach(requestSpec::header);

        if (metadata.consumes.length > 0) {
            requestSpec.header(org.springframework.http.HttpHeaders.CONTENT_TYPE, metadata.consumes[0]);
        }
        if (metadata.produces.length > 0) {
            requestSpec.header(org.springframework.http.HttpHeaders.ACCEPT, metadata.produces);
        }

        WebClient.RequestHeadersSpec<?> headersSpec;
        if (metadata.body != null) {
            headersSpec = requestSpec.bodyValue(metadata.body);
        } else {
            headersSpec = requestSpec;
        }

        Class<Object> bodyType = resolveBodyType(method);

        // Void return type (Mono<Void>) or 204 No Content: consume the response without a body.
        if (Void.class.equals(bodyType)) {
            @SuppressWarnings("unchecked")
            Mono<Object> voidMono = (Mono<Object>) (Mono<?>) headersSpec
                    .retrieve()
                    .toBodilessEntity()
                    .then();
            return voidMono;
        }

        return headersSpec
                .retrieve()
                .toEntity(bodyType)
                .flatMap(response -> {
                    Object body = response.getBody();
                    if (body == null) {
                        return Mono.error(new ClientException("WebClient returned null response"));
                    }
                    return Mono.just(body);
                })
                .switchIfEmpty(Mono.error(new ClientException("WebClient returned null response")));
    }

    /**
     * Resolves the deserialization type from {@code Mono<T>}.
     * Handles simple types ({@code Mono<String>}), raw types ({@code Mono}),
     * and complex generics ({@code Mono<List<Foo>>} → {@code List.class}).
     *
     * @param method the invoked method
     * @return the class to use when deserializing the response body
     */
    @SuppressWarnings("unchecked")
    private Class<Object> resolveBodyType(Method method) {
        java.lang.reflect.Type generic = method.getGenericReturnType();
        if (generic instanceof ParameterizedType pt) {
            java.lang.reflect.Type typeArg = pt.getActualTypeArguments()[0];
            if (typeArg instanceof Class<?> cls) {
                return (Class<Object>) cls;
            }
            if (typeArg instanceof ParameterizedType argPt) {
                return (Class<Object>) argPt.getRawType();
            }
        }
        return Object.class;
    }

    /**
     * Builds the url with proper URL encoding.
     *
     * @param metadata the metadata
     * @return the encoded URL string
     */
    private String buildUrl(RequestMetadata metadata) {
        String url = clientConfig.url() + metadata.path;

        for (Map.Entry<String, Object> entry : metadata.pathVariables.entrySet()) {
            String encodedValue = urlEncodePathSegment(String.valueOf(entry.getValue()));
            url = url.replace("{" + entry.getKey() + "}", encodedValue);
        }

        if (!metadata.queryParams.isEmpty()) {
            StringBuilder queryString = new StringBuilder("?");
            metadata.queryParams.forEach((key, value) -> {
                String encodedKey = urlEncode(key);
                String encodedValue = urlEncode(String.valueOf(value));
                queryString.append(encodedKey).append("=").append(encodedValue).append("&");
            });
            url += queryString.substring(0, queryString.length() - 1);
        }

        return url;
    }

    /**
     * URL-encodes a query parameter value using UTF-8 (application/x-www-form-urlencoded).
     * Spaces are encoded as {@code +}, which is the correct encoding for query string values.
     *
     * @param value the value to encode
     * @return the URL-encoded string
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to URL encode value: {}", value, e);
            return value;
        }
    }

    /**
     * URL-encodes a path segment value using UTF-8.
     * Spaces are encoded as {@code %20} (not {@code +}), because in the path portion of a URL
     * a literal {@code +} sign is not interpreted as a space — only {@code %20} is.
     *
     * @param value the value to encode
     * @return the path-segment-encoded string
     */
    private String urlEncodePathSegment(String value) {
        return urlEncode(value).replace("+", "%20");
    }

    record MappingHandler(
            HttpMethod httpMethod,
            BiConsumer<RequestMetadata, Method> extractor
    ) {}


    private boolean shouldOmit(Object effectiveArg, RequestParam rp) {
        return effectiveArg == null && !rp.required();
    }

    private boolean shouldOmit(Object effectiveArg, RequestHeader rh) {
        return effectiveArg == null && !rh.required();
    }
    /**
     * Extract metadata.
     *
     * @param method the method
     * @param args the args
     * @return the request metadata
     */
    private RequestMetadata extractMetadata(Method method, Object[] args) {
        RequestMetadata metadata = new RequestMetadata();

        for (var entry : mappingHandlerMap.entrySet()) {
            if (method.isAnnotationPresent(entry.getKey())) {
                MappingHandler handler = entry.getValue();
                metadata.httpMethod = handler.httpMethod();
                handler.extractor().accept(metadata, method);
                break;
            }
        }

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Object arg = args[i];

            if (param.isAnnotationPresent(PathVariable.class)) {
                PathVariable pv = param.getAnnotation(PathVariable.class);
                String name = resolveParamName(pv.value(), pv.name(), param.getName());
                metadata.pathVariables.put(name, arg);

            } else if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = param.getAnnotation(RequestParam.class);
                String name = resolveParamName(rp.value(), rp.name(), param.getName());
                Object effectiveArg = resolveDefaultValue(arg, rp.defaultValue());
                if (!shouldOmit(effectiveArg, rp)) {
                    metadata.queryParams.put(name, effectiveArg);
                }

            } else if (param.isAnnotationPresent(RequestBody.class)) {
                metadata.body = arg;

            } else if (param.isAnnotationPresent(RequestHeader.class)) {
                RequestHeader rh = param.getAnnotation(RequestHeader.class);
                String name = resolveParamName(rh.value(), rh.name(), param.getName());
                Object effectiveArg = resolveDefaultValue(arg, rh.defaultValue());
                if (!shouldOmit(effectiveArg, rh)) {
                    metadata.headers.put(name, String.valueOf(effectiveArg));
                }
            }
        }

        return metadata;
    }

    /**
     * Resolves the path from a mapping annotation's value() and path() attributes.
     * Spring's @AliasFor aliases between value/path are only resolved inside the Spring
     * application context. Via plain Java reflection both attributes are independent, so
     * we must check both and use whichever is non-empty.
     *
     * @param value the value() attribute of the mapping annotation
     * @param path  the path() attribute of the mapping annotation
     * @return the first non-empty path found, or an empty string
     */
    private String resolvePath(String[] value, String[] path) {
        if (value.length > 0 && !value[0].isEmpty()) return value[0];
        if (path.length > 0 && !path[0].isEmpty()) return path[0];
        return "";
    }

    /**
     * Extract headers.
     *
     * @param headerArray the header array
     * @return the map
     */
    private Map<String, String> extractHeaders(String[] headerArray) {
        Map<String, String> headers = new HashMap<>();
        for (String header : headerArray) {
            String[] parts = header.split(":", 2);
            if (parts.length == 2) {
                headers.put(parts[0].trim(), parts[1].trim());
            } else {
                log.warn("Invalid header format (missing colon): {}", header);
            }
        }
        return headers;
    }

    /**
     * Convert to spring http method.
     *
     * @param method the method
     * @return the org.springframework.http. http method
     */
    private HttpMethod convertToSpringHttpMethod(HttpMethod method) {
        return HttpMethod.valueOf(method.name());
    }

    /**
     * Resolves the HTTP method from a {@code @RequestMapping} method array.
     * Throws {@link ClientException} when no method is declared, since a client
     * invocation cannot proceed without knowing which HTTP verb to use.
     *
     * @param methods the RequestMethod array from the annotation
     * @return the resolved HttpMethod
     */
    private HttpMethod resolveRequestMethod(RequestMethod[] methods) {
        if (methods == null || methods.length == 0) {
            throw new ClientException(
                    "@RequestMapping must declare at least one HTTP method (e.g. method = RequestMethod.GET)");
        }
        if (methods.length > 1) {
            log.warn("@RequestMapping declares multiple HTTP methods {}; using first: {}",
                    methods, methods[0]);
        }
        return HttpMethod.valueOf(methods[0].name());
    }

    /**
     * Resolves the effective parameter name by checking, in order:
     * the {@code value} alias, the explicit {@code name} attribute, and finally
     * the Java parameter name (requires {@code -parameters} compiler flag).
     *
     * @param value     the {@code value()} of the annotation
     * @param name      the {@code name()} of the annotation
     * @param paramName the Java reflection parameter name
     * @return the resolved name
     */
    private String resolveParamName(String value, String name, String paramName) {
        if (!value.isEmpty()) return value;
        if (!name.isEmpty()) return name;
        return paramName;
    }

    /**
     * Returns the effective argument, substituting the annotation's
     * {@code defaultValue} when the argument itself is {@code null}.
     *
     * @param arg          the runtime argument
     * @param defaultValue the annotation default value (maybe {@link ValueConstants#DEFAULT_NONE})
     * @return the effective value, or {@code null} if no default is defined
     */
    private Object resolveDefaultValue(Object arg, String defaultValue) {
        if (arg != null) return arg;
        if (!ValueConstants.DEFAULT_NONE.equals(defaultValue)) return defaultValue;
        return null;
    }

    /**
     * Parses {@code key=value} entries from a mapping annotation's {@code params()}
     * attribute and adds them as static query parameters.
     * Entries with negation ({@code !key}) or without an equals sign are ignored
     * because they express server-side constraints that have no client-side meaning.
     *
     * @param params   the params array from the annotation
     * @param metadata the request metadata to populate
     */
    private void extractStaticParams(String[] params, RequestMetadata metadata) {
        for (String param : params) {
            int eqIdx = param.indexOf('=');
            if (eqIdx > 0 && !param.startsWith("!")) {
                String key = param.substring(0, eqIdx).trim();
                String value = param.substring(eqIdx + 1).trim();
                metadata.queryParams.put(key, value);
            }
        }
    }

    /**
     * The Class RequestMetadata.
     */
    private static class RequestMetadata {
        
        /** The path. */
        private String path;
        
        /** The http method. */
        private HttpMethod httpMethod;
        
        /** The path variables. */
        private final Map<String, Object> pathVariables = new HashMap<>();
        
        /** The query params. */
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /** The headers. */
        private Map<String, String> headers = new HashMap<>();

        /** The body. */
        private Object body;

        /** The consumes (Content-Type media types accepted by the endpoint). */
        private String[] consumes = new String[0];

        /** The produces (Accept media types produced by the endpoint). */
        private String[] produces = new String[0];
    }
}
