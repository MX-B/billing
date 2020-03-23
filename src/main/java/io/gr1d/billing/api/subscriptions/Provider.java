package io.gr1d.billing.api.subscriptions;

import io.gr1d.billing.api.recipients.Recipient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provider {

    private String uuid;
    private String name;
    private String walletId;
    private String phone;
    private String email;

    private Recipient wallet;

}
