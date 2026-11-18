alter table users add column email varchar(128);
alter table users add column enabled boolean default true not null;
alter table users add column confirmation_code varchar(64);

update users set email = username || '@bookapp.local';

alter table users add constraint uk_users_email unique (email);
alter table users add constraint uk_users_confirmation_code unique (confirmation_code);
