package com.cpp.project1011.compilationEngine;

public interface CompilationEngine {
    void compileClass();

    void compileClassVarDec();

    void compileSubroutine();

    void compileParameterList();

    void compileVarDec();

    void compileStatements();

    void compileDo();

    void compileLet();

    void compileWhile();

    void compileReturn();

    void compileIf();

    void compileExpression();

    void compileTerm();

    void compileExpressionList();
}
