package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.analyzer.model.enums.ConditionOperation;
import ru.yandex.practicum.analyzer.model.enums.ConditionType;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conditions")
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionOperation operation;

    @Column(nullable = false)
    private Integer value;
}
