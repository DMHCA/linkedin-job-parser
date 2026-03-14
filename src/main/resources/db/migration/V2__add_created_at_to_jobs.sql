alter table jobs
    add column if not exists created_at timestamptz;

update jobs
set created_at = now()
where created_at is null;

alter table jobs
    alter column created_at set not null;