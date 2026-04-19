# sw-common-client

Librería Java que provee un cliente HTTP REST declarativo con patrones de resiliencia integrados, similar a Feign pero construido sobre **WebClient** (Reactor Netty) + **Resilience4j**. 100% reactivo end-to-end: los métodos de la interfaz retornan `Mono<T>` o `Flux<T>` sin ningún bloqueo.

## Requisitos

- Java 17+
- Spring Boot 3.x / 4.x
- Gradle

## Instalación

Publicar en el repositorio Maven local:

```bash
./gradlew publishToMavenLocal
```

Agregar la dependencia en el proyecto consumidor:

**Gradle**
```groovy
dependencies {
    implementation 'io.github.ajuarez0021:sw-common-client:1.0.0'
}
```

**Maven**
```xml
<dependency>
    <groupId>io.github.ajuarez0021</groupId>
    <artifactId>sw-common-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Uso rápido

### 1. Habilitar el escaneo de clientes

Agregar `@EnableRestHttpClients` en una clase `@Configuration`:

```java
@Configuration
@EnableRestHttpClients(basePackages = "com.myapp.clients")
public class AppConfig {}
```

Si no se especifica `basePackages`, se usa el paquete de la clase anotada.

### 2. Declarar la interfaz cliente

La URL soporta **property placeholders** de Spring (p.ej. `${my.service.url}`), que se resuelven automáticamente desde el `Environment` al crear el bean.

```java
@RestHttpClient(url = "${users.service.url}", name = "user-client")
public interface UserClient {

    @GetMapping("/users/{id}")
    Mono<UserDto> getUser(@PathVariable("id") Long id);

    @GetMapping("/users")
    Flux<UserDto> streamUsers(
            @RequestParam("page") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestHeader("X-Tenant") String tenant
    );

    @PostMapping(value = "/users", consumes = "application/json", produces = "application/json")
    Mono<UserDto> createUser(@RequestBody UserDto user);

    @PostMapping(value = "/users/{id}/avatar", consumes = "multipart/form-data")
    Mono<Void> uploadAvatar(@PathVariable("id") Long id, @RequestPart("file") FilePart avatar);

    @PutMapping("/users/{id}")
    Mono<UserDto> updateUser(@PathVariable("id") Long id, @RequestBody UserDto user);

    @DeleteMapping("/users/{id}")
    Mono<Void> deleteUser(@PathVariable("id") Long id);
}
```

> La librería es **completamente reactiva end-to-end** (sin `.block()`). Los métodos de la interfaz pueden declarar `Mono<T>` para respuestas únicas o `Flux<T>` para respuestas en streaming.

### 3. Inyectar y usar

```java
@Service
public class UserService {

    private final UserClient userClient;

    public UserService(UserClient userClient) {
        this.userClient = userClient;
    }

    public Mono<UserDto> getUser(Long id) {
        return userClient.getUser(id);
    }
}
```

## Referencia de `@RestHttpClient`

| Atributo | Tipo | Default | Descripción |
|---|---|---|---|
| `name` | `String` | `""` | Identificador único del cliente (métricas y logs). Si está vacío usa el `simpleName` de la interfaz. |
| `url` | `String` | `""` | URL base del servicio REST. Admite property placeholders de Spring (p.ej. `${my.service.url}`). |
| `connectTimeout` | `long` | `5000` | Timeout de conexión en milisegundos. |
| `readTimeout` | `long` | `30000` | Timeout de lectura en milisegundos. |
| `maxConnections` | `int` | `100` | Máximo de conexiones en el pool (Reactor Netty). |
| `connectionTimeToLive` | `long` | `-1` | TTL de conexiones en milisegundos. `-1` = sin límite (usa 2 min de idle). |
| `ssl` | `@SSLConfig` | ver abajo | Configuración SSL/TLS. |
| `circuitBreaker` | `@CircuitBreakerConfig` | disabled | Circuit breaker (Resilience4j). |
| `retry` | `@RetryConfig` | 3 reintentos | Política de reintentos. |
| `rateLimiter` | `@RateLimiterConfig` | disabled | Límite de tasa de peticiones. |
| `timeLimiter` | `@TimeLimiterConfig` | disabled | Timeout global de ejecución. |
| `errorHandler` | `Class<? extends ErrorHandler>` | `DefaultErrorHandler` | Manejador de errores HTTP personalizado. |

## Anotaciones de mapeo soportadas

Se soportan todas las anotaciones estándar de Spring MVC:

| Anotación | Método HTTP |
|---|---|
| `@GetMapping` | GET |
| `@PostMapping` | POST |
| `@PutMapping` | PUT |
| `@DeleteMapping` | DELETE |
| `@PatchMapping` | PATCH |
| `@RequestMapping(method = RequestMethod.X)` | El indicado en `method` |

> `@RequestMapping` requiere declarar `method` explícitamente; si está vacío se lanza `ClientException`.

### Tipos de retorno soportados

| Tipo | Uso |
|---|---|
| `Mono<T>` | Respuesta única (JSON, objeto, etc.). |
| `Mono<Void>` | Respuestas sin cuerpo (p.ej. 204 No Content o DELETE). |
| `Flux<T>` | Respuestas en streaming (Server-Sent Events, listas, etc.). |

### Parámetros de métodos

| Anotación | Comportamiento |
|---|---|
| `@PathVariable` | Reemplaza `{variable}` en la URL. Los segmentos se codifican con `%20` (no `+`). |
| `@RequestParam` | Se agrega como query string. Con `required = false` se omite si es `null`. Admite `defaultValue`. Los valores se codifican con URL encoding estándar. |
| `@RequestHeader` | Se agrega como header HTTP. Con `required = false` se omite si es `null`. Admite `defaultValue`. |
| `@RequestBody` | Se envía como cuerpo de la petición. |
| `@RequestPart` | Construye un cuerpo `multipart/form-data`. `FilePart` y `Part` se transmiten como partes asíncronas; otros tipos se serializan normalmente. |

El atributo `params` de las anotaciones de mapeo (ej. `@GetMapping(params = "version=2")`) se agrega como query string estático.

## Resiliencia

Todos los patrones están **deshabilitados por defecto**. Se habilitan por cliente.

El orden de aplicación de los operadores Resilience4j es:

```
Retry → CircuitBreaker → RateLimiter → TimeLimiter → HTTP call
```

### Retry

Habilitado por defecto con backoff exponencial. Se retría en cualquier excepción salvo errores 4xx de cliente (según `DefaultErrorHandler`).

```java
@RestHttpClient(
    url = "http://api.example.com",
    retry = @RetryConfig(
        maxRetries = 3,
        retryDelay = 1000,
        exponentialBackoffMultiplier = 2.0
    )
)
```

| Atributo | Default | Descripción |
|---|---|---|
| `maxRetries` | `3` | Número máximo de intentos. |
| `retryDelay` | `1000` | Delay inicial en ms. |
| `exponentialBackoffMultiplier` | `2.0` | Multiplicador de backoff (1s, 2s, 4s…). |

### Circuit Breaker

```java
@RestHttpClient(
    url = "http://api.example.com",
    circuitBreaker = @CircuitBreakerConfig(
        enabled = true,
        failureRateThreshold = 50,
        slidingWindowSize = 100,
        minimumNumberOfCalls = 10,
        waitDurationInOpenState = 60000,
        permittedNumberOfCallsInHalfOpenState = 5
    )
)
```

| Atributo | Default | Descripción |
|---|---|---|
| `enabled` | `false` | Activa el circuit breaker. |
| `failureRateThreshold` | `50` | % de fallos para abrir el circuito. |
| `slidingWindowSize` | `100` | Llamadas en la ventana deslizante. |
| `minimumNumberOfCalls` | `10` | Mínimo de llamadas antes de calcular la tasa de fallos. |
| `waitDurationInOpenState` | `60000` | Ms en estado abierto antes de pasar a half-open. |
| `permittedNumberOfCallsInHalfOpenState` | `5` | Llamadas permitidas en half-open para probar recuperación. |

### Rate Limiter

```java
@RestHttpClient(
    url = "http://api.example.com",
    rateLimiter = @RateLimiterConfig(
        enabled = true,
        limitForPeriod = 100,
        limitRefreshPeriod = 1000,
        timeoutDuration = 5000
    )
)
```

| Atributo | Default | Descripción |
|---|---|---|
| `enabled` | `false` | Activa el rate limiter. |
| `limitForPeriod` | `100` | Peticiones máximas por periodo. |
| `limitRefreshPeriod` | `1000` | Duración del periodo en ms. |
| `timeoutDuration` | `5000` | Ms máximos que una petición espera permiso. |

### Time Limiter

```java
@RestHttpClient(
    url = "http://api.example.com",
    timeLimiter = @TimeLimiterConfig(
        enabled = true,
        timeout = 10000
    )
)
```

| Atributo | Default | Descripción |
|---|---|---|
| `enabled` | `false` | Activa el time limiter. |
| `timeout` | `30000` | Timeout global de ejecución en ms. |

## SSL / TLS

### mTLS (producción)

```java
@RestHttpClient(
    url = "https://secure.example.com",
    ssl = @SSLConfig(
        enabled = true,
        keystorePath = "classpath:keystore.jks",
        keystorePassword = "${SSL_KEYSTORE_PASSWORD}",
        truststorePath = "classpath:truststore.jks",
        truststorePassword = "${SSL_TRUSTSTORE_PASSWORD}"
    )
)
```

`keystorePath` y `truststorePath` admiten el prefijo `classpath:` para cargar desde el classpath, o una ruta absoluta en el sistema de archivos.

### SSL deshabilitado (solo desarrollo)

```java
@RestHttpClient(
    url = "http://localhost:8080",
    ssl = @SSLConfig(enabled = false)
)
```

> **Advertencia:** `enabled = false` deshabilita la validación de certificados y la verificación de hostname. Usar únicamente en entornos de desarrollo local.

| Atributo | Default | Descripción |
|---|---|---|
| `enabled` | `true` | `true` = mTLS con keystore/truststore. `false` = sin validación (dev only). |
| `keystorePath` | `""` | Ruta al keystore (JKS o PKCS12). |
| `keystorePassword` | `""` | Contraseña del keystore. |
| `truststorePath` | `""` | Ruta al truststore. |
| `truststorePassword` | `""` | Contraseña del truststore. |

## Manejo de errores personalizado

Implementar la interfaz `ErrorHandler` y referenciarla en `@RestHttpClient`:

```java
public class MyErrorHandler implements ErrorHandler {

    @Override
    public void handleError(HttpStatusCode statusCode, String responseBody, Exception exception) {
        if (statusCode.is4xxClientError()) {
            throw new MyBusinessException("Error del cliente: " + responseBody);
        }
        if (statusCode.is5xxServerError()) {
            throw new MyBusinessException("Error del servidor: " + responseBody);
        }
    }

    @Override
    public boolean shouldRetry(HttpStatusCode statusCode) {
        // Reintentar solo en 502, 503, 504
        return statusCode.value() == 502
            || statusCode.value() == 503
            || statusCode.value() == 504;
    }

    @Override
    public boolean shouldRecordAsFailure(HttpStatusCode statusCode) {
        // Contar como fallo en el circuit breaker solo los 5xx
        return statusCode.is5xxServerError();
    }
}
```

```java
@RestHttpClient(
    url = "http://api.example.com",
    errorHandler = MyErrorHandler.class,
    circuitBreaker = @CircuitBreakerConfig(enabled = true)
)
public interface MyClient { ... }
```

El `DefaultErrorHandler` incluido lanza `HttpClientErrorException` para 4xx/5xx y reintenta en 502/503/504.

## Ejemplo completo con todos los patrones

```java
@RestHttpClient(
    url = "${payments.service.url}",
    name = "payments-client",
    connectTimeout = 3000,
    readTimeout = 10000,
    maxConnections = 50,
    ssl = @SSLConfig(enabled = false),
    circuitBreaker = @CircuitBreakerConfig(
        enabled = true,
        failureRateThreshold = 40,
        minimumNumberOfCalls = 5,
        waitDurationInOpenState = 30000
    ),
    retry = @RetryConfig(
        maxRetries = 2,
        retryDelay = 500,
        exponentialBackoffMultiplier = 1.5
    ),
    rateLimiter = @RateLimiterConfig(
        enabled = true,
        limitForPeriod = 200,
        limitRefreshPeriod = 1000
    ),
    timeLimiter = @TimeLimiterConfig(
        enabled = true,
        timeout = 8000
    )
)
public interface PaymentsClient {

    @PostMapping(value = "/payments", consumes = "application/json", produces = "application/json")
    Mono<PaymentResponse> process(@RequestBody PaymentRequest request);

    @GetMapping("/payments/{id}/status")
    Mono<PaymentStatus> getStatus(@PathVariable("id") String paymentId,
                                  @RequestHeader("X-Correlation-Id") String correlationId);

    @GetMapping("/payments/stream")
    Flux<PaymentEvent> streamEvents(@RequestParam("since") String since);

    @PostMapping(value = "/payments/{id}/receipt", consumes = "multipart/form-data")
    Mono<Void> attachReceipt(@PathVariable("id") String paymentId,
                             @RequestPart("file") FilePart receipt);
}
```

## Proyecto de ejemplos

El directorio `examples/` contiene una aplicación Spring WebFlux completa que demuestra el uso de la librería en todos los escenarios: CRUD con paginación, path variables, query params, headers, multipart, streaming y autenticación.

Para ejecutarla (requiere haber publicado la librería con `./gradlew publishToMavenLocal`):

```bash
cd examples
mvn spring-boot:run
```

Una vez iniciada, la **Swagger UI** queda disponible en:

```
http://localhost:8080/swagger-ui.html
```

Los endpoints están agrupados en seis tags: **Users**, **Products**, **Files**, **Echo**, **Misc** y **Client Demo** (este último demuestra las interfaces cliente consumiendo los mismos endpoints).

## Comandos de desarrollo

```bash
# Compilar
./gradlew compileJava

# Ejecutar todos los tests (genera reporte JaCoCo)
./gradlew test

# Ejecutar un test específico
./gradlew test --tests "io.github.ajuarez0021.reactive.client.RestHttpClientInvocationHandlerTest"

# Build completo (incluye verificación de cobertura >= 80%)
./gradlew build

# Reporte de cobertura HTML
./gradlew jacocoTestReport
# → build/reports/jacoco/test/html/index.html

# Publicar en Maven local
./gradlew publishToMavenLocal
```

## Arquitectura interna

```
@EnableRestHttpClients
        │
        ▼
RestHttpClientRegistrar          ← Escanea el classpath buscando @RestHttpClient
        │
        ▼
RestHttpClientFactoryBean        ← Resuelve property placeholders en la URL
        │                          (ApplicationContextAware → Environment)
        ▼
RestHttpClientInvocationHandler  ← Dispatch de cada llamada de método
        │
        ├─ extractMetadata()     ← Lee anotaciones de mapeo y parámetros
        │                          (@PathVariable, @RequestParam, @RequestHeader,
        │                           @RequestBody, @RequestPart)
        │
        ├─ buildReactivePipeline()     ← Para Mono<T>
        ├─ buildReactiveFluxPipeline() ← Para Flux<T>
        │       │
        │       ├─ RetryOperator
        │       ├─ CircuitBreakerOperator
        │       ├─ RateLimiterOperator
        │       └─ TimeLimiterOperator
        │
        └─ executeWithWebClient()      ← Reactor Netty / WebClient (100% reactivo)
           executeWithWebClientFlux()
```

| Paquete | Responsabilidad |
|---|---|
| `io.github.ajuarez0021.reactive.client.autoconfigure` | Anotaciones públicas: `@EnableRestHttpClients`, `@RestHttpClient` y las configs de resiliencia. |
| `io.github.ajuarez0021.reactive.client` | Implementación: registrar, factory bean, invocation handler, SSL, pool de conexiones, manejo de errores. |
