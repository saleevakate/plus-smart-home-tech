package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.enums.ActionType;
import ru.yandex.practicum.collector.model.enums.ConditionOperation;
import ru.yandex.practicum.collector.model.enums.ConditionType;
import ru.yandex.practicum.collector.model.enums.DeviceType;
import ru.yandex.practicum.collector.model.hub.*;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.grpc.telemetry.messages.*;

import java.time.Instant;
import java.util.ArrayList;

@Component
public class HubEventProtoMapper {

    public HubEvent toModel(HubEventProto proto) {
        if (proto == null) {
            return null;
        }

        switch (proto.getPayloadCase()) {
            case DEVICE_ADDED:
                return toDeviceAddedEvent(proto);
            case DEVICE_REMOVED:
                return toDeviceRemovedEvent(proto);
            case SCENARIO_ADDED:
                return toScenarioAddedEvent(proto);
            case SCENARIO_REMOVED:
                return toScenarioRemovedEvent(proto);
            default:
                throw new IllegalArgumentException("Неизвестный тип события в хабе: " + proto.getPayloadCase());
        }
    }

    private DeviceAddedEvent toDeviceAddedEvent(HubEventProto proto) {
        DeviceAddedEventProto device = proto.getDeviceAdded();
        DeviceAddedEvent event = new DeviceAddedEvent();
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setHubId(device.getId());
        event.setDeviceType(DeviceType.valueOf(device.getType().name()));
        return event;
    }

    private DeviceRemovedEvent toDeviceRemovedEvent(HubEventProto proto) {
        DeviceRemovedEventProto device = proto.getDeviceRemoved();
        DeviceRemovedEvent event = new DeviceRemovedEvent();
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setHubId(device.getId());
        return event;
    }

    private ScenarioAddedEvent toScenarioAddedEvent(HubEventProto proto) {
        ScenarioAddedEventProto scenario = proto.getScenarioAdded();
        ScenarioAddedEvent event = new ScenarioAddedEvent();
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setName(scenario.getName());

        var conditions = new ArrayList<ScenarioCondition>();
        for (ScenarioConditionProto condition : scenario.getConditionList()) {
            var c = new ScenarioCondition();
            c.setSensorId(condition.getSensorId());
            c.setType(ConditionType.valueOf(condition.getType().name()));
            c.setOperation(ConditionOperation.valueOf(condition.getOperation().name()));
            if (condition.hasIntValue()) {
                c.setValue(condition.getIntValue());
            }
            conditions.add(c);
        }
        event.setConditions(conditions);

        var actions = new ArrayList<DeviceAction>();
        for (DeviceActionProto action : scenario.getActionList()) {
            var a = new DeviceAction();
            a.setSensorId(action.getSensorId());
            a.setType(ActionType.valueOf(action.getType().name()));
            if (action.hasValue()) {
                a.setValue(action.getValue());
            }
            actions.add(a);
        }
        event.setActions(actions);

        return event;
    }

    private ScenarioRemovedEvent toScenarioRemovedEvent(HubEventProto proto) {
        ScenarioRemovedEventProto scenario = proto.getScenarioRemoved();
        ScenarioRemovedEvent event = new ScenarioRemovedEvent();
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
        event.setName(scenario.getName());
        return event;
    }
}
