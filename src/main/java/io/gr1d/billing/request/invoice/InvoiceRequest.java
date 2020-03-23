package io.gr1d.billing.request.invoice;

import io.gr1d.core.validation.DateRange;
import io.gr1d.billing.validation.TenantExists;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@ToString
@Getter@Setter
@DateRange(startProperty = "periodStart", endProperty = "periodEnd")
public class InvoiceRequest {

    @NotEmpty
    @Size(min = 1, max = 42)
    private String userId;

    @FutureOrPresent
    private LocalDate chargeDate;

    @FutureOrPresent
    private LocalDate expirationDate;

    @Valid
    @NotEmpty
    @Size(max = 500)
    private List<InvoiceRequestItem> items;

    @Valid
    @Size(max = 100)
    private List<InvoiceRequestSplit> split;

    @NotNull
    @PastOrPresent
    private LocalDate periodStart;

    @NotNull
    @PastOrPresent
    private LocalDate periodEnd;

    @NotEmpty
    @TenantExists
    @Size(min = 1, max = 42)
    private String tenantRealm;

}
