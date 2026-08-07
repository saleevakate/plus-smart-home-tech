package ru.yandex.practicum.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void publicRoutes_areAvailableWithoutAuthentication() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void createOrder_withoutAuthentication_isUnauthorized() {
        webTestClient.post()
                .uri("/api/orders")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void createOrder_withUserCredentials_passesSecurity() {
        webTestClient.post()
                .uri("/api/orders")
                .headers(headers -> headers.setBasicAuth("ivan", "ivan"))
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void writeProduct_withUserCredentials_isForbidden() {
        webTestClient.patch()
                .uri("/api/products/10")
                .headers(headers -> headers.setBasicAuth("ivan", "ivan"))
                .bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void writeProduct_withAdminCredentials_passesSecurity() {
        webTestClient.patch()
                .uri("/api/products/10")
                .headers(headers -> headers.setBasicAuth("anna", "anna"))
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getAllOrders_withUserCredentials_isForbidden() {
        webTestClient.get()
                .uri("/api/orders")
                .headers(headers -> headers.setBasicAuth("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getAllOrders_withAdminCredentials_passesSecurity() {
        webTestClient.get()
                .uri("/api/orders")
                .headers(headers -> headers.setBasicAuth("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unknownRoute_withAdminCredentials_isForbidden() {
        webTestClient.get()
                .uri("/api/unknown")
                .headers(headers -> headers.setBasicAuth("anna", "anna"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        PasswordEncoder testPasswordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        @Primary
        MapReactiveUserDetailsService testUserDetailsService(PasswordEncoder passwordEncoder) {
            UserDetails ivan = User.builder()
                    .username("ivan")
                    .password(passwordEncoder.encode("ivan"))
                    .roles("USER")
                    .build();

            UserDetails anna = User.builder()
                    .username("anna")
                    .password(passwordEncoder.encode("anna"))
                    .roles("USER", "ADMIN")
                    .build();

            return new MapReactiveUserDetailsService(ivan, anna);
        }

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route()
                    .GET("/api/**", request -> ServerResponse.ok().build())
                    .POST("/api/**", request -> ServerResponse.ok().build())
                    .PUT("/api/**", request -> ServerResponse.ok().build())
                    .PATCH("/api/**", request -> ServerResponse.ok().build())
                    .DELETE("/api/**", request -> ServerResponse.ok().build())
                    .OPTIONS("/api/**", request -> {
                        // Добавляем CORS заголовки для тестов
                        return ServerResponse.ok()
                                .header("Access-Control-Allow-Origin", "*")
                                .header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
                                .header("Access-Control-Allow-Headers", "*")
                                .build();
                    })
                    .build();
        }
    }
}
