package ru.yandex.practicum.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.yandex.practicum.collector.model.enums.ConditionOperation;
import ru.yandex.practicum.collector.model.enums.ConditionType;

@Data
public class ScenarioCondition {
    @NotBlank
    private String sensorId;

    @NotNull
    private ConditionType type;

    @NotNull
    private ConditionOperation operation;

    private Integer value;
}
