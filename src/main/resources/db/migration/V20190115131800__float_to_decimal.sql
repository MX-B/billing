alter table `invoice` modify `value` decimal(18,2) not null;

alter table `invoice_item` modify `unit_value` decimal(18,6) not null;

