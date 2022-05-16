CREATE TABLE wb_dispatch.dead_webhooks
(
    id                 CHARACTER VARYING(64)       NOT NULL,
    webhook_id         BIGINT                      NOT NULL,
    source_id          CHARACTER VARYING           NOT NULL,
    event_id           BIGINT                      NOT NULL,
    parent_event_id    BIGINT                      NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    url                CHARACTER VARYING           NOT NULL,
    content_type       CHARACTER VARYING           NOT NULL,
    additional_headers CHARACTER VARYING           NOT NULL,
    request_body       BYTEA                       NOT NULL,
    retry_count        BIGINT,
    CONSTRAINT pk_dead_webhooks PRIMARY KEY (id)
);
