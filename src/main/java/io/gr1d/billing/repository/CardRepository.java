package io.gr1d.billing.repository;

import io.gr1d.billing.model.Card;
import io.gr1d.billing.model.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends CrudRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByUserAndRemovedAtIsNull(User user);
}
