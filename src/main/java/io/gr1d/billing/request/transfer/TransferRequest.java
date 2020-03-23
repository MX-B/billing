package io.gr1d.billing.request.transfer;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@ToString
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotEmpty
    private String providerUuid;

    @Size
    @Valid
    @NotNull
    private List<TransferPayableRequest> transfers;

}
