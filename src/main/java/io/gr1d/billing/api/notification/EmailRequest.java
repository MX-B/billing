package io.gr1d.billing.api.notification;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.*;

@Getter@Setter
public class EmailRequest implements Serializable {

    private String locale;
    private String tenantRealm;
    private String to;

    private String subject;
    private String template;
    private Map<String, Object> context = new HashMap<>();
    private List<EmailAttachmentRequest> attachments = new ArrayList<>();

    public void setLocale(final String locale) {
        this.locale = locale.replaceAll("_", "-").toUpperCase();
    }

    public EmailRequest addAttachment(final String id, final byte[] fileData, final String mimeType, final String fileName) {
        final String base64 = Base64.getEncoder().encodeToString(fileData);
        this.attachments.add(new EmailAttachmentRequest(id, base64, mimeType, fileName));
        return this;
    }

    public EmailRequest add(final String key, final Object value) {
        context.put(key, value);
        return this;
    }
}
