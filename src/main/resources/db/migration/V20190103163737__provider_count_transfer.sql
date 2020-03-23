ALTER TABLE `transfer_letter` ADD `provider_count` INT UNSIGNED NOT NULL AFTER `transfered_value`;
ALTER TABLE `transfer_letter` ADD `provider_transfered` INT UNSIGNED NOT NULL AFTER `provider_count`;

CREATE TABLE `transfer_letter_provider` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `uuid` VARCHAR(42) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME,
  `removed_at` DATETIME,

  `transfer_letter_id` BIGINT(20) NOT NULL,
  `gateway_transaction_id` varchar(64) NULL,
  `provider_uuid` varchar(128) NOT NULL,
  `total_value` float(18,2) NOT NULL,
  `transfered_value` float(18,2) NOT NULL,
  `status_id` INTEGER(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___transfer_letter_provider` (`uuid`),
  CONSTRAINT `FK_transfer_letter_provider___transfer_letter` FOREIGN KEY (`transfer_letter_id`) REFERENCES `transfer_letter` (`id`),
  CONSTRAINT `FK_transfer_letter_provider___status` FOREIGN KEY (`status_id`) REFERENCES `payable_status` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `payable` ADD `transfer_letter_provider_id` BIGINT(20) NOT NULL AFTER `transfer_letter_id`;

ALTER TABLE `payable`
ADD CONSTRAINT `FK_payable___transfer_letter_provider`
FOREIGN KEY (`transfer_letter_provider_id`)
REFERENCES `transfer_letter_provider` (`id`);

DELETE FROM `payable_status`;
INSERT INTO `payable_status` (`id`, `name`) VALUES (1, 'CREATED');
INSERT INTO `payable_status` (`id`, `name`) VALUES (2, 'IN_PROGRESS');
INSERT INTO `payable_status` (`id`, `name`) VALUES (3, 'PAID');

ALTER TABLE `payable` DROP COLUMN `provider_uuid`;
ALTER TABLE `payable` DROP COLUMN `gateway_transaction_id`;