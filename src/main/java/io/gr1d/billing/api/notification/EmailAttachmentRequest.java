package io.gr1d.billing.api.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class EmailAttachmentRequest implements Serializable {

    private final String id;
    private final String base64;
    private final String mimeType;
    private final String fileName;

}
