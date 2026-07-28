package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;

import java.util.List;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioCondition.ScenarioConditionId> {

    @Query("SELECT sc.sensor.id FROM ScenarioCondition sc WHERE sc.scenario.id = :scenarioId")
    List<String> findSensorIdsByScenarioId(@Param("scenarioId") Long scenarioId);

    List<ScenarioCondition> findByScenarioId(Long scenarioId);
}
