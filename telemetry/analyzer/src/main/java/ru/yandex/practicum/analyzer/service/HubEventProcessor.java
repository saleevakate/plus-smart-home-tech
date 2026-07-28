package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private static final String HUBS_TOPIC = "telemetry.hubs.v1";

    private final Consumer<String, Object> hubEventConsumer;
    private final ScenarioService scenarioService;

    @Value("${kafka.poll.timeout:1000}")
    private long pollTimeout;

    @Override
    public void run() {
        try {
            hubEventConsumer.subscribe(java.util.List.of(HUBS_TOPIC));
            log.info("HubEventProcessor подписался на топик: {}", HUBS_TOPIC);

            while (true) {
                ConsumerRecords<String, Object> records = hubEventConsumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Получено {} событий от хабов", records.count());

                for (var record : records) {
                    try {
                        HubEventAvro event = (HubEventAvro) record.value();
                        scenarioService.processHubEvent(event);
                    } catch (Exception e) {
                        log.error("Ошибка обработки события от хаба", e);
                    }
                }

                try {
                    hubEventConsumer.commitSync();
                    log.debug("Смещения для HubEventConsumer зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений", e);
                }
            }

        } catch (WakeupException e) {
            log.info("Получен сигнал завершения работы HubEventProcessor");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от хабов", e);
        } finally {
            try {
                hubEventConsumer.commitSync();
                log.info("Смещения HubEventConsumer зафиксированы при завершении");
            } catch (Exception e) {
                log.error("Ошибка фиксации смещений при завершении", e);
            } finally {
                log.info("Закрываем HubEventConsumer");
                hubEventConsumer.close();
            }
        }
    }
}
