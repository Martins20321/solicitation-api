CREATE TABLE tb_analyst_coverage
(
    id      BIGSERIAL     NOT NULL,
    user_id BIGINT UNIQUE NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES tb_users (id)
);

CREATE TABLE tb_analyst_coverage_states
(
    analyst_coverage_id BIGINT  NOT NULL,
    states              varchar(2) NOT NULL,

    PRIMARY KEY (analyst_coverage_id, states),
    FOREIGN KEY (analyst_coverage_id) REFERENCES tb_analyst_coverage (id)
);