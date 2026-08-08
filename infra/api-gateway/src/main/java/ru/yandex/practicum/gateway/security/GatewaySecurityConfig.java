package ru.yandex.practicum.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import ru.yandex.practicum.gateway.config.SecurityProperties;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        //все публичные запросы
                        .pathMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/inventory/**").permitAll()
                        //все запросы ADMIN
                        .pathMatchers("/api/products/**", "/api/categories/**", "/api/inventory/**").hasRole("ADMIN")
                         //список всех заказов ADMIN
                        .pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                        //все запросы USER
                        .pathMatchers("/api/orders/**").hasRole("USER")
                        .anyExchange().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(
            SecurityProperties securityProperties,
            PasswordEncoder passwordEncoder) {

        List<UserDetails> users = new ArrayList<>();

        for (SecurityProperties.User userConfig : securityProperties.getUsers()) {
            UserDetails user = User.builder()
                    .username(userConfig.getUsername())
                    .password(passwordEncoder.encode(userConfig.getPassword()))
                    .roles(userConfig.getRoles().toArray(new String[0]))
                    .build();
            users.add(user);
        }

        return new MapReactiveUserDetailsService(users);
    }
}
