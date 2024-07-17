package com.example.springbootdemo.demos.interfaceandclass;

public interface InterfaceAnimal {
    public static final String name = "animal";
    default void haha() {
        System.out.println("animal");
    }
    void call();
    void sleep();
}
