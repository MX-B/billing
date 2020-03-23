package io.gr1d.billing.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class ToStringConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void setToStringObjectMapper() {
        ToString.init(objectMapper);
    }

}
