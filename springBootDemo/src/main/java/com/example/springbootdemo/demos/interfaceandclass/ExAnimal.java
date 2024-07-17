package com.example.springbootdemo.demos.interfaceandclass;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ExAnimal extends AbstractAnimal {
    public String name;
    @Override
    void call() {
        System.out.println("哈哈哈哈哈哈哈");
    }

    @Override
    void sleep() {
        System.out.println("动物在休息");
    }

    public static void main(String[] args) throws Exception {
        //正常创建类
        Apple apple = new Apple();
        apple.setPrice(5);
        System.out.println(apple.getPrice());

        //通过反射机制创建
        Class<?> clz = Class.forName("com.example.springbootdemo.demos.interfaceandclass.Apple");
        Method setPriceMethod = clz.getMethod("setPrice", int.class);
        List<Constructor> constructors= Arrays.asList(clz.getDeclaredConstructors());
//        Constructor appleConstructor = clz.getConstructor();
//        Object appleObj = appleConstructor.newInstance();
//        setPriceMethod.invoke(appleObj, 14);
//        Method getPriceMethod = clz.getMethod("getPrice");
//        System.out.println("Apple Price:" + getPriceMethod.invoke(appleObj));

    }

}
class Apple {

    private int price;
    public Apple() {

    }
    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
class Person {
    private String name;

    public Person(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}