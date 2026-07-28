package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.*;
import ru.yandex.practicum.analyzer.model.enums.ActionType;
import ru.yandex.practicum.analyzer.model.enums.ConditionOperation;
import ru.yandex.practicum.analyzer.model.enums.ConditionType;
import ru.yandex.practicum.analyzer.repository.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final DeviceActionService deviceActionService;

    @Transactional
    public void processHubEvent(HubEventAvro event) {
        String hubId = event.getHubId().toString();

        if (event.getPayload() instanceof DeviceAddedEventAvro) {
            DeviceAddedEventAvro device = (DeviceAddedEventAvro) event.getPayload();
            processDeviceAdded(hubId, device);
        } else if (event.getPayload() instanceof DeviceRemovedEventAvro) {
            DeviceRemovedEventAvro device = (DeviceRemovedEventAvro) event.getPayload();
            processDeviceRemoved(device);
        } else if (event.getPayload() instanceof ScenarioAddedEventAvro) {
            ScenarioAddedEventAvro scenario = (ScenarioAddedEventAvro) event.getPayload();
            processScenarioAdded(hubId, scenario);
        } else if (event.getPayload() instanceof ScenarioRemovedEventAvro) {
            ScenarioRemovedEventAvro scenario = (ScenarioRemovedEventAvro) event.getPayload();
            processScenarioRemoved(hubId, scenario);
        }
    }

    @Transactional
    public void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId().toString();

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);

        if (scenarios.isEmpty()) {
            log.debug("Нет сценариев для хаба {}", hubId);
            return;
        }

        log.info("Проверка {} сценариев для хаба {}", scenarios.size(), hubId);

        for (Scenario scenario : scenarios) {
            List<ScenarioCondition> scenarioConditions = scenarioConditionRepository.findByScenarioId(scenario.getId());

            if (scenarioConditions.isEmpty()) {
                continue;
            }

            boolean conditionsMet = checkConditions(scenarioConditions, snapshot);

            if (conditionsMet) {
                log.info("Сценарий '{}' для хаба {} активирован", scenario.getName(), hubId);
                List<ScenarioAction> scenarioActions = scenarioActionRepository.findByScenarioId(scenario.getId());
                deviceActionService.executeActions(scenario, scenarioActions);
            }
        }
    }

    private boolean checkConditions(List<ScenarioCondition> scenarioConditions, SensorsSnapshotAvro snapshot) {
        for (ScenarioCondition sc : scenarioConditions) {
            String sensorId = sc.getSensor().getId();
            SensorStateAvro sensorState = snapshot.getSensorsState().get(sensorId);

            if (sensorState == null) {
                log.debug("Датчик {} не найден в снапшоте", sensorId);
                return false;
            }

            Condition condition = sc.getCondition();
            boolean conditionMet = evaluateCondition(condition, sensorState);
            if (!conditionMet) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateCondition(Condition condition, SensorStateAvro sensorState) {
        int intValue = getSensorValue(sensorState);

        return switch (condition.getOperation()) {
            case EQUALS -> intValue == condition.getValue();
            case GREATER_THAN -> intValue > condition.getValue();
            case LOWER_THAN -> intValue < condition.getValue();
        };
    }

    private int getSensorValue(SensorStateAvro state) {
        Object data = state.getData();
        String className = data.getClass().getSimpleName();

        return switch (className) {
            case "ClimateSensorAvro" -> ((ClimateSensorAvro) data).getTemperatureC();
            case "LightSensorAvro" -> ((LightSensorAvro) data).getLuminosity();
            case "MotionSensorAvro" -> ((MotionSensorAvro) data).getMotion() ? 1 : 0;
            case "SwitchSensorAvro" -> ((SwitchSensorAvro) data).getState() ? 1 : 0;
            case "TemperatureSensorAvro" -> ((TemperatureSensorAvro) data).getTemperatureC();
            default -> 0;
        };
    }

    @Transactional
    public void processDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        Sensor sensor = Sensor.builder()
                .id(event.getId().toString())
                .hubId(hubId)
                .build();
        sensorRepository.save(sensor);
        log.info("Датчик {} добавлен в хаб {}", event.getId(), hubId);
    }

    @Transactional
    public void processDeviceRemoved(DeviceRemovedEventAvro event) {
        String sensorId = event.getId().toString();
        sensorRepository.deleteById(sensorId);
        log.info("Датчик {} удалён", sensorId);
    }

    @Transactional
    public void processScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        Optional<Scenario> existing = scenarioRepository.findByHubIdAndName(hubId, event.getName().toString());

        if (existing.isPresent()) {
            log.warn("Сценарий {} уже существует для хаба {}", event.getName(), hubId);
            return;
        }

        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(event.getName().toString())
                .build();

        scenario = scenarioRepository.save(scenario);

        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            String sensorId = conditionAvro.getSensorId().toString();
            Optional<Sensor> sensorOpt = sensorRepository.findById(sensorId);

            if (sensorOpt.isEmpty()) {
                log.warn("Датчик {} не найден, пропускаем условие", sensorId);
                continue;
            }

            Condition condition = Condition.builder()
                    .type(ConditionType.valueOf(conditionAvro.getType().name()))
                    .operation(ConditionOperation.valueOf(conditionAvro.getOperation().name()))
                    .value(conditionAvro.getValue())
                    .build();

            condition = conditionRepository.save(condition);

            ScenarioCondition sc = ScenarioCondition.builder()
                    .scenario(scenario)
                    .condition(condition)
                    .sensor(sensorOpt.get())
                    .build();

            sc.setId(new ScenarioCondition.ScenarioConditionId(
                    scenario.getId(),
                    sensorId,
                    condition.getId()
            ));

            scenarioConditionRepository.save(sc);
        }

        for (DeviceActionAvro actionAvro : event.getActions()) {
            String sensorId = actionAvro.getSensorId().toString();
            Optional<Sensor> sensorOpt = sensorRepository.findById(sensorId);

            if (sensorOpt.isEmpty()) {
                log.warn("Датчик {} не найден, пропускаем действие", sensorId);
                continue;
            }

            Action action = Action.builder()
                    .type(ActionType.valueOf(actionAvro.getType().name()))
                    .value(actionAvro.getValue())
                    .build();

            action = actionRepository.save(action);

            ScenarioAction sa = ScenarioAction.builder()
                    .scenario(scenario)
                    .action(action)
                    .sensor(sensorOpt.get())
                    .build();

            sa.setId(new ScenarioAction.ScenarioActionId(
                    scenario.getId(),
                    sensorId,
                    action.getId()
            ));

            scenarioActionRepository.save(sa);
        }

        log.info("Сценарий {} добавлен для хаба {}", event.getName(), hubId);
    }

    @Transactional
    public void processScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String name = event.getName().toString();
        Optional<Scenario> existing = scenarioRepository.findByHubIdAndName(hubId, name);

        if (existing.isPresent()) {
            scenarioRepository.delete(existing.get());
            log.info("Сценарий {} удалён для хаба {}", name, hubId);
        } else {
            log.warn("Сценарий {} не найден для хаба {}", name, hubId);
        }
    }
}
