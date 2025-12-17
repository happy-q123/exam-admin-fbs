package com.user.test.service;

import com.domain.dto.UserDto;
import com.user.service.UserOptionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class UserOptionServiceTest {
    @Resource
    UserOptionService userOptionService;

    @Test
    public void testLoginUser() {
        UserDto userDtoForLogin = userOptionService.getUserForLogin("admin");
        log.info("user:{}",userDtoForLogin);
    }
}
