CREATE TABLE `transfer_letter_tenant` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `uuid` VARCHAR(42) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME,
  `removed_at` DATETIME,

  `transfer_letter_id` BIGINT(20) NOT NULL,
  `tenant_realm` varchar(128) NOT NULL,
  `total_value` decimal(18,2) NOT NULL,
  `transfered_value` decimal(18,2) NOT NULL,
  `status_id` INTEGER(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___transfer_letter_tenant` (`uuid`),
  CONSTRAINT `FK_transfer_letter_tenant___transfer_letter` FOREIGN KEY (`transfer_letter_id`) REFERENCES `transfer_letter` (`id`),
  CONSTRAINT `FK_transfer_letter_tenant___status` FOREIGN KEY (`status_id`) REFERENCES `payable_status` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `payable_status` (`id`, `name`) VALUES (4, 'PAID_AUTOMATICALLY');

ALTER TABLE `transfer_letter_provider`
ADD `transfered_value_auto` decimal(18,2) NOT NULL DEFAULT 0.00 AFTER `transfered_value`;