package com.domain.entity.attribute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * description 考试的安全设置
 * author zzq
 * date 2025/12/18 15:24
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExamSecuritySetting {

    //是否允许提前提交
    private Boolean allowEarlySubmit;

    //是否需要开启摄像头
    private Boolean faceRecognition;

    //最大允许的重连次数
    private Integer maxReconnectCount;
}