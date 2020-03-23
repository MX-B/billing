package io.gr1d.billing.repository;

import io.gr1d.billing.model.invoice.InvoicePaymentStatusHistory;
import org.springframework.data.repository.CrudRepository;

public interface InvoiceStatusHistoryRepository extends CrudRepository<InvoicePaymentStatusHistory, Long> {

}
