CREATE TABLE IF NOT EXISTS `${tableName}` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `type`           VARCHAR(64)  DEFAULT NULL,
    `category`       VARCHAR(64)  DEFAULT NULL,
    `biz_no`         VARCHAR(128) NOT NULL,
    `success`        TINYINT(1)   NOT NULL DEFAULT 1,
    `content`        VARCHAR(2000) NOT NULL,
    `detail`         TEXT,
    `operator_id`    VARCHAR(64)  DEFAULT NULL,
    `operator_name`  VARCHAR(128) DEFAULT NULL,
    `error_msg`      VARCHAR(2000) DEFAULT NULL,
    `method`         VARCHAR(255) DEFAULT NULL,
    `request_ip`     VARCHAR(64)  DEFAULT NULL,
    `user_agent`     VARCHAR(512) DEFAULT NULL,
    `request_params` TEXT,
    `created_at`     TIMESTAMP(3)  NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS idx_biz_no_created_at ON `${tableName}` (`biz_no`, `created_at`);
CREATE INDEX IF NOT EXISTS idx_type_category_created_at ON `${tableName}` (`type`, `category`, `created_at`);
CREATE INDEX IF NOT EXISTS idx_operator_created_at ON `${tableName}` (`operator_id`, `created_at`);
CREATE INDEX IF NOT EXISTS idx_created_at ON `${tableName}` (`created_at`);
