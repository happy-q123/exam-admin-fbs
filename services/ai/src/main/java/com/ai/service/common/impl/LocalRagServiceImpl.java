package com.ai.service.common.impl;

import com.ai.mapper.LocalRagMapper;
import com.ai.service.common.LocalRagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.LocalRag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class LocalRagServiceImpl extends ServiceImpl< LocalRagMapper,LocalRag> implements LocalRagService {
}
