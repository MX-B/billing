INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (10, 'CANCELED', false);

ALTER TABLE `invoice` ADD `cancel_reason` VARCHAR(512) NULL;
ALTER TABLE `invoice` ADD `cancel_user_id` VARCHAR(64) NULL;
