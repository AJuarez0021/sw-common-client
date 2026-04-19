package com.demo.webflux.service;

import com.demo.webflux.client.EchoClient;
import com.demo.webflux.model.Dto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EchoClientService {

    private final EchoClient echoClient;

    public Mono<Dto.EchoResponse> echoGet(String q, String customHeader) {
        log.debug("echoGet q={}", q);
        return echoClient.echoGet(q, customHeader)
                .doOnError(e -> log.error("Error on echoGet", e));
    }

    public Mono<Dto.EchoResponse> echoSegment(String segment, String q) {
        log.debug("echoSegment segment={} q={}", segment, q);
        return echoClient.echoSegment(segment, q)
                .doOnError(e -> log.error("Error on echoSegment segment={}", segment, e));
    }

    public Mono<Dto.EchoResponse> echoMultiPath(String segment, String sub, String q) {
        log.debug("echoMultiPath segment={} sub={}", segment, sub);
        return echoClient.echoMultiPath(segment, sub, q)
                .doOnError(e -> log.error("Error on echoMultiPath", e));
    }

    public Mono<Dto.EchoResponse> echoBody(Object body) {
        log.debug("echoBody");
        return echoClient.echoBody(body)
                .doOnError(e -> log.error("Error on echoBody", e));
    }

    public Mono<Dto.EchoResponse> echoForm(MultiValueMap<String, String> formData) {
        log.debug("echoForm fields={}", formData.keySet());
        String encoded = formData.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(v -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                                + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)))
                .collect(Collectors.joining("&"));
        return echoClient.echoForm(encoded)
                .doOnError(e -> log.error("Error on echoForm", e));
    }

    public Mono<Dto.EchoResponse> echoMultipart(String field1, String field2) {
        log.debug("echoMultipart field1={} field2={}", field1, field2);
        return echoClient.echoMultipart(field1, field2)
                .doOnError(e -> log.error("Error on echoMultipart", e));
    }

    public Mono<Dto.EchoResponse> echoPut(Long id, Object body) {
        log.debug("echoPut id={}", id);
        return echoClient.echoPut(id, body)
                .doOnError(e -> log.error("Error on echoPut id={}", id, e));
    }

    public Mono<Dto.EchoResponse> echoPatch(Long id, Object body) {
        log.debug("echoPatch id={}", id);
        return echoClient.echoPatch(id, body)
                .doOnError(e -> log.error("Error on echoPatch id={}", id, e));
    }

    public Mono<Dto.EchoResponse> echoDelete(Long id, String q) {
        log.debug("echoDelete id={}", id);
        return echoClient.echoDelete(id, q)
                .doOnError(e -> log.error("Error on echoDelete id={}", id, e));
    }
}
