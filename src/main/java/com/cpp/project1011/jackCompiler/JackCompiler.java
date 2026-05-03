package com.cpp.project1011.jackCompiler;

import com.cpp.project1011.compilationEngine.CompilationEngineCode;
import com.cpp.project1011.jackTokenizer.JackTokenizer;
import com.cpp.project1011.jackTokenizer.JackTokenizerImpl;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;

public class JackCompiler {
    private final String[] fileNames;
    private String sourcePath;

    public JackCompiler(String name) {
        File file = new File(name);

        if (file.isFile()) {
            fileNames = new String[]{FilenameUtils.getName(name)};
            sourcePath = FilenameUtils.getPath(name);
        } else if (file.isDirectory()) {
            String[] all = file.list();
            fileNames = all == null ? new String[0]
                    : Arrays.stream(all)
                      .filter(f -> f.endsWith(".jack"))
                      .toArray(String[]::new);
            sourcePath = file.getPath();
        } else {
            throw new InvalidParameterException("Unable to read path: " + name);
        }

        if (!sourcePath.endsWith("/")) {
            sourcePath += "/";
        }
    }

    public void compile() throws IOException {
        for (String fileName : fileNames) {
            JackTokenizer tokenizer = new JackTokenizerImpl(sourcePath + fileName);

            String vmOutputPath = sourcePath + FilenameUtils.getBaseName(fileName) + ".vm";

            CompilationEngineCode codeEngine = new CompilationEngineCode(vmOutputPath, tokenizer);

            codeEngine.compileClass();
        }
    }
}
