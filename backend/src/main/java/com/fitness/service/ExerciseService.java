package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.dto.ExerciseVO;
import com.fitness.dto.OptionItem;
import com.fitness.entity.Equipment;
import com.fitness.entity.Exercise;
import com.fitness.mapper.EquipmentMapper;
import com.fitness.mapper.ExerciseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private static final List<OptionItem> BODY_PARTS = List.of(
            new OptionItem("CHEST", "胸部"),
            new OptionItem("BACK", "背部"),
            new OptionItem("LEGS", "腿部"),
            new OptionItem("GLUTES", "臀部"),
            new OptionItem("SHOULDERS", "肩部"),
            new OptionItem("ARMS", "手臂"),
            new OptionItem("CORE", "核心"),
            new OptionItem("CARDIO", "有氧")
    );

    private final ExerciseMapper exerciseMapper;
    private final EquipmentMapper equipmentMapper;

    public PageResult<ExerciseVO> page(String keyword, String bodyPart, Long equipmentId,
                                       long page, long size) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));

        LambdaQueryWrapper<Exercise> wrapper = new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getStatus, "ACTIVE")
                .eq(bodyPart != null && !bodyPart.isBlank(), Exercise::getBodyPart, bodyPart)
                .eq(equipmentId != null, Exercise::getEquipmentId, equipmentId)
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(Exercise::getName, keyword)
                        .or()
                        .like(Exercise::getDescription, keyword))
                .orderByAsc(Exercise::getBodyPart)
                .orderByAsc(Exercise::getName);

        Page<Exercise> result = exerciseMapper.selectPage(new Page<>(current, pageSize), wrapper);
        fillEquipmentNames(result.getRecords());

        List<ExerciseVO> records = result.getRecords().stream()
                .map(ExerciseVO::from)
                .toList();
        return new PageResult<>(result.getTotal(), records);
    }

    public ExerciseVO getById(Long id) {
        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null || !"ACTIVE".equals(exercise.getStatus())) {
            throw new BusinessException(404, "动作不存在");
        }
        if (exercise.getEquipmentId() != null) {
            Equipment equipment = equipmentMapper.selectById(exercise.getEquipmentId());
            if (equipment != null) {
                exercise.setEquipmentName(equipment.getName());
            }
        }
        return ExerciseVO.from(exercise);
    }

    public Map<String, Object> meta() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bodyParts", BODY_PARTS);
        data.put("equipment", equipmentMapper.selectList(
                        new LambdaQueryWrapper<Equipment>()
                                .eq(Equipment::getStatus, "ACTIVE")
                                .orderByAsc(Equipment::getId)
                ).stream()
                .map(item -> new OptionItem(String.valueOf(item.getId()), item.getName()))
                .toList());
        return data;
    }

    private void fillEquipmentNames(List<Exercise> exercises) {
        Set<Long> equipmentIds = exercises.stream()
                .map(Exercise::getEquipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (equipmentIds.isEmpty()) {
            return;
        }

        Map<Long, String> names = equipmentMapper.selectBatchIds(equipmentIds).stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));
        exercises.forEach(exercise -> exercise.setEquipmentName(names.get(exercise.getEquipmentId())));
    }
}
