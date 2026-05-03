package com.cpp.project1011.symbolTable;

public interface SymbolTable {
    void startSubroutine();

    void define(String name, String type, String kind);

    int varCount(String kind);

    String kindOf(String name);

    String typeOf(String name);

    int indexOf(String name);
}
