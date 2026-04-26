package com.cpp.project1011.jackTokenizer;

import javax.xml.parsers.ParserConfigurationException;

public interface JackTokenizer {
    boolean hasMoreTokens();

    void advance();

    String tokenType();

    String keyWord();

    char symbol();

    String identifier();

    int intVal();

    String stringVal();

    void compileTokensXML() throws ParserConfigurationException;
}
