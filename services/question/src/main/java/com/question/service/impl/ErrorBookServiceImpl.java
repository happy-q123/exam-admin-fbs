package com.question.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.ErrorBook;
import com.question.mapper.ErrorBookMapper;
import com.question.service.ErrorBookService;
import org.springframework.stereotype.Service;

/**
 * 错题本服务实现类
 *
 * @author zzq
 * @date 2026-06-11
 */
@Service
public class ErrorBookServiceImpl extends ServiceImpl<ErrorBookMapper, ErrorBook> implements ErrorBookService {
}
