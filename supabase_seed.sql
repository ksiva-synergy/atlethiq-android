-- Idempotent SQL Seed Script for Atlethiq Android Mock User
-- Generates 45 days of synthetic history ending yesterday (2026-07-10)

-- 1. Ensure mock user exists in auth.users
-- Since this runs in SQL editor, we can query auth.users or create a dummy user if none exists.
-- Let's check for an existing user or insert a default dogfooding mock user.
DO $$
DECLARE
    v_user_id UUID;
    v_count INT;
BEGIN
    SELECT count(*) INTO v_count FROM auth.users;
    
    IF v_count = 0 THEN
        -- Insert a mock user if none exists
        INSERT INTO auth.users (id, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, created_at, updated_at, role, aud)
        VALUES (
            'da3ca9bb-d2be-4972-88f5-46aa32a514d8',
            'mock@atlethiq.com',
            crypt('password123', gen_salt('bf')),
            now(),
            '{"provider":"email","providers":["email"]}',
            '{}',
            now(),
            now(),
            'authenticated',
            'authenticated'
        )
        RETURNING id INTO v_user_id;
    ELSE
        SELECT id INTO v_user_id FROM auth.users LIMIT 1;
    END IF;
    
    RAISE NOTICE 'Seeding data for user_id: %', v_user_id;
END $$;

-- Define a temporary table or helper to hold seed generation variables
CREATE OR REPLACE FUNCTION seed_atlethiq_data()
RETURNS void AS $$
DECLARE
    v_user_id UUID;
    v_base_date DATE := '2026-07-10'::DATE; -- "yesterday"
    v_date DATE;
    v_day_idx INT;
    
    -- Physiological baselines
    v_hrv_baseline NUMERIC := 60.0;
    v_rhr_baseline NUMERIC := 55.0;
    
    -- Daily calculated variables
    v_hrv_rmssd NUMERIC;
    v_resting_hr NUMERIC;
    v_sleep_duration NUMERIC; -- in hours
    
    -- Overlapping sleep values
    v_sleep_start TIMESTAMPTZ;
    v_sleep_end TIMESTAMPTZ;
    
    -- Exercise variables
    v_duration INT;
    v_avg_hr INT;
    
    -- Idempotent Client Sample ID helper
    v_client_id_prefix TEXT;
    
    -- Drift helper
    v_consecutive_low_hrv INT := 0;
BEGIN
    -- Get the target user ID (the first user in auth.users)
    SELECT id INTO v_user_id FROM auth.users ORDER BY created_at LIMIT 1;
    
    -- Idempotency check: Delete only this user's seeded records
    DELETE FROM metric_samples WHERE user_id = v_user_id AND source_app IN ('mock_whoop', 'mock_oneplus', 'manual');
    DELETE FROM daily_snapshots WHERE user_id = v_user_id;

    -- Generate 45 days chronologically (Day 1 is v_base_date - 44 days, Day 45 is v_base_date)
    FOR v_day_idx IN 1..45 LOOP
        v_date := v_base_date - (45 - v_day_idx);
        v_client_id_prefix := 'seed_' || to_char(v_date, 'YYYYMMDD') || '_';

        ----------------------------------------------------
        -- PHYSIOLOGICAL SCENARIO MODELING ENGINE
        ----------------------------------------------------
        -- HRV personal baseline = 60ms. RHR = 55.
        
        -- Days 1-3: Sparse start (calibrating). Only HRV/sleep, steady.
        IF v_day_idx BETWEEN 1 AND 3 THEN
            v_hrv_rmssd := 59.0 + v_day_idx; -- 60, 61, 62
            v_resting_hr := 54.0 + (v_day_idx % 2); -- 55, 54, 55
            v_sleep_duration := 7.6;
            
        -- Days 4-13: Steady baseline building.
        ELSIF v_day_idx BETWEEN 4 AND 13 THEN
            v_hrv_rmssd := 60.0 + (sin(v_day_idx) * 2.0); -- minor oscillations around 60
            v_resting_hr := 55.0 - (cos(v_day_idx) * 1.5);
            v_sleep_duration := 7.5 + (v_day_idx % 3) * 0.2; -- 7.5, 7.7, 7.9...
            
        -- Days 14-20: Steady with one strong day (Day 18 has high sleep and HRV)
        ELSIF v_day_idx BETWEEN 14 AND 20 THEN
            IF v_day_idx = 18 THEN
                v_hrv_rmssd := 74.0;
                v_resting_hr := 50.0;
                v_sleep_duration := 8.5;
            ELSE
                v_hrv_rmssd := 61.0 + (sin(v_day_idx) * 1.5);
                v_resting_hr := 54.0;
                v_sleep_duration := 7.4;
            END IF;
            
        -- Days 21-27: Fresh/tapered (HRV climbing, good sleep, moderate load)
        ELSIF v_day_idx BETWEEN 21 AND 27 THEN
            v_hrv_rmssd := 68.0 + (v_day_idx - 21) * 1.5; -- climbing from 68 to 77
            v_resting_hr := 51.0 - (v_day_idx - 21) * 0.5; -- dropping to ~48
            v_sleep_duration := 8.0 + (v_day_idx % 2) * 0.4; -- 8.0 or 8.4 hours
            
        -- Days 28-35: Overload (7d load > 1.4x 28d average, HRV >=15% below baseline for 3+ consecutive days, RHR creeping up)
        ELSIF v_day_idx BETWEEN 28 AND 35 THEN
            -- HRV sagging >=15% below 60ms baseline => <= 51ms.
            v_hrv_rmssd := 47.0 - (v_day_idx % 3); -- 47, 46, 45, 47, 46...
            v_resting_hr := 58.0 + (v_day_idx - 28) * 0.6; -- creeping up to ~62
            v_sleep_duration := 6.0 - (v_day_idx % 2) * 0.5; -- short sleep: 6.0, 5.5...
            
        -- Days 36-40: Recovery with one bad-sleep night (Day 38 has terrible sleep, but overall load is cut, HRV rebounding)
        ELSIF v_day_idx BETWEEN 36 AND 40 THEN
            IF v_day_idx = 38 THEN
                v_hrv_rmssd := 51.0;
                v_resting_hr := 57.0;
                v_sleep_duration := 5.2;
            ELSE
                v_hrv_rmssd := 58.0 + (v_day_idx - 36) * 2.0; -- rebounding to 58, 62...
                v_resting_hr := 54.0 - (v_day_idx - 36) * 0.8;
                v_sleep_duration := 7.8;
            END IF;
            
        -- Days 41-45: Return to form
        ELSE
            v_hrv_rmssd := 62.0 + (sin(v_day_idx) * 2.0); -- back around baseline
            v_resting_hr := 53.0;
            v_sleep_duration := 7.6;
        END IF;

        ----------------------------------------------------
        -- WRITE HRV SAMPLES (Idempotent client_sample_id)
        ----------------------------------------------------
        -- mock_whoop (Priority 1)
        INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id)
        VALUES (v_user_id, 'hrv_rmssd', v_hrv_rmssd, 'ms', (v_date || ' 07:00:00+00')::timestamptz, (v_date || ' 07:00:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'hrv_whoop');
        
        -- mock_oneplus (Priority 2) - slightly different value
        INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id)
        VALUES (v_user_id, 'hrv_rmssd', v_hrv_rmssd - 2.0, 'ms', (v_date || ' 07:15:00+00')::timestamptz, (v_date || ' 07:15:00+00')::timestamptz, '+00:00', 'mock_oneplus', 'automatic', v_client_id_prefix || 'hrv_oneplus');

        ----------------------------------------------------
        -- WRITE RESTING HR SAMPLES
        ----------------------------------------------------
        -- mock_whoop
        INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id)
        VALUES (v_user_id, 'resting_hr', v_resting_hr, 'bpm', (v_date || ' 06:00:00+00')::timestamptz, (v_date || ' 06:00:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'rhr_whoop');
        
        -- mock_oneplus
        INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id)
        VALUES (v_user_id, 'resting_hr', v_resting_hr + 1.5, 'bpm', (v_date || ' 06:10:00+00')::timestamptz, (v_date || ' 06:10:00+00')::timestamptz, '+00:00', 'mock_oneplus', 'automatic', v_client_id_prefix || 'rhr_oneplus');

        ----------------------------------------------------
        -- WRITE SLEEP SESSIONS (With Stages Payload)
        ----------------------------------------------------
        -- mock_whoop: sleep starts at 22:30 previous night, runs for v_sleep_duration
        v_sleep_start := (v_date - 1 || ' 22:30:00+00')::timestamptz;
        v_sleep_end := v_sleep_start + (v_sleep_duration * interval '1 hour');
        
        INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
        VALUES (
            v_user_id, 'sleep_session', v_sleep_duration * 3600, 'seconds', v_sleep_start, v_sleep_end, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'sleep_whoop',
            jsonb_build_object(
                'deep_sleep_seconds', (v_sleep_duration * 0.20 * 3600)::int,
                'rem_sleep_seconds', (v_sleep_duration * 0.25 * 3600)::int,
                'light_sleep_seconds', (v_sleep_duration * 0.45 * 3600)::int,
                'awake_seconds', (v_sleep_duration * 0.10 * 3600)::int
            )
        );

        -- mock_oneplus: overlapping sleep session, slightly shorter/longer, starts at 23:00
        v_sleep_start := (v_date - 1 || ' 23:00:00+00')::timestamptz;
        v_sleep_end := v_sleep_start + ((v_sleep_duration - 0.2) * interval '1 hour');
        
        INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
        VALUES (
            v_user_id, 'sleep_session', (v_sleep_duration - 0.2) * 3600, 'seconds', v_sleep_start, v_sleep_end, '+00:00', 'mock_oneplus', 'automatic', v_client_id_prefix || 'sleep_oneplus',
            jsonb_build_object(
                'deep_sleep_seconds', ((v_sleep_duration - 0.2) * 0.18 * 3600)::int,
                'rem_sleep_seconds', ((v_sleep_duration - 0.2) * 0.22 * 3600)::int,
                'light_sleep_seconds', ((v_sleep_duration - 0.2) * 0.50 * 3600)::int,
                'awake_seconds', ((v_sleep_duration - 0.2) * 0.10 * 3600)::int
            )
        );

        ----------------------------------------------------
        -- WRITE DAILY STEPS
        ----------------------------------------------------
        -- Steady baseline steps: 8,000 to 12,000 steps normally.
        -- Fresh/tapered: ~6,000 steps (moderate).
        -- Overload block: 15,000 to 20,000 steps (high activity).
        -- Recovery: 4,000 steps.
        IF v_day_idx BETWEEN 21 AND 27 THEN
            v_duration := 6500;
        ELSIF v_day_idx BETWEEN 28 AND 35 THEN
            v_duration := 17000;
        ELSIF v_day_idx BETWEEN 36 AND 40 THEN
            v_duration := 4500;
        ELSE
            v_duration := 9500 + (v_day_idx % 5) * 500;
        END IF;

        INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id)
        VALUES (v_user_id, 'steps', v_duration, 'count', (v_date || ' 00:01:00+00')::timestamptz, (v_date || ' 23:59:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'steps');

        ----------------------------------------------------
        -- WRITE EXERCISE SESSIONS (Load modulation)
        ----------------------------------------------------
        -- Days 1-3: No exercise
        -- Days 4-13: Moderate steady exercise (duration 40-50 min)
        -- Days 14-20: Steady (with load increasing to prep for overload)
        -- Days 21-27: Fresh (tapering down, shorter workouts, low load)
        -- Days 28-35: Overload (2 workouts/day, long duration, high intensity)
        -- Days 36-40: Recovery (no exercise, active rest)
        -- Days 41-45: Return to form (moderate exercise)
        
        IF v_day_idx BETWEEN 4 AND 13 THEN
            -- 1 workout every alternate day
            IF v_day_idx % 2 = 0 THEN
                INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
                VALUES (
                    v_user_id, 'exercise_session', 45, 'minutes', (v_date || ' 17:00:00+00')::timestamptz, (v_date || ' 17:45:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'ex1',
                    jsonb_build_object('avg_hr', 140, 'intensity_proxy', 1.0)
                );
            END IF;
            
        ELSIF v_day_idx BETWEEN 14 AND 20 THEN
            -- Daily workout
            INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
            VALUES (
                v_user_id, 'exercise_session', 60, 'minutes', (v_date || ' 16:00:00+00')::timestamptz, (v_date || ' 17:00:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'ex1',
                jsonb_build_object('avg_hr', 145, 'intensity_proxy', 1.2)
            );
            
        ELSIF v_day_idx BETWEEN 21 AND 27 THEN
            -- Light taper workout every 3 days
            IF v_day_idx % 3 = 0 THEN
                INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
                VALUES (
                    v_user_id, 'exercise_session', 30, 'minutes', (v_date || ' 10:00:00+00')::timestamptz, (v_date || ' 10:30:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'ex1',
                    jsonb_build_object('avg_hr', 125, 'intensity_proxy', 0.6)
                );
            END IF;
            
        ELSIF v_day_idx BETWEEN 28 AND 35 THEN
            -- Overload: Double workouts on some days, long high-intensity sessions
            -- Session 1
            INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
            VALUES (
                v_user_id, 'exercise_session', 90, 'minutes', (v_date || ' 08:00:00+00')::timestamptz, (v_date || ' 09:30:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'ex1',
                jsonb_build_object('avg_hr', 160, 'intensity_proxy', 2.5)
            );
            -- Session 2 on odd days
            IF v_day_idx % 2 = 1 THEN
                INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
                VALUES (
                    v_user_id, 'exercise_session', 45, 'minutes', (v_date || ' 18:00:00+00')::timestamptz, (v_date || ' 18:45:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'ex2',
                    jsonb_build_object('avg_hr', 145, 'intensity_proxy', 1.1)
                );
            END IF;
            
        ELSIF v_day_idx BETWEEN 36 AND 40 THEN
            -- Recovery: No exercises seeded (0 load)
            
        ELSE
            -- Return to form: 50 min moderate workout every day
            INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
            VALUES (
                v_user_id, 'exercise_session', 50, 'minutes', (v_date || ' 16:30:00+00')::timestamptz, (v_date || ' 17:20:00+00')::timestamptz, '+00:00', 'mock_whoop', 'automatic', v_client_id_prefix || 'ex1',
                jsonb_build_object('avg_hr', 140, 'intensity_proxy', 1.0)
            );
        END IF;

        ----------------------------------------------------
        -- WRITE MANUAL RPE + SUBJECTIVE FEEL (~10 scattered days)
        ----------------------------------------------------
        -- Seed on days: 5, 8, 12, 16, 20, 24, 29, 32, 37, 43
        IF v_day_idx IN (5, 8, 12, 16, 20, 24, 29, 32, 37, 43) THEN
            DECLARE
                v_rpe INT;
                v_feel TEXT;
            BEGIN
                -- Overload days (29, 32) -> high RPE, "wrecked" feel
                -- Fresh days (20, 24) -> moderate RPE, "fresh" feel
                -- Default -> normal feel
                IF v_day_idx IN (29, 32) THEN
                    v_rpe := 9;
                    v_feel := 'felt_wrecked';
                ELSIF v_day_idx IN (20, 24) THEN
                    v_rpe := 4;
                    v_feel := 'felt_fresh';
                ELSE
                    v_rpe := 6;
                    v_feel := 'normal';
                END IF;

                -- RPE sample
                INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id)
                VALUES (v_user_id, 'rpe', v_rpe, 'scale_1_10', (v_date || ' 19:00:00+00')::timestamptz, (v_date || ' 19:00:00+00')::timestamptz, '+00:00', 'manual', 'manual', v_client_id_prefix || 'rpe_manual');

                -- Subjective feel sample
                INSERT INTO metric_samples (user_id, metric_type, value, unit, start_time, end_time, zone_offset, source_app, recording_method, client_sample_id, payload)
                VALUES (v_user_id, 'subjective_feel', NULL, 'category', (v_date || ' 19:05:00+00')::timestamptz, (v_date || ' 19:05:00+00')::timestamptz, '+00:00', 'manual', 'manual', v_client_id_prefix || 'feel_manual', jsonb_build_object('feel', v_feel));
            END;
        END IF;

    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Executing the seed data helper
SELECT seed_atlethiq_data();

-- Clean up the temporary generation helper function
DROP FUNCTION seed_atlethiq_data();

-- 3. Seed active engine_config v1 row per spec defaults
INSERT INTO engine_config (version, params, active)
VALUES (
    'v1',
    jsonb_build_object(
        'weights', jsonb_build_object(
            'hrv_z', 50,
            'sleep', 30,
            'rhr_deviation', 10,
            'load_direction', 10
        ),
        'windows', jsonb_build_object(
            'hrv_short', 7,
            'hrv_long', 30,
            'rhr_short', 7,
            'rhr_long', 30,
            'load_short', 7,
            'load_long', 28
        ),
        'thresholds', jsonb_build_object(
            'go_score', 75,
            'go_hrv_z', -0.5,
            'go_load_spike', 1.4,
            'back_off_score', 45,
            'back_off_hrv_z', -1.0,
            'drift_hrv_sag_threshold', 0.15,
            'drift_consecutive_days', 3
        ),
        'sleep_need_default_hours', 7.5
    ),
    true
)
ON CONFLICT (version) DO UPDATE 
SET params = EXCLUDED.params, active = EXCLUDED.active;
