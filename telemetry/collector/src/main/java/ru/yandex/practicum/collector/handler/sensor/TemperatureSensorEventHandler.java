package ru.yandex.practicum.collector.handler.sensor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.mapper.SensorEventProtoMapper;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemperatureSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;
    private final SensorEventProtoMapper mapper;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.TEMPERATURE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        SensorEvent sensorEvent = mapper.toModel(event);
        log.info("Датчик температуры: id={}, hubId={}", sensorEvent.getId(), sensorEvent.getHubId());
        eventService.sendSensorEvent(sensorEvent);
    }
}
