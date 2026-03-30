CREATE TABLE IF NOT EXISTS jobs (
                                    id bigserial PRIMARY KEY,

                                    job_id varchar(255) NOT NULL UNIQUE,

    title varchar(255) NOT NULL,
    company varchar(255) NOT NULL,
    location varchar(255) NOT NULL,
    link varchar(1000) NOT NULL,

    description text,

    fit_score integer,
    seniority varchar(50),
    stack text,
    responsibilities text,
    match_reason text,

    created_at timestamptz NOT NULL DEFAULT now()
    );