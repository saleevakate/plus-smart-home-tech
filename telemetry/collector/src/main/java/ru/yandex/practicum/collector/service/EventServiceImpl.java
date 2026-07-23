package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;

    @Override
    public void sendSensorEvent(SensorEvent event) {
        SensorEventAvro avroEvent = sensorEventMapper.toAvro(event);
        kafkaTemplate.send(SENSOR_TOPIC, event.getHubId(), avroEvent);
        log.info("Отправлено событие датчика в топик  {}: {}", SENSOR_TOPIC, avroEvent);
    }

    @Override
    public void sendHubEvent(HubEvent event) {
        HubEventAvro avroEvent = hubEventMapper.toAvro(event);
        kafkaTemplate.send(HUB_TOPIC, event.getHubId(), avroEvent);
        log.info("Отправлено событие хаба в топик {}: {}", HUB_TOPIC, avroEvent);
    }
}