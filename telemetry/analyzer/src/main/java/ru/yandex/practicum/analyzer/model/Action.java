package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.analyzer.model.enums.ActionType;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "actions")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType type;

    @Column
    private Integer value;
}
