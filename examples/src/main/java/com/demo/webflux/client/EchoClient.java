package com.demo.webflux.client;

import com.demo.webflux.model.Dto;
import com.work.common.autoconfigure.RestHttpClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestHttpClient(url = "${demo.webflux.base-url:http://localhost:8080}", name = "echoClient")
public interface EchoClient {

    @GetMapping("/api/echo")
    Mono<Dto.EchoResponse> echoGet(
            @RequestParam(required = false) String q,
            @RequestHeader(value = "X-Custom-Header", required = false) String customHeader);

    @GetMapping("/api/echo/{segment}")
    Mono<Dto.EchoResponse> echoSegment(
            @PathVariable String segment,
            @RequestParam(required = false) String q);

    @GetMapping("/api/echo/{segment}/{sub}")
    Mono<Dto.EchoResponse> echoMultiPath(
            @PathVariable String segment,
            @PathVariable String sub,
            @RequestParam(required = false) String q);

    @PostMapping("/api/echo/body")
    Mono<Dto.EchoResponse> echoBody(@RequestBody Object body);

    @PostMapping(value = "/api/echo/form", consumes = "application/x-www-form-urlencoded")
    Mono<Dto.EchoResponse> echoForm(@RequestBody String formData);

    @PostMapping(value = "/api/echo/multipart", consumes = "multipart/form-data")
    Mono<Dto.EchoResponse> echoMultipart(
            @RequestPart("field1") String field1,
            @RequestPart("field2") String field2);

    @PutMapping("/api/echo/{id}")
    Mono<Dto.EchoResponse> echoPut(@PathVariable Long id, @RequestBody Object body);

    @PatchMapping("/api/echo/{id}")
    Mono<Dto.EchoResponse> echoPatch(@PathVariable Long id, @RequestBody Object body);

    @DeleteMapping("/api/echo/{id}")
    Mono<Dto.EchoResponse> echoDelete(
            @PathVariable Long id,
            @RequestParam(required = false) String q);

    @RequestMapping(value = "/api/echo/{id}", method = RequestMethod.HEAD)
    Mono<Void> echoHead(@PathVariable Long id);

    @RequestMapping(value = "/api/echo", method = RequestMethod.OPTIONS)
    Mono<Void> echoOptions();
}
