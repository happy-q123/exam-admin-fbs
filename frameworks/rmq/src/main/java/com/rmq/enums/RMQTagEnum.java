package com.rmq.enums;

import lombok.Getter;

@Getter
public enum RMQTagEnum {

    //关闭考试
    CLOSE_EXAM("closeExam"),;

    private final String tag;

    RMQTagEnum(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return this.tag;
    }

    public String value(){
        return this.tag;
    }

}
