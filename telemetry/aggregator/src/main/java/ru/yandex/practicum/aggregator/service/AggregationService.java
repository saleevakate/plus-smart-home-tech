package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AggregationService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();

        SensorsSnapshotAvro snapshot = snapshots.get(hubId);

        if (snapshot == null) {
            snapshot = createNewSnapshot(hubId, event);
            snapshots.put(hubId, snapshot);
            log.info("Создан новый снапшот для хаба: {}", hubId);
            return Optional.of(snapshot);
        }

        SensorStateAvro existingState = snapshot.getSensorsState().get(sensorId);

        if (existingState != null) {
            long existingTimestamp = existingState.getTimestamp();
            long eventTimestamp = event.getTimestamp();

            if (existingTimestamp > eventTimestamp) {
                log.debug("Пропуск устаревшего события для датчика {}: существующий timestamp={}, новый={}",
                        sensorId, existingTimestamp, eventTimestamp);
                return Optional.empty();
            }
        }

        SensorStateAvro newState = createSensorState(event);
        snapshot.getSensorsState().put(sensorId, newState);
        snapshot.setTimestamp(event.getTimestamp());

        log.info("Обновлён снапшот для хаба {}: датчик {} обновлён (timestamp={})",
                hubId, sensorId, event.getTimestamp());
        return Optional.of(snapshot);
    }

    private SensorsSnapshotAvro createNewSnapshot(String hubId, SensorEventAvro event) {
        SensorsSnapshotAvro snapshot = new SensorsSnapshotAvro();
        snapshot.setHubId(hubId);
        snapshot.setTimestamp(event.getTimestamp());
        snapshot.setSensorsState(new java.util.HashMap<>());

        SensorStateAvro state = createSensorState(event);
        snapshot.getSensorsState().put(event.getId(), state);

        return snapshot;
    }

    private SensorStateAvro createSensorState(SensorEventAvro event) {
        SensorStateAvro state = new SensorStateAvro();
        state.setTimestamp(event.getTimestamp());
        state.setData(event.getPayload());
        return state;
    }

    public SensorsSnapshotAvro getSnapshot(String hubId) {
        return snapshots.get(hubId);
    }
}
