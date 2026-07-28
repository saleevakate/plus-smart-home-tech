package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private static final String SNAPSHOTS_TOPIC = "telemetry.snapshots.v1";

    private final Consumer<String, Object> snapshotConsumer;
    private final ScenarioService scenarioService;

    @Value("${kafka.poll.timeout:1000}")
    private long pollTimeout;

    public void start() {
        try {
            snapshotConsumer.subscribe(java.util.List.of(SNAPSHOTS_TOPIC));
            log.info("SnapshotProcessor подписался на топик: {}", SNAPSHOTS_TOPIC);

            while (true) {
                ConsumerRecords<String, Object> records = snapshotConsumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Получено {} снапшотов", records.count());

                for (var record : records) {
                    try {
                        SensorsSnapshotAvro snapshot = (SensorsSnapshotAvro) record.value();
                        scenarioService.processSnapshot(snapshot);
                    } catch (Exception e) {
                        log.error("Ошибка обработки снапшота", e);
                    }
                }

                try {
                    snapshotConsumer.commitSync();
                    log.debug("Смещения для SnapshotConsumer зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений", e);
                }
            }

        } catch (WakeupException e) {
            log.info("Получен сигнал завершения работы SnapshotProcessor");
        } catch (Exception e) {
            log.error("Ошибка во время обработки снапшотов", e);
        } finally {
            try {
                snapshotConsumer.commitSync();
                log.info("Смещения SnapshotConsumer зафиксированы при завершении");
            } catch (Exception e) {
                log.error("Ошибка фиксации смещений при завершении", e);
            } finally {
                log.info("Закрываем SnapshotConsumer");
                snapshotConsumer.close();
            }
        }
    }
}

