CREATE TABLE `payable_status` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_name___payable_status` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `transfer_letter` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `uuid` VARCHAR(42) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME,
  `removed_at` DATETIME,

  `total_value` float(18,2) NOT NULL,
  `transfered_value` float(18,2) NOT NULL,
  `start_dt` DATE NOT NULL,
  `finish_dt` DATE NOT NULL,
  `status_id` INTEGER(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___transfer_letter` (`uuid`),
  CONSTRAINT `FK_transfer_letter___status` FOREIGN KEY (`status_id`) REFERENCES `payable_status` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `payable` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `uuid` VARCHAR(42) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME,
  `removed_at` DATETIME,

  `gateway_transaction_id` varchar(64) NULL,
  `transfer_letter_id` BIGINT(20) NOT NULL,
  `total_value` float(18,2) NOT NULL,
  `transfered_value` float(18,2) NOT NULL,
  `provider_uuid` varchar(128) NOT NULL,
  `api_uuid` varchar(128) NOT NULL,
  `payable_status_id` INTEGER(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___payable` (`uuid`),
  CONSTRAINT `FK_payable___payable_status` FOREIGN KEY (`payable_status_id`) REFERENCES `payable_status` (`id`),
  CONSTRAINT `FK_payable___transfer_letter` FOREIGN KEY (`transfer_letter_id`) REFERENCES `transfer_letter` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `payable_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `invoice_item_id` bigint(20) NOT NULL,
  `payable_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_payable_item___payable_item` (`invoice_item_id`,`payable_id`),
  CONSTRAINT `FK_payable_item___payable` FOREIGN KEY (`payable_id`) REFERENCES `payable` (`id`),
  CONSTRAINT `FK_payable_item___invoice_item` FOREIGN KEY (`invoice_item_id`) REFERENCES `invoice_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;