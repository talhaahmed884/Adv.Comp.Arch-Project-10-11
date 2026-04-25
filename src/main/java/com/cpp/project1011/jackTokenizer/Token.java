package com.cpp.project1011.jackTokenizer;

public class Token {
    private final TokenType type;
    private final String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public static String toXMLToken(TokenType type, String value) {
        if (type == TokenType.SYMBOL) {
            Symbol symbol = Symbol.fromValue(value);
            if (!symbol.getEscapeValue().isEmpty()) {
                value = symbol.getEscapeValue();
            }
        }

        String tokenType = type.toString().toLowerCase();
        return String.format("<%s> %s </%s>", tokenType, value, tokenType);
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Token type: " + type + " value: " + value + "\n";
    }
}
