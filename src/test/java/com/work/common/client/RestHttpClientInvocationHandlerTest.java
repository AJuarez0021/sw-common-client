package com.work.common.client;

import com.work.common.autoconfigure.CircuitBreakerConfig;
import com.work.common.autoconfigure.RateLimiterConfig;
import com.work.common.autoconfigure.RestHttpClient;
import com.work.common.autoconfigure.TimeLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RestHttpClientInvocationHandlerTest {

    @Mock WebClient webClient;
    @Mock WebClient.RequestBodyUriSpec uriSpec;
    @Mock WebClient.RequestBodySpec requestSpec;
    @Mock WebClient.ResponseSpec responseSpec;

    // ── Test interfaces ───────────────────────────────────────────────────────

    /** @RequestMapping support */
    @RestHttpClient(url = "http://api.example.com", name = "request-mapping", readTimeout = 5000)
    interface RequestMappingClient {
        @RequestMapping(value = "/items/{id}", method = RequestMethod.GET)
        Mono<String> getItem(@PathVariable("id") Long id);

        @RequestMapping(value = "/items", method = RequestMethod.POST,
                consumes = "application/json", produces = "application/json")
        Mono<String> createItem(@RequestBody String body);

        @RequestMapping(value = "/items", method = RequestMethod.GET,
                params = {"format=json", "version=2"})
        Mono<String> listItemsWithParams();

        @RequestMapping(value = "/search", method = RequestMethod.GET,
                produces = {"application/json", "application/xml"})
        Mono<String> searchMultipleProduces();
    }

    /** @RequestParam.required=false and defaultValue */
    @RestHttpClient(url = "http://api.example.com", name = "optional-params", readTimeout = 5000)
    interface OptionalParamClient {
        @GetMapping("/items")
        Mono<String> listItems(
                @RequestParam("page") int page,
                @RequestParam(value = "size", required = false) Integer size,
                @RequestParam(value = "sort", required = false, defaultValue = "id") String sort
        );
    }

    /** @RequestHeader.required=false and defaultValue */
    @RestHttpClient(url = "http://api.example.com", name = "optional-headers", readTimeout = 5000)
    interface OptionalHeaderClient {
        @GetMapping("/items")
        Mono<String> listItems(
                @RequestHeader("X-Required") String required,
                @RequestHeader(value = "X-Optional", required = false) String optional,
                @RequestHeader(value = "X-Default", required = false, defaultValue = "default-val") String withDefault
        );
    }

    /** params attribute on @GetMapping */
    @RestHttpClient(url = "http://api.example.com", name = "static-params", readTimeout = 5000)
    interface StaticParamsClient {
        @GetMapping(value = "/items", params = {"format=json", "version=1"})
        Mono<String> list();

        /** negation and bare keys should be ignored */
        @GetMapping(value = "/items", params = {"!debug", "required", "key=value"})
        Mono<String> listFiltered();
    }

    /** Multiple produces values */
    @RestHttpClient(url = "http://api.example.com", name = "multi-produces", readTimeout = 5000)
    interface MultiProducesClient {
        @GetMapping(value = "/items", produces = {"application/json", "application/xml"})
        Mono<String> list();
    }

    @RestHttpClient(url = "http://api.example.com", name = "basic", readTimeout = 5000)
    interface BasicClient {
        @GetMapping("/items/{id}")
        Mono<String> getItem(@PathVariable("id") Long id);

        @PostMapping(value = "/items", consumes = "application/json", produces = "application/json")
        Mono<String> createItem(@RequestBody String body);

        @PutMapping("/items/{id}")
        Mono<String> updateItem(@PathVariable("id") Long id, @RequestBody String body);

        @DeleteMapping("/items/{id}")
        Mono<String> deleteItem(@PathVariable("id") Long id);

        @PatchMapping("/items/{id}")
        Mono<String> patchItem(@PathVariable("id") Long id, @RequestBody String body);

        @GetMapping("/items")
        Mono<String> listItems(@RequestParam("page") int page, @RequestHeader("X-Tenant") String tenant);

        @GetMapping("/items/{id}/sub/{subId}")
        Mono<String> getSubItem(@PathVariable("id") Long id, @PathVariable("subId") String subId);
    }

    @RestHttpClient(
            url = "http://api.example.com", name = "resilient", readTimeout = 5000,
            circuitBreaker = @CircuitBreakerConfig(enabled = true),
            rateLimiter  = @RateLimiterConfig(enabled = true),
            timeLimiter  = @TimeLimiterConfig(enabled = true, timeout = 5000)
    )
    interface ResilientClient {
        @GetMapping("/ping")
        Mono<String> ping();
    }

    // ── Branch-coverage helpers ───────────────────────────────────────────────

    /** Empty name → handler uses clientInterface.getSimpleName() */
    @RestHttpClient(url = "http://api.example.com", name = "", readTimeout = 5000)
    interface NoNameClient {
        @GetMapping("/ping")
        Mono<String> ping();
    }

    /** Exercises resolvePath path[] branch and extractHeaders loop */
    @RestHttpClient(url = "http://api.example.com", name = "extra", readTimeout = 5000)
    interface ExtraClient {
        /** path attribute instead of value → exercises resolvePath path[] branch */
        @GetMapping(path = "/status")
        Mono<String> getStatus();

        /** headers defined in mapping → exercises extractHeaders loop body */
        @GetMapping(value = "/data", headers = {"X-Auth: token123"})
        Mono<String> getDataWithHeader();

        /** annotation headers with invalid format (no colon) → exercises warn branch */
        @GetMapping(value = "/raw", headers = {"BadHeader"})
        Mono<String> getRaw();

        /** unannotated parameter → exercises the trailing else */
        @GetMapping("/items")
        Mono<String> getItems(String unannotated);

        /** @GetMapping with no path or value → exercises resolvePath empty-return */
        @GetMapping
        Mono<String> noPath();
    }

    /** Circuit breaker enabled — used to test shouldRecordAsFailure branches */
    @RestHttpClient(
            url = "http://api.example.com", name = "cb-only", readTimeout = 5000,
            circuitBreaker = @CircuitBreakerConfig(enabled = true)
    )
    interface CbOnlyClient {
        @GetMapping("/ping")
        Mono<String> ping();
    }

    /** Flux<T> return type — streaming responses */
    @RestHttpClient(url = "http://api.example.com", name = "flux-client", readTimeout = 5000)
    interface FluxClient {
        @GetMapping("/items")
        Flux<String> listItems();

        @GetMapping("/items/{id}/events")
        Flux<String> getEvents(@PathVariable("id") Long id);
    }

    /** Resilient Flux client — exercises all resilience operators on a Flux pipeline */
    @RestHttpClient(
            url = "http://api.example.com", name = "resilient-flux", readTimeout = 5000,
            circuitBreaker = @CircuitBreakerConfig(enabled = true),
            rateLimiter  = @RateLimiterConfig(enabled = true),
            timeLimiter  = @TimeLimiterConfig(enabled = true, timeout = 5000)
    )
    interface ResilientFluxClient {
        @GetMapping("/stream")
        Flux<String> stream();
    }

    /** @RequestPart — multipart/form-data with plain objects */
    @RestHttpClient(url = "http://api.example.com", name = "multipart-plain", readTimeout = 5000)
    interface MultipartPlainClient {
        @PostMapping(value = "/upload", consumes = "multipart/form-data")
        Mono<String> upload(@RequestPart("field") String field, @RequestPart("data") Object data);
    }

    /** @RequestPart — multipart/form-data with a FilePart */
    @RestHttpClient(url = "http://api.example.com", name = "multipart-file", readTimeout = 5000)
    interface MultipartFileClient {
        @PostMapping(value = "/upload", consumes = "multipart/form-data")
        Mono<String> uploadFile(@RequestPart("file") FilePart filePart);
    }

    /** @RequestPart — multipart/form-data with a generic Part (not FilePart) */
    @RestHttpClient(url = "http://api.example.com", name = "multipart-part", readTimeout = 5000)
    interface MultipartPartClient {
        @PostMapping(value = "/upload", consumes = "multipart/form-data")
        Mono<String> uploadPart(@RequestPart("part") FormFieldPart part);
    }

    /** @RequestPart with null arg — null parts must be omitted */
    @RestHttpClient(url = "http://api.example.com", name = "multipart-null", readTimeout = 5000)
    interface MultipartNullClient {
        @PostMapping(value = "/upload", consumes = "multipart/form-data")
        Mono<String> upload(@RequestPart("required") String required,
                            @RequestPart("optional") String optional);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RestHttpClientInvocationHandler createHandler(Class<?> clientInterface) {
        RestHttpClient config = clientInterface.getAnnotation(RestHttpClient.class);
        return new RestHttpClientInvocationHandler(
                webClient, config, new DefaultErrorHandler(),
                clientInterface, new Resilience4jConfiguration()
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, RestHttpClientInvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(), new Class<?>[]{iface}, handler
        );
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUpWebClientChain() {
        when(webClient.method(any(HttpMethod.class))).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(requestSpec);
        doReturn(requestSpec).when(requestSpec).header(anyString(), any(String[].class));
        doReturn(requestSpec).when(requestSpec).bodyValue(any());
        doReturn(requestSpec).when(requestSpec).body(any(BodyInserter.class));
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(Class.class)))
                .thenReturn(Mono.just(ResponseEntity.ok("response")));
        when(responseSpec.bodyToFlux(any(Class.class)))
                .thenReturn(Flux.just("item1", "item2"));
    }

    // ── HTTP method mapping ───────────────────────────────────────────────────

    @Test
    void getMapping_usesGetMethod_andCorrectUrl() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.getItem(42L))
                .expectNext("response")
                .verifyComplete();

        verify(webClient).method(HttpMethod.GET);
        verify(uriSpec).uri("http://api.example.com/items/42");
    }

    @Test
    void postMapping_usesPostMethod_andSendsBody() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.createItem("payload"))
                .expectNext("response")
                .verifyComplete();

        verify(webClient).method(HttpMethod.POST);
        verify(requestSpec).bodyValue("payload");
    }

    @Test
    void putMapping_usesPutMethod_andSendsBody() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.updateItem(1L, "updated"))
                .expectNext("response")
                .verifyComplete();

        verify(webClient).method(HttpMethod.PUT);
        verify(uriSpec).uri("http://api.example.com/items/1");
        verify(requestSpec).bodyValue("updated");
    }

    @Test
    void deleteMapping_usesDeleteMethod() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.deleteItem(99L))
                .expectNext("response")
                .verifyComplete();

        verify(webClient).method(HttpMethod.DELETE);
        verify(uriSpec).uri("http://api.example.com/items/99");
    }

    @Test
    void patchMapping_usesPatchMethod() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.patchItem(5L, "delta"))
                .expectNext("response")
                .verifyComplete();

        verify(webClient).method(HttpMethod.PATCH);
        verify(uriSpec).uri("http://api.example.com/items/5");
    }

    // ── consumes / produces ───────────────────────────────────────────────────

    @Test
    void postMapping_consumesProduces_setsContentTypeAndAcceptHeaders() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.createItem("payload"))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec, atLeast(2)).header(nameCaptor.capture(), any(String[].class));
        assertThat(nameCaptor.getAllValues()).contains(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT);
    }

    // ── Parameters ────────────────────────────────────────────────────────────

    @Test
    void requestParam_appendedAsQueryString() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.listItems(3, "tenant-x"))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("page=3");
    }

    @Test
    void requestHeader_addedToRequest() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.listItems(1, "my-tenant"))
                .expectNextCount(1)
                .verifyComplete();

        verify(requestSpec).header(eq("X-Tenant"), any(String[].class));
    }

    @Test
    void multiplePathVariables_allReplacedInUrl() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.getSubItem(10L, "abc"))
                .expectNextCount(1)
                .verifyComplete();

        verify(uriSpec).uri("http://api.example.com/items/10/sub/abc");
    }

    @Test
    void pathVariable_valueUrlEncoded() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.getItem(7L))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo("http://api.example.com/items/7");
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    void nullResponseFromWebClient_emitsClientException() {
        when(responseSpec.toEntity(any(Class.class))).thenReturn(Mono.empty());
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.getItem(1L))
                .expectErrorSatisfies(e -> assertThat(e)
                        .isInstanceOf(ClientException.class)
                        .hasMessageContaining("null response"))
                .verify();
    }

    @Test
    void webClientResponseException_4xx_emitsClientException() {
        WebClientResponseException ex = WebClientResponseException.create(404, "Not Found", null, null, null);
        when(responseSpec.toEntity(any(Class.class))).thenReturn(Mono.error(ex));
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.getItem(1L))
                .expectError(ClientException.class)
                .verify();
    }

    @Test
    void webClientResponseException_5xx_emitsClientException() {
        WebClientResponseException ex = WebClientResponseException.create(503, "Service Unavailable", null, null, null);
        when(responseSpec.toEntity(any(Class.class))).thenReturn(Mono.error(ex));
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.getItem(1L))
                .expectError(ClientException.class)
                .verify();
    }

    @Test
    void unsupportedHttpClientType_emitsClientException() {
        RestHttpClient config = BasicClient.class.getAnnotation(RestHttpClient.class);
        RestHttpClientInvocationHandler handler = new RestHttpClientInvocationHandler(
                "not-a-webclient", config, new DefaultErrorHandler(),
                BasicClient.class, new Resilience4jConfiguration()
        );
        BasicClient proxy = createProxy(BasicClient.class, handler);

        StepVerifier.create(proxy.getItem(1L))
                .expectErrorSatisfies(e -> assertThat(e)
                        .isInstanceOf(ClientException.class)
                        .hasMessageContaining("Unsupported HTTP client type"))
                .verify();
    }

    // ── Object class methods ──────────────────────────────────────────────────

    @Test
    void toString_doesNotCallWebClient() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        assertThatNoException().isThrownBy(proxy::toString);
        verify(webClient, never()).method(any());
    }

    @Test
    void hashCode_doesNotCallWebClient() {
        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        assertThatNoException().isThrownBy(proxy::hashCode);
        verify(webClient, never()).method(any());
    }

    // ── Resilience4j integration ──────────────────────────────────────────────

    @Test
    void withCircuitBreakerRateLimiterAndTimeLimiter_executesSuccessfully() {
        ResilientClient proxy = createProxy(ResilientClient.class, createHandler(ResilientClient.class));

        StepVerifier.create(proxy.ping())
                .expectNext("response")
                .verifyComplete();

        verify(webClient).method(HttpMethod.GET);
    }

    @Test
    void retryOnTransientError_retriesAndEventuallySucceeds() {
        when(responseSpec.toEntity(any(Class.class)))
                .thenReturn(Mono.error(new RuntimeException("transient")))
                .thenReturn(Mono.just(ResponseEntity.ok("response")));

        BasicClient proxy = createProxy(BasicClient.class, createHandler(BasicClient.class));

        StepVerifier.create(proxy.getItem(1L))
                .expectNext("response")
                .verifyComplete();

        verify(responseSpec, times(2)).toEntity(any(Class.class));
    }

    // ── Branch-coverage tests ─────────────────────────────────────────────────

    @Test
    void emptyClientName_usesInterfaceSimpleNameFallback() {
        NoNameClient proxy = createProxy(NoNameClient.class, createHandler(NoNameClient.class));

        StepVerifier.create(proxy.ping())
                .expectNext("response")
                .verifyComplete();
    }

    @Test
    void getMappingWithPathAttribute_resolvesPathCorrectly() {
        ExtraClient proxy = createProxy(ExtraClient.class, createHandler(ExtraClient.class));

        StepVerifier.create(proxy.getStatus())
                .expectNextCount(1)
                .verifyComplete();

        verify(uriSpec).uri("http://api.example.com/status");
    }

    @Test
    void getMappingWithNoPathOrValue_producesBaseUrlOnly() {
        ExtraClient proxy = createProxy(ExtraClient.class, createHandler(ExtraClient.class));

        StepVerifier.create(proxy.noPath())
                .expectNextCount(1)
                .verifyComplete();

        verify(uriSpec).uri("http://api.example.com");
    }

    @Test
    void mappingWithValidHeaders_extractsAndForwardsHeader() {
        ExtraClient proxy = createProxy(ExtraClient.class, createHandler(ExtraClient.class));

        StepVerifier.create(proxy.getDataWithHeader())
                .expectNextCount(1)
                .verifyComplete();

        verify(requestSpec).header(eq("X-Auth"), any(String[].class));
    }

    @Test
    void mappingWithInvalidHeaderFormat_logsWarnAndContinues() {
        ExtraClient proxy = createProxy(ExtraClient.class, createHandler(ExtraClient.class));

        StepVerifier.create(proxy.getRaw())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void methodWithUnannotatedParam_paramIsIgnored() {
        ExtraClient proxy = createProxy(ExtraClient.class, createHandler(ExtraClient.class));

        StepVerifier.create(proxy.getItems("ignored"))
                .expectNext("response")
                .verifyComplete();
    }

    @Test
    void webClientResponseException_5xx_withCircuitBreaker_recordsFailureAndThrows() {
        WebClientResponseException ex = WebClientResponseException.create(
                500, "Internal Server Error", null, null, null);
        when(responseSpec.toEntity(any(Class.class))).thenReturn(Mono.error(ex));

        CbOnlyClient proxy = createProxy(CbOnlyClient.class, createHandler(CbOnlyClient.class));

        StepVerifier.create(proxy.ping())
                .expectError(ClientException.class)
                .verify();
    }

    @Test
    void webClientResponseException_4xx_withCircuitBreaker_handlesErrorAndThrows() {
        WebClientResponseException ex = WebClientResponseException.create(
                400, "Bad Request", null, null, null);
        when(responseSpec.toEntity(any(Class.class))).thenReturn(Mono.error(ex));

        CbOnlyClient proxy = createProxy(CbOnlyClient.class, createHandler(CbOnlyClient.class));

        StepVerifier.create(proxy.ping())
                .expectError(ClientException.class)
                .verify();
    }

    // ── Flux<T> return type ───────────────────────────────────────────────────

    @Test
    void fluxReturnType_usesBodyToFlux_andEmitsElements() {
        FluxClient proxy = createProxy(FluxClient.class, createHandler(FluxClient.class));

        StepVerifier.create(proxy.listItems())
                .expectNext("item1", "item2")
                .verifyComplete();

        verify(webClient).method(HttpMethod.GET);
        verify(responseSpec).bodyToFlux(String.class);
        verify(responseSpec, never()).toEntity(any(Class.class));
    }

    @Test
    void fluxReturnType_withPathVariable_buildsCorrectUrl() {
        FluxClient proxy = createProxy(FluxClient.class, createHandler(FluxClient.class));

        StepVerifier.create(proxy.getEvents(7L))
                .expectNext("item1", "item2")
                .verifyComplete();

        verify(uriSpec).uri("http://api.example.com/items/7/events");
        verify(responseSpec).bodyToFlux(String.class);
    }

    @Test
    void fluxReturnType_withAllResilienceOperators_executesSuccessfully() {
        ResilientFluxClient proxy = createProxy(
                ResilientFluxClient.class, createHandler(ResilientFluxClient.class));

        StepVerifier.create(proxy.stream())
                .expectNext("item1", "item2")
                .verifyComplete();

        verify(webClient).method(HttpMethod.GET);
    }

    @Test
    void fluxReturnType_unsupportedClient_emitsClientException() {
        RestHttpClient config = FluxClient.class.getAnnotation(RestHttpClient.class);
        RestHttpClientInvocationHandler handler = new RestHttpClientInvocationHandler(
                "not-a-webclient", config, new DefaultErrorHandler(),
                FluxClient.class, new Resilience4jConfiguration()
        );
        FluxClient proxy = createProxy(FluxClient.class, handler);

        StepVerifier.create(proxy.listItems())
                .expectErrorSatisfies(e -> assertThat(e)
                        .isInstanceOf(ClientException.class)
                        .hasMessageContaining("Unsupported HTTP client type"))
                .verify();
    }

    @Test
    void fluxReturnType_webClientResponseException_emitsClientException() {
        WebClientResponseException ex = WebClientResponseException.create(
                404, "Not Found", null, null, null);
        when(responseSpec.bodyToFlux(any(Class.class))).thenReturn(Flux.error(ex));
        FluxClient proxy = createProxy(FluxClient.class, createHandler(FluxClient.class));

        StepVerifier.create(proxy.listItems())
                .expectError(ClientException.class)
                .verify();
    }

    @Test
    void fluxReturnType_5xx_withCircuitBreaker_emitsClientException() {
        WebClientResponseException ex = WebClientResponseException.create(
                500, "Internal Server Error", null, null, null);
        when(responseSpec.bodyToFlux(any(Class.class))).thenReturn(Flux.error(ex));

        @RestHttpClient(url = "http://api.example.com", name = "flux-cb", readTimeout = 5000,
                circuitBreaker = @CircuitBreakerConfig(enabled = true))
        interface FluxCbClient {
            @GetMapping("/stream")
            Flux<String> stream();
        }
        FluxCbClient proxy = createProxy(FluxCbClient.class, createHandler(FluxCbClient.class));

        StepVerifier.create(proxy.stream())
                .expectError(ClientException.class)
                .verify();
    }

    // ── @RequestPart / multipart support ─────────────────────────────────────

    @Test
    void requestPart_plainObjects_buildsMultipartBody() {
        MultipartPlainClient proxy = createProxy(
                MultipartPlainClient.class, createHandler(MultipartPlainClient.class));

        StepVerifier.create(proxy.upload("hello", "world"))
                .expectNext("response")
                .verifyComplete();

        verify(requestSpec).body(any(BodyInserter.class));
        verify(requestSpec, never()).bodyValue(any());
    }

    @Test
    void requestPart_nullArg_isOmittedFromMultipart() {
        MultipartNullClient proxy = createProxy(
                MultipartNullClient.class, createHandler(MultipartNullClient.class));

        StepVerifier.create(proxy.upload("req-value", null))
                .expectNext("response")
                .verifyComplete();

        verify(requestSpec).body(any(BodyInserter.class));
    }

    @Test
    void requestPart_filePart_buildsAsyncMultipartEntry() {
        FilePart filePart = mock(FilePart.class);
        when(filePart.content()).thenReturn(Flux.empty());
        when(filePart.headers()).thenReturn(new HttpHeaders());
        when(filePart.filename()).thenReturn("document.pdf");

        MultipartFileClient proxy = createProxy(
                MultipartFileClient.class, createHandler(MultipartFileClient.class));

        StepVerifier.create(proxy.uploadFile(filePart))
                .expectNext("response")
                .verifyComplete();

        verify(requestSpec).body(any(BodyInserter.class));
        verify(requestSpec, never()).bodyValue(any());
    }

    @Test
    void requestPart_genericPart_buildsAsyncMultipartEntry() {
        FormFieldPart part = mock(FormFieldPart.class);
        when(part.content()).thenReturn(Flux.empty());
        when(part.headers()).thenReturn(new HttpHeaders());

        MultipartPartClient proxy = createProxy(
                MultipartPartClient.class, createHandler(MultipartPartClient.class));

        StepVerifier.create(proxy.uploadPart(part))
                .expectNext("response")
                .verifyComplete();

        verify(requestSpec).body(any(BodyInserter.class));
    }

    // ── baseUrl trailing slash normalization ──────────────────────────────────

    @Test
    void baseUrl_withTrailingSlash_doesNotProduceDoubleSlashInFinalUrl() {
        @RestHttpClient(url = "http://api.example.com/", name = "trailing-slash", readTimeout = 5000)
        interface TrailingSlashClient {
            @GetMapping("/items/{id}")
            Mono<String> getItem(@PathVariable("id") Long id);
        }
        TrailingSlashClient proxy = createProxy(TrailingSlashClient.class, createHandler(TrailingSlashClient.class));

        StepVerifier.create(proxy.getItem(1L))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue())
                .doesNotContain("//items")
                .isEqualTo("http://api.example.com/items/1");
    }

    // ── @RequestMapping support ───────────────────────────────────────────────

    @Test
    void requestMapping_GET_resolvesCorrectly() {
        RequestMappingClient proxy = createProxy(
                RequestMappingClient.class, createHandler(RequestMappingClient.class));

        StepVerifier.create(proxy.getItem(42L))
                .expectNextCount(1)
                .verifyComplete();

        verify(webClient).method(HttpMethod.GET);
        verify(uriSpec).uri("http://api.example.com/items/42");
    }

    @Test
    void requestMapping_POST_withBody_resolvesCorrectly() {
        RequestMappingClient proxy = createProxy(
                RequestMappingClient.class, createHandler(RequestMappingClient.class));

        StepVerifier.create(proxy.createItem("payload"))
                .expectNextCount(1)
                .verifyComplete();

        verify(webClient).method(HttpMethod.POST);
        verify(requestSpec).bodyValue("payload");
    }

    @Test
    void requestMapping_params_addedAsQueryParameters() {
        RequestMappingClient proxy = createProxy(
                RequestMappingClient.class, createHandler(RequestMappingClient.class));

        StepVerifier.create(proxy.listItemsWithParams())
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue())
                .contains("format=json")
                .contains("version=2");
    }

    @Test
    void requestMapping_multipleProduces_allSentInAcceptHeader() {
        RequestMappingClient proxy = createProxy(
                RequestMappingClient.class, createHandler(RequestMappingClient.class));

        StepVerifier.create(proxy.searchMultipleProduces())
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String[]> valuesCaptor = ArgumentCaptor.forClass(String[].class);
        verify(requestSpec).header(eq(HttpHeaders.ACCEPT), valuesCaptor.capture());
        assertThat(valuesCaptor.getValue())
                .contains("application/json", "application/xml");
    }

    @Test
    void requestMapping_noMethod_emitsClientException() {
        @RestHttpClient(url = "http://api.example.com", name = "no-method", readTimeout = 5000)
        interface NoMethodClient {
            @RequestMapping("/ping")
            Mono<String> ping();
        }

        NoMethodClient proxy = createProxy(NoMethodClient.class, createHandler(NoMethodClient.class));

        StepVerifier.create(proxy.ping())
                .expectErrorSatisfies(e -> assertThat(e)
                        .isInstanceOf(ClientException.class)
                        .hasMessageContaining("HTTP method"))
                .verify();
    }

    // ── params attribute ──────────────────────────────────────────────────────

    @Test
    void getMapping_params_addedAsQueryParameters() {
        StaticParamsClient proxy = createProxy(StaticParamsClient.class, createHandler(StaticParamsClient.class));

        StepVerifier.create(proxy.list())
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue())
                .contains("format=json")
                .contains("version=1");
    }

    @Test
    void getMapping_params_negationAndBareKeysIgnored() {
        StaticParamsClient proxy = createProxy(StaticParamsClient.class, createHandler(StaticParamsClient.class));

        StepVerifier.create(proxy.listFiltered())
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        String url = urlCaptor.getValue();
        assertThat(url).doesNotContain("debug");
        assertThat(url).contains("key=value");
    }

    // ── Multiple produces → all values in Accept ─────────────────────────────

    @Test
    void multipleProduces_allSentInAcceptHeader() {
        MultiProducesClient proxy = createProxy(MultiProducesClient.class, createHandler(MultiProducesClient.class));

        StepVerifier.create(proxy.list())
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String[]> valuesCaptor = ArgumentCaptor.forClass(String[].class);
        verify(requestSpec).header(eq(HttpHeaders.ACCEPT), valuesCaptor.capture());
        assertThat(valuesCaptor.getValue())
                .contains("application/json", "application/xml");
    }

    // ── @RequestParam: required=false + defaultValue ──────────────────────────

    @Test
    void requestParam_required_presentInQueryString() {
        OptionalParamClient proxy = createProxy(OptionalParamClient.class, createHandler(OptionalParamClient.class));

        StepVerifier.create(proxy.listItems(1, null, null))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("page=1");
    }

    @Test
    void requestParam_optionalNullValue_omittedFromQueryString() {
        OptionalParamClient proxy = createProxy(OptionalParamClient.class, createHandler(OptionalParamClient.class));

        StepVerifier.create(proxy.listItems(1, null, null))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).doesNotContain("size=");
    }

    @Test
    void requestParam_defaultValue_usedWhenArgIsNull() {
        OptionalParamClient proxy = createProxy(OptionalParamClient.class, createHandler(OptionalParamClient.class));

        StepVerifier.create(proxy.listItems(1, null, null))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(uriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("sort=id");
    }

    // ── @RequestHeader: required=false + defaultValue ─────────────────────────

    @Test
    void requestHeader_optionalNullValue_omittedFromRequest() {
        OptionalHeaderClient proxy = createProxy(OptionalHeaderClient.class, createHandler(OptionalHeaderClient.class));

        StepVerifier.create(proxy.listItems("req-value", null, null))
                .expectNextCount(1)
                .verifyComplete();

        verify(requestSpec).header(eq("X-Required"), any(String[].class));
        verify(requestSpec, never()).header(eq("X-Optional"), any(String[].class));
    }

    @Test
    void requestHeader_defaultValue_usedWhenArgIsNull() {
        OptionalHeaderClient proxy = createProxy(OptionalHeaderClient.class, createHandler(OptionalHeaderClient.class));

        StepVerifier.create(proxy.listItems("req-value", null, null))
                .expectNextCount(1)
                .verifyComplete();

        verify(requestSpec).header(eq("X-Default"), any(String[].class));
    }
}
