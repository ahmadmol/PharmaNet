-- Phase 1: Persist profile notification preference (do not auto-apply)
-- Adds a real toggle source for app notification badges/attention indicators.

alter table if exists public.profiles
add column if not exists notifications_enabled boolean not null default true;
