package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.dto.PlanRequests;
import com.fitness.dto.PlanViews;
import com.fitness.entity.Equipment;
import com.fitness.entity.Exercise;
import com.fitness.entity.PlanDay;
import com.fitness.entity.PlanExercise;
import com.fitness.entity.WorkoutPlan;
import com.fitness.mapper.EquipmentMapper;
import com.fitness.mapper.ExerciseMapper;
import com.fitness.mapper.PlanDayMapper;
import com.fitness.mapper.PlanExerciseMapper;
import com.fitness.mapper.WorkoutPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final WorkoutPlanMapper planMapper;
    private final PlanDayMapper dayMapper;
    private final PlanExerciseMapper planExerciseMapper;
    private final ExerciseMapper exerciseMapper;
    private final EquipmentMapper equipmentMapper;

    public List<PlanViews.Summary> list(Long userId) {
        List<WorkoutPlan> plans = planMapper.selectList(
                new LambdaQueryWrapper<WorkoutPlan>()
                        .eq(WorkoutPlan::getUserId, userId)
                        .orderByDesc(WorkoutPlan::getCreatedAt)
                        .orderByDesc(WorkoutPlan::getId)
        );
        if (plans.isEmpty()) {
            return List.of();
        }
        return toSummaries(plans);
    }

    public List<PlanViews.Summary> templates() {
        List<WorkoutPlan> templates = planMapper.selectList(
                new LambdaQueryWrapper<WorkoutPlan>()
                        .eq(WorkoutPlan::getIsTemplate, true)
                        .orderByAsc(WorkoutPlan::getId)
        );
        return toSummaries(templates);
    }

    private List<PlanViews.Summary> toSummaries(List<WorkoutPlan> plans) {
        if (plans.isEmpty()) {
            return List.of();
        }

        List<Long> planIds = plans.stream().map(WorkoutPlan::getId).toList();
        List<PlanDay> days = dayMapper.selectList(
                new LambdaQueryWrapper<PlanDay>()
                        .in(PlanDay::getPlanId, planIds)
                        .orderByAsc(PlanDay::getWeekNo)
                        .orderByAsc(PlanDay::getDayOfWeek)
        );
        Map<Long, List<PlanDay>> daysByPlan = days.stream()
                .collect(Collectors.groupingBy(PlanDay::getPlanId));

        List<Long> dayIds = days.stream().map(PlanDay::getId).toList();
        Map<Long, Long> exerciseCountByDay = dayIds.isEmpty()
                ? Collections.emptyMap()
                : planExerciseMapper.selectList(
                        new LambdaQueryWrapper<PlanExercise>().in(PlanExercise::getPlanDayId, dayIds)
                ).stream().collect(Collectors.groupingBy(PlanExercise::getPlanDayId, Collectors.counting()));

        return plans.stream().map(plan -> {
            List<PlanDay> planDays = daysByPlan.getOrDefault(plan.getId(), List.of());
            int exerciseCount = planDays.stream()
                    .mapToInt(day -> exerciseCountByDay.getOrDefault(day.getId(), 0L).intValue())
                    .sum();
            return new PlanViews.Summary(
                    plan.getId(),
                    plan.getName(),
                    plan.getGoal(),
                    plan.getStartDate(),
                    plan.getWeeks(),
                    plan.getStatus(),
                    planDays.size(),
                    exerciseCount,
                    plan.getCreatedAt()
            );
        }).toList();
    }

    @Transactional
    public PlanViews.Detail copyTemplate(Long userId, Long templateId) {
        WorkoutPlan template = planMapper.selectById(templateId);
        if (template == null || !Boolean.TRUE.equals(template.getIsTemplate())) {
            throw new BusinessException(404, "计划模板不存在");
        }

        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setName(template.getName() + "（复制）");
        plan.setGoal(template.getGoal());
        plan.setStartDate(LocalDate.now());
        plan.setWeeks(template.getWeeks());
        plan.setStatus("DRAFT");
        plan.setIsTemplate(false);
        plan.setSourceTemplateId(template.getId());
        planMapper.insert(plan);

        List<PlanDay> days = dayMapper.selectList(
                new LambdaQueryWrapper<PlanDay>()
                        .eq(PlanDay::getPlanId, templateId)
                        .orderByAsc(PlanDay::getWeekNo)
                        .orderByAsc(PlanDay::getDayOfWeek)
        );
        List<Long> dayIds = days.stream().map(PlanDay::getId).toList();
        Map<Long, List<PlanExercise>> exercisesByDay = dayIds.isEmpty()
                ? Collections.emptyMap()
                : planExerciseMapper.selectList(
                        new LambdaQueryWrapper<PlanExercise>().in(PlanExercise::getPlanDayId, dayIds)
                ).stream().collect(Collectors.groupingBy(PlanExercise::getPlanDayId));

        for (PlanDay day : days) {
            PlanDay newDay = new PlanDay();
            newDay.setPlanId(plan.getId());
            newDay.setWeekNo(day.getWeekNo());
            newDay.setDayOfWeek(day.getDayOfWeek());
            newDay.setTitle(day.getTitle());
            newDay.setSortNo(day.getSortNo());
            dayMapper.insert(newDay);

            for (PlanExercise item : exercisesByDay.getOrDefault(day.getId(), List.of())) {
                PlanExercise newItem = new PlanExercise();
                newItem.setPlanDayId(newDay.getId());
                newItem.setExerciseId(item.getExerciseId());
                newItem.setSortNo(item.getSortNo());
                newItem.setTargetSets(item.getTargetSets());
                newItem.setTargetRepsMin(item.getTargetRepsMin());
                newItem.setTargetRepsMax(item.getTargetRepsMax());
                newItem.setTargetWeightKg(item.getTargetWeightKg());
                newItem.setTargetDurationSec(item.getTargetDurationSec());
                newItem.setRestSeconds(item.getRestSeconds());
                newItem.setProgressionType(item.getProgressionType());
                newItem.setIncrementKg(item.getIncrementKg());
                newItem.setIncrementReps(item.getIncrementReps());
                newItem.setNote(item.getNote());
                planExerciseMapper.insert(newItem);
            }
        }
        return detail(userId, plan.getId());
    }

    @Transactional
    public PlanViews.Detail create(Long userId, PlanRequests.Create request) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setName(request.name().trim());
        plan.setGoal(request.goal());
        plan.setStartDate(request.startDate() == null ? LocalDate.now() : request.startDate());
        plan.setWeeks(request.weeks() == null ? 4 : request.weeks());
        plan.setStatus("DRAFT");
        plan.setIsTemplate(false);
        planMapper.insert(plan);
        return detail(userId, plan.getId());
    }

    @Transactional
    public PlanViews.Detail update(Long userId, Long planId, PlanRequests.Update request) {
        WorkoutPlan plan = requirePlan(planId, userId);
        requireEditable(plan);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException("计划名称不能为空");
            }
            plan.setName(request.name().trim());
        }
        if (request.goal() != null) {
            plan.setGoal(request.goal());
        }
        if (request.startDate() != null) {
            plan.setStartDate(request.startDate());
        }
        if (request.weeks() != null && !Objects.equals(request.weeks(), plan.getWeeks())) {
            Long maxWeek = dayMapper.selectList(
                    new LambdaQueryWrapper<PlanDay>().eq(PlanDay::getPlanId, planId)
            ).stream().map(PlanDay::getWeekNo).max(Integer::compareTo).orElse(0).longValue();
            if (request.weeks() < maxWeek) {
                throw new BusinessException("新的计划周数小于已有训练日所在周");
            }
            plan.setWeeks(request.weeks());
        }
        if (request.status() != null) {
            plan.setStatus(request.status());
        }
        planMapper.updateById(plan);
        return detail(userId, planId);
    }

    @Transactional
    public void delete(Long userId, Long planId) {
        requirePlan(planId, userId);
        planMapper.deleteById(planId);
    }

    public PlanViews.Detail detail(Long userId, Long planId) {
        WorkoutPlan plan = requirePlan(planId, userId);
        List<PlanDay> days = dayMapper.selectList(
                new LambdaQueryWrapper<PlanDay>()
                        .eq(PlanDay::getPlanId, planId)
                        .orderByAsc(PlanDay::getWeekNo)
                        .orderByAsc(PlanDay::getDayOfWeek)
                        .orderByAsc(PlanDay::getSortNo)
        );

        List<Long> dayIds = days.stream().map(PlanDay::getId).toList();
        List<PlanExercise> planExercises = dayIds.isEmpty()
                ? List.of()
                : planExerciseMapper.selectList(
                        new LambdaQueryWrapper<PlanExercise>()
                                .in(PlanExercise::getPlanDayId, dayIds)
                                .orderByAsc(PlanExercise::getSortNo)
                );

        Set<Long> exerciseIds = planExercises.stream()
                .map(PlanExercise::getExerciseId)
                .collect(Collectors.toSet());
        Map<Long, Exercise> exerciseMap = exerciseIds.isEmpty()
                ? Collections.emptyMap()
                : exerciseMapper.selectBatchIds(exerciseIds).stream()
                        .collect(Collectors.toMap(Exercise::getId, Function.identity()));

        Set<Long> equipmentIds = exerciseMap.values().stream()
                .map(Exercise::getEquipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> equipmentNames = equipmentIds.isEmpty()
                ? Collections.emptyMap()
                : equipmentMapper.selectBatchIds(equipmentIds).stream()
                        .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        Map<Long, List<PlanExercise>> exercisesByDay = planExercises.stream()
                .collect(Collectors.groupingBy(PlanExercise::getPlanDayId));

        List<PlanViews.Day> dayViews = days.stream().map(day -> {
            List<PlanViews.Exercise> exerciseViews = exercisesByDay
                    .getOrDefault(day.getId(), List.of())
                    .stream()
                    .map(item -> toExerciseView(item, exerciseMap.get(item.getExerciseId()), equipmentNames))
                    .toList();
            return new PlanViews.Day(
                    day.getId(),
                    day.getWeekNo(),
                    day.getDayOfWeek(),
                    day.getTitle(),
                    day.getSortNo(),
                    exerciseViews
            );
        }).toList();

        return new PlanViews.Detail(
                plan.getId(),
                plan.getName(),
                plan.getGoal(),
                plan.getStartDate(),
                plan.getWeeks(),
                plan.getStatus(),
                dayViews
        );
    }

    @Transactional
    public PlanViews.Detail addDay(Long userId, Long planId, PlanRequests.Day request) {
        WorkoutPlan plan = requirePlan(planId, userId);
        requireEditable(plan);
        validateDay(plan, request, null);

        Long sortCount = dayMapper.selectCount(
                new LambdaQueryWrapper<PlanDay>()
                        .eq(PlanDay::getPlanId, planId)
                        .eq(PlanDay::getWeekNo, request.weekNo())
        );

        PlanDay day = new PlanDay();
        day.setPlanId(planId);
        day.setWeekNo(request.weekNo());
        day.setDayOfWeek(request.dayOfWeek());
        day.setTitle(request.title().trim());
        day.setSortNo(request.sortNo() == null ? (sortCount == null ? 1 : sortCount.intValue() + 1) : request.sortNo());
        dayMapper.insert(day);
        return detail(userId, planId);
    }

    @Transactional
    public PlanViews.Detail updateDay(Long userId, Long dayId, PlanRequests.Day request) {
        PlanDay day = requireDay(dayId);
        WorkoutPlan plan = requirePlan(day.getPlanId(), userId);
        requireEditable(plan);
        validateDay(plan, request, dayId);

        day.setWeekNo(request.weekNo());
        day.setDayOfWeek(request.dayOfWeek());
        day.setTitle(request.title().trim());
        if (request.sortNo() != null) {
            day.setSortNo(request.sortNo());
        }
        dayMapper.updateById(day);
        return detail(userId, plan.getId());
    }

    @Transactional
    public PlanViews.Detail deleteDay(Long userId, Long dayId) {
        PlanDay day = requireDay(dayId);
        WorkoutPlan plan = requirePlan(day.getPlanId(), userId);
        requireEditable(plan);
        dayMapper.deleteById(dayId);
        return detail(userId, plan.getId());
    }

    @Transactional
    public PlanViews.Detail addExercise(Long userId, Long dayId, PlanRequests.Exercise request) {
        PlanDay day = requireDay(dayId);
        WorkoutPlan plan = requirePlan(day.getPlanId(), userId);
        requireEditable(plan);
        Exercise exercise = requireExercise(request.exerciseId());
        validateExercise(request, exercise);

        List<PlanExercise> existing = planExerciseMapper.selectList(
                new LambdaQueryWrapper<PlanExercise>()
                        .eq(PlanExercise::getPlanDayId, dayId)
                        .orderByDesc(PlanExercise::getSortNo)
        );
        int nextSort = existing.isEmpty() || existing.get(0).getSortNo() == null
                ? 1
                : existing.get(0).getSortNo() + 1;

        PlanExercise item = new PlanExercise();
        item.setPlanDayId(dayId);
        item.setExerciseId(request.exerciseId());
        item.setSortNo(request.sortNo() == null ? nextSort : request.sortNo());
        applyExercise(item, request);
        planExerciseMapper.insert(item);
        return detail(userId, plan.getId());
    }

    @Transactional
    public PlanViews.Detail updateExercise(Long userId, Long planExerciseId, PlanRequests.Exercise request) {
        PlanExercise item = requirePlanExercise(planExerciseId);
        PlanDay day = requireDay(item.getPlanDayId());
        WorkoutPlan plan = requirePlan(day.getPlanId(), userId);
        requireEditable(plan);
        Exercise exercise = requireExercise(request.exerciseId());
        validateExercise(request, exercise);

        item.setExerciseId(request.exerciseId());
        if (request.sortNo() != null) {
            item.setSortNo(request.sortNo());
        }
        applyExercise(item, request);
        planExerciseMapper.updateById(item);
        return detail(userId, plan.getId());
    }

    @Transactional
    public PlanViews.Detail deleteExercise(Long userId, Long planExerciseId) {
        PlanExercise item = requirePlanExercise(planExerciseId);
        PlanDay day = requireDay(item.getPlanDayId());
        WorkoutPlan plan = requirePlan(day.getPlanId(), userId);
        requireEditable(plan);
        planExerciseMapper.deleteById(planExerciseId);
        return detail(userId, plan.getId());
    }

    public WorkoutPlan requirePlan(Long planId, Long userId) {
        WorkoutPlan plan = planMapper.selectById(planId);
        if (plan == null || !Objects.equals(plan.getUserId(), userId)) {
            throw new BusinessException(404, "训练计划不存在");
        }
        return plan;
    }

    private PlanDay requireDay(Long dayId) {
        PlanDay day = dayMapper.selectById(dayId);
        if (day == null) {
            throw new BusinessException(404, "训练日不存在");
        }
        return day;
    }

    private PlanExercise requirePlanExercise(Long planExerciseId) {
        PlanExercise item = planExerciseMapper.selectById(planExerciseId);
        if (item == null) {
            throw new BusinessException(404, "计划动作不存在");
        }
        return item;
    }

    private Exercise requireExercise(Long exerciseId) {
        Exercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null || !"ACTIVE".equals(exercise.getStatus())) {
            throw new BusinessException(404, "动作不存在或已归档");
        }
        return exercise;
    }

    private void requireEditable(WorkoutPlan plan) {
        if ("ARCHIVED".equals(plan.getStatus())) {
            throw new BusinessException("已归档的计划不能修改，请先恢复为草稿或进行中");
        }
    }

    private void validateDay(WorkoutPlan plan, PlanRequests.Day request, Long excludeDayId) {
        if (request.weekNo() > plan.getWeeks()) {
            throw new BusinessException("训练日超出计划周数");
        }
        Long duplicates = dayMapper.selectCount(
                new LambdaQueryWrapper<PlanDay>()
                        .eq(PlanDay::getPlanId, plan.getId())
                        .eq(PlanDay::getWeekNo, request.weekNo())
                        .eq(PlanDay::getDayOfWeek, request.dayOfWeek())
                        .ne(excludeDayId != null, PlanDay::getId, excludeDayId)
        );
        if (duplicates != null && duplicates > 0) {
            throw new BusinessException("该周的同一天已经安排了训练");
        }
    }

    private void validateExercise(PlanRequests.Exercise request, Exercise exercise) {
        if (request.targetRepsMin() != null && request.targetRepsMax() != null
                && request.targetRepsMin() > request.targetRepsMax()) {
            throw new BusinessException("目标次数下限不能大于上限");
        }
        if ("REPS".equals(exercise.getMode())
                && request.targetRepsMin() == null && request.targetRepsMax() == null) {
            throw new BusinessException("次数类动作请填写目标次数");
        }
        if (("TIME".equals(exercise.getMode()) || "CARDIO".equals(exercise.getMode()))
                && request.targetDurationSec() == null) {
            throw new BusinessException("计时或有氧动作请填写目标时长");
        }
    }

    private void applyExercise(PlanExercise item, PlanRequests.Exercise request) {
        item.setTargetSets(request.targetSets());
        item.setTargetRepsMin(request.targetRepsMin());
        item.setTargetRepsMax(request.targetRepsMax());
        item.setTargetWeightKg(request.targetWeightKg());
        item.setTargetDurationSec(request.targetDurationSec());
        item.setRestSeconds(request.restSeconds() == null ? 90 : request.restSeconds());
        item.setProgressionType(request.progressionType() == null ? "NONE" : request.progressionType());
        item.setIncrementKg(request.incrementKg() == null ? new BigDecimal("2.50") : request.incrementKg());
        item.setIncrementReps(request.incrementReps() == null ? 1 : request.incrementReps());
        item.setNote(request.note());
    }

    private PlanViews.Exercise toExerciseView(PlanExercise item, Exercise exercise,
                                              Map<Long, String> equipmentNames) {
        String equipmentName = exercise == null || exercise.getEquipmentId() == null
                ? null
                : equipmentNames.get(exercise.getEquipmentId());
        return new PlanViews.Exercise(
                item.getId(),
                item.getExerciseId(),
                exercise == null ? "已归档动作" : exercise.getName(),
                exercise == null ? null : exercise.getBodyPart(),
                equipmentName,
                exercise == null ? null : exercise.getMode(),
                item.getSortNo(),
                item.getTargetSets(),
                item.getTargetRepsMin(),
                item.getTargetRepsMax(),
                item.getTargetWeightKg(),
                item.getTargetDurationSec(),
                item.getRestSeconds(),
                item.getProgressionType(),
                item.getIncrementKg(),
                item.getIncrementReps(),
                item.getNote()
        );
    }
}
