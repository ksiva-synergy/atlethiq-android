import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.8";

// Readiness resolutions
export type CallResolution = "go" | "hold" | "back_off" | "calibrating" | "no_data";
export type ConfidenceResolution = "calibrating" | "provisional" | "reliable" | "high";

export interface EngineConfig {
  weights: {
    hrv_z: number;
    sleep: number;
    rhr_deviation: number;
    load_direction: number;
  };
  windows: {
    hrv_short: number;
    hrv_long: number;
    rhr_short: number;
    rhr_long: number;
    load_short: number;
    load_long: number;
  };
  thresholds: {
    go_score: number;
    go_hrv_z: number;
    go_load_spike: number;
    back_off_score: number;
    back_off_hrv_z: number;
    drift_hrv_sag_threshold: number;
    drift_consecutive_days: number;
  };
  sleep_need_default_hours: number;
}

export interface MetricSample {
  metric_type: string;
  value: number;
  unit: string;
  start_time: string;
  source_app: string;
  payload?: any;
}

// Computes the readiness daily snapshot for a single user on a single day.
export function computeDailyState(
  dayStr: string,
  samples: MetricSample[],
  pastSnapshots: { day: string; call: string; decode: any; debug: any }[],
  config: EngineConfig
) {
  const targetDay = new Date(dayStr + "T00:00:00Z");

  // Helper to filter samples within a rolling window [targetDay - days + 1, targetDay]
  const getSamplesInWindow = (metricType: string, days: number): MetricSample[] => {
    const startWindow = new Date(targetDay.toISOString());
    startWindow.setUTCDate(startWindow.getUTCDate() - days + 1);
    
    return samples.filter(s => {
      if (s.metric_type !== metricType) return false;
      const sDate = new Date(s.start_time);
      const sDateOnly = new Date(sDate.toISOString().split("T")[0] + "T00:00:00Z");
      return sDateOnly.getTime() >= startWindow.getTime() && sDateOnly.getTime() <= targetDay.getTime();
    });
  };

  // Helper to get daily values for a metric type, applying dedup (mock_whoop > mock_oneplus > others)
  const getDailyValuesMap = (metricType: string, windowDays: number): Map<string, { value: number; source: string }> => {
    const windowSamples = getSamplesInWindow(metricType, windowDays);
    const map = new Map<string, { value: number; source: string }>();
    
    // Group by date
    for (const sample of windowSamples) {
      const dateStr = new Date(sample.start_time).toISOString().split("T")[0];
      const priority = sample.source_app === "mock_whoop" ? 1 : (sample.source_app === "mock_oneplus" ? 2 : 3);
      const existing = map.get(dateStr);
      
      let val = Number(sample.value);
      if (metricType === "sleep_session") {
        val = val / 3600; // convert seconds to hours
      }

      if (!existing) {
        map.set(dateStr, { value: val, source: sample.source_app });
      } else {
        const existingPriority = existing.source === "mock_whoop" ? 1 : (existing.source === "mock_oneplus" ? 2 : 3);
        if (priority < existingPriority) {
          map.set(dateStr, { value: val, source: sample.source_app });
        }
      }
    }
    return map;
  };

  const hrvMap = getDailyValuesMap("hrv_rmssd", 90);
  const rhrMap = getDailyValuesMap("resting_hr", 90);
  const sleepMap = getDailyValuesMap("sleep_session", 90); // sleep history
  const stepsMap = getDailyValuesMap("steps", 90);
  const exerciseMap = getSamplesInWindow("exercise_session", config.windows.load_long);

  // Calculate training load per day (exercise duration * intensity + steps * 0.0001)
  const getDailyLoadMap = (days: number): Map<string, number> => {
    const loadMap = new Map<string, number>();
    const startWindow = new Date(targetDay);
    startWindow.setUTCDate(startWindow.getUTCDate() - days + 1);

    for (let d = 0; d < days; d++) {
      const curDate = new Date(startWindow);
      curDate.setUTCDate(curDate.getUTCDate() + d);
      const curDateStr = curDate.toISOString().split("T")[0];
      
      let dayLoad = 0;
      // Steps contribution
      const stepsData = stepsMap.get(curDateStr);
      if (stepsData) {
        dayLoad += stepsData.value * 0.0001;
      }
      // Exercise contribution
      const dayExercises = exerciseMap.filter(e => e.start_time.startsWith(curDateStr));
      for (const ex of dayExercises) {
        const duration = Number(ex.value) || 0; // minutes
        const intensity = ex.payload?.intensity_proxy || 1.0;
        dayLoad += duration * intensity;
      }
      loadMap.set(curDateStr, dayLoad);
    }
    return loadMap;
  };

  const loadMap = getDailyLoadMap(config.windows.load_long);

  // Current day values
  const todayHrv = hrvMap.get(dayStr);
  const todayRhr = rhrMap.get(dayStr);
  const todaySleep = sleepMap.get(dayStr);

  // Confidence calculation based on historical count of valid HRV+sleep days
  // Let's count how many days in the past 30 days had BOTH HRV and sleep
  let validDaysCount = 0;
  for (let i = 0; i < 45; i++) {
    const checkDate = new Date(targetDay);
    checkDate.setUTCDate(checkDate.getUTCDate() - i);
    const checkDateStr = checkDate.toISOString().split("T")[0];
    if (hrvMap.has(checkDateStr) && sleepMap.has(checkDateStr)) {
      validDaysCount++;
    }
  }

  let confidence: ConfidenceResolution = "calibrating";
  if (validDaysCount < 4) {
    confidence = "calibrating";
  } else if (validDaysCount <= 13) {
    confidence = "provisional";
  } else if (validDaysCount <= 29) {
    confidence = "reliable";
  } else {
    confidence = "high";
  }

  if (!todayHrv || !todaySleep || !todayRhr) {
    return {
      day: dayStr,
      call: "no_data" as CallResolution,
      confidence,
      signal_score: 0,
      decode: [],
      debug: { message: "Missing HRV, sleep, or RHR for today." },
      engine_version: "v1"
    };
  }

  // Rolling short/long metrics
  const getRollingStats = (map: Map<string, { value: number }>, days: number) => {
    let sum = 0;
    let count = 0;
    const vals: number[] = [];
    const startWindow = new Date(targetDay);
    startWindow.setUTCDate(startWindow.getUTCDate() - days + 1);

    for (let d = 0; d < days; d++) {
      const curDate = new Date(startWindow);
      curDate.setUTCDate(curDate.getUTCDate() + d);
      const curDateStr = curDate.toISOString().split("T")[0];
      const entry = map.get(curDateStr);
      if (entry) {
        sum += entry.value;
        count++;
        vals.push(entry.value);
      }
    }
    const mean = count > 0 ? sum / count : 0;
    const variance = count > 1 ? vals.reduce((acc, v) => acc + Math.pow(v - mean, 2), 0) / (count - 1) : 0;
    const stdDev = Math.sqrt(variance);
    return { mean, stdDev, count };
  };

  // HRV Stats
  const hrv7 = getRollingStats(hrvMap, config.windows.hrv_short);
  const hrv30 = getRollingStats(hrvMap, config.windows.hrv_long);
  const hrvZ = hrv30.stdDev > 0 ? (hrv7.mean - hrv30.mean) / hrv30.stdDev : 0.0;

  // RHR Stats
  const rhr7 = getRollingStats(rhrMap, config.windows.rhr_short);
  const rhr30 = getRollingStats(rhrMap, config.windows.rhr_long);
  const rhrDev = rhr30.mean > 0 ? (rhr7.mean - rhr30.mean) / rhr30.mean : 0.0; // fractional deviation

  // Sleep Score (duration vs need, capped at 100)
  const sleepNeed = config.sleep_need_default_hours;
  const sleepScore = Math.min(100, Math.max(0, (todaySleep.value / sleepNeed) * 100));

  // Load calculation
  const getSum = (map: Map<string, number>, days: number) => {
    let sum = 0;
    const startWindow = new Date(targetDay);
    startWindow.setUTCDate(startWindow.getUTCDate() - days + 1);
    for (let d = 0; d < days; d++) {
      const curDate = new Date(startWindow);
      curDate.setUTCDate(curDate.getUTCDate() + d);
      const curDateStr = curDate.toISOString().split("T")[0];
      sum += map.get(curDateStr) || 0;
    }
    return sum;
  };

  const load7 = getSum(loadMap, config.windows.load_short);
  const load28 = getSum(loadMap, config.windows.load_long);
  const weeklyLoadAvg = load28 / 4.0;
  const loadRatio = weeklyLoadAvg > 0 ? load7 / weeklyLoadAvg : 1.0;

  // =========================================================================
  // NORMALIZATION FUNCTIONS DOCUMENTATION:
  // 1. HRV Mapped Score: Mapped linearly from z-score range [-2.0, 2.0] to [0, 100].
  //    Formula: Math.min(100, Math.max(0, ((z + 2) / 4) * 100))
  // 2. RHR Mapped Score: Mapped linearly from fractional deviation range [+0.15, -0.15] to [0, 100].
  //    Formula: Math.min(100, Math.max(0, ((-deviation + 0.15) / 0.3) * 100))
  // 3. Sleep Score: Duration versus baseline need. Capped at 100%.
  //    Formula: Math.min(100, Math.max(0, (duration / need) * 100))
  // 4. Load Mapped Score: Perfect baseline ratio of <= 1.0 maps to 100.
  //    Spikes reduce score linearly such that 1.4 ratio is 50%, and >= 2.0 ratio is 0%.
  //    Formula: 100 - (ratio - 1.0) * 125
  // =========================================================================

  // HRV z-score mapped to 0-100
  const hrvZScoreMapped = Math.min(100, Math.max(0, ((hrvZ + 2) / 4) * 100));
  
  // RHR deviation mapped to 0-100
  const rhrScore = Math.min(100, Math.max(0, ((-rhrDev + 0.15) / 0.3) * 100));

  // Load ratio mapped to 0-100
  let loadScore = 100;
  if (loadRatio > 1.0) {
    loadScore = Math.max(0, 100 - (loadRatio - 1.0) * 125);
  }

  // Composite Weighted Score
  const rawSignalScore = (
    (hrvZScoreMapped * config.weights.hrv_z) +
    (sleepScore * config.weights.sleep) +
    (rhrScore * config.weights.rhr_deviation) +
    (loadScore * config.weights.load_direction)
  ) / 100.0;
  
  const signalScore = Math.round(rawSignalScore);

  // Drift Rule: HRV 7d mean >= 15% below 30d baseline for >=3 consecutive days.
  let isDriftRising = false;
  let consecutiveSagDays = 0;
  
  // We evaluate the last 5 days starting today to find a streak of >=3 consecutive sag days
  const getRollingStatsForDate = (date: Date, days: number) => {
    let sum = 0;
    let count = 0;
    const startWindow = new Date(date.toISOString());
    startWindow.setUTCDate(startWindow.getUTCDate() - days + 1);

    for (let d = 0; d < days; d++) {
      const curDate = new Date(startWindow.toISOString());
      curDate.setUTCDate(curDate.getUTCDate() + d);
      const curDateStr = curDate.toISOString().split("T")[0];
      const entry = hrvMap.get(curDateStr);
      if (entry) {
        sum += entry.value;
        count++;
      }
    }
    return count > 0 ? sum / count : 0;
  };

  // Count consecutive sag days starting from targetDay going backwards.
  // We need at least 3 consecutive days where 7d mean is >= 15% below 30d mean.
  for (let offset = 0; offset < 10; offset++) {
    const dCheck = new Date(targetDay.toISOString());
    dCheck.setUTCDate(dCheck.getUTCDate() - offset);
    
    const checkHrv7Mean = getRollingStatsForDate(dCheck, config.windows.hrv_short);
    const checkHrv30Mean = getRollingStatsForDate(dCheck, config.windows.hrv_long);

    if (checkHrv30Mean > 0) {
      const sagRatio = (checkHrv30Mean - checkHrv7Mean) / checkHrv30Mean;
      if (sagRatio >= config.thresholds.drift_hrv_sag_threshold) {
        consecutiveSagDays++;
        if (consecutiveSagDays >= config.thresholds.drift_consecutive_days) {
          isDriftRising = true;
          break; // found the sequence!
        }
      } else {
        if (offset < config.thresholds.drift_consecutive_days) {
          break;
        }
      }
    } else {
      if (offset < config.thresholds.drift_consecutive_days) {
        break;
      }
    }
  }
  
  if (consecutiveSagDays >= config.thresholds.drift_consecutive_days) {
    isDriftRising = true;
  }

  // Call mapping
  let call: CallResolution = "hold";
  if (confidence === "calibrating") {
    call = "calibrating";
  } else {
    const isLoadSpiking = loadRatio > config.thresholds.go_load_spike;
    const isHrvSaggingOrScoreLow = signalScore < config.thresholds.back_off_score || hrvZ <= config.thresholds.back_off_hrv_z || isDriftRising;
    const isHrvGoodAndScoreHigh = signalScore >= config.thresholds.go_score && hrvZ >= config.thresholds.go_hrv_z;

    if (isHrvSaggingOrScoreLow) {
      call = "back_off";
    } else if (isHrvGoodAndScoreHigh && !isLoadSpiking) {
      call = "go";
    } else {
      call = "hold";
    }
  }

  // Decode Payload list
  const decode = [
    {
      name: "HRV RMSSD",
      direction: hrvZ >= 0 ? "climbing" : "sagging",
      value: `${todayHrv.value.toFixed(1)} ms`,
      baseline: `${hrv30.mean.toFixed(1)} ms`,
      source: todayHrv.source
    },
    {
      name: "Sleep",
      direction: todaySleep.value >= sleepNeed ? "fresh" : "debt",
      value: `${todaySleep.value.toFixed(1)} h`,
      baseline: `${sleepNeed.toFixed(1)} h`,
      source: todaySleep.source
    },
    {
      name: "Resting HR",
      direction: rhrDev <= 0 ? "optimal" : "creeping",
      value: `${todayRhr.value.toFixed(1)} bpm`,
      baseline: `${rhr30.mean.toFixed(1)} bpm`,
      source: todayRhr.source
    },
    {
      name: "Training Load",
      direction: loadRatio > 1.4 ? "overload" : (loadRatio > 1.0 ? "climbing" : "stable"),
      value: `${load7.toFixed(1)} arbitrary units`,
      baseline: `${weeklyLoadAvg.toFixed(1)} units/week`,
      source: "derived"
    }
  ];

  // Debug payload to explain scores
  const debug = {
    hrv_z_score: hrvZ,
    hrv_mean_30d: hrv30.mean,
    hrv_std_30d: hrv30.stdDev,
    hrv_mean_7d: hrv7.mean,
    rhr_deviation: rhrDev,
    rhr_mean_7d: rhr7.mean,
    rhr_mean_30d: rhr30.mean,
    hrv_component_score: hrvZScoreMapped,
    rhr_component_score: rhrScore,
    sleep_score: sleepScore,
    load_component_score: loadScore,
    load_7d_sum: load7,
    load_28d_sum: load28,
    load_ratio: loadRatio,
    consecutive_sag_days: consecutiveSagDays,
    drift_rising: isDriftRising,
    weights_evaluated: config.weights,
    thresholds_evaluated: config.thresholds,
    engine_version: "v1"
  };

  return {
    day: dayStr,
    call,
    confidence,
    signal_score: signalScore,
    decode,
    debug,
    engine_version: "v1"
  };
}
