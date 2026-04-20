package com.demo.webflux.config;

import io.github.ajuarez0021.reactive.client.autoconfigure.EnableRestHttpClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRestHttpClients(basePackages = "com.demo.webflux.client")
public class RestClientConfig {
}
