create table if not exists jobs (
    id bigserial primary key,
    job_id varchar(255) not null,
    title varchar(255) not null,
    company varchar(255) not null,
    location varchar(255) not null,
    link varchar(1000) not null,
    description text,
    fit boolean,
    fit_score integer,
    role_type varchar(255),
    seniority_match varchar(255),
    tech_match varchar(255),
    reason text,
    verdict varchar(255),
    cover_letter text,
    created_at timestamptz not null
    );

create unique index if not exists uk_job_job_id on jobs(job_id);