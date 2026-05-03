package com.cpp.project1011.symbolTable;

public class Symbol {
    public String name;
    public String type;
    public String kind;
    public int index;

    public Symbol(String name, String type, String kind, int index) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.index = index;
    }
}
