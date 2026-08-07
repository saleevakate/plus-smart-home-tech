package ru.yandex.practicum.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private List<User> users = new ArrayList<>();

    @Data
    public static class User {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();
    }
}
