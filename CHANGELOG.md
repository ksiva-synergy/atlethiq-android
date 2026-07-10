# Changelog

## [Milestone 0] - 2026-07-10

Initialized the core Android application architecture and design system. Created Room database entities and separate DAOs for `MetricSample`, `DailySnapshot`, and `CallFeedback` conforming to the canonical model. Configured Hilt dependency injection modules for local database and Supabase client integration. Integrated Google Fonts provider for Inter, Space Grotesk, and JetBrains Mono fonts and extracted design tokens (Color, Type, Shape) from the reference HTML. Built the bottom navigation scaffold with custom selected-tab indicators and screen header placeholders, and wired a supabase-kt authentication flow with a customized login screen. Created the Supabase SQL schema migrations and RLS policies.
