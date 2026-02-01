package com.ai.service.common.impl;

import com.ai.mapper.LocalRagMapper;
import com.ai.service.common.LocalRagService;
import com.ai.utils.EmbedOptionsUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.LocalRag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
@Service
public class LocalRagServiceImpl extends ServiceImpl< LocalRagMapper,LocalRag> implements LocalRagService {
    @Resource
    private OllamaEmbeddingModel embeddingModel;
    private final int defaultTopK = 10;

    @Override
    public List<LocalRag> searchSimilarRag(String query, @Nullable List<String> sources, @Nullable Integer limit) {
        String vectorStr = EmbedOptionsUtil.queryToJson(query,embeddingModel);
        int topK = limit == null ? defaultTopK : limit;
        List<LocalRag> l=getBaseMapper().searchKnowledgeWithFilter(vectorStr, sources,topK);
        return l;
    }
}
