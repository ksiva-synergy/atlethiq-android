-- canonical samples (mirror of the app's Room table)
create table metric_samples (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users not null,
  metric_type text not null,          -- 'hrv_rmssd' | 'resting_hr' | 'sleep_session' | 'heart_rate' | 'exercise_session' | 'steps' | 'rpe' | 'subjective_feel' | ...
  value numeric,
  unit text,
  start_time timestamptz not null,
  end_time timestamptz,
  zone_offset text,
  source_app text not null,           -- e.g. 'com.whoop.android', 'com.heytap.health', 'manual'
  source_device text,
  recording_method text not null,     -- 'automatic' | 'manual'
  payload jsonb,                      -- stages, raw extras
  client_sample_id text not null,     -- HC record id or generated; dedup key
  created_at timestamptz default now(),
  unique (user_id, metric_type, client_sample_id)
);

-- one row per day: the computed Call
create table daily_snapshots (
  user_id uuid references auth.users not null,
  day date not null,
  call text not null,                 -- 'go' | 'hold' | 'back_off' | 'calibrating' | 'no_data'
  signal_score int,                   -- 0–100
  confidence text not null,           -- 'calibrating' | 'provisional' | 'reliable' | 'high'
  decode jsonb not null,              -- factor list: name, direction, value, baseline, source
  debug jsonb not null,               -- z-scores, weights, thresholds, engine version
  engine_version text not null,
  computed_at timestamptz default now(),
  primary key (user_id, day)
);

-- founder feedback: the labeled dataset
create table call_feedback (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users not null,
  day date not null,
  issued_call text not null,
  user_verdict text not null,         -- 'go' | 'hold' | 'back_off'
  reasons text[],                     -- 'felt_fresh' | 'felt_wrecked' | 'life_stress' | 'illness' | 'other'
  note text,
  created_at timestamptz default now()
);

-- tunable engine parameters (A/B hooks)
create table engine_config (
  version text primary key,
  params jsonb not null,              -- weights, windows, thresholds
  active boolean default false
);

-- Enable RLS on all tables
alter table metric_samples enable row level security;
alter table daily_snapshots enable row level security;
alter table call_feedback enable row level security;
alter table engine_config enable row level security;

-- RLS policies
create policy "Users can perform all actions on their own metric samples" on metric_samples
  for all using (auth.uid() = user_id);

create policy "Users can perform all actions on their own daily snapshots" on daily_snapshots
  for all using (auth.uid() = user_id);

create policy "Users can perform all actions on their own call feedback" on call_feedback
  for all using (auth.uid() = user_id);

create policy "Allow read access to engine_config for authenticated users" on engine_config
  for select using (auth.role() = 'authenticated');
