package com.cpp.project1011.symbolTable;

public enum SymbolKind {
    STATIC("STATIC"),
    FIELD("FIELD"),
    ARG("ARG"),
    VAR("VAR"),
    NONE("NONE");

    private final String value;

    SymbolKind(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
