package com.example.springbootdemo.Proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyTest {
    public static void main(String[] args) {
//        UserService userService= new UserServiceImplProxy();
//        userService.findName();
        UserService userService = new UserServiceImpl();
        UserServiceImplProxyDy userServiceImplProxyDy = new UserServiceImplProxyDy();
        UserService userServiceProxy = (UserService) userServiceImplProxyDy.getObject(userService);
        userServiceProxy.findName();
    }
}
interface UserService {
    void findName();
}
class UserServiceImpl implements UserService {

    @Override
    public void findName() {
        System.out.println("查找数据中");
    }
}
class UserServiceImplProxy implements UserService {
    private UserService userService = new UserServiceImpl();
    @Override
    public void findName() {
        System.out.println("准备数据中");
        userService.findName();
        System.out.println("数据查询完毕");
    }
}

class UserServiceImplProxyDy implements InvocationHandler {
    private Object object;


    public Object getObject(Object object) {
        this.object = object;
        return Proxy.newProxyInstance(this.object.getClass().getClassLoader(), this.object.getClass().getInterfaces(), this);
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println(1);
        return method.invoke(object,args);
    }
}