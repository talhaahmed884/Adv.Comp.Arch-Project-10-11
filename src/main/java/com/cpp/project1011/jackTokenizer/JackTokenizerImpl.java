package com.cpp.project1011.jackTokenizer;

import com.cpp.project1011.compilationEngine.TerminalTag;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class JackTokenizerImpl implements JackTokenizer {
    private final List<Token> tokenList;
    private final String outputFilePath;
    private int tokenCounter;

    public JackTokenizerImpl(String fileName) throws IOException {
        this.tokenList = new ArrayList<>();
        this.tokenizeCommandsList(this.readFile(fileName));
        this.tokenCounter = 0;
        this.outputFilePath = FilenameUtils.getFullPath(fileName) + FilenameUtils.getBaseName(fileName) + "T" + ".xml";
    }

    @Override
    public boolean hasMoreTokens() {
        return this.tokenCounter < this.tokenList.size();
    }

    @Override
    public void advance() {
        this.tokenCounter++;
    }

    @Override
    public String tokenType() {
        return this.tokenList.get(this.tokenCounter).getType().toString();
    }

    @Override
    public String keyWord() {
        if (!this.tokenType().equals(TokenType.KEYWORD.toString())) {
            throw new IllegalArgumentException("Invalid token type. Expected: " + TokenType.KEYWORD);
        }

        return this.tokenList.get(this.tokenCounter).getValue();
    }

    @Override
    public char symbol() {
        if (!this.tokenType().equals(TokenType.SYMBOL.toString())) {
            throw new IllegalArgumentException("Invalid token type. Expected: " + TokenType.SYMBOL);
        }

        return this.tokenList.get(this.tokenCounter).getValue().charAt(0);
    }

    @Override
    public String identifier() {
        if (!this.tokenType().equals(TokenType.IDENTIFIER.toString())) {
            throw new IllegalArgumentException("Invalid token type. Expected: " + TokenType.IDENTIFIER);
        }

        return this.tokenList.get(this.tokenCounter).getValue();
    }

    @Override
    public int intVal() {
        if (!this.tokenType().equals(TokenType.INT_CONST.toString())) {
            throw new IllegalArgumentException("Invalid token type. Expected: " + TokenType.INT_CONST);
        }

        return Integer.parseInt(this.tokenList.get(this.tokenCounter).getValue());
    }

    @Override
    public String stringVal() {
        if (!this.tokenType().equals(TokenType.STRING_CONST.toString())) {
            throw new IllegalArgumentException("Invalid token type. Expected: " + TokenType.STRING_CONST);
        }

        return this.tokenList.get(this.tokenCounter).getValue();
    }

    @Override
    public void compileTokensXML() throws ParserConfigurationException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element tokenElement = document.createElement("tokens");
        document.appendChild(tokenElement);

        while (this.hasMoreTokens()) {
            TokenType tokenType = TokenType.valueOf(tokenType());

            switch (tokenType) {
                case KEYWORD: {
                    Element childElement = document.createElement(TerminalTag.KEYWORD.toString());
                    childElement.setTextContent(" " + keyWord() + " ");
                    tokenElement.appendChild(childElement);
                }
                break;

                case SYMBOL: {
                    Element childElement = document.createElement(TerminalTag.SYMBOL.toString());
                    childElement.setTextContent(" " + symbol() + " ");
                    tokenElement.appendChild(childElement);
                }
                break;

                case INT_CONST: {
                    Element childElement = document.createElement(TerminalTag.INTEGER_CONST.toString());
                    childElement.setTextContent(" " + intVal() + " ");
                    tokenElement.appendChild(childElement);
                }
                break;

                case STRING_CONST: {
                    Element childElement = document.createElement(TerminalTag.STRING_CONST.toString());
                    childElement.setTextContent(" " + stringVal() + " ");
                    tokenElement.appendChild(childElement);
                }
                break;

                case IDENTIFIER: {
                    Element childElement = document.createElement(TerminalTag.IDENTIFIER.toString());
                    childElement.setTextContent(" " + identifier() + " ");
                    tokenElement.appendChild(childElement);
                }
                break;
            }
            advance();
        }
        this.tokenCounter = 0;

        try {
            OutputStream outputStream = new FileOutputStream(outputFilePath);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(outputStream);
            transformer.transform(domSource, streamResult);
        } catch (Exception e) {
            System.out.println("Unable to write XML file");
            throw new RuntimeException(e);
        }
    }

    private List<String> readFile(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            throw new InvalidParameterException("Unable to read file: " + fileName);
        }

        List<String> commandsList;

        try {
            commandsList = Files.readAllLines(file.toPath());
        } catch (Exception e) {
            throw new IOException("Unable to read file: " + fileName, e);
        }

        return this.cleanseCommandsList(commandsList);
    }

    private List<String> cleanseCommandsList(List<String> commandsList) {
        CommandCleanser commandCleanser = new CommandCleanserImpl(commandsList);
        return commandCleanser.CleanseCommand();
    }

    private void tokenizeCommandsList(List<String> commandsList) {
        for (String command : commandsList) {
            this.tokenizeCommand(command);
        }
    }

    private void tokenizeCommand(String command) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int a = 0; a < command.length(); a++) {
            if (isSymbol(String.valueOf(command.charAt(a))) && !stringBuilder.toString().trim().isEmpty()) {
                this.tokenList.add(new Token(TokenType.IDENTIFIER, stringBuilder.toString().trim()));
                stringBuilder.setLength(0);
            }

            stringBuilder.append(command.charAt(a));
            String token = stringBuilder.toString().trim();

            if (token.equals("\"")) {
                a++;
                stringBuilder.setLength(0);

                while (a < command.length() && command.charAt(a) != '\"') {
                    stringBuilder.append(command.charAt(a));
                    a++;
                }
                token = stringBuilder.toString();

                this.tokenList.add(new Token(TokenType.STRING_CONST, token));
                stringBuilder.setLength(0);
            } else if (isSymbol(token)) {
                this.tokenList.add(new Token(TokenType.SYMBOL, token));
                stringBuilder.setLength(0);
            } else if (isKeyWord(token) && (a + 1 < command.length() &&
                    (command.charAt(a + 1) == ' ' || isSymbol(String.valueOf(command.charAt(a + 1)))))) {
                this.tokenList.add(new Token(TokenType.KEYWORD, token));
                stringBuilder.setLength(0);
            } else if (!token.isEmpty() && Character.isDigit(token.charAt(0))) {
                a++;
                while (a < command.length() && Character.isDigit(command.charAt(a))) {
                    stringBuilder.append(command.charAt(a));
                    a++;
                }
                a--;
                token = stringBuilder.toString().trim();
                this.tokenList.add(new Token(TokenType.INT_CONST, token));
                stringBuilder.setLength(0);
            } else if (!token.isEmpty() && a + 1 < command.length() && command.charAt(a + 1) == ' ') {
                this.tokenList.add(new Token(TokenType.IDENTIFIER, token));
                stringBuilder.setLength(0);
            }
        }
    }

    private boolean isSymbol(String token) {
        for (Symbol symbol : Symbol.values()) {
            if (token.equals(symbol.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean isKeyWord(String token) {
        for (KeyWord keyWord : KeyWord.values()) {
            if (token.equals(keyWord.toString().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
