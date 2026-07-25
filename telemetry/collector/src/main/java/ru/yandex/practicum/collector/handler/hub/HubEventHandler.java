package ru.yandex.practicum.collector.handler.hub;

import ru.yandex.practicum.grpc.telemetry.messages.HubEventProto;

public interface HubEventHandler {

    HubEventProto.PayloadCase getMessageType();

    void handle(HubEventProto event);
}
