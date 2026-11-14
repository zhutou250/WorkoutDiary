package com.fitness.dto;

import com.fitness.entity.User;

public record UserVO(Long id, String username, String nickname, String role) {

    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }
}
