package com.domain.common;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 */
public class RestResponse<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public RestResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public RestResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> RestResponse<T> success() {
        return new RestResponse<>(200, "操作成功", null);
    }
    
    /**
     * 成功响应（有数据）
     */
    public static <T> RestResponse<T> success(T data) {
        return new RestResponse<>(200, "操作成功", data);
    }
    
    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> RestResponse<T> success(String message, T data) {
        return new RestResponse<>(200, message, data);
    }
    
    /**
     * 失败响应（默认错误）
     */
    public static <T> RestResponse<T> fail() {
        return new RestResponse<>(500, "操作失败", null);
    }
    
    /**
     * 失败响应（自定义错误消息）
     */
    public static <T> RestResponse<T> fail(String message) {
        return new RestResponse<>(500, message, null);
    }
    
    /**
     * 失败响应（自定义错误码和消息）
     */
    public static <T> RestResponse<T> fail(Integer code, String message) {
        return new RestResponse<>(code, message, null);
    }
    
    /**
     * 失败响应（自定义错误码、消息和数据）
     */
    public static <T> RestResponse<T> fail(Integer code, String message, T data) {
        return new RestResponse<>(code, message, data);
    }
    
    // Getter and Setter methods
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
