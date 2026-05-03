package com.cpp.project1011.symbolTable;

public enum Segment {
    CONST("CONST", "constant"),
    ARG("ARG", "argument"),
    LOCAL("LOCAL", "local"),
    STATIC("STATIC", "static"),
    THIS("THIS", "this"),
    THAT("THAT", "that"),
    POINTER("POINTER", "pointer"),
    TEMP("TEMP", "temp"),
    NONE("NONE", "none");

    private final String value;
    private final String vmCodeValue;

    Segment(String value, String vmCodeValue) {
        this.value = value;
        this.vmCodeValue = vmCodeValue;
    }

    public String getVmCodeValue() {
        return vmCodeValue;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
