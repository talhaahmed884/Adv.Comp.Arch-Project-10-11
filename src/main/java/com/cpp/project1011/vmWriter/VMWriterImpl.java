package com.cpp.project1011.vmWriter;

import com.cpp.project1011.symbolTable.Segment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class VMWriterImpl implements VMWriter {
    private final PrintWriter writer;

    public VMWriterImpl(String outputFilePath) throws IOException {
        this.writer = new PrintWriter(new FileWriter(outputFilePath));
    }

    @Override
    public void writePush(String segment, int index) {
        Segment seg = Segment.valueOf(segment);
        writer.println("push " + seg.getVmCodeValue() + " " + index);
    }

    @Override
    public void writePop(String segment, int index) {
        Segment seg = Segment.valueOf(segment);
        writer.println("pop " + seg.getVmCodeValue() + " " + index);
    }

    @Override
    public void writeArithmetic(String command) {
        writer.println(command.toLowerCase());
    }

    @Override
    public void writeLabel(String label) {
        writer.println("label " + label);
    }

    @Override
    public void writeGoto(String label) {
        writer.println("goto " + label);
    }

    @Override
    public void writeIf(String label) {
        writer.println("if-goto " + label);
    }

    @Override
    public void writeCall(String name, int nArgs) {
        writer.println("call " + name + " " + nArgs);
    }

    @Override
    public void writeFunction(String name, int nLocals) {
        writer.println("function " + name + " " + nLocals);
    }

    @Override
    public void writeReturn() {
        writer.println("return");
    }

    @Override
    public void close() {
        writer.close();
    }
}
