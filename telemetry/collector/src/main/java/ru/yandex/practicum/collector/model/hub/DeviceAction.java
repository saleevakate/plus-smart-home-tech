package ru.yandex.practicum.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.yandex.practicum.collector.model.enums.ActionType;

@Data
public class DeviceAction {
    @NotBlank
    private String sensorId;

    @NotNull
    private ActionType type;

    private Integer value;
}
