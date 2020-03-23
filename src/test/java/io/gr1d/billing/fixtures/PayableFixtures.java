package io.gr1d.billing.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.billing.fixtures.functions.RandomUuid;
import io.gr1d.billing.model.enumerations.PayableStatus;
import io.gr1d.billing.model.invoice.InvoiceItem;
import io.gr1d.billing.model.transfer.Payable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayableFixtures implements TemplateLoader {

	@Override
	public void load() {
		Fixture.of(InvoiceItem.class).addTemplate("IIPAY1", new Rule() {
			{
				add("id", random(Long.class, range(1L, 200L)));
				add("uuid", new RandomUuid("II-"));
				add("createdAt", LocalDateTime.now());
				add("updatedAt", null);

				add("externalId", new RandomUuid("INVOICE_ITEM_EXTERNAL_ID-"));
				add("description", "Item description");
				add("quantity", 1);
                add("hits", 10L);
				add("unitValue", BigDecimal.valueOf(100.00));
                add("apiUuid", new RandomUuid("API-b09b2bd1-7f62-40fe-b2da-1c4fae5bf915"));
                add("providerUuid", "PAR-97d75353-3790-45c1-992f-3e9216b269ec");
			}
		});
		
		Fixture.of(InvoiceItem.class).addTemplate("IIPAY2", new Rule() {
			{
				add("id", random(Long.class, range(1L, 200L)));
				add("uuid", new RandomUuid("II-"));
				add("createdAt", LocalDateTime.now());
				add("updatedAt", null);

				add("externalId", new RandomUuid("INVOICE_ITEM_EXTERNAL_ID-"));
				add("description", "Item description");
				add("quantity", 5);
                add("hits", 10L);
                add("endpoint", "/test2");
				add("unitValue", BigDecimal.valueOf(50.00));
                add("apiUuid", "API-b09b2bd1-7f62-40fe-b2da-1c4fae5bf915");
                add("providerUuid", "PAR-97d75353-3790-45c1-992f-3e9216b269ec");
			}
		});

	}
	
}
