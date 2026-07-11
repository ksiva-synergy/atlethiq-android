import { withSupabase } from "@supabase/server";
import { computeDailyState, EngineConfig } from "./engine.ts";

export default {
  fetch: withSupabase({ auth: ["publishable", "secret"] }, async (req, ctx) => {
    try {
      const payload = await req.json().catch(() => ({}));
      const { user_id, start_date, end_date, backfill = false } = payload;

      // Ensure user_id is provided or resolve first user
      let targetUserId = user_id;
      if (!targetUserId) {
        const { data: users, error: userError } = await ctx.supabaseAdmin
          .from("auth.users")
          .select("id")
          .limit(1);
        if (userError || !users || users.length === 0) {
          return Response.json({ error: "No user found to compute Call." }, { status: 400 });
        }
        targetUserId = users[0].id;
      }

      // Fetch active engine config
      const { data: configs, error: configError } = await ctx.supabaseAdmin
        .from("engine_config")
        .select("*")
        .eq("active", true)
        .limit(1);

      if (configError || !configs || configs.length === 0) {
        return Response.json({ error: "Active engine_config v1 not found." }, { status: 500 });
      }

      const activeConfig = configs[0].params as EngineConfig;

      // Resolve date range for computation
      let datesToProcess: string[] = [];
      if (backfill && start_date && end_date) {
        let current = new Date(start_date);
        const end = new Date(end_date);
        while (current <= end) {
          datesToProcess.push(current.toISOString().split("T")[0]);
          current.setDate(current.getDate() + 1);
        }
      } else {
        // Compute for a single date (defaults to yesterday if not specified)
        const dateStr = start_date || new Date(Date.now() - 86400000).toISOString().split("T")[0];
        datesToProcess.push(dateStr);
      }

      // Fetch all metric samples for this user
      const { data: samples, error: sampleError } = await ctx.supabaseAdmin
        .from("metric_samples")
        .select("metric_type, value, unit, start_time, source_app, payload")
        .eq("user_id", targetUserId);

      if (sampleError) {
        return Response.json({ error: `Failed to fetch metric samples: ${sampleError.message}` }, { status: 500 });
      }

      const results = [];

      // Process chronologically so rolling metrics build naturally
      for (const dayStr of datesToProcess) {
        // Fetch past daily snapshots computed before dayStr for context
        const { data: pastSnapshots, error: pastError } = await ctx.supabaseAdmin
          .from("daily_snapshots")
          .select("day, call, decode, debug")
          .eq("user_id", targetUserId)
          .lt("day", dayStr)
          .order("day", { ascending: true });

        if (pastError) {
          return Response.json({ error: `Failed to fetch past daily snapshots: ${pastError.message}` }, { status: 500 });
        }

        const computed = computeDailyState(dayStr, samples || [], pastSnapshots || [], activeConfig);

        // Save daily snapshot to DB
        const { error: upsertError } = await ctx.supabaseAdmin
          .from("daily_snapshots")
          .upsert({
            user_id: targetUserId,
            day: computed.day,
            call: computed.call,
            signal_score: computed.signal_score,
            confidence: computed.confidence,
            decode: computed.decode,
            debug: computed.debug,
            engine_version: computed.engine_version,
            computed_at: new Date().toISOString()
          }, { onConflict: "user_id,day" });

        if (upsertError) {
          return Response.json({ error: `Failed to save snapshot for ${dayStr}: ${upsertError.message}` }, { status: 500 });
        }

        results.push({ day: dayStr, call: computed.call, confidence: computed.confidence, signal_score: computed.signal_score });
      }

      return Response.json({
        success: true,
        user_id: targetUserId,
        processed: results
      });

    } catch (e) {
      return Response.json({ error: e.message }, { status: 500 });
    }
  }),
};
