CREATE TABLE `address` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `uuid` VARCHAR(42) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME,
  `removed_at` DATETIME,

  `street` VARCHAR(256) NOT NULL,
  `complementary` VARCHAR(128) NULL,
  `street_number` VARCHAR(128) NOT NULL,
  `neighborhood` VARCHAR(128) NOT NULL,
  `city` VARCHAR(256) NOT NULL,
  `state` VARCHAR(128) NOT NULL,
  `zipcode` VARCHAR(32) NOT NULL,
  `country` VARCHAR(8) NOT NULL,

  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_uuid___address` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `card` ADD `address_id` BIGINT(20) NOT NULL;
ALTER TABLE `card` ADD CONSTRAINT `FK_card___address` FOREIGN KEY (`address_id`) REFERENCES `address` (`id`);