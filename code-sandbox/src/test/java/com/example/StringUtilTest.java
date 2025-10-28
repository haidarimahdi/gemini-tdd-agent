package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilTest {

    @Test
    void testAllUpperCaseToLowerCase() {
        String input = "HELLO WORLD";
        String expected = "hello world";
        String actual = StringUtil.toLowerCase(input);
        assertEquals(expected, actual);
    }

    @Test
    void testMixedCaseToLowerCase() {
        String input = "Hello World";
        String expected = "hello world";
        String actual = StringUtil.toLowerCase(input);
        assertEquals(expected, actual);
    }

    @Test
    void testAllLowerCaseToLowerCase() {
        String input = "hello world";
        String expected = "hello world";
        String actual = StringUtil.toLowerCase(input);
        assertEquals(expected, actual);
    }

    @Test
    void testEmptyStringToLowerCase() {
        String input = "";
        String expected = "";
        String actual = StringUtil.toLowerCase(input);
        assertEquals(expected, actual);
    }

    @Test
    void testStringWithNumbersToLowerCase() {
        String input = "Hello123";
        String expected = "hello123";
        String actual = StringUtil.toLowerCase(input);
        assertEquals(expected, actual);
    }

    @Test
    void testStringWithSpecialCharactersToLowerCase() {
        String input = "Hello@World!";
        String expected = "hello@world!";
        String actual = StringUtil.toLowerCase(input);
        assertEquals(expected, actual);
    }
}