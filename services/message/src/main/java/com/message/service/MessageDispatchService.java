package com.message.service;

import java.util.Collection;

public interface MessageDispatchService {

    /**
     * description 把消息发给某个用户
     * author zzq
     * date 2025/12/20 21:17
     * param
     * return
     */
    void sendToUser(String userId, String destination, Object payload);


    void sendToUsers(Collection<String> userIds, String destination, Object payload);

    /**
     * description 把消息发给所有用户
     * author zzq
     * date 2025/12/20 21:17
     * param
     * return
     */
    void sendToAll(String destination, Object payload);
}
