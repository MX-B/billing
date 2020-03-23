package io.gr1d.billing.service;

import io.gr1d.billing.api.recipients.RecipientsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class RecipientService {

    private final RecipientsApi recipientsApi;

    @Autowired
    public RecipientService(final RecipientsApi recipientsApi) {
        this.recipientsApi = recipientsApi;
    }

    @Cacheable("recipient_metadata")
    public Map<String, String> getRecipientMetadata(final String recipientUuid) {
        log.debug("Recipient UUID: {}", recipientUuid);
        return recipientsApi.getRecipientData(recipientUuid).getMetadata();
    }

}
