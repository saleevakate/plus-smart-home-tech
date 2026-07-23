package ru.yandex.practicum.collector.model.hub;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.yandex.practicum.collector.model.enums.DeviceType;
import ru.yandex.practicum.collector.model.enums.HubEventType;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceAddedEvent extends HubEvent {
    @NotBlank
    private String hubId;

    private Instant timestamp = Instant.now();

    @NotBlank
    private String id;

    @NotNull
    private DeviceType deviceType;

    @NotNull
    @JsonProperty("type")
    private HubEventType type = HubEventType.DEVICE_ADDED;
}
