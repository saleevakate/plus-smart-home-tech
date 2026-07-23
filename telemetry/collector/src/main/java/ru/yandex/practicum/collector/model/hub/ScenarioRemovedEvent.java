package ru.yandex.practicum.collector.model.hub;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.yandex.practicum.collector.model.enums.HubEventType;

@Data
public class ScenarioRemovedEvent extends HubEvent {

    @NotBlank
    @Size(min = 3)
    private String name;

    @NotNull
    @JsonProperty("type")
    private HubEventType type = HubEventType.SCENARIO_REMOVED;

    @Override
    public HubEventType getType() {
        return type;
    }
}
