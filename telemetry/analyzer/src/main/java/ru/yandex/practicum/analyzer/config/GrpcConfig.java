package ru.yandex.practicum.analyzer.config;

import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GrpcClientAutoConfiguration.class)
public class GrpcConfig {
}
