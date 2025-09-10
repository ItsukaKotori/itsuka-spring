create table if not exists dict
(
    id          bigint primary key,
    name        varchar(255) constraint dict_name_unq_key unique,
    description varchar(255)
);

create table if not exists dict_detail
(
    id          bigint primary key,
    dict_id     bigint,
    label       varchar(255),
    value       varchar(255),
    sort        integer,
    constraint fk_dict_detail_dict foreign key (dict_id) references dict(id)
);