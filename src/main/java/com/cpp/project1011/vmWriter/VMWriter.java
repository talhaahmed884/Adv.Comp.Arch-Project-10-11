package com.cpp.project1011.vmWriter;

public interface VMWriter {
    void writePush(String segment, int index);

    void writePop(String segment, int index);

    void writeArithmetic(String command);

    void writeLabel(String label);

    void writeGoto(String label);

    void writeIf(String label);

    void writeCall(String name, int nArgs);

    void writeFunction(String name, int nLocals);

    void writeReturn();

    void close();
}
