create table IF NOT EXISTS crypto_price (usd_price decimal(19,4), id bigint not null auto_increment, price_timestamp datetime, crypto_symbol varchar(20), primary key (id));
create index crypto_symbol_index on crypto_price (crypto_symbol);
create index price_timestamp_index on crypto_price (price_timestamp);