import { assertEquals, assertNotEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { computeDailyState, EngineConfig, MetricSample } from "./engine.ts";

const MOCK_CONFIG: EngineConfig = {
  weights: {
    hrv_z: 50,
    sleep: 30,
    rhr_deviation: 10,
    load_direction: 10
  },
  windows: {
    hrv_short: 7,
    hrv_long: 30,
    rhr_short: 7,
    rhr_long: 30,
    load_short: 7,
    load_long: 28
  },
  thresholds: {
    go_score: 75,
    go_hrv_z: -0.5,
    go_load_spike: 1.4,
    back_off_score: 45,
    back_off_hrv_z: -1.0,
    drift_hrv_sag_threshold: 0.15,
    drift_consecutive_days: 3
  },
  sleep_need_default_hours: 7.5
};

// Generates synthetic metric samples for testing scenario blocks
function generateTestSamples(): MetricSample[] {
  const samples: MetricSample[] = [];
  const baseDate = new Date("2026-07-10");
  
  for (let dayIdx = 1; dayIdx <= 45; dayIdx++) {
    const curDate = new Date(baseDate);
    curDate.setUTCDate(curDate.getUTCDate() - (45 - dayIdx));
    const dateStr = curDate.toISOString().split("T")[0];

    let hrv = 60.0;
    let rhr = 55.0;
    let sleepHours = 7.5;
    let steps = 9500;
    let exerciseDuration = 0;
    let intensity = 1.0;

    // Apply scenario rules identical to seed dataset
    if (dayIdx <= 3) {
      // Sparse start
      hrv = 59.0 + dayIdx;
      rhr = 54.0 + (dayIdx % 2);
      sleepHours = 7.6;
    } else if (dayIdx <= 13) {
      // Steady baseline
      hrv = 60.0 + (Math.sin(dayIdx) * 2.0);
      rhr = 55.0 - (Math.cos(dayIdx) * 1.5);
      sleepHours = 7.5 + (dayIdx % 3) * 0.2;
      exerciseDuration = dayIdx % 2 === 0 ? 45 : 0;
    } else if (dayIdx <= 20) {
      // Steady training
      if (dayIdx === 18) {
        hrv = 74.0;
        rhr = 50.0;
        sleepHours = 8.5;
      } else {
        hrv = 61.0 + (Math.sin(dayIdx) * 1.5);
        rhr = 54.0;
        sleepHours = 7.4;
      }
      exerciseDuration = 60;
      intensity = 1.2;
    } else if (dayIdx <= 27) {
      // Fresh & tapered
      hrv = 68.0 + (dayIdx - 21) * 1.5;
      rhr = 51.0 - (dayIdx - 21) * 0.5;
      sleepHours = 8.0 + (dayIdx % 2) * 0.4;
      exerciseDuration = dayIdx % 3 === 0 ? 30 : 0;
      intensity = 0.6;
      steps = 6500;
    } else if (dayIdx <= 35) {
      // Overload ramp
      hrv = 47.0 - (dayIdx % 3);
      rhr = 58.0 + (dayIdx - 28) * 0.6;
      sleepHours = 6.0 - (dayIdx % 2) * 0.5;
      exerciseDuration = 90;
      intensity = 2.5;
      steps = 17000;
    } else if (dayIdx <= 40) {
      // Recovery
      if (dayIdx === 38) {
        hrv = 51.0;
        rhr = 57.0;
        sleepHours = 5.2;
      } else {
        hrv = 58.0 + (dayIdx - 36) * 2.0;
        rhr = 54.0 - (dayIdx - 36) * 0.8;
        sleepHours = 7.8;
      }
      exerciseDuration = 0;
      steps = 4500;
    } else {
      // Return to form
      hrv = 62.0 + (Math.sin(dayIdx) * 2.0);
      rhr = 53.0;
      sleepHours = 7.6;
      exerciseDuration = 50;
    }

    // Add metric samples
    samples.push({
      metric_type: "hrv_rmssd",
      value: hrv,
      unit: "ms",
      start_time: `${dateStr}T07:00:00Z`,
      source_app: "mock_whoop"
    } as any);

    samples.push({
      metric_type: "resting_hr",
      value: rhr,
      unit: "bpm",
      start_time: `${dateStr}T06:00:00Z`,
      source_app: "mock_whoop"
    } as any);

    samples.push({
      metric_type: "sleep_session",
      value: sleepHours * 3600,
      unit: "seconds",
      start_time: `${dateStr}T22:30:00Z`,
      source_app: "mock_whoop"
    } as any);

    samples.push({
      metric_type: "steps",
      value: steps,
      unit: "count",
      start_time: `${dateStr}T00:01:00Z`,
      source_app: "mock_whoop"
    } as any);

    if (exerciseDuration > 0) {
      samples.push({
        metric_type: "exercise_session",
        value: exerciseDuration,
        unit: "minutes",
        start_time: `${dateStr}T16:00:00Z`,
        source_app: "mock_whoop",
        payload: { intensity_proxy: intensity }
      } as any);
    }
  }

  return samples;
}

Deno.test("Readiness Engine - Complete 45-day Scenario Checks", () => {
  const samples = generateTestSamples();
  const pastSnapshots: any[] = [];
  const baseDate = new Date("2026-07-10");
  
  for (let dayIdx = 1; dayIdx <= 45; dayIdx++) {
    const curDate = new Date(baseDate);
    curDate.setUTCDate(curDate.getUTCDate() - (45 - dayIdx));
    const dateStr = curDate.toISOString().split("T")[0];

    const result = computeDailyState(dateStr, samples, pastSnapshots, MOCK_CONFIG);
    pastSnapshots.push({
      day: result.day,
      call: result.call,
      decode: result.decode,
      debug: result.debug
    });

    // Assertions matching Milestone Spec
    if (dayIdx <= 3) {
      // Days 1-3: calibrating, no Call
      assertEquals(result.call, "calibrating");
      assertEquals(result.confidence, "calibrating");
    } else if (dayIdx <= 13) {
      // Days 4-13: provisional, baseline building
      assertEquals(result.confidence, "provisional");
      assertNotEquals(result.call, "calibrating");
    } else if (dayIdx <= 29) {
      // Days 14-29: reliable
      assertEquals(result.confidence, "reliable");
    } else {
      // Days 30+: high
      assertEquals(result.confidence, "high");
    }

    // Overload block Drift check (Days 28-35)
    if (dayIdx >= 28 && dayIdx <= 35) {
      if (dayIdx >= 33) {
        assertEquals(result.call, "back_off");
      }
      if (dayIdx === 35) {
        assertEquals((result.debug as any).drift_rising, true);
      }
    }

    // Fresh tapered block (Days 21-27)
    if (dayIdx === 25) {
      assertEquals(result.call, "go");
    }

    // Debug payload validation: must contain all parameters to verify scores manually
    if (result.call !== "calibrating" && result.call !== "no_data") {
      assertNotEquals((result.debug as any).hrv_z_score, undefined);
      assertNotEquals((result.debug as any).rhr_deviation, undefined);
      assertNotEquals((result.debug as any).sleep_score, undefined);
      assertNotEquals((result.debug as any).load_ratio, undefined);
      assertNotEquals((result.debug as any).weights_evaluated, undefined);
      assertNotEquals((result.debug as any).thresholds_evaluated, undefined);
    }
  }

  // Ensure fresh / tapered contains at least one 'go'
  const taperedCalls = pastSnapshots.slice(20, 27).map(s => s.call);
  assertEquals(taperedCalls.includes("go"), true);

  // Recovery block transition back to hold/go (Days 36-45)
  const recoveryCalls = pastSnapshots.slice(35, 45).map(s => s.call);
  assertEquals(recoveryCalls.includes("hold") || recoveryCalls.includes("go"), true);
});
