package io.gr1d.billing.request.transfer;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@ToString
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferPayableRequest {

    @NotEmpty
    @Length(min = 1, max = 64)
    private String payableUuid;

    @NotNull
    @Max(10000000)
    @PositiveOrZero
    private BigDecimal transferValue;

}
