package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.UpdateProfileRequest;
import com.fitness.entity.UserProfile;
import com.fitness.security.SecurityUtils;
import com.fitness.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public Result<UserProfile> get() {
        return Result.ok(profileService.getOrCreate(SecurityUtils.currentUserId()));
    }

    @PutMapping
    public Result<UserProfile> update(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(profileService.update(SecurityUtils.currentUserId(), request));
    }
}
