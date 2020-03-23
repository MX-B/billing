package io.gr1d.billing.repository;

import io.gr1d.billing.model.User;
import io.gr1d.billing.model.enumerations.PaymentStatus;
import io.gr1d.billing.model.invoice.Invoice;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends CrudRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByUuid(String uuid);

    Optional<Invoice> findByUuidAndRemovedAtIsNull(String uuid);

    Optional<Invoice> findByUuidAndPaymentStatusAndRemovedAtIsNull(String uuid, PaymentStatus paymentStatus);

    @Query("SELECT invoice " +
           "FROM Invoice invoice " +
           "WHERE invoice.transferLetter IS NULL " +
           "  AND invoice.removedAt IS NULL " +
           "  AND invoice.paymentStatus = :paymentStatus" +
           "  AND invoice.paymentDate <= :endDate")
    List<Invoice> findInvoicesToTransferLetter(@Param("paymentStatus") PaymentStatus paymentStatus,
                                               @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("UPDATE Invoice invoice SET invoice.transferLetter.id = :transferLetterId WHERE invoice.id IN :invoicesId")
    void updateInvoicesToTransferLetter(@Param("transferLetterId") Long transferLetterId,
                                        @Param("invoicesId") List<Long> invoicesId);

    @Query("SELECT i FROM Invoice i " +
           "WHERE i.scheduledChargeTime = :scheduledChargeTime " +
           "  AND i.removedAt IS NULL " +
           "  AND i.chargeTries < :maxTries " +
           "  AND i.paymentStatus.chargeable = true")
    List<Invoice> findInvoicesToCharge(@Param("scheduledChargeTime") LocalDate scheduledChargeTime,
                                       @Param("maxTries") int maxTries);

    @Query("SELECT Count(i) FROM Invoice i WHERE i.periodStart <= :date AND i.periodEnd >= :date AND i.user = :user")
    Integer countInvoicesByPeriod(@Param("date") LocalDate date, @Param("user") User user);


    @Modifying
    @Query("UPDATE Invoice invoice SET invoice.paymentStatus = 11 WHERE invoice.settlementDate <= :date AND invoice.paymentStatus = 7")
    void invoiceSettlementUpdate(@Param("date") LocalDate date);

}
