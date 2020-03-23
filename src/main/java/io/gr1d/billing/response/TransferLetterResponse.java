package io.gr1d.billing.response;

import io.gr1d.billing.model.transfer.TransferLetter;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class TransferLetterResponse {

    private final String uuid;
    private final String status;
    private final LocalDate startDate;
    private final LocalDate finishDate;
    private final BigDecimal totalValue;
    private final BigDecimal transferedValue;
    private final Integer providerCount;
    private final Integer providerTransfered;

    private final LocalDateTime createdAt;

    public TransferLetterResponse(final TransferLetter transferLetter) {
        createdAt = transferLetter.getCreatedAt();
        uuid = transferLetter.getUuid();
        status = transferLetter.getStatus().getName();
        startDate = transferLetter.getStartDate();
        finishDate = transferLetter.getFinishDate();
        totalValue = transferLetter.getTotalValue();
        transferedValue = transferLetter.getTransferedValue();
        providerCount = transferLetter.getProviderCount();
        providerTransfered = transferLetter.getProviderTransfered();
    }
}
