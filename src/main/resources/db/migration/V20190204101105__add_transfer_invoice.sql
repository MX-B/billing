ALTER TABLE `invoice` ADD `transfer_letter_id` BIGINT(20) NULL;

ALTER TABLE `invoice` ADD CONSTRAINT `FK_invoice___transfer_letter`
FOREIGN KEY (`transfer_letter_id`) REFERENCES `transfer_letter` (`id`);