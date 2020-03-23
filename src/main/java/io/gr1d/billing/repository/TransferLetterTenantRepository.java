package io.gr1d.billing.repository;

import io.gr1d.billing.model.transfer.TransferLetterTenant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferLetterTenantRepository extends CrudRepository<TransferLetterTenant, Long> {

    @Query("SELECT tlt FROM TransferLetterTenant tlt " +
           "WHERE tlt.transferLetter.uuid = :transferLetterUuid " +
           "ORDER BY tlt.status.id ASC, tlt.tenantRealm ASC")
    List<TransferLetterTenant> findByTransferLetterUuid(@Param("transferLetterUuid") String transferLetterUuid);

    TransferLetterTenant findByTransferLetterUuidAndTenantRealm(String transferLetterUuid, String tenantRealm);

}
