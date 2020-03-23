CREATE TABLE `user_status` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_name___user_status` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `payment_status` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  `chargeable` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_name___payment_status` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NULL,
  `removed_at` datetime NULL,
  `email` varchar(128) NOT NULL,
  `keycloak_id` varchar(64) DEFAULT NULL,
  `status_id` int(3) NOT NULL,
  `pending_sync` bit(1) DEFAULT b'0',
  `tenant_realm` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___user` (`uuid`),
  CONSTRAINT `FK_user___user_status` FOREIGN KEY (`status_id`) REFERENCES `user_status` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `card` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NULL,
  `removed_at` datetime NULL,
  `card_id` varchar(64) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `tenant_realm` varchar(42) DEFAULT NULL,
  `last_digits` varchar(4) NOT NULL,
  `card_holder_name` varchar(64) NOT NULL,
  `full_name` varchar(64) NOT NULL,
  `document_type` INT(3) NOT NULL,
  `document` VARCHAR(16) NOT NULL,
  `phone` VARCHAR(24) NOT NULL,
  `nationality` VARCHAR(4) NOT NULL,
  `birth_dt` DATE NOT NULL,
  `brand` INT(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___card` (`uuid`),
  CONSTRAINT `FK_card___user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `invoice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NULL,
  `removed_at` datetime NULL,
  `gateway_transaction_id` varchar(64) NULL,
  `value` float(18,2) NOT NULL,
  `payment_status_id` int(3) NOT NULL,
  `scheduled_charge_time` datetime NULL,
  `expiration_date` date NOT NULL,
  `charge_tries` int(11) NOT NULL DEFAULT '0',
  `user_id` bigint(20) NOT NULL,
  `card_id` bigint(20) NOT NULL,
  `period_start` datetime NOT NULL,
  `period_end` datetime NOT NULL,
  `tenant_realm` varchar(64) NOT NULL,
  `pdf_file_id` varchar(255) NULL,
  `payment_date` datetime NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___invoice` (`uuid`),
  CONSTRAINT `FK_invoice___payment_status` FOREIGN KEY (`payment_status_id`) REFERENCES `payment_status` (`id`),
  CONSTRAINT `FK_invoice___user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK_invoice___card` FOREIGN KEY (`card_id`) REFERENCES `card` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `invoice_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NULL,
  `removed_at` datetime NULL,
  `invoice_id` bigint(20) NOT NULL,
  `external_id` varchar(64) NOT NULL,
  `quantity` bigint(20) NOT NULL,
  `unit_value` float(18,6) NOT NULL,
  `description` varchar(256) NOT NULL,
  `endpoint` varchar(256) DEFAULT NULL,
  `api_uuid` varchar(64) NOT NULL,
  `hits` bigint(20) DEFAULT NULL,
  `plan_uuid` varchar(64) NOT NULL,
  `provider_uuid` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___invoice_item` (`uuid`),
  CONSTRAINT `FK_invoice_item___invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `invoice_payment_status_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint(20) NOT NULL,
  `previous_status_id` int(3) DEFAULT NULL,
  `status_id` int(3) NOT NULL,
  `card_id` bigint(20) NOT NULL,
  `gateway_status` varchar(256) DEFAULT NULL,
  `timestamp` datetime NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_invoice_payment_status_history___invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`id`),
  CONSTRAINT `FK_invoice_payment_status_history___card` FOREIGN KEY (`card_id`) REFERENCES `card` (`id`),
  CONSTRAINT `FK_invoice_payment_status_history___payment_status` FOREIGN KEY (`status_id`) REFERENCES `payment_status` (`id`),
  CONSTRAINT `FK_invoice_payment_status_history___previous_payment_status` FOREIGN KEY (`previous_status_id`) REFERENCES `payment_status` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `invoice_recipient_split` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(42) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NULL,
  `removed_at` datetime NULL,
  `invoice_id` bigint(20) NOT NULL,
  `recipient_uuid` varchar(64) NOT NULL,
  `value` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___invoice_recipient_split` (`uuid`),
  CONSTRAINT `FK_invoice_recipient_split___invoice` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
