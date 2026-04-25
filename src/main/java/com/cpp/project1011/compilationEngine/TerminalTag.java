package com.cpp.project1011.compilationEngine;

public enum TerminalTag {
    KEYWORD("keyword"),
    SYMBOL("symbol"),
    IDENTIFIER("identifier"),
    INTEGER_CONST("integerConstant"),
    STRING_CONST("stringConstant");

    private final String value;

    TerminalTag(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
