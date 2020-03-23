CREATE TABLE `transfer_letter_provider_data`
(
  `id`                          BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `transfer_letter_provider_id` BIGINT(20)   NOT NULL,
  `name`                        varchar(128) NULL,
  `document_type`               varchar(128) NULL,
  `document_number`             varchar(128) NULL,
  `phone`                       varchar(128) NULL,
  `email`                       varchar(128) NULL,
  `bank`                        varchar(128) NULL,
  `agency`                      varchar(128) NULL,
  `account`                     varchar(128) NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_letter_provider_data___letter_provider` FOREIGN KEY (`transfer_letter_provider_id`) REFERENCES `transfer_letter_provider` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = UTF8MB4;
