-- Whether the product is shown in the online shop. A hidden product is still sold at the counter.
alter table products add column published boolean default true not null;
