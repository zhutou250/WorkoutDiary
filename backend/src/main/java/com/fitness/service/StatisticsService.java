package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.dto.StatsViews;
import com.fitness.entity.BodyMetric;
import com.fitness.entity.Exercise;
import com.fitness.entity.ExerciseMuscle;
import com.fitness.entity.MuscleGroup;
import com.fitness.entity.SessionExercise;
import com.fitness.entity.WorkoutSession;
import com.fitness.entity.WorkoutSet;
import com.fitness.mapper.BodyMetricMapper;
import com.fitness.mapper.ExerciseMapper;
import com.fitness.mapper.ExerciseMuscleMapper;
import com.fitness.mapper.MuscleGroupMapper;
import com.fitness.mapper.SessionExerciseMapper;
import com.fitness.mapper.WorkoutSessionMapper;
import com.fitness.mapper.WorkoutSetMapper;
import com.fitness.statistics.StatisticsCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final WorkoutSessionMapper sessionMapper;
    private final SessionExerciseMapper sessionExerciseMapper;
    private final WorkoutSetMapper workoutSetMapper;
    private final ExerciseMapper exerciseMapper;
    private final ExerciseMuscleMapper exerciseMuscleMapper;
    private final MuscleGroupMapper muscleGroupMapper;
    private final BodyMetricMapper bodyMetricMapper;

    public StatsViews.Overview overview(Long userId, int weeks) {
        int window = Math.min(52, Math.max(1, weeks));
        LocalDate weekStart = LocalDate.now(ZONE).with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate periodStart = weekStart.minusWeeks(window - 1L);

        List<WorkoutSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getStatus, "COMPLETED")
                        .ge(WorkoutSession::getSessionDate, periodStart)
                        .orderByAsc(WorkoutSession::getSessionDate)
        );

        SessionData data = loadSessionData(sessions);
        Map<Long, Exercise> exerciseMap = loadExercises(data.exerciseIds());
        Map<Long, List<ExerciseMuscle>> muscleLinks = loadMuscleLinks(data.exerciseIds());
        Map<Long, MuscleGroup> muscleMap = loadMuscleMap();

        BigDecimal totalVolume = BigDecimal.ZERO;
        int completedSets = 0;
        int plannedSets = 0;
        Map<LocalDate, WeekAccumulator> weekAccumulators = new LinkedHashMap<>();
        Map<Long, BigDecimal> muscleSets = new LinkedHashMap<>();
        Map<Long, BigDecimal> exerciseVolume = new LinkedHashMap<>();
        Map<Long, BigDecimal> exerciseBest1Rm = new LinkedHashMap<>();

        for (WorkoutSession session : sessions) {
            LocalDate weekOf = session.getSessionDate().with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
            WeekAccumulator week = weekAccumulators.computeIfAbsent(weekOf, key -> new WeekAccumulator());

            List<SessionExercise> exercises = data.exercisesBySession().getOrDefault(session.getId(), List.of());
            int sessionPlanned = 0;
            int sessionCompleted = 0;
            BigDecimal sessionVolume = BigDecimal.ZERO;

            for (SessionExercise item : exercises) {
                if (item.getPlannedSets() != null) {
                    plannedSets += item.getPlannedSets();
                    sessionPlanned += item.getPlannedSets();
                }
                for (WorkoutSet set : data.setsByExercise().getOrDefault(item.getId(), List.of())) {
                    if (!isEffectiveSet(set)) {
                        continue;
                    }
                    completedSets++;
                    sessionCompleted++;

                    BigDecimal volume = StatisticsCalculator.volume(set.getWeightKg(), set.getReps());
                    totalVolume = totalVolume.add(volume);
                    sessionVolume = sessionVolume.add(volume);
                    exerciseVolume.merge(item.getExerciseId(), volume, BigDecimal::add);

                    BigDecimal side = StatisticsCalculator.sideFactor(set.getSide());
                    for (ExerciseMuscle link : muscleLinks.getOrDefault(item.getExerciseId(), List.of())) {
                        BigDecimal contribution = side.multiply(
                                link.getWeight() == null ? BigDecimal.ONE : link.getWeight()
                        );
                        muscleSets.merge(link.getMuscleGroupId(), contribution, BigDecimal::add);
                    }

                    Exercise exercise = exerciseMap.get(item.getExerciseId());
                    if (exercise != null && "REPS".equals(exercise.getMode()) && set.getReps() != null) {
                        BigDecimal e1rm = StatisticsCalculator.epley1Rm(set.getWeightKg(), set.getReps());
                        if (e1rm != null
                                && e1rm.compareTo(exerciseBest1Rm.getOrDefault(item.getExerciseId(), BigDecimal.ZERO)) > 0) {
                            exerciseBest1Rm.put(item.getExerciseId(), e1rm);
                        }
                    }
                }
            }

            week.dates.add(session.getSessionDate());
            week.completedSets += sessionCompleted;
            week.plannedSets += sessionPlanned;
            week.volume = week.volume.add(sessionVolume);
        }

        List<StatsViews.Week> weekViews = new ArrayList<>();
        for (LocalDate day = periodStart; !day.isAfter(weekStart); day = day.plusWeeks(1)) {
            WeekAccumulator acc = weekAccumulators.getOrDefault(day, new WeekAccumulator());
            weekViews.add(new StatsViews.Week(
                    day, acc.dates.size(), acc.completedSets, acc.plannedSets, acc.volume
            ));
        }

        List<StatsViews.Muscle> muscleViews = muscleSetViews(muscleSets, muscleMap);
        List<StatsViews.ExerciseStat> exerciseStats = exerciseVolume.entrySet().stream()
                .map(entry -> new StatsViews.ExerciseStat(
                        entry.getKey(),
                        exerciseName(exerciseMap, entry.getKey()),
                        entry.getValue(),
                        exerciseBest1Rm.get(entry.getKey())
                ))
                .sorted(Comparator.comparing(StatsViews.ExerciseStat::volumeKg).reversed())
                .limit(10)
                .toList();

        BigDecimal completion = StatisticsCalculator.completionPct(completedSets, plannedSets);
        return new StatsViews.Overview(
                periodStart,
                weekStart,
                totalVolume,
                sessions.size(),
                sessions.size(),
                completedSets,
                plannedSets,
                completion,
                weekViews,
                muscleViews,
                exerciseStats
        );
    }

    public StatsViews.ExerciseTrend exerciseTrend(Long userId, Long exerciseId) {
        Exercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null) {
            return null;
        }

        List<WorkoutSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getStatus, "COMPLETED")
                        .orderByAsc(WorkoutSession::getSessionDate)
        );
        if (sessions.isEmpty()) {
            return new StatsViews.ExerciseTrend(exerciseId, exercise.getName(), List.of(), null);
        }

        List<Long> sessionIds = sessions.stream().map(WorkoutSession::getId).toList();
        List<SessionExercise> sessionExercises = sessionExerciseMapper.selectList(
                new LambdaQueryWrapper<SessionExercise>()
                        .in(SessionExercise::getSessionId, sessionIds)
                        .eq(SessionExercise::getExerciseId, exerciseId)
        );
        List<Long> seIds = sessionExercises.stream().map(SessionExercise::getId).toList();
        List<WorkoutSet> sets = seIds.isEmpty() ? List.of() : workoutSetMapper.selectList(
                new LambdaQueryWrapper<WorkoutSet>().in(WorkoutSet::getSessionExerciseId, seIds)
        );

        Map<Long, Long> sessionIdByExercise = sessionExercises.stream()
                .collect(Collectors.toMap(SessionExercise::getId, SessionExercise::getSessionId));
        Map<Long, LocalDate> dateBySession = sessions.stream()
                .collect(Collectors.toMap(WorkoutSession::getId, WorkoutSession::getSessionDate));
        Map<Long, List<WorkoutSet>> setsByExercise = sets.stream()
                .collect(Collectors.groupingBy(WorkoutSet::getSessionExerciseId));

        List<StatsViews.Point> points = new ArrayList<>();
        StatsViews.Point pr = null;
        for (SessionExercise item : sessionExercises) {
            LocalDate date = dateBySession.get(sessionIdByExercise.get(item.getId()));
            BigDecimal volume = BigDecimal.ZERO;
            BigDecimal best = null;
            for (WorkoutSet set : setsByExercise.getOrDefault(item.getId(), List.of())) {
                if (!isEffectiveSet(set)) {
                    continue;
                }
                volume = volume.add(StatisticsCalculator.volume(set.getWeightKg(), set.getReps()));
                if ("REPS".equals(exercise.getMode()) && set.getReps() != null) {
                    BigDecimal e1rm = StatisticsCalculator.epley1Rm(set.getWeightKg(), set.getReps());
                    if (e1rm != null && (best == null || e1rm.compareTo(best) > 0)) {
                        best = e1rm;
                    }
                }
            }
            StatsViews.Point point = new StatsViews.Point(date, best, volume);
            points.add(point);
            if (best != null && (pr == null || (pr.e1rmKg() != null && best.compareTo(pr.e1rmKg()) > 0))) {
                pr = point;
            }
        }

        points.sort(Comparator.comparing(StatsViews.Point::date));
        return new StatsViews.ExerciseTrend(exerciseId, exercise.getName(), points, pr);
    }

    public StatsViews.WeightTrend weightTrend(Long userId) {
        List<BodyMetric> metrics = bodyMetricMapper.selectList(
                new LambdaQueryWrapper<BodyMetric>()
                        .eq(BodyMetric::getUserId, userId)
                        .orderByAsc(BodyMetric::getRecordDate)
        );
        List<StatsViews.WeightPoint> points = new ArrayList<>();
        for (int i = 0; i < metrics.size(); i++) {
            BodyMetric current = metrics.get(i);
            if (current.getWeightKg() == null) {
                continue;
            }
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            LocalDate from = current.getRecordDate().minusDays(6);
            for (int j = 0; j <= i; j++) {
                BodyMetric prev = metrics.get(j);
                if (prev.getWeightKg() != null
                        && !prev.getRecordDate().isBefore(from)
                        && !prev.getRecordDate().isAfter(current.getRecordDate())) {
                    sum = sum.add(prev.getWeightKg());
                    count++;
                }
            }
            BigDecimal avg = count > 0
                    ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : current.getWeightKg();
            points.add(new StatsViews.WeightPoint(current.getRecordDate(), current.getWeightKg(), avg));
        }
        return new StatsViews.WeightTrend(points);
    }

    public List<StatsViews.MuscleWeek> muscleWeekly(Long userId, int weeks) {
        int window = Math.min(52, Math.max(1, weeks));
        LocalDate weekStart = LocalDate.now(ZONE).with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate periodStart = weekStart.minusWeeks(window - 1L);

        List<WorkoutSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getStatus, "COMPLETED")
                        .ge(WorkoutSession::getSessionDate, periodStart)
        );
        if (sessions.isEmpty()) {
            return List.of();
        }

        SessionData data = loadSessionData(sessions);
        Map<Long, List<ExerciseMuscle>> muscleLinks = loadMuscleLinks(data.exerciseIds());
        Map<Long, MuscleGroup> muscleMap = loadMuscleMap();
        Map<LocalDate, Map<Long, BigDecimal>> byWeek = new LinkedHashMap<>();

        for (WorkoutSession session : sessions) {
            LocalDate weekOf = session.getSessionDate().with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
            Map<Long, BigDecimal> muscleSets = byWeek.computeIfAbsent(weekOf, key -> new LinkedHashMap<>());
            for (SessionExercise item : data.exercisesBySession().getOrDefault(session.getId(), List.of())) {
                for (WorkoutSet set : data.setsByExercise().getOrDefault(item.getId(), List.of())) {
                    if (!isEffectiveSet(set)) {
                        continue;
                    }
                    BigDecimal side = StatisticsCalculator.sideFactor(set.getSide());
                    for (ExerciseMuscle link : muscleLinks.getOrDefault(item.getExerciseId(), List.of())) {
                        BigDecimal contribution = side.multiply(
                                link.getWeight() == null ? BigDecimal.ONE : link.getWeight()
                        );
                        muscleSets.merge(link.getMuscleGroupId(), contribution, BigDecimal::add);
                    }
                }
            }
        }

        List<StatsViews.MuscleWeek> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Map<Long, BigDecimal>> entry : byWeek.entrySet()) {
            result.add(new StatsViews.MuscleWeek(
                    entry.getKey(),
                    muscleSetViews(entry.getValue(), muscleMap)
            ));
        }
        result.sort(Comparator.comparing(StatsViews.MuscleWeek::weekStart));
        return result;
    }

    public StatsViews.Calendar calendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate first = yearMonth.atDay(1);
        LocalDate last = yearMonth.atEndOfMonth();

        List<WorkoutSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getStatus, "COMPLETED")
                        .ge(WorkoutSession::getSessionDate, first)
                        .le(WorkoutSession::getSessionDate, last)
        );
        Map<LocalDate, DayAccumulator> byDate = new HashMap<>();
        if (!sessions.isEmpty()) {
            SessionData data = loadSessionData(sessions);
            for (WorkoutSession session : sessions) {
                DayAccumulator day = byDate.computeIfAbsent(session.getSessionDate(), key -> new DayAccumulator());
                for (SessionExercise item : data.exercisesBySession().getOrDefault(session.getId(), List.of())) {
                    for (WorkoutSet set : data.setsByExercise().getOrDefault(item.getId(), List.of())) {
                        if (!isEffectiveSet(set)) {
                            continue;
                        }
                        day.completedSets++;
                        day.volume = day.volume.add(StatisticsCalculator.volume(set.getWeightKg(), set.getReps()));
                    }
                }
            }
        }

        List<StatsViews.Day> days = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            DayAccumulator acc = byDate.getOrDefault(day, new DayAccumulator());
            days.add(new StatsViews.Day(day, acc.completedSets > 0, acc.completedSets, acc.volume));
        }
        return new StatsViews.Calendar(days);
    }

    private SessionData loadSessionData(List<WorkoutSession> sessions) {
        if (sessions.isEmpty()) {
            return new SessionData(Map.of(), Map.of(), Set.of());
        }
        List<Long> sessionIds = sessions.stream().map(WorkoutSession::getId).toList();
        List<SessionExercise> exercises = sessionExerciseMapper.selectList(
                new LambdaQueryWrapper<SessionExercise>()
                        .in(SessionExercise::getSessionId, sessionIds)
                        .orderByAsc(SessionExercise::getSortNo)
        );
        List<Long> exerciseIds = exercises.stream().map(SessionExercise::getId).toList();
        List<WorkoutSet> sets = exerciseIds.isEmpty() ? List.of() : workoutSetMapper.selectList(
                new LambdaQueryWrapper<WorkoutSet>().in(WorkoutSet::getSessionExerciseId, exerciseIds)
        );
        Map<Long, List<SessionExercise>> exercisesBySession = exercises.stream()
                .collect(Collectors.groupingBy(SessionExercise::getSessionId));
        Map<Long, List<WorkoutSet>> setsByExercise = sets.stream()
                .collect(Collectors.groupingBy(WorkoutSet::getSessionExerciseId));
        Set<Long> actualExerciseIds = exercises.stream()
                .map(SessionExercise::getExerciseId)
                .collect(Collectors.toSet());
        return new SessionData(exercisesBySession, setsByExercise, actualExerciseIds);
    }

    private Map<Long, Exercise> loadExercises(Set<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Map.of();
        }
        return exerciseMapper.selectBatchIds(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));
    }

    private Map<Long, List<ExerciseMuscle>> loadMuscleLinks(Set<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Map.of();
        }
        return exerciseMuscleMapper.selectList(
                        new LambdaQueryWrapper<ExerciseMuscle>().in(ExerciseMuscle::getExerciseId, exerciseIds)
                ).stream()
                .collect(Collectors.groupingBy(ExerciseMuscle::getExerciseId));
    }

    private Map<Long, MuscleGroup> loadMuscleMap() {
        return muscleGroupMapper.selectList(null).stream()
                .collect(Collectors.toMap(MuscleGroup::getId, Function.identity()));
    }

    private List<StatsViews.Muscle> muscleSetViews(Map<Long, BigDecimal> muscleSets,
                                                   Map<Long, MuscleGroup> muscleMap) {
        List<MuscleGroup> groups = muscleMap.values().stream()
                .sorted(Comparator.comparing(MuscleGroup::getSortNo))
                .toList();
        List<StatsViews.Muscle> result = new ArrayList<>();
        for (MuscleGroup group : groups) {
            BigDecimal sets = muscleSets.getOrDefault(group.getId(), BigDecimal.ZERO);
            if (sets.compareTo(BigDecimal.ZERO) > 0) {
                result.add(new StatsViews.Muscle(group.getCode(), group.getName(), sets.setScale(1, RoundingMode.HALF_UP), null));
            }
        }
        return result;
    }

    private String exerciseName(Map<Long, Exercise> exerciseMap, Long id) {
        Exercise exercise = exerciseMap.get(id);
        return exercise == null ? "已归档动作" : exercise.getName();
    }

    private boolean isEffectiveSet(WorkoutSet set) {
        return "WORK".equals(set.getPhase()) && Boolean.TRUE.equals(set.getIsCompleted());
    }

    private record SessionData(
            Map<Long, List<SessionExercise>> exercisesBySession,
            Map<Long, List<WorkoutSet>> setsByExercise,
            Set<Long> exerciseIds
    ) {
    }

    private static final class WeekAccumulator {
        private final Set<LocalDate> dates = new LinkedHashSet<>();
        private int completedSets;
        private int plannedSets;
        private BigDecimal volume = BigDecimal.ZERO;
    }

    private static final class DayAccumulator {
        private int completedSets;
        private BigDecimal volume = BigDecimal.ZERO;
    }
}
