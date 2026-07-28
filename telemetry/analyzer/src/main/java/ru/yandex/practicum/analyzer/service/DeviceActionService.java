package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceActionService {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterStub;

    public void executeActions(Scenario scenario, List<ScenarioAction> scenarioActions) {
        for (ScenarioAction sa : scenarioActions) {
            Action action = sa.getAction();
            String sensorId = sa.getSensor().getId();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(scenario.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(DeviceActionProto.newBuilder()
                            .setSensorId(sensorId)
                            .setType(ActionTypeProto.valueOf(action.getType().name()))
                            .setValue(action.getValue() != null ? action.getValue() : 0)
                            .build())
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            try {
                hubRouterStub.handleDeviceAction(request);
                log.info("Действие {} для датчика {} выполнено", action.getType(), sensorId);
            } catch (Exception e) {
                log.error("Ошибка выполнения действия для датчика {}", sensorId, e);
            }
        }
    }
}
