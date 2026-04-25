package com.cpp.project1011.jackTokenizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class JackTokenizerImpl implements JackTokenizer {
    private final List<Token> tokenList;
    private int tokenCounter;

    public JackTokenizerImpl(String fileName) throws IOException {
        this.tokenList = new ArrayList<>();
        this.tokenizeCommandsList(this.readFile(fileName));
        this.tokenCounter = 0;
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
            if ((isSymbol(String.valueOf(command.charAt(a))) || isKeyWord(String.valueOf(command.charAt(a))))
                    && !stringBuilder.toString().trim().isEmpty()) {
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
            } else if (isKeyWord(token)) {
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
