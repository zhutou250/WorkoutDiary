package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.dto.BodyMetricRequests;
import com.fitness.entity.BodyMetric;
import com.fitness.mapper.BodyMetricMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BodyMetricService {

    private final BodyMetricMapper bodyMetricMapper;

    public List<BodyMetric> list(Long userId, LocalDate start, LocalDate end) {
        LocalDate from = start == null ? LocalDate.now().minusDays(90) : start;
        LocalDate to = end == null ? LocalDate.now() : end;
        return bodyMetricMapper.selectList(
                new LambdaQueryWrapper<BodyMetric>()
                        .eq(BodyMetric::getUserId, userId)
                        .ge(BodyMetric::getRecordDate, from)
                        .le(BodyMetric::getRecordDate, to)
                        .orderByAsc(BodyMetric::getRecordDate)
        );
    }

    @Transactional
    public BodyMetric upsert(Long userId, BodyMetricRequests.Upsert request) {
        BodyMetric metric = bodyMetricMapper.selectOne(
                new LambdaQueryWrapper<BodyMetric>()
                        .eq(BodyMetric::getUserId, userId)
                        .eq(BodyMetric::getRecordDate, request.recordDate())
        );
        if (metric == null) {
            metric = new BodyMetric();
            metric.setUserId(userId);
            metric.setRecordDate(request.recordDate());
        }
        metric.setWeightKg(request.weightKg());
        metric.setBodyFatPct(request.bodyFatPct());
        metric.setWaistCm(request.waistCm());
        metric.setNote(request.note());
        if (metric.getId() == null) {
            bodyMetricMapper.insert(metric);
        } else {
            bodyMetricMapper.updateById(metric);
        }
        return metric;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        BodyMetric metric = bodyMetricMapper.selectById(id);
        if (metric == null || !Objects.equals(metric.getUserId(), userId)) {
            throw new BusinessException(404, "身体数据记录不存在");
        }
        bodyMetricMapper.deleteById(id);
    }
}
