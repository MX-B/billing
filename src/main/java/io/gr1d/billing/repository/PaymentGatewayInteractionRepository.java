package io.gr1d.billing.repository;

import io.gr1d.billing.model.invoice.PaymentGatewayInteraction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentGatewayInteractionRepository extends CrudRepository<PaymentGatewayInteraction, Long> {

}
