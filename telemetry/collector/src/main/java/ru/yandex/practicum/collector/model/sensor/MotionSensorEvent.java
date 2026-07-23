package ru.yandex.practicum.collector.model.sensor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.yandex.practicum.collector.model.enums.SensorEventType;

@Data
@EqualsAndHashCode(callSuper = true)
public class MotionSensorEvent extends SensorEvent {
    private Integer linkQuality;
    private Boolean motion;
    private Integer voltage;

    @Override
    public SensorEventType getType() {
        return SensorEventType.MOTION_SENSOR_EVENT;
    }
}
