package com.java.agent.demo;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

/**
 * @author qinfuxiang
 * @Date 2020/7/6 14:49
 */
public class MyInterceptor {

    @RuntimeType
    public static Object intercept(@Origin Method method,
        @SuperCall Callable<?> callable)
        throws Exception {
        long start = System.currentTimeMillis();
        try {
            //执行原方法
            return callable.call();
        } finally {
            //打印调用时长
            System.out.println(method.getName() + ":" +
                (System.currentTimeMillis() - start) + "ms");
        }
    }

}
