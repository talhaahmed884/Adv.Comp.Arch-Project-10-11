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
        writer.println(Command.push + " " + seg.getVmCodeValue() + " " + index);
    }

    @Override
    public void writePop(String segment, int index) {
        Segment seg = Segment.valueOf(segment);
        writer.println(Command.pop + " " + seg.getVmCodeValue() + " " + index);
    }

    @Override
    public void writeArithmetic(String command) {
        writer.println(command.toLowerCase());
    }

    @Override
    public void writeLabel(String label) {
        writer.println(Command.label + " " + label);
    }

    @Override
    public void writeGoto(String label) {
        writer.println(Command._goto + " " + label);
    }

    @Override
    public void writeIf(String label) {
        writer.println(Command.if_goto + " " + label);
    }

    @Override
    public void writeCall(String name, int nArgs) {
        writer.println(Command.call + " " + name + " " + nArgs);
    }

    @Override
    public void writeFunction(String name, int nLocals) {
        writer.println(Command.function + " " + name + " " + nLocals);
    }

    @Override
    public void writeReturn() {
        writer.println(Command._return);
    }

    @Override
    public void close() {
        writer.close();
    }
}
