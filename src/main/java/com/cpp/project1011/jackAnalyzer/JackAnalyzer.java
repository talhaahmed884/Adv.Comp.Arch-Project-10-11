package com.cpp.project1011.jackAnalyzer;

import com.cpp.project1011.compilationEngine.CompilationEngine;
import com.cpp.project1011.compilationEngine.CompilationEngineXML;
import com.cpp.project1011.jackTokenizer.JackTokenizer;
import com.cpp.project1011.jackTokenizer.JackTokenizerImpl;
import org.apache.commons.io.FilenameUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

public class JackAnalyzer {
    private final String[] fileNames;
    private String sourcePath;

    public JackAnalyzer(String name) {
        File file = new File(name);

        if (file.isFile()) {
            fileNames = new String[]{FilenameUtils.getName(name)};
            sourcePath = FilenameUtils.getPath(name);
        } else if (file.isDirectory()) {
            fileNames = file.list();
            sourcePath = file.getPath();
        } else {
            throw new InvalidParameterException("Unable to read path: " + name);
        }

        if (!sourcePath.endsWith("/")) {
            sourcePath += "/";
        }
    }

    public void analyze() throws IOException, ParserConfigurationException {
        for (String fileName : fileNames) {
            JackTokenizer tokenizer = new JackTokenizerImpl(sourcePath + fileName);
            tokenizer.compileTokensXML();

            String outputFilePath = sourcePath + FilenameUtils.getBaseName(fileName) + ".xml";

            CompilationEngine compilationEngine = new CompilationEngineXML(outputFilePath, tokenizer);

            compilationEngine.compileClass();
        }
    }
}
