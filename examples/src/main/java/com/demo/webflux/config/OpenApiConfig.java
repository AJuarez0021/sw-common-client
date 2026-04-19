package com.demo.webflux.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Demo WebFlux API")
                        .description("Ejemplo de API reactiva con Spring WebFlux que demuestra todos los verbos HTTP, " +
                                "path variables, query params, headers, body y manejo de archivos. " +
                                "Construido sobre sw-common-client para ilustrar el cliente HTTP declarativo.")
                        .version("1.0.0")
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local")))
                .tags(List.of(
                        new Tag().name("Users").description("CRUD de usuarios con filtros, paginación y headers"),
                        new Tag().name("Products").description("CRUD de productos con búsqueda, precio y stock"),
                        new Tag().name("Files").description("Subida y descarga de archivos (multipart)"),
                        new Tag().name("Echo").description("Endpoints de eco para depurar el cliente HTTP"),
                        new Tag().name("Misc").description("Endpoints misceláneos de demostración"),
                        new Tag().name("Client Demo").description("Demostración del cliente HTTP declarativo sw-common-client")));
    }
}
