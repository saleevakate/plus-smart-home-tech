package ru.yandex.practicum.aggregator.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.aggregator.service.AggregationService;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private static final String SENSORS_TOPIC = "telemetry.sensors.v1";
    private static final String SNAPSHOTS_TOPIC = "telemetry.snapshots.v1";

    private final Consumer<String, SensorEventAvro> consumer;
    private final Producer<String, Object> producer;
    private final AggregationService aggregationService;

    @Value("${kafka.poll.timeout:1000}")
    private long pollTimeout;

    public void start() {
        try {
            consumer.subscribe(java.util.List.of(SENSORS_TOPIC));
            log.info("Aggregator подписался на топик: {}", SENSORS_TOPIC);

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Получено {} событий от датчиков", records.count());

                for (var record : records) {
                    try {
                        SensorEventAvro event = record.value();
                        log.info("Обработка события: hubId={}, sensorId={}, type={}",
                                event.getHubId(), event.getId(), event.getPayload().getClass().getSimpleName());

                        Optional<SensorsSnapshotAvro> optionalSnapshot = aggregationService.updateState(event);

                        if (optionalSnapshot.isPresent()) {
                            SensorsSnapshotAvro snapshot = optionalSnapshot.get();
                            String key = snapshot.getHubId();

                            ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                                    SNAPSHOTS_TOPIC,
                                    key,
                                    snapshot
                            );

                            producer.send(producerRecord, (metadata, exception) -> {
                                if (exception != null) {
                                    log.error("Ошибка отправки снапшота в топик {}", SNAPSHOTS_TOPIC, exception);
                                } else {
                                    log.info("Снапшот для хаба {} отправлен в топик {}, offset={}",
                                            key, SNAPSHOTS_TOPIC, metadata.offset());
                                }
                            });

                            producer.flush();
                        }

                    } catch (Exception e) {
                        log.error("Ошибка обработки события от датчика", e);
                    }
                }

                try {
                    consumer.commitSync();
                    log.debug("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений", e);
                }
            }

        } catch (WakeupException e) {
            log.info("Получен сигнал завершения работы");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                producer.flush();
                log.info("Все сообщения отправлены");
            } finally {
                try {
                    consumer.commitSync();
                    log.info("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений при завершении", e);
                } finally {
                    log.info("Закрываем консьюмер");
                    consumer.close();
                    log.info("Закрываем продюсер");
                    producer.close();
                }
            }
        }
    }
}
