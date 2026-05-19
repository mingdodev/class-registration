CREATE TABLE IF NOT EXISTS creators
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(10)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_creator_email (email)
);

CREATE TABLE IF NOT EXISTS klassmates
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(10)  NOT NULL,
    email        VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20)  NOT NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_klassmate_email (email)
);

CREATE TABLE IF NOT EXISTS klasses
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    creator_id         BIGINT      NOT NULL,
    title              VARCHAR(20) NOT NULL,
    description        TEXT,
    price              INT         NOT NULL,
    status             VARCHAR(20) NOT NULL,
    max_capacity       INT         NOT NULL,
    remaining_capacity INT         NOT NULL,
    start_date         DATE,
    end_date           DATE,
    created_at         DATETIME    NOT NULL,
    updated_at         DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_klass_creator FOREIGN KEY (creator_id) REFERENCES creators (id)
);

CREATE TABLE IF NOT EXISTS enrollments
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    klassmate_id      BIGINT       NOT NULL,
    klass_id          BIGINT       NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    cancel_reason     VARCHAR(30),
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NOT NULL,
    active_unique_key VARCHAR(100) GENERATED ALWAYS AS (
        IF(status != 'CANCELLED', CONCAT(klassmate_id, '-', klass_id), NULL)
        ) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_active_enrollment (active_unique_key),
    CONSTRAINT fk_enrollment_klassmate FOREIGN KEY (klassmate_id) REFERENCES klassmates (id),
    CONSTRAINT fk_enrollment_klass FOREIGN KEY (klass_id) REFERENCES klasses (id)
);

CREATE TABLE IF NOT EXISTS waitlists
(
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    klassmate_id BIGINT   NOT NULL,
    klass_id     BIGINT   NOT NULL,
    created_at   DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_waitlist (klassmate_id, klass_id),
    KEY idx_waitlist_klass_created (klass_id, created_at),
    CONSTRAINT fk_waitlist_klassmate FOREIGN KEY (klassmate_id) REFERENCES klassmates (id),
    CONSTRAINT fk_waitlist_klass FOREIGN KEY (klass_id) REFERENCES klasses (id)
);
