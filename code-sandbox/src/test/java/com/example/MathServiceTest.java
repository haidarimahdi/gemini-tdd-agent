package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathServiceTest {

    @Test
    void testMultiply() {
        int result = MathService.multiply(2,3);

        assertEquals(6, result, "Multiplication failed");
    }

    @Test
    void testDecimalMultiply() {
        double result = MathService.multiply(2.5, 2.0);

        assertEquals(5.0, result, "Decimal Multiplication failed");
    }
}