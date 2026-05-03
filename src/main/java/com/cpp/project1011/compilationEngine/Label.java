package com.cpp.project1011.compilationEngine;

public enum Label {
    WHILE_EXP("WHILE_EXP"),
    WHILE_END("WHILE_END"),
    IF_FALSE("IF_FALSE"),
    IF_END("IF_END"),
    STRING_NEW("String.new"),
    STRING_APPEND_CHAR("String.appendChar"),
    MEMORY_ALLOC("Memory.alloc"),
    MATH_MULTIPLY("Math.multiply"),
    MATH_DIVIDE("Math.divide");

    private final String value;

    Label(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
