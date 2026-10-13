create table users(
	id 			bigint generated always as identity primary key,
	full_name 		varchar(100) not null,
	phone 			varchar(20) not null unique,
	password_hash 	varchar(255) not null,
	created_at 		timestamp not null default now(),
	created_by		varchar(100),
	updated_at 		timestamp,
	updated_by		varchar(100)
);

create table wallets(
	id			bigint generated always as identity primary key,
	user_id			bigint not null references users(id),
	number			varchar(20) not null unique,
	wallet_type		varchar(20) not null default 'USER' check(wallet_type in ('USER', 'SYSTEM')),
	currency		varchar(3) not null default 'UZS' check(currency in ('UZS')),
	balance			numeric(19,2) not null default 0,
	status			varchar(20) not null check(status in ('ACTIVE', 'BLOCKED')),
	created_at 		timestamp not null default now(),
	created_by		varchar(100),
	updated_at 		timestamp,
	updated_by		varchar(100),
	check (wallet_type = 'SYSTEM' or (wallet_type = 'USER' and balance >= 0))
);

create table transactions(
	id			bigint generated always as identity primary key,
	amount			numeric(19,2) not null check(amount > 0),
	status			varchar(20) not null check(status in ('SUCCESS', 'FAILED', 'PENDING', 'REVERSED')),
	fail_reason 		varchar,
	idempotency_key 	varchar not null unique,
	from_wallet_id 		bigint not null references wallets(id),
	to_wallet_id		bigint not null references wallets(id),
	created_at 		timestamp not null default now(),
	created_by		varchar(100),
	updated_at 		timestamp,
	updated_by		varchar(100),
	check(from_wallet_id <> to_wallet_id)
);


create table ledger_entries(
	id			bigint generated always as identity primary key,
	transaction_id		bigint not null references transactions(id),
	wallet_id		bigint not null references wallets(id),
	amount 			numeric(19,2) not null check(amount <> 0),
	created_at 		timestamp not null default now(),
	created_by		varchar(100)
);


create index on ledger_entries(wallet_id);
create index on ledger_entries(transaction_id);