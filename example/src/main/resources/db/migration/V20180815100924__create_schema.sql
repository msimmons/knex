create table users (
   id uuid primary key,
   created_at timestamptz not null,
   updated_at timestamptz,
   data jsonb
);