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

        Object httpClient = createHttpClient(annotation);
        ErrorHandler errorHandler = annotation.errorHandler().getDeclaredConstructor().newInstance();

        RestHttpClientInvocationHandler handler = new RestHttpClientInvocationHandler(
                httpClient,
                annotation,
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
     * Creates the http client.
     *
     * @param annotation the annotation
     * @return the object
     */
    private Object createHttpClient(RestHttpClient annotation) {
       return HttpClientConfigurer.configureWebClient(annotation);
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
