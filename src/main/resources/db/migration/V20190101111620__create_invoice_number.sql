ALTER TABLE `invoice` ADD `invoice_number` VARCHAR(32) NOT NULL AFTER `removed_at`;
ALTER TABLE `invoice` ADD UNIQUE KEY `UK_invoice_number___invoice` (`invoice_number`);