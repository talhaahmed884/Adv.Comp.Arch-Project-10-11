package jackTokenizer;

public enum TokenType {
    KEYWORD("KEYWORD"),
    SYMBOL("SYMBOL"),
    IDENTIFIER("IDENTIFIER"),
    INT_CONST("INT_CONST"),
    STRING_CONST("STRING_CONST");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
