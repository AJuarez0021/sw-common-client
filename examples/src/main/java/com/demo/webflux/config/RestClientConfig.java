package com.demo.webflux.config;

import com.work.common.autoconfigure.EnableRestHttpClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRestHttpClients(basePackages = "com.demo.webflux.client")
public class RestClientConfig {
}
