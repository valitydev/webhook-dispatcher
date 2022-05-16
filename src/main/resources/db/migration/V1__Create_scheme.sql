create schema if not exists wb_dispatch;

CREATE TABLE wb_dispatch.commit_log
(
    id            character varying(64)       NOT NULL,
    creation_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    CONSTRAINT pk_commit_log PRIMARY KEY (id)
);
