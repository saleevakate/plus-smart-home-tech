package ru.yandex.practicum.collector.model.hub;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.yandex.practicum.collector.model.enums.HubEventType;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceRemovedEvent extends HubEvent {

    @NotBlank
    private String id;

    @NotNull
    @JsonProperty("type")
    private HubEventType type = HubEventType.DEVICE_REMOVED;

    @Override
    public HubEventType getType() {
        return type;
    }

}
