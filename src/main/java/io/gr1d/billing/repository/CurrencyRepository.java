package io.gr1d.billing.repository;

import io.gr1d.billing.model.Currency;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends CrudRepository<Currency, Long>, JpaSpecificationExecutor<Currency> {

}
