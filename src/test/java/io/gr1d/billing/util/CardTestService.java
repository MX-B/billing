package io.gr1d.billing.util;

import io.gr1d.billing.model.Card;
import io.gr1d.billing.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardTestService {

    private final CardRepository cardRepository;

    @Autowired
    public CardTestService(final CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Transactional
    public void save(final Card card) {
        cardRepository.save(card);
    }

}
