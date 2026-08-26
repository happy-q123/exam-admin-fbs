package com.exam.websocket;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.websocket.server.ServerEndpointConfig;

/**
 * 将 Jakarta WebSocket Endpoint 交给 Spring 容器创建，保证构造器和字段依赖可以正常注入。
 *
 * <p>Spring 的 {@code SpringConfigurator} 依赖传统的 ContextLoaderListener，而本服务运行在
 * Spring Boot 内嵌 Tomcat 中并没有该根上下文。使用静态上下文桥接可以兼容
 * {@link ServerEndpointConfig.Configurator} 由 Tomcat 反射创建的生命周期。</p>
 */
@Component
public class SpringEndpointConfigurator extends ServerEndpointConfig.Configurator
        implements ApplicationContextAware {
    private static volatile ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        ApplicationContext context = applicationContext;
        if (context == null) {
            throw new InstantiationException("Spring ApplicationContext 尚未初始化");
        }
        return context.getBean(endpointClass);
    }
}
