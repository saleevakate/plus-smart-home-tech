package ru.yandex.practicum.collector.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private static final String SENSOR_TOPIC = "telemetry.sensors.v1";
    private static final String HUB_TOPIC = "telemetry.hubs.v1";

    private final KafkaProducer<String, SpecificRecordBase> kafkaProducer;
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;

    @Override
    public void sendSensorEvent(SensorEvent event) {
        SensorEventAvro avroEvent = sensorEventMapper.toAvro(event);
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                SENSOR_TOPIC,
                null,
                event.getTimestamp().toEpochMilli(),
                event.getHubId(),
                avroEvent
        );
        kafkaProducer.send(record);
        log.info("Отправлено событие датчика в топик  {}: {}", SENSOR_TOPIC, avroEvent);
    }

    @Override
    public void sendHubEvent(HubEvent event) {
        HubEventAvro avroEvent = hubEventMapper.toAvro(event);
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                HUB_TOPIC,
                null,
                event.getTimestamp().toEpochMilli(),
                event.getHubId(),
                avroEvent
        );
        kafkaProducer.send(record);
        log.info("Отправлено событие хаба в топик {}: {}", HUB_TOPIC, avroEvent);
    }

    @PreDestroy
    public void close() {
        if (kafkaProducer != null) {
            kafkaProducer.flush();
            kafkaProducer.close();
        }
    }
}