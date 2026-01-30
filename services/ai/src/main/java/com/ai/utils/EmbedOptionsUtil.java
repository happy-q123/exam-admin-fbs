package com.ai.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class EmbedOptionsUtil {

    public static List<Double> floatArrayToDoubleList(float[] embedding){
        return IntStream.range(0, embedding.length)
                .mapToDouble(i -> embedding[i])
                .boxed()
                .toList();
    }

//    public static float[] doubleListToFloatArray(List<Double> embedding){
//        return embedding.stream()
//                .mapToFloat(Double::floatValue)
//                .toArray();
//    }
}
