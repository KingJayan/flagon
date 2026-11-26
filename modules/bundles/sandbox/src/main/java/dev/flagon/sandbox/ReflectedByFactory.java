package dev.flagon.sandbox;

public class ReflectedByFactory {

    public static String create(String name) {
        return "parent " + name;
    }
}
