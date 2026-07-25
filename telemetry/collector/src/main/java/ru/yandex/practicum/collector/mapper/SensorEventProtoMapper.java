package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.*;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.grpc.telemetry.messages.*;

import java.time.Instant;

@Component
public class SensorEventProtoMapper {

    public SensorEvent toModel(SensorEventProto proto) {
        if (proto == null) {
            return null;
        }

        switch (proto.getPayloadCase()) {
            case MOTION_SENSOR:
                return toMotionSensorEvent(proto);
            case TEMPERATURE_SENSOR:
                return toTemperatureSensorEvent(proto);
            case LIGHT_SENSOR:
                return toLightSensorEvent(proto);
            case CLIMATE_SENSOR:
                return toClimateSensorEvent(proto);
            case SWITCH_SENSOR:
                return toSwitchSensorEvent(proto);
            default:
                throw new IllegalArgumentException("Неизвестный тип датчика: " + proto.getPayloadCase());
        }
    }

    private MotionSensorEvent toMotionSensorEvent(SensorEventProto proto) {
        MotionSensorProto sensor = proto.getMotionSensor();
        MotionSensorEvent event = new MotionSensorEvent();
        event.setId(proto.getId());
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setLinkQuality(sensor.getLinkQuality());
        event.setMotion(sensor.getMotion());
        event.setVoltage(sensor.getVoltage());
        return event;
    }

    private TemperatureSensorEvent toTemperatureSensorEvent(SensorEventProto proto) {
        TemperatureSensorProto sensor = proto.getTemperatureSensor();
        TemperatureSensorEvent event = new TemperatureSensorEvent();
        event.setId(proto.getId());
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setTemperatureC(sensor.getTemperatureC());
        event.setTemperatureF(sensor.getTemperatureF());
        return event;
    }

    private LightSensorEvent toLightSensorEvent(SensorEventProto proto) {
        LightSensorProto sensor = proto.getLightSensor();
        LightSensorEvent event = new LightSensorEvent();
        event.setId(proto.getId());
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setLinkQuality(sensor.getLinkQuality());
        event.setLuminosity(sensor.getLuminosity());
        return event;
    }

    private ClimateSensorEvent toClimateSensorEvent(SensorEventProto proto) {
        ClimateSensorProto sensor = proto.getClimateSensor();
        ClimateSensorEvent event = new ClimateSensorEvent();
        event.setId(proto.getId());
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setTemperatureC(sensor.getTemperatureC());
        event.setHumidity(sensor.getHumidity());
        event.setCo2Level(sensor.getCo2Level());
        return event;
    }

    private SwitchSensorEvent toSwitchSensorEvent(SensorEventProto proto) {
        SwitchSensorProto sensor = proto.getSwitchSensor();
        SwitchSensorEvent event = new SwitchSensorEvent();
        event.setId(proto.getId());
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setState(sensor.getState());
        return event;
    }
}
