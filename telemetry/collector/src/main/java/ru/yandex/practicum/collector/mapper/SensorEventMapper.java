package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEvent event) {
        if (event == null) return null;

        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder();
        builder.setId(event.getId());
        builder.setHubId(event.getHubId());
        builder.setTimestamp(event.getTimestamp().toEpochMilli());

        switch (event.getType()) {
            case LIGHT_SENSOR_EVENT -> {
                LightSensorEvent lightEvent = (LightSensorEvent) event;
                LightSensorAvro lightAvro = LightSensorAvro.newBuilder()
                        .setLinkQuality(lightEvent.getLinkQuality() != null ? lightEvent.getLinkQuality() : 0)
                        .setLuminosity(lightEvent.getLuminosity() != null ? lightEvent.getLuminosity() : 0)
                        .build();
                builder.setPayload(lightAvro);
            }
            case TEMPERATURE_SENSOR_EVENT -> {
                TemperatureSensorEvent tempEvent = (TemperatureSensorEvent) event;
                TemperatureSensorAvro tempAvro = TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(tempEvent.getTemperatureC() != null ? tempEvent.getTemperatureC() : 0)
                        .setTemperatureF(tempEvent.getTemperatureF() != null ? tempEvent.getTemperatureF() : 0)
                        .build();
                builder.setPayload(tempAvro);
            }
            case MOTION_SENSOR_EVENT -> {
                MotionSensorEvent motionEvent = (MotionSensorEvent) event;
                MotionSensorAvro motionAvro = MotionSensorAvro.newBuilder()
                        .setLinkQuality(motionEvent.getLinkQuality() != null ? motionEvent.getLinkQuality() : 0)
                        .setMotion(motionEvent.getMotion() != null && motionEvent.getMotion())
                        .setVoltage(motionEvent.getVoltage() != null ? motionEvent.getVoltage() : 0)
                        .build();
                builder.setPayload(motionAvro);
            }
            case SWITCH_SENSOR_EVENT -> {
                SwitchSensorEvent switchEvent = (SwitchSensorEvent) event;
                SwitchSensorAvro switchAvro = SwitchSensorAvro.newBuilder()
                        .setState(switchEvent.getState() != null && switchEvent.getState())
                        .build();
                builder.setPayload(switchAvro);
            }
            case CLIMATE_SENSOR_EVENT -> {
                ClimateSensorEvent climateEvent = (ClimateSensorEvent) event;
                ClimateSensorAvro climateAvro = ClimateSensorAvro.newBuilder()
                        .setTemperatureC(climateEvent.getTemperatureC() != null ? climateEvent.getTemperatureC() : 0)
                        .setHumidity(climateEvent.getHumidity() != null ? climateEvent.getHumidity() : 0)
                        .setCo2Level(climateEvent.getCo2Level() != null ? climateEvent.getCo2Level() : 0)
                        .build();
                builder.setPayload(climateAvro);
            }
            default -> throw new IllegalArgumentException("Неизвестный тип: " + event.getType());
        }

        return builder.build();
    }
}