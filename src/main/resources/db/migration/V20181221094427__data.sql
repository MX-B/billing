INSERT INTO `payable_status` (`id`, `name`) VALUES (1, 'PENDING');
INSERT INTO `payable_status` (`id`, `name`) VALUES (2, 'PAID');

INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (1, 'CREATED', 1);
INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (2, 'PROCESSING', 0);
INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (3, 'PROCESSING_RETRY', 0);
INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (4, 'REFUNDING', 0);
INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (5, 'REFUNDED', 0);
INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (6, 'FAILED', 1);
INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (7, 'SUCCESS', 0);
INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (8, 'NO_CARD', 1);
INSERT INTO `payment_status` (`id`, `name`, `chargeable`) VALUES (9, 'FAILED_COMMUNICATION', 1);

INSERT INTO `user_status` (`id`, `name`) VALUES (1, 'ACTIVE');
INSERT INTO `user_status` (`id`, `name`) VALUES (2, 'BLOCKED');