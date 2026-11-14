package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("muscle_group")
public class MuscleGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String bodyRegion;

    private Integer sortNo;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
