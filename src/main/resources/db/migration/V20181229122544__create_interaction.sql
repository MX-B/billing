CREATE TABLE `payment_gateway_interaction` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,

	`timestamp` DATETIME NOT NULL,
	`operation` VARCHAR(256) NOT NULL,
	`endpoint` VARCHAR(256),
	`request_id` VARCHAR(42),
	`invoice_id` BIGINT(20),
	`return_code` INTEGER(3),
	`method` VARCHAR(16),
	`payload` MEDIUMTEXT,

	PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;