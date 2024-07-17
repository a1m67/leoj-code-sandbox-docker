package com.example.springbootdemo.demos.classtest;

public class Animal {
    private String name;
    public void call() {
        System.out.println("动物叫");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
