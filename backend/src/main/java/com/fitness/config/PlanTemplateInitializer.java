package com.fitness.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.entity.Exercise;
import com.fitness.entity.PlanDay;
import com.fitness.entity.PlanExercise;
import com.fitness.entity.WorkoutPlan;
import com.fitness.mapper.ExerciseMapper;
import com.fitness.mapper.PlanDayMapper;
import com.fitness.mapper.PlanExerciseMapper;
import com.fitness.mapper.WorkoutPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanTemplateInitializer implements CommandLineRunner {

    private final WorkoutPlanMapper planMapper;
    private final PlanDayMapper dayMapper;
    private final PlanExerciseMapper planExerciseMapper;
    private final ExerciseMapper exerciseMapper;

    @Override
    @Transactional
    public void run(String... args) {
        Long count = planMapper.selectCount(
                new LambdaQueryWrapper<WorkoutPlan>().eq(WorkoutPlan::getIsTemplate, true)
        );
        if (count != null && count > 0) {
            return;
        }
        createPushPullTemplate();
        createUpperLowerTemplate();
        log.info("系统计划模板已初始化");
    }

    private void createPushPullTemplate() {
        WorkoutPlan plan = template("三练增肌模板", "HYPERTROPHY", 4);
        PlanDay push = day(plan.getId(), 1, 1, "上肢推");
        PlanDay legs = day(plan.getId(), 1, 3, "下肢");
        PlanDay pull = day(plan.getId(), 1, 5, "上肢拉");

        exercise(push.getId(), "杠铃卧推", 1, 4, 8, 10, "60", 90);
        exercise(push.getId(), "上斜哑铃卧推", 2, 3, 8, 12, "24", 90);
        exercise(push.getId(), "哑铃肩推", 3, 3, 8, 12, "20", 90);
        exercise(push.getId(), "绳索下压", 4, 3, 10, 12, "25", 60);

        exercise(legs.getId(), "杠铃深蹲", 1, 4, 6, 8, "80", 120);
        exercise(legs.getId(), "罗马尼亚硬拉", 2, 3, 8, 10, "70", 120);
        exercise(legs.getId(), "保加利亚分腿蹲", 3, 3, 10, 12, "16", 90);
        timedExercise(legs.getId(), "平板支撑", 4, 3, 60, 60);

        exercise(pull.getId(), "引体向上", 1, 3, 6, 10, null, 120);
        exercise(pull.getId(), "杠铃划船", 2, 4, 8, 10, "50", 120);
        exercise(pull.getId(), "高位下拉", 3, 3, 10, 12, "45", 90);
        exercise(pull.getId(), "杠铃弯举", 4, 3, 10, 12, "20", 60);
    }

    private void createUpperLowerTemplate() {
        WorkoutPlan plan = template("上下分化四练模板", "STRENGTH", 4);
        PlanDay upperStrength = day(plan.getId(), 1, 1, "上肢力量");
        PlanDay lowerStrength = day(plan.getId(), 1, 2, "下肢力量");
        PlanDay upperHypertrophy = day(plan.getId(), 1, 4, "上肢增肌");
        PlanDay lowerHypertrophy = day(plan.getId(), 1, 6, "下肢增肌");

        exercise(upperStrength.getId(), "杠铃卧推", 1, 5, 5, 5, "70", 180);
        exercise(upperStrength.getId(), "杠铃划船", 2, 5, 5, 5, "60", 180);
        exercise(upperStrength.getId(), "哑铃肩推", 3, 4, 6, 8, "24", 120);

        exercise(lowerStrength.getId(), "杠铃深蹲", 1, 5, 5, 5, "90", 180);
        exercise(lowerStrength.getId(), "罗马尼亚硬拉", 2, 4, 6, 8, "80", 180);
        exercise(lowerStrength.getId(), "腿举", 3, 4, 8, 10, "120", 120);

        exercise(upperHypertrophy.getId(), "上斜哑铃卧推", 1, 4, 8, 10, "28", 90);
        exercise(upperHypertrophy.getId(), "高位下拉", 2, 4, 10, 12, "50", 90);
        exercise(upperHypertrophy.getId(), "哑铃侧平举", 3, 3, 12, 15, "8", 60);
        exercise(upperHypertrophy.getId(), "杠铃弯举", 4, 3, 10, 12, "20", 60);

        exercise(lowerHypertrophy.getId(), "保加利亚分腿蹲", 1, 4, 10, 12, "16", 90);
        exercise(lowerHypertrophy.getId(), "臀桥", 2, 4, 10, 12, "60", 90);
        timedExercise(lowerHypertrophy.getId(), "平板支撑", 3, 3, 60, 60);
    }

    private WorkoutPlan template(String name, String goal, int weeks) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setName(name);
        plan.setGoal(goal);
        plan.setWeeks(weeks);
        plan.setStatus("ACTIVE");
        plan.setIsTemplate(true);
        planMapper.insert(plan);
        return plan;
    }

    private PlanDay day(Long planId, int weekNo, int dayOfWeek, String title) {
        PlanDay day = new PlanDay();
        day.setPlanId(planId);
        day.setWeekNo(weekNo);
        day.setDayOfWeek(dayOfWeek);
        day.setTitle(title);
        day.setSortNo(dayOfWeek);
        dayMapper.insert(day);
        return day;
    }

    private void exercise(Long dayId, String name, int sort, int sets, int min, int max,
                          String weight, int rest) {
        Exercise found = exerciseMapper.selectOne(
                new LambdaQueryWrapper<Exercise>().eq(Exercise::getName, name)
        );
        if (found == null) {
            return;
        }
        PlanExercise item = new PlanExercise();
        item.setPlanDayId(dayId);
        item.setExerciseId(found.getId());
        item.setSortNo(sort);
        item.setTargetSets(sets);
        item.setTargetRepsMin(min);
        item.setTargetRepsMax(max);
        item.setTargetWeightKg(weight == null ? null : new BigDecimal(weight));
        item.setRestSeconds(rest);
        item.setProgressionType("NONE");
        item.setIncrementKg(new BigDecimal("2.50"));
        item.setIncrementReps(1);
        planExerciseMapper.insert(item);
    }

    private void timedExercise(Long dayId, String name, int sort, int sets, int duration, int rest) {
        Exercise found = exerciseMapper.selectOne(
                new LambdaQueryWrapper<Exercise>().eq(Exercise::getName, name)
        );
        if (found == null) {
            return;
        }
        PlanExercise item = new PlanExercise();
        item.setPlanDayId(dayId);
        item.setExerciseId(found.getId());
        item.setSortNo(sort);
        item.setTargetSets(sets);
        item.setTargetDurationSec(duration);
        item.setRestSeconds(rest);
        item.setProgressionType("NONE");
        item.setIncrementKg(new BigDecimal("2.50"));
        item.setIncrementReps(1);
        planExerciseMapper.insert(item);
    }
}
