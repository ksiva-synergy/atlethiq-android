# Changelog

## [Milestone 0] - 2026-07-11

Initialized the core Android application architecture and design system. Created Room database entities and separate DAOs for `MetricSample`, `DailySnapshot`, and `CallFeedback` conforming to the canonical model. Configured Hilt dependency injection modules for local database and Supabase client integration. Integrated Google Fonts provider for Inter, Space Grotesk, and JetBrains Mono fonts and extracted design tokens (Color, Type, Shape) from the reference HTML. Built the bottom navigation scaffold with custom selected-tab indicators and screen header placeholders, and wired a supabase-kt authentication flow with a customized login screen. Created the Supabase SQL schema migrations and RLS policies. Fixed a startup crash (`NoClassDefFoundError: Failed resolution of: Lio/ktor/client/plugins/HttpTimeout`) by upgrading the explicit Ktor client version pin to `3.0.0` in the versions catalog, aligning it with the runtime expectations of `supabase-kt 3.0.1`.

### Verification Results
Closed out Milestone 0 with full end-to-end integration and verification:
- Wired live Supabase credentials through `local.properties` (fully gitignored).
- Added `INTERNET` permission to `AndroidManifest.xml`.
- Successfully ran the SQL schema to create database tables and enable RLS with policies.
- Programmatically verified database connectivity: successfully performed queries on all four tables (`metric_samples`, `daily_snapshots`, `call_feedback`, and `engine_config`) via the Supabase client on app startup upon login.
- Validated login flow against the live Supabase instance with a real user.

