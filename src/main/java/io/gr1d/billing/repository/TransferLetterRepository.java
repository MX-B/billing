package io.gr1d.billing.repository;

import io.gr1d.billing.model.transfer.TransferLetter;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferLetterRepository extends CrudRepository<TransferLetter, Long>, JpaSpecificationExecutor<TransferLetter> {

    TransferLetter findByUuidAndRemovedAtIsNull(final String uuid);

    TransferLetter findFirstByOrderByCreatedAtDesc();

}
