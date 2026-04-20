package jackTokenizer;

public interface JackTokenizer {
    boolean hasMoreTokens();

    void advance();

    String tokenType();

    String keyWord();

    char symbol();

    String identifier();

    int intVal();

    String stringVal();
}
