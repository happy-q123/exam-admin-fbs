package com.user.test.mapper;

import com.domain.entity.User;
import com.domain.enums.UserRoleEnum;
import com.user.mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class UserMapperTest {
    @Resource
    UserMapper userMapper;

    @Order(1)
    @Test
    public void testInsertUser() {
        User user = User.builder()
                .username("admin")
                .password("123456")
                .nickName("admin")
                .id(111)
                .role(UserRoleEnum.Admin)
                .build();
        userMapper.insert(user);
        log.info("{}",UserRoleEnum.Admin);
    }

    @Order(2)
    @Test
    public void selectUser() {
        User user = userMapper.selectById(111);
        log.info(user.toString());
    }

}
