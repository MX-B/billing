CREATE TABLE `notification`
(
  `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `template_name` VARCHAR(256) NOT NULL,
  `email`         BLOB,
  `tries`         BIGINT(20)   NOT NULL,

  PRIMARY KEY (`id`)
) DEFAULT CHARSET = UTF8MB4;