package com.athyj.rpc.annotation;

import java.lang.annotation.*;

/**
 * 该注解用于注入远程服务
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectService {

}
