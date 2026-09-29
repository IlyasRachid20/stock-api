-- PostgreSQL doesn't index foreign keys automatically. These columns are used to
-- filter lists (?customerId=, ?saleId=) and to check for sales before a delete.
create index idx_sales_customer_id on sales (customer_id);
create index idx_sale_items_sale_id on sale_items (sale_id);
create index idx_sale_items_product_id on sale_items (product_id);
