package ru.yandex.practicum.collector.handler.hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.mapper.HubEventProtoMapper;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.service.EventService;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceAddedEventHandler implements HubEventHandler {

    private final EventService eventService;
    private final HubEventProtoMapper mapper;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        HubEvent hubEvent = mapper.toModel(event);
        log.info("Добавление устройства: hubId={}", hubEvent.getHubId());
        eventService.sendHubEvent(hubEvent);
    }
}
