package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.common.BusinessException;
import com.fitness.dto.AdminRequests;
import com.fitness.dto.AdviceViews;
import com.fitness.dto.StatsViews;
import com.fitness.entity.AiAdvice;
import com.fitness.entity.AiAdviceFeedback;
import com.fitness.entity.AiAdviceItem;
import com.fitness.entity.Exercise;
import com.fitness.entity.SessionExercise;
import com.fitness.entity.UserProfile;
import com.fitness.entity.WorkoutSession;
import com.fitness.entity.WorkoutSet;
import com.fitness.mapper.AiAdviceFeedbackMapper;
import com.fitness.mapper.AiAdviceItemMapper;
import com.fitness.mapper.AiAdviceMapper;
import com.fitness.mapper.ExerciseMapper;
import com.fitness.mapper.SessionExerciseMapper;
import com.fitness.mapper.UserProfileMapper;
import com.fitness.mapper.WorkoutSessionMapper;
import com.fitness.mapper.WorkoutSetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdviceService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Map<String, Integer> SEVERITY_ORDER = Map.of(
            "ALERT", 0, "WARN", 1, "INFO", 2
    );

    private final StatisticsService statisticsService;
    private final WorkoutSessionMapper sessionMapper;
    private final SessionExerciseMapper sessionExerciseMapper;
    private final WorkoutSetMapper workoutSetMapper;
    private final ExerciseMapper exerciseMapper;
    private final UserProfileMapper profileMapper;
    private final AiAdviceMapper adviceMapper;
    private final AiAdviceItemMapper adviceItemMapper;
    private final AiAdviceFeedbackMapper feedbackMapper;
    private final ObjectMapper objectMapper;

    public List<AdviceViews.Summary> list(Long userId) {
        List<AiAdvice> advices = adviceMapper.selectList(
                new LambdaQueryWrapper<AiAdvice>()
                        .eq(AiAdvice::getUserId, userId)
                        .orderByDesc(AiAdvice::getCreatedAt)
                        .orderByDesc(AiAdvice::getId)
        );
        return advices.stream().map(advice -> {
            Long count = adviceItemMapper.selectCount(
                    new LambdaQueryWrapper<AiAdviceItem>().eq(AiAdviceItem::getAdviceId, advice.getId())
            );
            return new AdviceViews.Summary(
                    advice.getId(),
                    advice.getPeriodStart(),
                    advice.getPeriodEnd(),
                    advice.getStatus(),
                    advice.getModelName(),
                    Boolean.TRUE.equals(advice.getFallbackUsed()),
                    advice.getCreatedAt(),
                    count == null ? 0 : count.intValue()
            );
        }).toList();
    }

    public AdviceViews.Detail detail(Long userId, Long adviceId) {
        AiAdvice advice = adviceMapper.selectById(adviceId);
        if (advice == null || !Objects.equals(advice.getUserId(), userId)) {
            throw new BusinessException(404, "建议记录不存在");
        }
        List<AiAdviceItem> items = adviceItemMapper.selectList(
                new LambdaQueryWrapper<AiAdviceItem>()
                        .eq(AiAdviceItem::getAdviceId, adviceId)
                        .orderByAsc(AiAdviceItem::getCreatedAt)
        );
        items.sort(Comparator.comparingInt(
                item -> SEVERITY_ORDER.getOrDefault(item.getSeverity(), 2)
        ));
        return new AdviceViews.Detail(
                advice.getId(),
                advice.getPeriodStart(),
                advice.getPeriodEnd(),
                advice.getStatus(),
                advice.getModelName(),
                Boolean.TRUE.equals(advice.getFallbackUsed()),
                advice.getCreatedAt(),
                items.stream().map(this::toItemView).toList()
        );
    }

    @Transactional
    public AdviceViews.Detail generate(Long userId) {
        LocalDate end = LocalDate.now(ZONE);
        LocalDate start = end.minusDays(13);

        List<Finding> findings = new ArrayList<>();
        findings.addAll(dataInsufficient(userId, end));
        findings.addAll(lowFrequency(userId));
        findings.addAll(volumeSpike(userId));
        findings.addAll(lowPlanCompletion(userId));
        findings.addAll(muscleBalance(userId));
        findings.addAll(weightStall(userId));
        findings.addAll(planDeviation(userId));
        findings.addAll(painFlag(userId));
        findings.addAll(bodyweightTrend(userId));

        findings.sort(Comparator
                .comparingInt((Finding finding) -> SEVERITY_ORDER.getOrDefault(finding.severity(), 2))
                .thenComparing(Finding::ruleCode));
        if (findings.size() > 5) {
            findings = new ArrayList<>(findings.subList(0, 5));
        }
        if (findings.isEmpty()) {
            findings.add(new Finding(
                    "DATA_INSUFFICIENT",
                    "INFO",
                    "数据不足，暂无法给出趋势判断",
                    Map.of("sessionCount", 0),
                    "继续记录 2—3 次训练后再分析；当前数据不足以判断趋势。"
            ));
        }

        AiAdvice advice = new AiAdvice();
        advice.setUserId(userId);
        advice.setPeriodStart(start);
        advice.setPeriodEnd(end);
        advice.setStatus("SUCCESS");
        advice.setModelName("rule-engine-v1");
        advice.setPromptVersion("v1");
        advice.setFallbackUsed(false);
        adviceMapper.insert(advice);

        int index = 0;
        for (Finding finding : findings) {
            AiAdviceItem item = new AiAdviceItem();
            item.setAdviceId(advice.getId());
            item.setFindingKey(finding.ruleCode() + ":" + index);
            item.setRuleCode(finding.ruleCode());
            item.setSeverity(finding.severity());
            item.setTitle(finding.title());
            item.setEvidenceJson(writeJson(finding.evidence()));
            item.setSuggestionText(finding.suggestion());
            item.setSource("RULE");
            adviceItemMapper.insert(item);
            index++;
        }
        return detail(userId, advice.getId());
    }

    @Transactional
    public void feedback(Long userId, Long adviceId, AdminRequests.Feedback request) {
        AiAdvice advice = adviceMapper.selectById(adviceId);
        if (advice == null || !Objects.equals(advice.getUserId(), userId)) {
            throw new BusinessException(404, "建议记录不存在");
        }
        AiAdviceFeedback feedback = feedbackMapper.selectOne(
                new LambdaQueryWrapper<AiAdviceFeedback>()
                        .eq(AiAdviceFeedback::getAdviceId, adviceId)
                        .eq(AiAdviceFeedback::getUserId, userId)
        );
        if (feedback == null) {
            feedback = new AiAdviceFeedback();
            feedback.setAdviceId(adviceId);
            feedback.setUserId(userId);
        }
        feedback.setRating(request.rating());
        feedback.setHelpful(request.helpful());
        feedback.setComment(request.comment());
        if (feedback.getId() == null) {
            feedbackMapper.insert(feedback);
        } else {
            feedbackMapper.updateById(feedback);
        }
    }

    private List<Finding> dataInsufficient(Long userId, LocalDate end) {
        List<WorkoutSession> sessions = completedSessions(userId, null, null);
        if (sessions.size() >= 3) {
            return List.of();
        }
        LocalDate first = sessions.stream()
                .map(WorkoutSession::getSessionDate)
                .min(Comparator.naturalOrder())
                .orElse(end);
        long daySpan = java.time.temporal.ChronoUnit.DAYS.between(first, end);
        return List.of(new Finding(
                "DATA_INSUFFICIENT",
                "INFO",
                "训练数据还不够，暂不判断趋势",
                Map.of("sessionCount", sessions.size(), "daySpan", daySpan),
                "继续记录 2—3 次训练后再分析；当前数据不足以判断趋势。"
        ));
    }

    private List<Finding> lowFrequency(Long userId) {
        StatsViews.Overview overview = statisticsService.overview(userId, 2);
        List<StatsViews.Week> weeks = overview.weeks();
        if (weeks.size() < 2) {
            return List.of();
        }
        StatsViews.Week last = weeks.get(weeks.size() - 1);
        StatsViews.Week previous = weeks.get(weeks.size() - 2);
        if (last.sessionCount() >= 2 || previous.sessionCount() >= 2) {
            return List.of();
        }
        int weeklyTarget = weeklyTarget(userId);
        return List.of(new Finding(
                "LOW_FREQUENCY",
                "WARN",
                "近两周训练频率偏低",
                Map.of(
                        "sessionsPerWeek", List.of(previous.sessionCount(), last.sessionCount()),
                        "weeklyTarget", weeklyTarget
                ),
                "先固定 2—3 个训练日，优先保证频率，再逐步增加训练量。"
        ));
    }

    private List<Finding> volumeSpike(Long userId) {
        StatsViews.Overview overview = statisticsService.overview(userId, 5);
        List<StatsViews.Week> weeks = overview.weeks();
        if (weeks.size() < 5) {
            return List.of();
        }
        BigDecimal current = weeks.get(weeks.size() - 1).volumeKg();
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int i = 0; i < weeks.size() - 1; i++) {
            sum = sum.add(weeks.get(i).volumeKg());
            count++;
        }
        BigDecimal average = count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        if (average.compareTo(BigDecimal.ZERO) <= 0
                || current.compareTo(average.multiply(new BigDecimal("1.3"))) <= 0) {
            return List.of();
        }
        BigDecimal increasePct = current.subtract(average)
                .divide(average, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
        return List.of(new Finding(
                "VOLUME_SPIKE",
                "WARN",
                "本周训练量上升较快",
                Map.of(
                        "weeklyVolumeKg", current,
                        "fourWeekAverageKg", average,
                        "increasePct", increasePct
                ),
                "本周不继续加量，优先完成原计划；下周再根据恢复情况调整。"
        ));
    }

    private List<Finding> lowPlanCompletion(Long userId) {
        StatsViews.Overview overview = statisticsService.overview(userId, 2);
        List<StatsViews.Week> weeks = overview.weeks();
        if (weeks.size() < 2) {
            return List.of();
        }
        StatsViews.Week last = weeks.get(weeks.size() - 1);
        StatsViews.Week previous = weeks.get(weeks.size() - 2);
        if (last.plannedSets() == 0 || previous.plannedSets() == 0
                || completion(last) >= 60 || completion(previous) >= 60) {
            return List.of();
        }
        return List.of(new Finding(
                "LOW_PLAN_COMPLETION",
                "WARN",
                "连续两周计划完成度偏低",
                Map.of(
                        "plannedSets", List.of(previous.plannedSets(), last.plannedSets()),
                        "completedSets", List.of(previous.completedSets(), last.completedSets()),
                        "completionPct", List.of(completion(previous), completion(last))
                ),
                "先把计划组数减少约 20%，或把训练日调整到更容易坚持的时间。"
        ));
    }

    private List<Finding> muscleBalance(Long userId) {
        List<StatsViews.MuscleWeek> weeks = statisticsService.muscleWeekly(userId, 2);
        if (weeks.size() < 2) {
            return List.of();
        }
        StatsViews.MuscleWeek lastWeek = weeks.get(weeks.size() - 1);
        StatsViews.MuscleWeek prevWeek = weeks.get(weeks.size() - 2);
        List<BigDecimal> trained = lastWeek.muscles().stream()
                .map(StatsViews.Muscle::effectiveSets)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .sorted()
                .toList();
        if (trained.isEmpty()) {
            return List.of();
        }
        BigDecimal median = trained.get(trained.size() / 2);
        BigDecimal underThreshold = new BigDecimal("4").max(median.multiply(new BigDecimal("0.5")));
        BigDecimal overThreshold = median.multiply(new BigDecimal("1.5"));

        Map<String, BigDecimal> prevByName = prevWeek.muscles().stream()
                .collect(Collectors.toMap(StatsViews.Muscle::code, StatsViews.Muscle::effectiveSets));
        List<Finding> findings = new ArrayList<>();
        for (StatsViews.Muscle muscle : lastWeek.muscles()) {
            BigDecimal prev = prevByName.get(muscle.code());
            if (prev == null) {
                continue;
            }
            BigDecimal current = muscle.effectiveSets();
            if (current.compareTo(underThreshold) < 0 && prev.compareTo(underThreshold) < 0) {
                findings.add(new Finding(
                        "MUSCLE_UNDERTRAINED",
                        "WARN",
                        "部位「" + muscle.name() + "」训练量偏少",
                        Map.of(
                                "muscleCode", muscle.code(),
                                "weeklyEffectiveSets", List.of(prev, current),
                                "medianEffectiveSets", median
                        ),
                        "下周为该部位增加 2—4 组，优先安排复合动作，并观察恢复情况。"
                ));
            }
            if (current.compareTo(overThreshold) > 0
                    && current.compareTo(new BigDecimal("20")) > 0
                    && prev.compareTo(overThreshold) > 0) {
                findings.add(new Finding(
                        "MUSCLE_OVERTRAINED",
                        "WARN",
                        "部位「" + muscle.name() + "」训练量偏多",
                        Map.of(
                                "muscleCode", muscle.code(),
                                "weeklyEffectiveSets", List.of(prev, current),
                                "medianEffectiveSets", median
                        ),
                        "保持或减少 2—4 组，优先保证动作质量和恢复，不继续加量。"
                ));
            }
        }
        return findings;
    }

    private List<Finding> weightStall(Long userId) {
        StatsViews.Overview overview = statisticsService.overview(userId, 8);
        List<Finding> findings = new ArrayList<>();
        for (StatsViews.ExerciseStat stat : overview.exercises()) {
            StatsViews.ExerciseTrend trend = statisticsService.exerciseTrend(userId, stat.exerciseId());
            if (trend == null) {
                continue;
            }
            List<BigDecimal> e1rms = trend.points().stream()
                    .map(StatsViews.Point::e1rmKg)
                    .filter(Objects::nonNull)
                    .toList();
            if (e1rms.size() < 3) {
                continue;
            }
            List<BigDecimal> recent = e1rms.subList(e1rms.size() - 3, e1rms.size());
            BigDecimal max = recent.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal min = recent.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if (max.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal changePct = max.subtract(min)
                    .divide(max, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
            if (changePct.compareTo(new BigDecimal("2")) > 0) {
                continue;
            }
            findings.add(new Finding(
                    "WEIGHT_STALL",
                    "WARN",
                    "动作「" + stat.exerciseName() + "」的力量近期停滞",
                    Map.of(
                            "exerciseId", stat.exerciseId(),
                            "exerciseName", stat.exerciseName(),
                            "sessions", e1rms.size(),
                            "bestE1rmKg", max,
                            "changePct", changePct
                    ),
                    "保持当前重量，尝试每组增加 1 次；若连续 2 次仍未完成，下周减载约 10% 后重建。"
            ));
        }
        return findings;
    }

    private List<Finding> planDeviation(Long userId) {
        List<WorkoutSession> sessions = completedSessions(userId, LocalDate.now(ZONE).minusWeeks(4), LocalDate.now(ZONE));
        if (sessions.size() < 4) {
            return List.of();
        }
        List<Long> sessionIds = sessions.stream().map(WorkoutSession::getId).toList();
        List<SessionExercise> exercises = sessionExerciseMapper.selectList(
                new LambdaQueryWrapper<SessionExercise>().in(SessionExercise::getSessionId, sessionIds)
        );
        Map<Long, List<SessionExercise>> bySession = exercises.stream()
                .collect(Collectors.groupingBy(SessionExercise::getSessionId));
        int deviationSessions = 0;
        List<String> swapped = new ArrayList<>();
        for (WorkoutSession session : sessions) {
            List<SessionExercise> list = bySession.getOrDefault(session.getId(), List.of());
            boolean deviated = list.stream().anyMatch(item ->
                    !"NONE".equals(item.getDeviationType()) && item.getDeviationType() != null);
            if (deviated) {
                deviationSessions++;
            }
            list.stream()
                    .filter(item -> "SWAPPED".equals(item.getDeviationType()))
                    .map(SessionExercise::getExerciseNameSnapshot)
                    .distinct()
                    .forEach(swapped::add);
        }
        double deviationPct = deviationSessions * 100.0 / sessions.size();
        if (deviationPct <= 30) {
            return List.of();
        }
        return List.of(new Finding(
                "PLAN_DEVIATION",
                "INFO",
                "近期计划与实际训练偏差较大",
                Map.of(
                        "deviationSessions", deviationSessions,
                        "totalSessions", sessions.size(),
                        "deviationPct", BigDecimal.valueOf(deviationPct).setScale(1, RoundingMode.HALF_UP),
                        "topSwappedExercises", swapped.stream().distinct().limit(5).toList()
                ),
                "把经常替换的动作直接写进计划，减少“计划一套、实际一套”的偏差。"
        ));
    }

    private List<Finding> painFlag(Long userId) {
        List<WorkoutSession> sessions = completedSessions(userId, LocalDate.now(ZONE).minusWeeks(4), LocalDate.now(ZONE));
        List<String> painKeywords = List.of("疼痛", "受伤", "痛", "拉伤", "扭伤");
        for (WorkoutSession session : sessions) {
            String note = session.getNote() == null ? "" : session.getNote();
            boolean pain = "PAIN".equals(session.getFeeling())
                    || painKeywords.stream().anyMatch(note::contains);
            if (pain) {
                return List.of(new Finding(
                        "PAIN_FLAG",
                        "ALERT",
                        "近期训练记录到疼痛信号",
                        Map.of("sessionDate", session.getSessionDate().toString(), "note", note),
                        "停止会加重疼痛的动作，记录出现时间和部位，并咨询医生或康复专业人士。"
                ));
            }
        }
        return List.of();
    }

    private List<Finding> bodyweightTrend(Long userId) {
        StatsViews.WeightTrend trend = statisticsService.weightTrend(userId);
        if (trend.points().size() < 2) {
            return List.of();
        }
        BigDecimal first = trend.points().get(0).movingAvg7();
        BigDecimal last = trend.points().get(trend.points().size() - 1).movingAvg7();
        if (first.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        BigDecimal changePct = last.subtract(first)
                .divide(first, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
        if (changePct.abs().compareTo(BigDecimal.ONE) <= 0) {
            return List.of();
        }
        String goal = goal(userId);
        boolean increasing = changePct.compareTo(BigDecimal.ZERO) > 0;
        boolean inconsistent = ("FAT_LOSS".equals(goal) && increasing)
                || (("HYPERTROPHY".equals(goal) || "STRENGTH".equals(goal)) && !increasing);
        if (!inconsistent) {
            return List.of();
        }
        return List.of(new Finding(
                "BODYWEIGHT_TREND",
                "INFO",
                "体重变化方向与训练目标不一致",
                Map.of("weeklyChangePct", changePct, "goal", goal),
                "先确认记录是否稳定；体重趋势仅作参考，不提供饮食或医疗处方。"
        ));
    }

    private List<WorkoutSession> completedSessions(Long userId, LocalDate from, LocalDate to) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getStatus, "COMPLETED")
                        .ge(from != null, WorkoutSession::getSessionDate, from)
                        .le(to != null, WorkoutSession::getSessionDate, to)
                        .orderByAsc(WorkoutSession::getSessionDate)
        );
    }

    private double completion(StatsViews.Week week) {
        if (week.plannedSets() == 0) {
            return 0;
        }
        return week.completedSets() * 100.0 / week.plannedSets();
    }

    private int weeklyTarget(Long userId) {
        UserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );
        return profile == null || profile.getWeeklyTarget() == null ? 3 : profile.getWeeklyTarget();
    }

    private String goal(Long userId) {
        UserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );
        return profile == null || profile.getGoal() == null ? "GENERAL" : profile.getGoal();
    }

    private AdviceViews.Item toItemView(AiAdviceItem item) {
        Object evidence;
        try {
            evidence = objectMapper.readValue(item.getEvidenceJson(), Object.class);
        } catch (Exception e) {
            evidence = Map.of();
        }
        return new AdviceViews.Item(
                item.getFindingKey(),
                item.getRuleCode(),
                item.getSeverity(),
                item.getTitle(),
                evidence,
                item.getSuggestionText(),
                item.getSource()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private record Finding(String ruleCode, String severity, String title,
                           Map<String, Object> evidence, String suggestion) {
    }
}
