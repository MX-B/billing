package io.gr1d.billing.request.invoice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@ToString
@Getter@Setter
public class InvoiceCancelRequest {

    @NotEmpty
    @Size(min = 1, max = 512)
    private String cancelReason;

}
