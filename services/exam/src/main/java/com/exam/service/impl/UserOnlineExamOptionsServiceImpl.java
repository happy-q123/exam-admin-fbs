package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.UserOnlineExamOptionsDto;
import com.domain.entity.relation.UserOnlineExamOptions;
import com.domain.enums.redis.UserOnlineKeyEnum;
import com.exam.mapper.UserOnlineExamOptionsMapper;
import com.exam.service.UserOnlineExamOptionsService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserOnlineExamOptionsServiceImpl extends ServiceImpl<UserOnlineExamOptionsMapper, UserOnlineExamOptions>
        implements UserOnlineExamOptionsService {
    @Resource
    StringRedisTemplate stringRedisTemplate;

    private final String USER_ONLINE_KEY = UserOnlineKeyEnum.ONLINE_USERS.buildKey();
    @Override
    public void saveUserOnlineExamOption(UserOnlineExamOptionsDto userOnlineExamOptionsDto) {
        UserOnlineExamOptions userOnlineExamOptions = userOnlineExamOptionsDto.toEntityForSave();
        String userId = userOnlineExamOptions.getUserId().toString();
        Boolean isOnline= stringRedisTemplate.opsForSet().isMember(USER_ONLINE_KEY, userId);

        if(Boolean.FALSE.equals(isOnline)){
            throw new RuntimeException("用户不在线");
        }

        try {
            this.save(userOnlineExamOptions);
        }catch (DuplicateKeyException e){
            throw new RuntimeException("已经提交过用户行为");
        }
    }
}
