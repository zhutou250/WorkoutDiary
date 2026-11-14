package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.dto.AdminRequests;
import com.fitness.dto.AdminViews;
import com.fitness.dto.ExerciseVO;
import com.fitness.entity.AdminAuditLog;
import com.fitness.entity.Equipment;
import com.fitness.entity.Exercise;
import com.fitness.entity.User;
import com.fitness.mapper.AdminAuditLogMapper;
import com.fitness.mapper.AiAdviceMapper;
import com.fitness.mapper.EquipmentMapper;
import com.fitness.mapper.ExerciseMapper;
import com.fitness.mapper.UserMapper;
import com.fitness.mapper.WorkoutPlanMapper;
import com.fitness.mapper.WorkoutSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final ExerciseMapper exerciseMapper;
    private final EquipmentMapper equipmentMapper;
    private final WorkoutPlanMapper planMapper;
    private final WorkoutSessionMapper sessionMapper;
    private final AiAdviceMapper adviceMapper;
    private final AdminAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public PageResult<AdminViews.User> users(String keyword, long page, long size) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        Page<User> result = userMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<User>()
                        .and(keyword != null && !keyword.isBlank(), query -> query
                                .like(User::getUsername, keyword)
                                .or()
                                .like(User::getNickname, keyword))
                        .orderByDesc(User::getCreatedAt)
        );
        List<AdminViews.User> records = result.getRecords().stream()
                .map(user -> new AdminViews.User(
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        user.getRole(),
                        user.getStatus(),
                        user.getLastLoginAt(),
                        user.getCreatedAt()
                ))
                .toList();
        return new PageResult<>(result.getTotal(), records);
    }

    @Transactional
    public void updateUserStatus(Long adminUserId, Long userId, AdminRequests.UserStatus request) {
        if (Objects.equals(adminUserId, userId)) {
            throw new BusinessException("不能修改自己的账号状态");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(request.status());
        userMapper.updateById(user);
        audit(adminUserId, "UPDATE_USER_STATUS", "USER", String.valueOf(userId), request.status());
    }

    public AdminViews.Platform platform() {
        return new AdminViews.Platform(
                count(userMapper.selectCount(null)),
                count(exerciseMapper.selectCount(null)),
                count(planMapper.selectCount(null)),
                count(sessionMapper.selectCount(null)),
                count(adviceMapper.selectCount(null))
        );
    }

    public PageResult<ExerciseVO> exercises(String keyword, String bodyPart, String status,
                                            long page, long size) {
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        Page<Exercise> result = exerciseMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<Exercise>()
                        .eq(status != null && !status.isBlank(), Exercise::getStatus, status)
                        .eq(bodyPart != null && !bodyPart.isBlank(), Exercise::getBodyPart, bodyPart)
                        .like(keyword != null && !keyword.isBlank(), Exercise::getName, keyword)
                        .orderByDesc(Exercise::getId)
        );
        fillEquipmentNames(result.getRecords());
        List<ExerciseVO> records = result.getRecords().stream().map(ExerciseVO::from).toList();
        return new PageResult<>(result.getTotal(), records);
    }

    @Transactional
    public ExerciseVO createExercise(Long adminUserId, AdminRequests.ExerciseSave request) {
        Exercise exercise = new Exercise();
        exercise.setName(request.name().trim());
        exercise.setBodyPart(request.bodyPart());
        exercise.setEquipmentId(request.equipmentId());
        exercise.setCategory(request.category());
        exercise.setMode(request.mode());
        exercise.setDescription(request.description());
        exercise.setInstructions(request.instructions() == null || request.instructions().isEmpty()
                ? null
                : writeJson(request.instructions()));
        exercise.setMediaUrl(request.mediaUrl());
        exercise.setIsCustom(false);
        exercise.setCreatedBy(adminUserId);
        exercise.setStatus("ACTIVE");
        exerciseMapper.insert(exercise);
        audit(adminUserId, "CREATE_EXERCISE", "EXERCISE", String.valueOf(exercise.getId()), request.name());
        fillEquipmentNames(List.of(exercise));
        return ExerciseVO.from(exercise);
    }

    @Transactional
    public ExerciseVO updateExercise(Long adminUserId, Long exerciseId, AdminRequests.ExerciseSave request) {
        Exercise exercise = requireExercise(exerciseId);
        exercise.setName(request.name().trim());
        exercise.setBodyPart(request.bodyPart());
        exercise.setEquipmentId(request.equipmentId());
        exercise.setCategory(request.category());
        exercise.setMode(request.mode());
        exercise.setDescription(request.description());
        exercise.setInstructions(request.instructions() == null || request.instructions().isEmpty()
                ? null
                : writeJson(request.instructions()));
        exercise.setMediaUrl(request.mediaUrl());
        exerciseMapper.updateById(exercise);
        audit(adminUserId, "UPDATE_EXERCISE", "EXERCISE", String.valueOf(exerciseId), request.name());
        fillEquipmentNames(List.of(exercise));
        return ExerciseVO.from(exercise);
    }

    @Transactional
    public void changeExerciseStatus(Long adminUserId, Long exerciseId, String status) {
        if (!"ACTIVE".equals(status) && !"ARCHIVED".equals(status)) {
            throw new BusinessException("状态取值不正确");
        }
        Exercise exercise = requireExercise(exerciseId);
        exercise.setStatus(status);
        exerciseMapper.updateById(exercise);
        audit(adminUserId, "CHANGE_EXERCISE_STATUS", "EXERCISE", String.valueOf(exerciseId), status);
    }

    public List<AdminAuditLog> auditLogs(int limit) {
        return auditLogMapper.selectList(
                new LambdaQueryWrapper<AdminAuditLog>()
                        .orderByDesc(AdminAuditLog::getCreatedAt)
                        .last("LIMIT " + Math.min(100, Math.max(1, limit)))
        );
    }

    private Exercise requireExercise(Long exerciseId) {
        Exercise exercise = exerciseMapper.selectById(exerciseId);
        if (exercise == null) {
            throw new BusinessException(404, "动作不存在");
        }
        return exercise;
    }

    private void fillEquipmentNames(List<Exercise> exercises) {
        Set<Long> ids = exercises.stream()
                .map(Exercise::getEquipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = equipmentMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));
        exercises.forEach(item -> item.setEquipmentName(names.get(item.getEquipmentId())));
    }

    private void audit(Long adminUserId, String action, String targetType, String targetId, Object detail) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetailJson(detail == null ? null : writeJson(detail));
        auditLogMapper.insert(log);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
