-- Store sale dates as absolute instants (timestamp with time zone) instead of a local
-- date-time with no zone. Existing values are read in the database session's time zone,
-- which is the time zone the app wrote them in, so they keep the same meaning.
alter table sales alter column sale_date set data type timestamp(6) with time zone;
