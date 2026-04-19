package com.work.common.client;

import com.work.common.autoconfigure.RestHttpClient;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;
import java.util.Objects;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The Class RestHttpClientFactoryBean.
 *
 * @param <T> the generic type
 */
public class RestHttpClientFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware {
    
    /** The client interface. */
    private final Class<T> clientInterface;
    
    /** The resilience 4 j configuration. */
    private final Resilience4jConfiguration resilience4jConfiguration;

    /** The application context. */
    @Getter
    private ApplicationContext applicationContext;
    /**
     * Instantiates a new rest http client factory bean.
     *
     * @param clientInterface the client interface
     * @param resilience4jConfiguration the resilience 4 j configuration
     */
    public RestHttpClientFactoryBean(Class<T> clientInterface,
                                     Resilience4jConfiguration resilience4jConfiguration) {
        this.clientInterface = Objects.requireNonNull(clientInterface);
        this.resilience4jConfiguration = resilience4jConfiguration;
    }

    /**
     * Set Application Context.
     *
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws BeansException the exception
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    /**
     * Gets the object.
     *
     * @return the object
     * @throws Exception the exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public T getObject() throws Exception {
        RestHttpClient annotation = clientInterface.getAnnotation(RestHttpClient.class);
        String resolvedUrl = resolveUrl(annotation.url());

        Object httpClient = HttpClientConfigurer.configureWebClient(annotation, resolvedUrl);
        ErrorHandler errorHandler = annotation.errorHandler().getDeclaredConstructor().newInstance();

        RestHttpClientInvocationHandler handler = new RestHttpClientInvocationHandler(
                httpClient,
                annotation,
                resolvedUrl,
                errorHandler,
                clientInterface,
                resilience4jConfiguration
        );

        return (T) Proxy.newProxyInstance(
                clientInterface.getClassLoader(),
                new Class<?>[]{clientInterface},
                handler
        );
    }

    /**
     * Resolves Spring property placeholders (e.g. {@code ${my.url}}) in the given URL
     * using the application {@code Environment}. Falls back to the raw value when the
     * context is not yet available (e.g. during unit tests).
     *
     * @param url the raw URL string, possibly containing placeholders
     * @return the resolved URL
     */
    private String resolveUrl(String url) {
        if (applicationContext != null) {
            return applicationContext.getEnvironment().resolvePlaceholders(url);
        }
        return url;
    }

    /**
     * Gets the object type.
     *
     * @return the object type
     */
    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    /**
     * Checks if is singleton.
     *
     * @return true, if is singleton
     */
    @Override
    public boolean isSingleton() {
        return true;
    }


}
