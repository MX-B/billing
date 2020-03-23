package io.gr1d.billing.repository;

import io.gr1d.billing.model.transfer.TransferLetterProvider;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferLetterProviderRepository extends CrudRepository<TransferLetterProvider, Long> {

    @Query("SELECT tlp FROM TransferLetterProvider tlp " +
           "WHERE tlp.transferLetter.uuid = :transferLetterUuid " +
           "ORDER BY tlp.status.id ASC, tlp.totalValue DESC")
    List<TransferLetterProvider> findByTransferLetterUuid(@Param("transferLetterUuid") String transferLetterUuid);

    TransferLetterProvider findByTransferLetterUuidAndProviderUuid(String transferLetterUuid, String providerUuid);

}
