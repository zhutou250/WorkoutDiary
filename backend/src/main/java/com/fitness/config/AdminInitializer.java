package com.fitness.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.entity.User;
import com.fitness.entity.UserProfile;
import com.fitness.mapper.UserMapper;
import com.fitness.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, adminUsername)
        );
        if (count != null && count > 0) {
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setNickname("系统管理员");
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        userMapper.insert(admin);

        UserProfile profile = new UserProfile();
        profile.setUserId(admin.getId());
        profile.setWeeklyTarget(3);
        profile.setPreferredUnit("KG");
        profileMapper.insert(profile);

        log.warn("已创建默认管理员账号 {}，请尽快修改默认密码", adminUsername);
    }
}
