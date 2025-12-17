package com.user.test.other;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Slf4j
@SpringBootTest
public class PasswordCode {
    @Test
    public void test() {
        String password = "123456";
        String encode = new BCryptPasswordEncoder().encode(password);
        System.out.println(encode);
    }

    @Test
    public void test2() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456"; // 你想设置的明文密码
        String encodedPassword = encoder.encode(password);

        log.info("数据库里应该存这个串: {}", encodedPassword);
    }


    @Test
    public void test3() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456"; // 明文密码
        String encodedPassword = "$2a$10$94zk57u.vwnF1P7xG3Lp8e9Q7NaVARTueD1e/DfYbUojMHYQdeQ9S"; // 数据库中存储的加密密码
        
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        log.info("密码匹配结果: {}", matches);
    }
}
