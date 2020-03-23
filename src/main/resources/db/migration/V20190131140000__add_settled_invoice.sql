INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (11, 'SETTLED', false);

ALTER TABLE `invoice` ADD `settlement_date` datetime NULL;
