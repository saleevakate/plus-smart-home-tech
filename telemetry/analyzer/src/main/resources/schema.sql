CREATE TABLE sensors (
    id VARCHAR(255) PRIMARY KEY,
    hub_id VARCHAR(255) NOT NULL
);

CREATE TABLE scenarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hub_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    UNIQUE(hub_id, name)
);

CREATE TABLE conditions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    value INTEGER NOT NULL
);

CREATE TABLE actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    value INTEGER
);

CREATE TABLE scenario_conditions (
    scenario_id BIGINT NOT NULL REFERENCES scenarios(id),
    sensor_id VARCHAR(255) NOT NULL REFERENCES sensors(id),
    condition_id BIGINT NOT NULL REFERENCES conditions(id),
    PRIMARY KEY (scenario_id, sensor_id, condition_id)
);

CREATE TABLE scenario_actions (
    scenario_id BIGINT NOT NULL REFERENCES scenarios(id),
    sensor_id VARCHAR(255) NOT NULL REFERENCES sensors(id),
    action_id BIGINT NOT NULL REFERENCES actions(id),
    PRIMARY KEY (scenario_id, sensor_id, action_id)
);