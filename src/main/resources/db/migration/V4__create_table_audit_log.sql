CREATE TABLE tb_audit_logs
(
    id            BIGSERIAL    NOT NULL,
    action        varchar(255) NOT NULL,
    user_email    varchar(80)  NOT NULL,
    user_role     varchar(50)  NOT NULL,
    duration_ms   BIGINT       NOT NULL,
    error_message TEXT,
    status        varchar(50)      NOT NULL,
    created_at    timestamp    NOT NULL,
    PRIMARY KEY (id)
)