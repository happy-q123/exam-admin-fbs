package com.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.domain.base.BasePojo;
import com.domain.entity.Exam;
import com.domain.entity.attribute.ExamSecuritySetting;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.math.BigDecimal;


/**
 * description 考试dto
 * author zzq
 * date 2025/12/19 15:29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)//默认不让父类字段参与equals/hashCode，这里显式加上，防止ide提示
public class ExamDto extends BasePojo {

    // JS的精度 53 位，Long 最大值为 2^63-1，超过这个数字 JS 会丢失精度，因此给前端时，转为字符串
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    //考试标题
    private String title;

    //考试介绍
    private String introduce;

    // 开始时间，对应 PG 的 timestamp
    private LocalDateTime beginTime;

    // 持续时间，单位为分钟
    private Integer durationTime;

    // 指定 typeHandler
    @TableField(typeHandler = JacksonTypeHandler.class,value = "security_setting")
    private ExamSecuritySetting securitySetting;

    //考试状态，true 表示启用，false 表示禁用
    private Boolean status;

    //创建时间，对应 PG 的 timestamp
    private LocalDateTime createTime;

    //更新时间，对应 PG 的 timestamp
    private LocalDateTime latestUpdateTime;

    // 最大考试人数
    private Integer maxUserNum;

    // 剩余考试人数
    private Integer restUserNum;

    // JS的精度 53 位，Long 最大值为 2^63-1，超过这个数字 JS 会丢失精度，因此给前端时，转为字符串
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creator;

    private BigDecimal passScore;

    public Exam toExamForInsert(){
        Assert.isTrue(StringUtils.hasText(title), "考试标题不能为空");
        Assert.notNull(beginTime, "考试开始时间不能为空");
        Assert.notNull(durationTime, "考试持续时间不能为空");
        Assert.notNull(maxUserNum, "考试最大人数不能为空");
        Assert.notNull(creator, "考试创建者不能为空");
        Assert.isTrue(StringUtils.hasText(introduce), "考试介绍不能为空");
        Assert.isTrue(title.trim().length() <= 255, "考试标题不能超过255个字符");
        Assert.isTrue(introduce.trim().length() <= 2000, "考试介绍不能超过2000个字符");
        Assert.isTrue(durationTime > 0 && durationTime <= 1440, "考试持续时间必须在1到1440分钟之间");
        Assert.isTrue(maxUserNum > 0 && maxUserNum <= 1_000_000, "考试最大人数必须在1到1000000之间");
        if (passScore != null) {
            Assert.isTrue(passScore.signum() >= 0 && passScore.compareTo(java.math.BigDecimal.valueOf(100_000)) <= 0,
                    "及格分数不合法");
        }
        if (securitySetting != null && securitySetting.getMaxReconnectCount() != null) {
            Assert.isTrue(securitySetting.getMaxReconnectCount() >= -1
                            && securitySetting.getMaxReconnectCount() <= 100,
                    "最大重连次数必须在-1到100之间");
        }

        title = title.trim();
        introduce = introduce.trim();

        if (createTime==null)
            createTime=LocalDateTime.now();
        if (id == null || restUserNum == null)
            restUserNum=maxUserNum;
        Assert.isTrue(restUserNum >= 0 && restUserNum <= maxUserNum, "剩余人数配置不合法");
        if (status==null)
            status=true;
        if (latestUpdateTime == null)
            latestUpdateTime =LocalDateTime.now();
        if (securitySetting==null)
            securitySetting=new ExamSecuritySetting(false,true,3);
        if (passScore == null)
            passScore = BigDecimal.valueOf(60);

        return Exam.builder()
                .id(id)
                .title(title)
                .introduce(introduce)
                .beginTime(beginTime)
                .durationTime(durationTime)
                .securitySetting(securitySetting)
                .status(status)
                .createTime(createTime)
                .latestUpdateTime(latestUpdateTime)
                .maxUserNum(maxUserNum)
                .restUserNum(restUserNum)
                .creator(creator)
                .passScore(passScore)
                .build();
    }
}
