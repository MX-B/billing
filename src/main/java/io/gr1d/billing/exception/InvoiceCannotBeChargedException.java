package io.gr1d.billing.exception;

public class InvoiceCannotBeChargedException extends ChargeException {

	public InvoiceCannotBeChargedException() {
		super("io.gr1d.billing.invoiceCannotBeCharged");
	}
}