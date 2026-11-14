package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.dto.UpdateProfileRequest;
import com.fitness.entity.UserProfile;
import com.fitness.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileMapper profileMapper;

    public UserProfile getOrCreate(Long userId) {
        UserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setWeeklyTarget(3);
            profile.setPreferredUnit("KG");
            profileMapper.insert(profile);
        }
        return profile;
    }

    @Transactional
    public UserProfile update(Long userId, UpdateProfileRequest request) {
        UserProfile profile = getOrCreate(userId);
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }
        if (request.birthDate() != null) {
            profile.setBirthDate(request.birthDate());
        }
        if (request.heightCm() != null) {
            profile.setHeightCm(request.heightCm());
        }
        if (request.goal() != null) {
            profile.setGoal(request.goal());
        }
        if (request.trainingYears() != null) {
            profile.setTrainingYears(request.trainingYears());
        }
        if (request.weeklyTarget() != null) {
            profile.setWeeklyTarget(request.weeklyTarget());
        }
        if (request.preferredUnit() != null) {
            profile.setPreferredUnit(request.preferredUnit());
        }
        profileMapper.updateById(profile);
        return profile;
    }
}
