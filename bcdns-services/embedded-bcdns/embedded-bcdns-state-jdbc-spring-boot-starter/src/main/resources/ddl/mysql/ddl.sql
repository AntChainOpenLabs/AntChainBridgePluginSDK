/*
 * Copyright 2024 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

DROP TABLE IF EXISTS `embedded_bcdns_cert_application`;
CREATE TABLE IF NOT EXISTS `embedded_bcdns_cert_application`
(
    `id`           INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `cert_type`    VARCHAR(128)        NOT NULL,
    `raw_csr`      LONGBLOB            NOT NULL,
    `receipt`      VARCHAR(128)        NOT NULL,
    `state`        VARCHAR(128)        NOT NULL,
    `gmt_create`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `ebcdns_application_receipt_uindex` (`receipt`)
);

DROP TABLE IF EXISTS `embedded_bcdns_domain_cert`;
CREATE TABLE IF NOT EXISTS `embedded_bcdns_domain_cert`
(
    `id`           INT(11)             NOT NULL AUTO_INCREMENT,
    `domain`       VARCHAR(128) BINARY NOT NULL,
    `domain_space` VARCHAR(128) BINARY DEFAULT NULL,
    `subject_oid`  BLOB                DEFAULT NULL,
    `issuer_oid`   BLOB                DEFAULT NULL,
    `domain_cert`  LONGBLOB,
    `gmt_create`   DATETIME            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ebcdns_domain_cert_domain_uindex` (`domain`)
);

DROP TABLE IF EXISTS `embedded_bcdns_domain_space_cert`;
CREATE TABLE `embedded_bcdns_domain_space_cert`
(
    `id`                INT(11)      NOT NULL AUTO_INCREMENT,
    `domain_space`      VARCHAR(128) BINARY DEFAULT NULL,
    `parent_space`      VARCHAR(128) BINARY DEFAULT NULL,
    `owner_oid_hex`     VARCHAR(255) NOT NULL,
    `description`       VARCHAR(128)        DEFAULT NULL,
    `domain_space_cert` LONGBLOB,
    `gmt_create`        DATETIME            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`      DATETIME            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ebcdns_domain_space_uindex` (`domain_space`)
);
CREATE INDEX ebcdns_domain_space_cert_owner_oid_hex_uindex
    ON embedded_bcdns_domain_space_cert (owner_oid_hex);

DROP TABLE IF EXISTS `embedded_bcdns_relayer_cert`;
CREATE TABLE IF NOT EXISTS `embedded_bcdns_relayer_cert`
(
    `id`           INT(11)             NOT NULL AUTO_INCREMENT,
    `cert_id`      VARCHAR(128) BINARY NOT NULL,
    `subject_oid`  BLOB     DEFAULT NULL,
    `issuer_oid`   BLOB     DEFAULT NULL,
    `raw_cert`     LONGBLOB,
    `gmt_create`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ebcdns_relayer_uindex` (`cert_id`)
);

DROP TABLE IF EXISTS `embedded_bcdns_ptc_cert`;
CREATE TABLE IF NOT EXISTS `embedded_bcdns_ptc_cert`
(
    `id`           INT(11)             NOT NULL AUTO_INCREMENT,
    `cert_id`      VARCHAR(128) BINARY NOT NULL,
    `subject_oid`  BLOB     DEFAULT NULL,
    `issuer_oid`   BLOB     DEFAULT NULL,
    `raw_cert`     LONGBLOB,
    `gmt_create`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ebcdns_ptc_uindex` (`cert_id`)
);

DROP TABLE IF EXISTS embedded_bcdns_domain_router;
CREATE TABLE `embedded_bcdns_domain_router`
(
    `id`               INT(11)             NOT NULL AUTO_INCREMENT,
    `dest_domain`      VARCHAR(128) BINARY DEFAULT NULL,
    `relayer_cert_id`  VARCHAR(128) BINARY NOT NULL,
    `relayer_cert`     LONGBLOB            NOT NULL,
    `net_address_list` VARCHAR(255) BINARY NOT NULL,
    `gmt_create`       DATETIME            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`     DATETIME            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ebcdns_domain_router_dest_domain` (`dest_domain`)
);
