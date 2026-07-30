package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "scenario_actions")
public class ScenarioAction {

    @EmbeddedId
    private ScenarioActionId id;

    @ManyToOne
    @MapsId("scenarioId")
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @ManyToOne
    @MapsId("actionId")
    @JoinColumn(name = "action_id")
    private Action action;

    @ManyToOne
    @MapsId("sensorId")
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioActionId implements Serializable {
        private Long scenarioId;
        private String sensorId;
        private Long actionId;
    }
}
