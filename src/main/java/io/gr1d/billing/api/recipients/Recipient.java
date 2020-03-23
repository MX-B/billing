package io.gr1d.billing.api.recipients;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Recipient
 * 
 * @author Sérgio Filho
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recipient {

	private String documentNumber;
	private String documentType;
	private String bankName;
	private String agency;
	private String bankAccount;

	private Map<String, String> metadata;

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(final Map<String, String> metadata) {
		this.metadata = metadata;
	}
}
