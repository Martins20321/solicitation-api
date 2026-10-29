CREATE TABLE tb_users
(
    id            BIGSERIAL    NOT NULL,
    name          varchar(250) NOT NULL,
    email         varchar(80)  UNIQUE NOT NULL,
    password_hash varchar(80)  NOT NULL,
    role          varchar(50)  NOT NULL,
    enabled       BOOLEAN  NOT NULL,
    created_at    timestamp    NOT NULL,
    PRIMARY KEY (id)
)