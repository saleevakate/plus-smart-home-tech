package ru.yandex.practicum.collector.handler.sensor;


import ru.yandex.practicum.grpc.telemetry.messages.SensorEventProto;

public interface SensorEventHandler {

    SensorEventProto.PayloadCase getMessageType();

    void handle(SensorEventProto event);
}
