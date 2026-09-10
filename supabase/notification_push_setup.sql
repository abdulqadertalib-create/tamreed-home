-- إشعارات الطلبات للممرضين
-- شغّل هذا الملف مرة واحدة في Supabase SQL Editor.

create table if not exists public.notification_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    token text not null unique,
    role text not null check (role in ('patient','nurse','admin')),
    updated_at timestamptz not null default now()
);

alter table public.notification_tokens enable row level security;

drop policy if exists "notification tokens own select" on public.notification_tokens;
create policy "notification tokens own select"
on public.notification_tokens for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "notification tokens own insert" on public.notification_tokens;
create policy "notification tokens own insert"
on public.notification_tokens for insert
to authenticated
with check (auth.uid() = user_id);

drop policy if exists "notification tokens own update" on public.notification_tokens;
create policy "notification tokens own update"
on public.notification_tokens for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

grant select, insert, update on public.notification_tokens to authenticated;

create index if not exists notification_tokens_user_role_idx
on public.notification_tokens(user_id, role);

create index if not exists nurses_push_eligible_idx
on public.nurses(is_verified, is_available, subscription_status, subscription_end);
