package com.cpp.project1011.compilationEngine;

public enum NonTerminalTag {
    CLASS("class"),
    CLASS_VAR_DEC("classVarDec"),
    SUBROUTINE_DEC("subroutineDec"),
    PARAMETER_LIST("parameterList"),
    SUBROUTINE_BODY("subroutineBody"),
    VAR_DEC("varDec"),
    STATEMENTS("statements"),
    LET_STATEMENT("letStatement"),
    IF_STATEMENT("ifStatement"),
    WHILE_STATEMENT("whileStatement"),
    DO_STATEMENT("doStatement"),
    RETURN_STATEMENT("returnStatement"),
    EXPRESSION("expression"),
    TERM("term"),
    EXPRESSION_LIST("expressionList");

    private final String value;

    NonTerminalTag(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
