create extension if not exists pgcrypto;

create table if not exists public.users (
  id uuid primary key references auth.users(id) on delete cascade,
  phone text,
  display_name text not null default 'Kaval User',
  emergency_note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.journey_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users(id) on delete cascade,
  destination_name text not null default 'Emergency location sharing',
  status text not null check (status in ('active', 'completed', 'incomplete', 'emergency')),
  session_token text not null unique default encode(gen_random_bytes(24), 'hex'),
  expires_at timestamptz not null default now() + interval '12 hours',
  last_lat double precision,
  last_lng double precision,
  last_accuracy_meters integer,
  last_updated_at timestamptz,
  guardian_last_viewed_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists journey_sessions_user_status_idx
  on public.journey_sessions(user_id, status);
create index if not exists journey_sessions_token_idx
  on public.journey_sessions(session_token);

alter table public.users enable row level security;
alter table public.journey_sessions enable row level security;

drop policy if exists user_self on public.users;
create policy user_self on public.users
  for all to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

drop policy if exists journey_owner on public.journey_sessions;
create policy journey_owner on public.journey_sessions
  for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

revoke all on public.users from anon;
revoke all on public.journey_sessions from anon;
grant select, insert, update on public.users to authenticated;
grant select, insert, update on public.journey_sessions to authenticated;

create or replace function public.guardian_session_by_token(input_token text)
returns table (
  display_name text,
  status text,
  last_lat double precision,
  last_lng double precision,
  last_accuracy_meters integer,
  last_updated_at timestamptz,
  expires_at timestamptz
)
language sql
security definer
set search_path = public
stable
as $$
  select
    u.display_name,
    s.status,
    s.last_lat,
    s.last_lng,
    s.last_accuracy_meters,
    s.last_updated_at,
    s.expires_at
  from public.journey_sessions s
  join public.users u on u.id = s.user_id
  where s.session_token = input_token
    and s.status in ('active','emergency')
    and s.expires_at > now()
  limit 1;
$$;

revoke all on function public.guardian_session_by_token(text) from public;
grant execute on function public.guardian_session_by_token(text) to anon, authenticated;

create or replace function public.guardian_mark_viewed(input_token text)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.journey_sessions
  set guardian_last_viewed_at = now()
  where session_token = input_token
    and status in ('active','emergency')
    and expires_at > now();
  return found;
end;
$$;

revoke all on function public.guardian_mark_viewed(text) from public;
grant execute on function public.guardian_mark_viewed(text) to anon, authenticated;
