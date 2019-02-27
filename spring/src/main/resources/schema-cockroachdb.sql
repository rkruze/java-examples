DROP TABLE IF EXISTS spring_example CASCADE;

CREATE TABLE IF NOT EXISTS spring_example (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), balance INT);;