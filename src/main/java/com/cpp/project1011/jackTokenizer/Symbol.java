package com.cpp.project1011.jackTokenizer;

public enum Symbol {
    CURLY_START("{", ""),
    CURLY_END("}", ""),
    PARENTHESES_START("(", ""),
    PARENTHESES_END(")", ""),
    SQUARE_START("[", ""),
    SQUARE_END("]", ""),
    PERIOD(".", ""),
    COMMA(",", ""),
    SEMICOLON(";", ""),
    PLUS("+", ""),
    MINUS("-", ""),
    STAR("*", ""),
    SLASH("/", ""),
    AMPERSAND("&", "&amp"),
    BAR("|", ""),
    LESS_THAN("<", "&lt"),
    GREATER_THAN(">", "&gt"),
    EQUAL("=", ""),
    TILDE("~", ""),
    QUOT("\"", "&quot");

    private final String value;
    private final String escapeValue;

    Symbol(String value, String escapeValue) {
        this.value = value;
        this.escapeValue = escapeValue;
    }

    public static Symbol fromValue(String value) {
        for (Symbol symbol : Symbol.values()) {
            if (symbol.value.equals(value)) {
                return symbol;
            }
        }
        throw new IllegalArgumentException("invalid symbol value: " + value);
    }

    public String getEscapeValue() {
        return escapeValue;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
