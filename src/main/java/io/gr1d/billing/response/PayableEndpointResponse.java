package io.gr1d.billing.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PayableEndpointResponse {

    private String endpoint;
    private BigDecimal unitValue;
    private Long hits;
    private Long quantity;

}
