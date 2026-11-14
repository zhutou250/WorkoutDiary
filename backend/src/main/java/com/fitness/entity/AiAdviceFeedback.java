package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_advice_feedback")
public class AiAdviceFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adviceId;

    private Long userId;

    private Integer rating;

    private Boolean helpful;

    private String comment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
