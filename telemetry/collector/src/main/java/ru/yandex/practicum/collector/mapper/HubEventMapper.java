package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.stream.Collectors;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent event) {
        if (event == null) {
            return null;
        }

        HubEventAvro.Builder builder = HubEventAvro.newBuilder();
        builder.setHubId(event.getHubId());
        builder.setTimestamp(event.getTimestamp().toEpochMilli());

        switch (event.getType()) {
            case DEVICE_ADDED -> {
                DeviceAddedEvent addedEvent = (DeviceAddedEvent) event;
                DeviceAddedEventAvro addedAvro = DeviceAddedEventAvro.newBuilder()
                        .setId(addedEvent.getId())
                        .setType(DeviceTypeAvro.valueOf(addedEvent.getDeviceType().name()))
                        .build();
                builder.setPayload(addedAvro);
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEvent removedEvent = (DeviceRemovedEvent) event;
                DeviceRemovedEventAvro removedAvro = DeviceRemovedEventAvro.newBuilder()
                        .setId(removedEvent.getId())
                        .build();
                builder.setPayload(removedAvro);
            }

            case SCENARIO_ADDED -> {
                ScenarioAddedEvent scenarioAddedEvent = (ScenarioAddedEvent) event;
                java.util.List<ScenarioConditionAvro> conditionAvros = scenarioAddedEvent.getConditions().stream()
                        .map(this::toScenarioConditionAvro)
                        .collect(Collectors.toList());
                java.util.List<DeviceActionAvro> actionAvros = scenarioAddedEvent.getActions().stream()
                        .map(this::toDeviceActionAvro)
                        .collect(Collectors.toList());

                ScenarioAddedEventAvro scenarioAddedAvro = ScenarioAddedEventAvro.newBuilder()
                        .setName(scenarioAddedEvent.getName())
                        .setConditions(conditionAvros)
                        .setActions(actionAvros)
                        .build();
                builder.setPayload(scenarioAddedAvro);
            }

            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent scenarioRemovedEvent = (ScenarioRemovedEvent) event;
                ScenarioRemovedEventAvro scenarioRemovedAvro = ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemovedEvent.getName())
                        .build();
                builder.setPayload(scenarioRemovedAvro);
            }

            default -> throw new IllegalArgumentException("Неизвестный тип события в хабе: " + event.getType());
        }

        return builder.build();
    }

    private ScenarioConditionAvro toScenarioConditionAvro(ScenarioCondition condition) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder();
        builder.setSensorId(condition.getSensorId());
        builder.setType(ConditionTypeAvro.valueOf(condition.getType().name()));
        builder.setOperation(ConditionOperationAvro.valueOf(condition.getOperation().name()));

        if (condition.getValue() != null) {
            builder.setValue(condition.getValue());
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }

    private DeviceActionAvro toDeviceActionAvro(DeviceAction action) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder();
        builder.setSensorId(action.getSensorId());
        builder.setType(ActionTypeAvro.valueOf(action.getType().name()));

        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }
}