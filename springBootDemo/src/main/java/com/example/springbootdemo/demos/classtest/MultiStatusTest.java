package com.example.springbootdemo.demos.classtest;

public class MultiStatusTest {
    public static void main(String[] args) {
        Animal animal = new Animal();
        Animal cat = new Cat();
        Animal dog = new Dog();
        animal.call();
        cat.call();
        dog.call();
    }
}
