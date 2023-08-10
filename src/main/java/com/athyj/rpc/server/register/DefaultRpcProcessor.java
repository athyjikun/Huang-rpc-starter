package com.athyj.rpc.server.register;

import com.athyj.rpc.annotation.InjectService;
import com.athyj.rpc.annotation.Service;
import com.athyj.rpc.client.ClientProxyFactory;
import com.athyj.rpc.server.RpcServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

/**
 * RPC处理者，支持服务启动暴露、自动注入Service
 */
// 效果就是在Spring启动完毕过后会收到一个事件通知，基于这个机制，就可以在这里开启服务，以及注入服务
public class DefaultRpcProcessor implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private ClientProxyFactory clientProxyFactory;

    @Resource
    private ServiceRegister serviceRegister;

    @Resource
    private RpcServer rpcServer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (Objects.isNull(event.getApplicationContext().getParent())) {
            ApplicationContext context = event.getApplicationContext();
            // 开启服务
            startServer(context);

            // 注入Service
            injectService(context);
        }
    }

    private void startServer(ApplicationContext context) {
        // 获取ioc容器中所有使用了Service接口的所有bean
        Map<String, Object> beans = context.getBeansWithAnnotation(Service.class);
        if (beans.size() != 0) {
            boolean startServerFlag = true;
            for (Object obj : beans.values()) {
                try {
                    Class<?> clazz = obj.getClass();
                    Class<?>[] interfaces = clazz.getInterfaces();  // 获取实现的接口
                    ServiceObject so;
                    if (interfaces.length != 1) {
                        Service service = clazz.getAnnotation(Service.class);
                        String value = service.value();
                        if (value.equals("")) {
                            startServerFlag = false;
                            throw new UnsupportedOperationException("The exposed interface is not specific with '" + obj.getClass().getName() + "'");
                        }
                        so = new ServiceObject(value, Class.forName(value), obj);
                    } else {
                        Class<?> superClass = interfaces[0];
                        so = new ServiceObject(superClass.getName(), superClass, obj);
                    }
                    serviceRegister.register(so);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (startServerFlag) {
                rpcServer.start();
            }
        }
    }

    private void injectService(ApplicationContext context) {
        // 获取 Spring 容器中定义的所有 JavaBean 的名称。
        String[] names = context.getBeanDefinitionNames();
        for (String name : names) {
            Class<?> clazz = context.getType(name);
            if (Objects.isNull(clazz)) continue;
            // 获得某个类的所有声明的字段，即包括public、private和proteced，但是不包括父类的申明字段。
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                InjectService injectLeisure = field.getAnnotation(InjectService.class);
                if (Objects.isNull(injectLeisure)) continue;
                Class<?> fieldClass = field.getType();
                Object object = context.getBean(name);
                field.setAccessible(true);
                try {
                    field.set(object, clientProxyFactory.getProxy(fieldClass));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
