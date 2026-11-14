package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_advice_item")
public class AiAdviceItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adviceId;

    private String findingKey;

    private String ruleCode;

    private String severity;

    private String title;

    private String evidenceJson;

    private String suggestionText;

    private String source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
