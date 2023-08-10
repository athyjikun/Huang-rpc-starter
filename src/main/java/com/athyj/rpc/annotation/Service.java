package com.athyj.rpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 被该注解标记的服务可提供远程RPC访问的能力
 */
@Target({ElementType.TYPE})  // 用于描述注解的使用范围
@Retention(RetentionPolicy.RUNTIME)  // 注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在
@Documented  // 修饰注解，使其显示
@Component
public @interface Service {
    String value() default "";
}
