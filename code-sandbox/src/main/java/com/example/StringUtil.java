package com.example;

public class StringUtil {
    public static String toLowerCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.toLowerCase(); 
    }
}