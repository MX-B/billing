package io.gr1d.billing.repository;

import io.gr1d.billing.model.enumerations.PayableStatus;
import io.gr1d.billing.model.transfer.Payable;
import io.gr1d.billing.model.transfer.TransferLetter;
import io.gr1d.billing.model.transfer.TransferLetterProvider;
import io.gr1d.billing.response.PayableEndpointResponse;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayableRepository extends CrudRepository<Payable, Long> {

    @Query("SELECT p FROM Payable p " +
           "JOIN p.transferLetterProvider tlp " +
           "WHERE tlp = :transferLetterProvider " +
           "ORDER BY p.payableStatus.id, p.totalValue DESC")
    List<Payable> findByTransferLetterProvider(@Param("transferLetterProvider") TransferLetterProvider transferLetterProvider);

    @Query("SELECT count(p) FROM Payable p WHERE p.transferLetter = :transferLetter AND p.payableStatus = :payableStatus")
    long countPayables(@Param("transferLetter") TransferLetter transferLetter,
                       @Param("payableStatus") PayableStatus payableStatus);

    @Query("SELECT new io.gr1d.billing.response.PayableEndpointResponse(item.endpoint, item.unitValue, SUM(item.hits), SUM(item.quantity))" +
           "FROM InvoiceItem item, Payable p " +
           "WHERE item MEMBER OF p.items " +
           "  AND p.uuid = :payableUuid " +
           "  AND item.endpoint IS NOT NULL " +
           "GROUP BY item.endpoint, item.unitValue")
    List<PayableEndpointResponse> listEndpoints(@Param("payableUuid") String payableUuid);

}
