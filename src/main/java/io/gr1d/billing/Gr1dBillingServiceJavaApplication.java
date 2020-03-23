package io.gr1d.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
@ComponentScan("io.gr1d")
@EnableFeignClients("io.gr1d.billing.api")
public class Gr1dBillingServiceJavaApplication {

    public static void main(final String[] args) {
        SpringApplication.run(Gr1dBillingServiceJavaApplication.class, args);
    }

}
