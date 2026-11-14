package com.fitness.dto;

import com.fitness.entity.Exercise;

public record ExerciseVO(
        Long id,
        String name,
        String bodyPart,
        Long equipmentId,
        String equipmentName,
        String category,
        String mode,
        String description,
        String instructions,
        String mediaUrl,
        String status
) {
    public static ExerciseVO from(Exercise exercise) {
        return new ExerciseVO(
                exercise.getId(),
                exercise.getName(),
                exercise.getBodyPart(),
                exercise.getEquipmentId(),
                exercise.getEquipmentName(),
                exercise.getCategory(),
                exercise.getMode(),
                exercise.getDescription(),
                exercise.getInstructions(),
                exercise.getMediaUrl(),
                exercise.getStatus()
        );
    }
}
