alter table jobs drop column if exists fit;
alter table jobs drop column if exists role_type;
alter table jobs drop column if exists seniority_match;
alter table jobs drop column if exists tech_match;
alter table jobs drop column if exists reason;
alter table jobs drop column if exists verdict;
alter table jobs drop column if exists cover_letter;

alter table jobs add column if not exists seniority varchar(50);
alter table jobs add column if not exists stack text;
alter table jobs add column if not exists responsibilities text;
alter table jobs add column if not exists match_reason text;