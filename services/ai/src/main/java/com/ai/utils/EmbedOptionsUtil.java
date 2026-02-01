package com.ai.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class EmbedOptionsUtil {

    /**
     * description 将float[] 转换为List<Double>
     * author zzq
     * date 2026/2/1 16:47
    */

    public static List<Double> floatArrayToDoubleList(float[] embedding){
        return IntStream.range(0, embedding.length)
                .mapToDouble(i -> embedding[i])
                .boxed()
                .toList();
    }

    /**
     * description 将float[] 转换为json
     * author zzq
     * date 2026/2/1 16:47
    */

    public static String queryToJson(float[] fv){
        return queryToJsonExecutor(fv);
    }

    /**
     * description 将float[] 转换为json
     * author zzq
     * date 2026/2/1 16:47
     */
    public static String queryToJson(String query, EmbeddingModel embeddingModel){
        float[] fv= embeddingModel.embed(query);
        return queryToJsonExecutor(fv);
    }

    private static String queryToJsonExecutor(float[] fv){
        // 将 float 数组转换为 List<Double>
        List<Double> vector = EmbedOptionsUtil.floatArrayToDoubleList(fv);

        // 将 List<Double> 转换为 JSON 字符串
        ObjectMapper objectMapper = new ObjectMapper();
        String vectorStr;
        try {
            vectorStr = objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("向量序列化失败", e);
        }
        return vectorStr;
    }

//    public static float[] doubleListToFloatArray(List<Double> embedding){
//        return embedding.stream()
//                .mapToFloat(Double::floatValue)
//                .toArray();
//    }
}
