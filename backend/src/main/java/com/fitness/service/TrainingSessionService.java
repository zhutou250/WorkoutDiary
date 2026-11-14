package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.dto.SessionRequests;
import com.fitness.dto.SessionViews;
import com.fitness.entity.Exercise;
import com.fitness.entity.PlanDay;
import com.fitness.entity.PlanExercise;
import com.fitness.entity.SessionExercise;
import com.fitness.entity.WorkoutPlan;
import com.fitness.entity.WorkoutSession;
import com.fitness.entity.WorkoutSet;
import com.fitness.mapper.ExerciseMapper;
import com.fitness.mapper.PlanDayMapper;
import com.fitness.mapper.PlanExerciseMapper;
import com.fitness.mapper.SessionExerciseMapper;
import com.fitness.mapper.WorkoutSessionMapper;
import com.fitness.mapper.WorkoutSetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingSessionService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> SESSION_STATUSES = Set.of("IN_PROGRESS", "COMPLETED", "CANCELLED");
    private static final Set<String> PHASES = Set.of("WARMUP", "WORK");
    private static final Set<String> SET_TYPES = Set.of("STRAIGHT", "DROP", "REST_PAUSE");
    private static final Set<String> SIDES = Set.of("BOTH", "L", "R");
    private static final Set<String> FEELINGS = Set.of("GREAT", "OK", "TIRED", "PAIN");

    private final WorkoutSessionMapper sessionMapper;
    private final SessionExerciseMapper sessionExerciseMapper;
    private final WorkoutSetMapper workoutSetMapper;
    private final PlanDayMapper dayMapper;
    private final PlanExerciseMapper planExerciseMapper;
    private final ExerciseMapper exerciseMapper;
    private final TrainingPlanService planService;

    public PageResult<SessionViews.Summary> list(Long userId, String status, long page, long size) {
        if (status != null && !status.isBlank() && !SESSION_STATUSES.contains(status)) {
            throw new BusinessException("训练状态取值不正确");
        }

        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        Page<WorkoutSession> result = sessionMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(status != null && !status.isBlank(), WorkoutSession::getStatus, status)
                        .orderByDesc(WorkoutSession::getSessionDate)
                        .orderByDesc(WorkoutSession::getId)
        );

        List<SessionViews.Summary> records = result.getRecords().stream()
                .map(this::toSummary)
                .toList();
        return new PageResult<>(result.getTotal(), records);
    }

    public SessionViews.Detail active(Long userId) {
        List<WorkoutSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getStatus, "IN_PROGRESS")
                        .orderByDesc(WorkoutSession::getStartedAt)
                        .last("LIMIT 1")
        );
        return sessions.isEmpty() ? null : detail(userId, sessions.get(0).getId());
    }

    @Transactional
    public SessionViews.Detail start(Long userId, Long planDayId) {
        PlanDay day = dayMapper.selectById(planDayId);
        if (day == null) {
            throw new BusinessException(404, "训练日不存在");
        }
        WorkoutPlan plan = planService.requirePlan(day.getPlanId(), userId);
        if ("ARCHIVED".equals(plan.getStatus())) {
            throw new BusinessException("已归档的计划不能开始训练");
        }

        ensureNoActive(userId);

        List<PlanExercise> planExercises = planExerciseMapper.selectList(
                new LambdaQueryWrapper<PlanExercise>()
                        .eq(PlanExercise::getPlanDayId, planDayId)
                        .orderByAsc(PlanExercise::getSortNo)
        );
        if (planExercises.isEmpty()) {
            throw new BusinessException("该训练日还没有动作，请先完善计划");
        }

        LocalDateTime now = LocalDateTime.now(ZONE);
        WorkoutSession session = new WorkoutSession();
        session.setUserId(userId);
        session.setPlanDayId(day.getId());
        session.setPlanNameSnapshot(plan.getName());
        session.setPlanDayTitleSnapshot(day.getTitle());
        session.setSessionDate(now.toLocalDate());
        session.setStartedAt(now);
        session.setStatus("IN_PROGRESS");
        sessionMapper.insert(session);

        for (PlanExercise planExercise : planExercises) {
            Exercise exercise = exerciseMapper.selectById(planExercise.getExerciseId());
            SessionExercise item = new SessionExercise();
            item.setSessionId(session.getId());
            item.setExerciseId(planExercise.getExerciseId());
            item.setPlanExerciseId(planExercise.getId());
            item.setExerciseNameSnapshot(exercise == null ? "已归档动作" : exercise.getName());
            item.setSortNo(planExercise.getSortNo());
            item.setPlannedSets(planExercise.getTargetSets());
            item.setPlannedRepsMin(planExercise.getTargetRepsMin());
            item.setPlannedRepsMax(planExercise.getTargetRepsMax());
            item.setPlannedWeightKg(planExercise.getTargetWeightKg());
            item.setPlannedDurationSec(planExercise.getTargetDurationSec());
            item.setDeviationType("NONE");
            sessionExerciseMapper.insert(item);
        }

        return detail(userId, session.getId());
    }

    @Transactional
    public SessionViews.Detail startFree(Long userId) {
        ensureNoActive(userId);
        LocalDateTime now = LocalDateTime.now(ZONE);
        WorkoutSession session = new WorkoutSession();
        session.setUserId(userId);
        session.setSessionDate(now.toLocalDate());
        session.setStartedAt(now);
        session.setStatus("IN_PROGRESS");
        sessionMapper.insert(session);
        return detail(userId, session.getId());
    }

    public SessionViews.Detail detail(Long userId, Long sessionId) {
        WorkoutSession session = requireSession(sessionId, userId);
        List<SessionExercise> exercises = sessionExerciseMapper.selectList(
                new LambdaQueryWrapper<SessionExercise>()
                        .eq(SessionExercise::getSessionId, sessionId)
                        .orderByAsc(SessionExercise::getSortNo)
                        .orderByAsc(SessionExercise::getId)
        );

        List<Long> exerciseIds = exercises.stream().map(SessionExercise::getId).toList();
        List<WorkoutSet> allSets = exerciseIds.isEmpty()
                ? List.of()
                : workoutSetMapper.selectList(
                        new LambdaQueryWrapper<WorkoutSet>()
                                .in(WorkoutSet::getSessionExerciseId, exerciseIds)
                                .orderByAsc(WorkoutSet::getSetNo)
                                .orderByAsc(WorkoutSet::getId)
                );
        Map<Long, List<WorkoutSet>> setsByExercise = allSets.stream()
                .collect(Collectors.groupingBy(WorkoutSet::getSessionExerciseId));

        Set<Long> actualExerciseIds = exercises.stream()
                .map(SessionExercise::getExerciseId)
                .collect(Collectors.toSet());
        Map<Long, Exercise> exerciseMap = actualExerciseIds.isEmpty()
                ? Collections.emptyMap()
                : exerciseMapper.selectBatchIds(actualExerciseIds).stream()
                        .collect(Collectors.toMap(Exercise::getId, Function.identity()));

        List<SessionViews.Exercise> exerciseViews = exercises.stream()
                .map(item -> toExerciseView(
                        item,
                        exerciseMap.get(item.getExerciseId()),
                        setsByExercise.getOrDefault(item.getId(), List.of())
                ))
                .toList();

        int completedSets = exerciseViews.stream().mapToInt(SessionViews.Exercise::completedSets).sum();
        int totalSets = exerciseViews.stream()
                .mapToInt(item -> item.sets().size())
                .sum();
        BigDecimal volume = exerciseViews.stream()
                .map(SessionViews.Exercise::volumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SessionViews.Detail(
                session.getId(),
                session.getPlanDayId(),
                session.getPlanNameSnapshot(),
                session.getPlanDayTitleSnapshot(),
                session.getSessionDate(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getStatus(),
                session.getOverallRpe(),
                session.getFeeling(),
                session.getNote(),
                exerciseViews.size(),
                completedSets,
                totalSets,
                volume,
                exerciseViews
        );
    }

    @Transactional
    public SessionViews.Detail addExercise(Long userId, Long sessionId, SessionRequests.AddExercise request) {
        WorkoutSession session = requireSession(sessionId, userId);
        requireInProgress(session);
        Exercise exercise = requireExercise(request.exerciseId());

        List<SessionExercise> existing = sessionExerciseMapper.selectList(
                new LambdaQueryWrapper<SessionExercise>()
                        .eq(SessionExercise::getSessionId, sessionId)
                        .orderByDesc(SessionExercise::getSortNo)
        );
        int nextSort = existing.isEmpty() || existing.get(0).getSortNo() == null
                ? 1
                : existing.get(0).getSortNo() + 1;

        SessionExercise item = new SessionExercise();
        item.setSessionId(sessionId);
        item.setExerciseId(exercise.getId());
        item.setExerciseNameSnapshot(exercise.getName());
        item.setSortNo(nextSort);
        item.setDeviationType("ADDED");
        item.setActualNote(request.actualNote());
        sessionExerciseMapper.insert(item);
        return detail(userId, sessionId);
    }

    @Transactional
    public SessionViews.Detail replaceExercise(Long userId, Long sessionId, Long sessionExerciseId,
                                               SessionRequests.ReplaceExercise request) {
        WorkoutSession session = requireSession(sessionId, userId);
        requireInProgress(session);
        SessionExercise item = requireSessionExercise(sessionId, sessionExerciseId);
        Exercise exercise = requireExercise(request.exerciseId());

        item.setExerciseId(exercise.getId());
        item.setExerciseNameSnapshot(exercise.getName());
        item.setDeviationType("SWAPPED");
        sessionExerciseMapper.updateById(item);
        return detail(userId, sessionId);
    }

    @Transactional
    public SessionViews.Detail skipExercise(Long userId, Long sessionId, Long sessionExerciseId) {
        WorkoutSession session = requireSession(sessionId, userId);
        requireInProgress(session);
        SessionExercise item = requireSessionExercise(sessionId, sessionExerciseId);
        Long setCount = workoutSetMapper.selectCount(
                new LambdaQueryWrapper<WorkoutSet>().eq(WorkoutSet::getSessionExerciseId, sessionExerciseId)
        );
        if (setCount != null && setCount > 0) {
            throw new BusinessException("该动作已有组记录，不能直接跳过");
        }
        item.setDeviationType("SKIPPED");
        sessionExerciseMapper.updateById(item);
        return detail(userId, sessionId);
    }

    @Transactional
    public SessionViews.SetItem addSet(Long userId, Long sessionId, Long sessionExerciseId,
                                       SessionRequests.SetUpsert request) {
        WorkoutSession session = requireSession(sessionId, userId);
        requireInProgress(session);
        requireSessionExercise(sessionId, sessionExerciseId);

        String side = normalize(request.side(), "BOTH", SIDES, "支撑侧");
        int setNo = request.setNo() == null
                ? nextSetNo(sessionExerciseId)
                : request.setNo();
        Long duplicates = workoutSetMapper.selectCount(
                new LambdaQueryWrapper<WorkoutSet>()
                        .eq(WorkoutSet::getSessionExerciseId, sessionExerciseId)
                        .eq(WorkoutSet::getSetNo, setNo)
                        .eq(WorkoutSet::getSide, side)
        );
        if (duplicates != null && duplicates > 0) {
            throw new BusinessException("这一组已经存在，请直接编辑");
        }

        WorkoutSet set = new WorkoutSet();
        set.setSessionExerciseId(sessionExerciseId);
        set.setSetNo(setNo);
        set.setPhase(normalize(request.phase(), "WORK", PHASES, "组类型"));
        set.setSetType(normalize(request.setType(), "STRAIGHT", SET_TYPES, "组方式"));
        set.setSide(side);
        set.setWeightKg(request.weightKg());
        set.setReps(request.reps());
        set.setDurationSec(request.durationSec());
        set.setDistanceM(request.distanceM());
        set.setRir(request.rir());
        set.setRpe(request.rpe());
        set.setIsFailure(Boolean.TRUE.equals(request.isFailure()));
        set.setIsCompleted(request.isCompleted() == null || request.isCompleted());
        set.setCompletedAt(set.getIsCompleted() ? LocalDateTime.now(ZONE) : null);
        set.setNote(request.note());
        validateSetValues(set);
        workoutSetMapper.insert(set);
        return toSetView(set);
    }

    @Transactional
    public SessionViews.SetItem updateSet(Long userId, Long setId, SessionRequests.SetUpsert request) {
        OwnedSet owned = requireOwnedSet(setId, userId);
        WorkoutSet set = owned.set();
        requireInProgress(owned.session());

        if (request.setNo() != null && !Objects.equals(request.setNo(), set.getSetNo())) {
            String side = request.side() == null ? set.getSide() : normalize(request.side(), "BOTH", SIDES, "支撑侧");
            Long duplicates = workoutSetMapper.selectCount(
                    new LambdaQueryWrapper<WorkoutSet>()
                            .eq(WorkoutSet::getSessionExerciseId, set.getSessionExerciseId())
                            .eq(WorkoutSet::getSetNo, request.setNo())
                            .eq(WorkoutSet::getSide, side)
                            .ne(WorkoutSet::getId, setId)
            );
            if (duplicates != null && duplicates > 0) {
                throw new BusinessException("这一组已经存在");
            }
            set.setSetNo(request.setNo());
        }
        if (request.phase() != null) {
            set.setPhase(normalize(request.phase(), "WORK", PHASES, "组类型"));
        }
        if (request.setType() != null) {
            set.setSetType(normalize(request.setType(), "STRAIGHT", SET_TYPES, "组方式"));
        }
        if (request.side() != null) {
            set.setSide(normalize(request.side(), "BOTH", SIDES, "支撑侧"));
        }
        if (request.weightKg() != null) {
            set.setWeightKg(request.weightKg());
        }
        if (request.reps() != null) {
            set.setReps(request.reps());
        }
        if (request.durationSec() != null) {
            set.setDurationSec(request.durationSec());
        }
        if (request.distanceM() != null) {
            set.setDistanceM(request.distanceM());
        }
        if (request.rir() != null) {
            set.setRir(request.rir());
        }
        if (request.rpe() != null) {
            set.setRpe(request.rpe());
        }
        if (request.isFailure() != null) {
            set.setIsFailure(request.isFailure());
        }
        if (request.isCompleted() != null) {
            set.setIsCompleted(request.isCompleted());
            set.setCompletedAt(request.isCompleted() ? LocalDateTime.now(ZONE) : null);
        }
        if (request.note() != null) {
            set.setNote(request.note());
        }
        validateSetValues(set);
        workoutSetMapper.updateById(set);
        return toSetView(set);
    }

    @Transactional
    public void deleteSet(Long userId, Long setId) {
        OwnedSet owned = requireOwnedSet(setId, userId);
        requireInProgress(owned.session());
        workoutSetMapper.deleteById(setId);
    }

    @Transactional
    public SessionViews.Detail finish(Long userId, Long sessionId, SessionRequests.Finish request) {
        WorkoutSession session = requireSession(sessionId, userId);
        requireInProgress(session);

        List<Long> exerciseIds = sessionExerciseMapper.selectList(
                new LambdaQueryWrapper<SessionExercise>().eq(SessionExercise::getSessionId, sessionId)
        ).stream().map(SessionExercise::getId).toList();
        Long completedWorkSets = exerciseIds.isEmpty() ? 0L : workoutSetMapper.selectCount(
                new LambdaQueryWrapper<WorkoutSet>()
                        .in(WorkoutSet::getSessionExerciseId, exerciseIds)
                        .eq(WorkoutSet::getPhase, "WORK")
                        .eq(WorkoutSet::getIsCompleted, true)
        );
        if (completedWorkSets == null || completedWorkSets == 0) {
            throw new BusinessException("至少记录一组已完成的正式组后才能结束训练");
        }

        if (request.feeling() != null && !request.feeling().isBlank()) {
            session.setFeeling(normalize(request.feeling(), null, FEELINGS, "训练感受"));
        }
        session.setOverallRpe(request.overallRpe());
        session.setNote(request.note());
        session.setStatus("COMPLETED");
        session.setEndedAt(LocalDateTime.now(ZONE));
        sessionMapper.updateById(session);
        return detail(userId, sessionId);
    }

    @Transactional
    public SessionViews.Detail cancel(Long userId, Long sessionId) {
        WorkoutSession session = requireSession(sessionId, userId);
        requireInProgress(session);
        session.setStatus("CANCELLED");
        session.setEndedAt(LocalDateTime.now(ZONE));
        sessionMapper.updateById(session);
        return detail(userId, sessionId);
    }

    private SessionViews.Summary toSummary(WorkoutSession session) {
        List<SessionExercise> exercises = sessionExerciseMapper.selectList(
                new LambdaQueryWrapper<SessionExercise>().eq(SessionExercise::getSessionId, session.getId())
        );
        List<Long> exerciseIds = exercises.stream().map(SessionExercise::getId).toList();
        List<WorkoutSet> sets = exerciseIds.isEmpty()
                ? List.of()
                : workoutSetMapper.selectList(
                        new LambdaQueryWrapper<WorkoutSet>().in(WorkoutSet::getSessionExerciseId, exerciseIds)
                );

        int completedSets = 0;
        BigDecimal volume = BigDecimal.ZERO;
        for (WorkoutSet set : sets) {
            if (isEffectiveSet(set)) {
                completedSets++;
                if (set.getWeightKg() != null && set.getReps() != null) {
                    volume = volume.add(set.getWeightKg().multiply(BigDecimal.valueOf(set.getReps())));
                }
            }
        }
        return new SessionViews.Summary(
                session.getId(),
                session.getSessionDate(),
                session.getPlanNameSnapshot(),
                session.getPlanDayTitleSnapshot(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                exercises.size(),
                completedSets,
                sets.size(),
                volume
        );
    }

    private SessionViews.Exercise toExerciseView(SessionExercise item, Exercise exercise, List<WorkoutSet> sets) {
        int completedSets = 0;
        BigDecimal volume = BigDecimal.ZERO;
        BigDecimal bestE1rm = null;
        for (WorkoutSet set : sets) {
            if (!isEffectiveSet(set)) {
                continue;
            }
            completedSets++;
            if (set.getWeightKg() != null && set.getReps() != null) {
                volume = volume.add(set.getWeightKg().multiply(BigDecimal.valueOf(set.getReps())));
            }
            if (set.getWeightKg() != null && set.getReps() != null
                    && set.getWeightKg().compareTo(BigDecimal.ZERO) > 0
                    && set.getReps() >= 1 && set.getReps() <= 10) {
                BigDecimal factor = BigDecimal.ONE.add(
                        BigDecimal.valueOf(set.getReps())
                                .divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP)
                );
                BigDecimal estimated = set.getWeightKg().multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (bestE1rm == null || estimated.compareTo(bestE1rm) > 0) {
                    bestE1rm = estimated;
                }
            }
        }

        return new SessionViews.Exercise(
                item.getId(),
                item.getExerciseId(),
                item.getExerciseNameSnapshot(),
                exercise == null ? null : exercise.getBodyPart(),
                exercise == null ? null : exercise.getMode(),
                item.getSortNo(),
                item.getPlannedSets(),
                item.getPlannedRepsMin(),
                item.getPlannedRepsMax(),
                item.getPlannedWeightKg(),
                item.getPlannedDurationSec(),
                item.getDeviationType(),
                item.getActualNote(),
                completedSets,
                volume,
                bestE1rm,
                sets.stream().map(this::toSetView).toList()
        );
    }

    private SessionViews.SetItem toSetView(WorkoutSet set) {
        return new SessionViews.SetItem(
                set.getId(),
                set.getSetNo(),
                set.getPhase(),
                set.getSetType(),
                set.getSide(),
                set.getWeightKg(),
                set.getReps(),
                set.getDurationSec(),
                set.getDistanceM(),
                set.getRir(),
                set.getRpe(),
                set.getIsFailure(),
                set.getIsCompleted(),
                set.getCompletedAt(),
                set.getNote()
        );
    }

    private boolean isEffectiveSet(WorkoutSet set) {
        return "WORK".equals(set.getPhase()) && Boolean.TRUE.equals(set.getIsCompleted());
    }

    private int nextSetNo(Long sessionExerciseId) {
        List<WorkoutSet> sets = workoutSetMapper.selectList(
                new LambdaQueryWrapper<WorkoutSet>()
                        .eq(WorkoutSet::getSessionExerciseId, sessionExerciseId)
                        .orderByDesc(WorkoutSet::getSetNo)
                        .last("LIMIT 1")
        );
        return sets.isEmpty() || sets.get(0).getSetNo() == null ? 1 : sets.get(0).getSetNo() + 1;
    }

    private WorkoutSession requireSession(Long sessionId, Long userId) {
        WorkoutSession session = sessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getUserId(), userId)) {
            throw new BusinessException(404, "训练记录不存在");
        }
        return session;
    }

    private void ensureNoActive(Long userId) {
        Long activeCount = sessionMapper.selectCount(
                new LambdaQueryWrapper<WorkoutSession>()
                        .eq(WorkoutSession::getUserId, userId)
                        .eq(WorkoutSession::getStatus, "IN_PROGRESS")
        );
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(409, "已有进行中的训练，请先完成或取消");
        }
    }

    private SessionExercise requireSessionExercise(Long sessionId, Long sessionExerciseId) {
        SessionExercise item = sessionExerciseMapper.selectById(sessionExerciseId);
        if (item == null || !Objects.equals(item.getSessionId(), sessionId)) {
            throw new BusinessException(404, "训练动作不存在");
        }
        return item;
    }

    private OwnedSet requireOwnedSet(Long setId, Long userId) {
        WorkoutSet set = workoutSetMapper.selectById(setId);
        if (set == null) {
            throw new BusinessException(404, "训练组不存在");
        }
        SessionExercise item = sessionExerciseMapper.selectById(set.getSessionExerciseId());
        if (item == null) {
            throw new BusinessException(404, "训练组不存在");
        }
        WorkoutSession session = requireSession(item.getSessionId(), userId);
        return new OwnedSet(set, session);
    }

    private Exercise requireExercise(Long exerciseId) {
        Exercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null || !"ACTIVE".equals(exercise.getStatus())) {
            throw new BusinessException(404, "动作不存在或已归档");
        }
        return exercise;
    }

    private void requireInProgress(WorkoutSession session) {
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException("训练已经结束，不能继续修改");
        }
    }

    private String normalize(String value, String defaultValue, Set<String> allowed, String label) {
        String result = value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
        if (result == null || !allowed.contains(result)) {
            throw new BusinessException(label + "取值不正确");
        }
        return result;
    }

    private void validateSetValues(WorkoutSet set) {
        if (set.getWeightKg() == null && set.getReps() == null && set.getDurationSec() == null) {
            throw new BusinessException("请至少填写重量、次数或时长中的一项");
        }
    }

    private record OwnedSet(WorkoutSet set, WorkoutSession session) {
    }
}
