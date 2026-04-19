package io.github.ajuarez0021.reactive.client.autoconfigure;


import io.github.ajuarez0021.reactive.client.RestHttpClientRegistrar;
import io.github.ajuarez0021.reactive.client.RestHttpConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * The Interface EnableRestHttpClients.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({RestHttpClientRegistrar.class, RestHttpConfig.class})
public @interface EnableRestHttpClients {
    
    /**
     * Base packages.
     *
     * @return the string[]
     */
    String[] basePackages() default {};
}
