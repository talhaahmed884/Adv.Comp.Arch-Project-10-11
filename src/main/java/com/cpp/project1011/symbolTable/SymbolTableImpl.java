package com.cpp.project1011.symbolTable;

import java.util.Hashtable;

public class SymbolTableImpl implements SymbolTable {
    private final Hashtable<String, Symbol> classSymbolTable;
    private final Hashtable<String, Symbol> subroutineSymbolTable;

    private int varSymbolCount;
    private int argSymbolCount;
    private int staticSymbolCount;
    private int fieldSymbolCount;

    public SymbolTableImpl() {
        classSymbolTable = new Hashtable<>();
        subroutineSymbolTable = new Hashtable<>();

        varSymbolCount = 0;
        argSymbolCount = 0;
        staticSymbolCount = 0;
        fieldSymbolCount = 0;
    }

    @Override
    public void startSubroutine() {
        subroutineSymbolTable.clear();
        varSymbolCount = 0;
        argSymbolCount = 0;
    }

    @Override
    public void define(String name, String type, String kind) {
        SymbolKind symbolKind = SymbolKind.valueOf(kind.toUpperCase());
        Symbol targetSymbol;

        switch (symbolKind) {
            case VAR, ARG -> targetSymbol = subroutineSymbolTable.get(name);
            case STATIC, FIELD -> targetSymbol = classSymbolTable.get(name);
            default -> throw new IllegalArgumentException("Unknown symbol kind: " + symbolKind);
        }

        if (targetSymbol != null) {
            throw new IllegalArgumentException("Symbol already defined");
        }

        switch (symbolKind) {
            case VAR, ARG: {
                if (symbolKind == SymbolKind.VAR) {
                    targetSymbol = new Symbol(name, type, kind, varSymbolCount++);
                } else {
                    targetSymbol = new Symbol(name, type, kind, argSymbolCount++);
                }
                subroutineSymbolTable.put(name, targetSymbol);
            }
            break;

            case STATIC, FIELD: {
                if (symbolKind == SymbolKind.STATIC) {
                    targetSymbol = new Symbol(name, type, kind, staticSymbolCount++);
                } else {
                    targetSymbol = new Symbol(name, type, kind, fieldSymbolCount++);
                }
                classSymbolTable.put(name, targetSymbol);
            }
            break;

            default:
                throw new IllegalArgumentException("Unknown symbol kind: " + symbolKind);
        }
    }

    @Override
    public int varCount(String kind) {
        SymbolKind symbolKind = SymbolKind.valueOf(kind.toUpperCase());
        return switch (symbolKind) {
            case VAR -> varSymbolCount;
            case STATIC -> staticSymbolCount;
            case FIELD -> fieldSymbolCount;
            case ARG -> argSymbolCount;
            default -> throw new IllegalArgumentException("Unknown symbol kind: " + symbolKind);
        };
    }

    @Override
    public String kindOf(String name) {
        Symbol targetSymbol = subroutineSymbolTable.get(name);
        if (targetSymbol == null) {
            targetSymbol = classSymbolTable.get(name);
        }
        return targetSymbol == null ? SymbolKind.NONE.toString() : targetSymbol.kind;
    }

    @Override
    public String typeOf(String name) {
        Symbol targetSymbol = subroutineSymbolTable.get(name);
        if (targetSymbol == null) {
            targetSymbol = classSymbolTable.get(name);
        }
        if (targetSymbol == null) {
            throw new IllegalArgumentException("Unknown symbol name: " + name);
        }
        return targetSymbol.type;
    }

    @Override
    public int indexOf(String name) {
        Symbol targetSymbol = subroutineSymbolTable.get(name);
        if (targetSymbol == null) {
            targetSymbol = classSymbolTable.get(name);
        }
        if (targetSymbol == null) {
            throw new IllegalArgumentException("Unknown symbol name: " + name);
        }
        return targetSymbol.index;
    }
}
