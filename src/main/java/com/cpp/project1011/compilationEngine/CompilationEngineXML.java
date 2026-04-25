package com.cpp.project1011.compilationEngine;

import com.cpp.project1011.jackTokenizer.JackTokenizer;
import com.cpp.project1011.jackTokenizer.KeyWord;
import com.cpp.project1011.jackTokenizer.Symbol;
import com.cpp.project1011.jackTokenizer.TokenType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public class CompilationEngineXML implements CompilationEngine {
    private final String outputFilePath;
    private final JackTokenizer tokenizer;
    private final Document document;
    private Element parentElement;

    public CompilationEngineXML(String outputFilePath, JackTokenizer tokenizer) throws ParserConfigurationException {
        this.outputFilePath = outputFilePath;
        this.tokenizer = tokenizer;
        this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        this.parentElement = null;
    }

    @Override
    public void compileClass() {
        // 1. adding class section
        Element classSectionElement = document.createElement(NonTerminalTag.CLASS.toString());
        document.appendChild(classSectionElement);

        // 2. adding class keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.CLASS});
        writeKeywordElement(classSectionElement);
        tokenizer.advance();

        // 3. adding class identifier element
        verifyIdentifierOrThrowError();
        writeIdentifierElement(classSectionElement);
        tokenizer.advance();

        // 4. adding class symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_START});
        writeSymbolElement(classSectionElement);
        tokenizer.advance();

        // 5. adding class variables, subroutines, or symbol (class end) elements
        while (tokenizer.hasMoreTokens()) {
            TokenType type = TokenType.valueOf(tokenizer.tokenType());

            switch (type) {
                case KEYWORD: {
                    // 5A. adding class variables elements
                    verifyKeywordOrThrowError(new KeyWord[]{KeyWord.FIELD, KeyWord.STATIC, KeyWord.CONSTRUCTOR, KeyWord.METHOD,
                            KeyWord.FUNCTION});
                    if (isClassVarDecKeyword(tokenizer.keyWord())) {
                        this.parentElement = classSectionElement;
                        compileClassVarDec();
                    }
                    // 5B. adding class methods elements
                    else if (isSubroutineType(tokenizer.keyWord())) {
                        this.parentElement = classSectionElement;
                        compileSubroutine();
                    }
                }
                break;

                // 6. adding symbol (class end) element
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});
                    writeSymbolElement(classSectionElement);
                }
                break;

                default: {
                    throw new InvalidParameterException("Invalid token type. Expected KEYWORD or SYMBOL. Got token type: "
                            + tokenizer.tokenType());
                }
            }

            tokenizer.advance();
        }

        try {
            writeToFile();
        } catch (Exception e) {
            System.out.println("Unable to write XML file");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void compileClassVarDec() {
        // 1. adding class variables section
        Element classVarSectionElement = document.createElement(NonTerminalTag.CLASS_VAR_DEC.toString());
        this.parentElement.appendChild(classVarSectionElement);

        // 2. adding field or static keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.FIELD, KeyWord.STATIC});
        writeKeywordElement(classVarSectionElement);
        tokenizer.advance();

        // 3. adding int, char, or boolean keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.INT, KeyWord.CHAR, KeyWord.BOOLEAN});
        writeKeywordElement(classVarSectionElement);
        tokenizer.advance();

        // 4. adding identifier or symbol elements
        while (tokenizer.hasMoreTokens()) {
            TokenType type = TokenType.valueOf(tokenizer.tokenType());

            switch (type) {
                // 4A. adding identifier element
                case IDENTIFIER: {
                    verifyIdentifierOrThrowError();
                    writeIdentifierElement(classVarSectionElement);
                }
                break;

                // 4B. adding symbol COMMA or SEMICOLON element
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.COMMA, Symbol.SEMICOLON});
                    writeSymbolElement(classVarSectionElement);

                    Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
                    // 4C. class variable declaration has ended. Traverse back to caller!!
                    if (symbol == Symbol.SEMICOLON) {
                        return;
                    }
                }
                break;

                default: {
                    throw new InvalidParameterException("Invalid token type. Expected IDENTIFIER or SYMBOL. Got token type: "
                            + tokenizer.tokenType());
                }
            }

            tokenizer.advance();
        }
    }

    @Override
    public void compileSubroutine() {
        // 1. adding subroutine section
        Element subroutineSectionElement = document.createElement(NonTerminalTag.SUBROUTINE_DEC.toString());
        this.parentElement.appendChild(subroutineSectionElement);

        // 2. adding constructor, method, or function keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.CONSTRUCTOR, KeyWord.METHOD, KeyWord.FUNCTION});
        writeKeywordElement(subroutineSectionElement);
        tokenizer.advance();


        TokenType type = TokenType.valueOf(tokenizer.tokenType());
        // 3. branching between constructor or method & function return types declaration
        if (type == TokenType.IDENTIFIER) {
            // 3A. adding identifier element
            verifyIdentifierOrThrowError();
            writeIdentifierElement(subroutineSectionElement);
            tokenizer.advance();
        } else if (type == TokenType.KEYWORD) {
            // 3B. adding keyword element
            verifyKeywordOrThrowError(new KeyWord[]{KeyWord.INT, KeyWord.CHAR, KeyWord.BOOLEAN, KeyWord.VOID});
            writeKeywordElement(subroutineSectionElement);
            tokenizer.advance();
        } else {
            throw new InvalidParameterException("Invalid token type. Expected IDENTIFIER or KEYWORD. " +
                    "Got token type: " + tokenizer.tokenType());
        }

        // 4. adding identifier element
        verifyIdentifierOrThrowError();
        writeIdentifierElement(subroutineSectionElement);
        tokenizer.advance();

        // 5. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START});
        writeSymbolElement(subroutineSectionElement);
        tokenizer.advance();

        // 6. adding parameter list elements
        Element parentCopy = this.parentElement;
        this.parentElement = subroutineSectionElement;
        compileParameterList();
        this.parentElement = parentCopy;

        // 7. adding symbol (method declaration end) element
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
        writeSymbolElement(subroutineSectionElement);
        tokenizer.advance();

        // 8. adding subroutine body elements
        parentCopy = this.parentElement;
        this.parentElement = subroutineSectionElement;
        this.compileSubroutineBody();
        this.parentElement = parentCopy;
    }

    @Override
    public void compileParameterList() {
        // 1. adding parameter list section
        Element parameterListSection = document.createElement(NonTerminalTag.PARAMETER_LIST.toString());
        this.parentElement.appendChild(parameterListSection);
        boolean hasParameters = false;

        // 2. adding parameters elements
        while (tokenizer.hasMoreTokens()) {
            TokenType type = TokenType.valueOf(tokenizer.tokenType());

            switch (type) {
                // 2A. adding keyword element
                case KEYWORD: {
                    verifyKeywordOrThrowError(new KeyWord[]{KeyWord.INT, KeyWord.CHAR, KeyWord.BOOLEAN});
                    writeKeywordElement(parameterListSection);
                }
                break;

                // 2B. adding identifier element
                case IDENTIFIER: {
                    verifyIdentifierOrThrowError();
                    writeIdentifierElement(parameterListSection);
                }
                break;

                // 2C. adding symbol element
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.COMMA, Symbol.PARENTHESES_END});

                    Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
                    if (symbol == Symbol.COMMA) {
                        writeSymbolElement(parameterListSection);
                    }
                    // 2D. parameters list has ended. Traverse back to caller!!
                    else {
                        if (!hasParameters) {
                            parameterListSection.setTextContent("\n");
                        }
                        return;
                    }
                }
                break;

                default: {
                    throw new InvalidParameterException("Invalid token type. Expected KEYWORD, IDENTIFIER, or SYMBOL. " +
                            "Got token type: " + tokenizer.tokenType());
                }
            }

            tokenizer.advance();
            hasParameters = true;
        }
    }

    @Override
    public void compileVarDec() {
        Element varSection = document.createElement(NonTerminalTag.VAR_DEC.toString());
        this.parentElement.appendChild(varSection);

        //  1. adding keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.VAR});
        writeKeywordElement(varSection);
        tokenizer.advance();

        // 2. adding var elements
        while (tokenizer.hasMoreTokens()) {
            TokenType type = TokenType.valueOf(tokenizer.tokenType());

            switch (type) {
                // 2A. adding identifier element
                case IDENTIFIER: {
                    verifyIdentifierOrThrowError();
                    writeIdentifierElement(varSection);
                }
                break;

                // 2B. adding symbol element
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.COMMA, Symbol.SEMICOLON});
                    Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
                    writeSymbolElement(varSection);

                    if (symbol == Symbol.SEMICOLON) {
                        return;
                    }
                }
                break;

                // 2C. adding keyword element
                case KEYWORD: {
                    verifyKeywordOrThrowError(new KeyWord[]{KeyWord.INT, KeyWord.CHAR, KeyWord.BOOLEAN});
                    writeKeywordElement(varSection);
                }
                break;

                default: {
                    throw new InvalidParameterException("Invalid token type. Expected IDENTIFIER or SYMBOL. " +
                            "Got token type: " + tokenizer.tokenType());
                }
            }
            tokenizer.advance();
        }
    }

    @Override
    public void compileStatements() {
        // 1. adding statements section
        Element statementsSection = document.createElement(NonTerminalTag.STATEMENTS.toString());
        this.parentElement.appendChild(statementsSection);
        boolean hasStatements = false;

        while (tokenizer.hasMoreTokens()) {
            TokenType type = TokenType.valueOf(tokenizer.tokenType());

            // 2. adding statements
            switch (type) {
                // 3. adding keyword statements
                case KEYWORD: {
                    KeyWord keyWord = KeyWord.fromValue(tokenizer.keyWord());
                    switch (keyWord) {
                        // 3A. adding LET statements
                        case LET: {
                            verifyKeywordOrThrowError(new KeyWord[]{KeyWord.LET});
                            Element parentCopy = this.parentElement;
                            this.parentElement = statementsSection;
                            this.compileLet();
                            this.parentElement = parentCopy;
                        }
                        break;

                        // 3B. adding DO statements
                        case DO: {
                            verifyKeywordOrThrowError(new KeyWord[]{KeyWord.DO});
                            Element parentCopy = this.parentElement;
                            this.parentElement = statementsSection;
                            this.compileDo();
                            this.parentElement = parentCopy;
                        }
                        break;

                        // 3C. adding RETURN statements
                        case RETURN: {
                            verifyKeywordOrThrowError(new KeyWord[]{KeyWord.RETURN});
                            Element parentCopy = this.parentElement;
                            this.parentElement = statementsSection;
                            this.compileReturn();
                            this.parentElement = parentCopy;
                        }
                        break;

                        // 3D. adding IF statements
                        case IF: {
                            verifyKeywordOrThrowError(new KeyWord[]{KeyWord.IF});
                            Element parentCopy = this.parentElement;
                            this.parentElement = statementsSection;
                            this.compileIf();
                            this.parentElement = parentCopy;
                            continue;
                        }

                        default: {
                            throw new InvalidParameterException("Invalid token type. Expected LET, DO, RETURN, or IF. " +
                                    "Got token type: " + tokenizer.tokenType());
                        }
                    }
                }
                break;

                // 4. adding symbol statements
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});
                    // 4A. statements has ended. Traverse back to caller!!
                    if (!hasStatements) {
                        statementsSection.setTextContent("\n");
                    }
                    return;
                }

                default: {
                    throw new InvalidParameterException("Invalid token type. Expected KEYWORD or SYMBOL. Got token type: " +
                            tokenizer.tokenType());
                }
            }

            hasStatements = true;
            tokenizer.advance();
        }
    }

    @Override
    public void compileDo() {
        // 1. adding DO section
        Element doSection = document.createElement(NonTerminalTag.DO_STATEMENT.toString());
        this.parentElement.appendChild(doSection);

        // 2. adding keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.DO});
        writeKeywordElement(doSection);
        tokenizer.advance();

        // 3. adding identifier element
        verifyIdentifierOrThrowError();
        writeIdentifierElement(doSection);
        tokenizer.advance();

        // 4. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START, Symbol.PERIOD});
        writeSymbolElement(doSection);

        // 5. checking chaining of methods
        Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
        tokenizer.advance();
        if (symbol == Symbol.PERIOD) {
            // 5A. adding identifier element
            verifyIdentifierOrThrowError();
            writeIdentifierElement(doSection);
            tokenizer.advance();

            // 5B. adding symbol element
            verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START});
            writeSymbolElement(doSection);
            tokenizer.advance();
        }

        // 6. adding expression list section
        Element parentCopy = this.parentElement;
        this.parentElement = doSection;
        this.compileExpressionList();
        this.parentElement = parentCopy;

        // 7. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
        writeSymbolElement(doSection);
        tokenizer.advance();

        // 8. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.SEMICOLON});
        writeSymbolElement(doSection);
    }

    @Override
    public void compileLet() {
        // 1. adding LET section
        Element letSection = document.createElement(NonTerminalTag.LET_STATEMENT.toString());
        this.parentElement.appendChild(letSection);

        // 2. adding keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.LET});
        writeKeywordElement(letSection);
        tokenizer.advance();

        // 3. adding identifier element
        verifyIdentifierOrThrowError();
        writeIdentifierElement(letSection);
        tokenizer.advance();

        // 4. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.EQUAL});
        writeSymbolElement(letSection);
        tokenizer.advance();

        // 5. adding expression section
        verifyIdentifierOrThrowError();
        Element parentCopy = this.parentElement;
        this.parentElement = letSection;
        this.compileExpression();
        this.parentElement = parentCopy;
        tokenizer.advance();

        // 6. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.SEMICOLON});
        writeSymbolElement(letSection);
    }

    @Override
    public void compileWhile() {

    }

    @Override
    public void compileReturn() {
        // 1. adding return section
        Element returnSection = document.createElement(NonTerminalTag.RETURN_STATEMENT.toString());
        this.parentElement.appendChild(returnSection);

        // 2. adding keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.RETURN});
        writeKeywordElement(returnSection);
        tokenizer.advance();

        // 3. adding expression section
        TokenType tokenType = TokenType.valueOf(tokenizer.tokenType());
        if (tokenType == TokenType.IDENTIFIER) {
            verifyIdentifierOrThrowError();
            Element parentCopy = this.parentElement;
            this.parentElement = returnSection;
            this.compileExpression();
            this.parentElement = parentCopy;
            tokenizer.advance();
        }

        // 4. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.SEMICOLON});
        writeSymbolElement(returnSection);
    }

    @Override
    public void compileIf() {
        // 1. adding if section
        Element ifSection = document.createElement(NonTerminalTag.IF_STATEMENT.toString());
        this.parentElement.appendChild(ifSection);

        // 2. adding keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.IF});
        writeKeywordElement(ifSection);
        tokenizer.advance();

        // 3. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START});
        writeSymbolElement(ifSection);
        tokenizer.advance();

        // 4. adding expression section
        verifyIdentifierOrThrowError();
        Element parentCopy = this.parentElement;
        this.parentElement = ifSection;
        this.compileExpression();
        this.parentElement = parentCopy;
        tokenizer.advance();

        // 5. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
        writeSymbolElement(ifSection);
        tokenizer.advance();

        // 6. adding if section body
        this.ifElseBody(ifSection);

        // 7. verifying existence of else block
        TokenType tokenType = TokenType.valueOf(tokenizer.tokenType());
        if (tokenType != TokenType.KEYWORD) {
            return;
        }
        KeyWord keyWord = KeyWord.fromValue(tokenizer.keyWord());
        if (keyWord != KeyWord.ELSE) {
            return;
        }

        // 8. adding keyword element
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.ELSE});
        writeKeywordElement(ifSection);
        tokenizer.advance();

        // 9. adding else section body
        this.ifElseBody(ifSection);
    }

    @Override
    public void compileExpression() {
        // 1. adding expression section
        Element expressionSection = document.createElement(NonTerminalTag.EXPRESSION.toString());
        this.parentElement.appendChild(expressionSection);

        // 2. adding term section
        Element parentCopy = this.parentElement;
        this.parentElement = expressionSection;
        this.compileTerm();
        this.parentElement = parentCopy;
    }

    @Override
    public void compileTerm() {
        // 1. adding term section
        Element termSection = document.createElement(NonTerminalTag.TERM.toString());
        this.parentElement.appendChild(termSection);

        // 2. branching between identifier or symbol element
        TokenType tokenType = TokenType.valueOf(tokenizer.tokenType());
        // 2A. adding identifier element
        if (tokenType == TokenType.IDENTIFIER) {
            verifyIdentifierOrThrowError();
            writeIdentifierElement(termSection);
        }
        // 2B. adding keyword element
        else if (tokenType == TokenType.KEYWORD) {
            verifyKeywordOrThrowError(new KeyWord[]{KeyWord.THIS});
            writeKeywordElement(termSection);
        } else {
            throw new InvalidParameterException("Invalid token type. Expected IDENTIFIER or KEYWORD. " +
                    "Got token type: " + tokenizer.tokenType());
        }
    }

    @Override
    public void compileExpressionList() {
        // 1. adding expression list section
        Element expressionListSection = document.createElement(NonTerminalTag.EXPRESSION_LIST.toString());
        this.parentElement.appendChild(expressionListSection);
        boolean hasExpressions = false;

        // 2. adding expression list elements
        while (tokenizer.hasMoreTokens()) {
            TokenType tokenType = TokenType.valueOf(tokenizer.tokenType());

            switch (tokenType) {
                // 2A. adding identifier element
                case IDENTIFIER: {
                    verifyIdentifierOrThrowError();
                    Element parentCopy = this.parentElement;
                    this.parentElement = expressionListSection;
                    this.compileExpression();
                    this.parentElement = parentCopy;
                }
                break;

                // 2B. adding symbol element
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.COMMA, Symbol.PARENTHESES_END});
                    Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
                    if (symbol == Symbol.COMMA) {
                        writeSymbolElement(expressionListSection);
                    }
                    // 2C. expressions list has ended. Traverse back to caller!!
                    else {
                        if (!hasExpressions) {
                            expressionListSection.setTextContent("\n");
                        }
                        return;
                    }
                }
                break;

                // 2C. adding keyword element
                case KEYWORD: {
                    verifyKeywordOrThrowError(new KeyWord[]{KeyWord.THIS});
                    Element parentCopy = this.parentElement;
                    this.parentElement = expressionListSection;
                    this.compileExpression();
                    this.parentElement = parentCopy;
                }
                break;

                default: {
                    throw new InvalidParameterException("Invalid token type. Expected IDENTIFIER, SYMBOL or KEYWORD. " +
                            "Got token type: " + tokenizer.tokenType());
                }
            }

            hasExpressions = true;
            tokenizer.advance();
        }
    }

    private void compileSubroutineBody() {
        // 1. adding subroutine body section
        Element subroutineBodySection = document.createElement(NonTerminalTag.SUBROUTINE_BODY.toString());
        this.parentElement.appendChild(subroutineBodySection);

        // 2. adding symbol (body start) element
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_START});
        writeSymbolElement(subroutineBodySection);
        tokenizer.advance();

        // 3. branching to subroutine variables declaration
        TokenType tokenType = TokenType.valueOf(tokenizer.tokenType());
        if (tokenType == TokenType.KEYWORD) {
            KeyWord keyWord = KeyWord.fromValue(tokenizer.keyWord());
            if (keyWord == KeyWord.VAR) {
                // 3A. adding variables declaration
                Element parentCopy = this.parentElement;
                this.parentElement = subroutineBodySection;
                this.compileVarDec();
                this.parentElement = parentCopy;
                tokenizer.advance();
            }
        }

        // 4. adding subroutine body statements section
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.LET, KeyWord.DO, KeyWord.IF, KeyWord.WHILE});
        Element parentCopy = this.parentElement;
        this.parentElement = subroutineBodySection;
        this.compileStatements();
        this.parentElement = parentCopy;

        // 5. add symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});
        writeSymbolElement(subroutineBodySection);
    }

    private void ifElseBody(Element ifSection) {
        // 1. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_START});
        writeSymbolElement(ifSection);
        tokenizer.advance();

        // 2. adding statements section
        Element parentCopy = this.parentElement;
        this.parentElement = ifSection;
        this.compileStatements();
        this.parentElement = parentCopy;

        // 3. adding symbol element
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});
        writeSymbolElement(ifSection);
        tokenizer.advance();
    }

    private void verifyKeywordOrThrowError(KeyWord[] expectedKeywords) {
        if (!tokenizer.hasMoreTokens()) {
            throw new InvalidParameterException("Invalid end of tokens. Expected KEYWORD.");
        }

        boolean isKeywordToken = true;
        boolean isExpectedKeyword = false;
        StringBuilder expectedKeywordsString = new StringBuilder();

        if (TokenType.valueOf(tokenizer.tokenType()) != TokenType.KEYWORD) {
            isKeywordToken = false;
        }
        if (isKeywordToken) {
            KeyWord keyWord = KeyWord.fromValue(tokenizer.keyWord());
            for (int a = 0; a < expectedKeywords.length; a++) {
                expectedKeywordsString.append(expectedKeywords[a].toString());
                if (!isExpectedKeyword) {
                    isExpectedKeyword = expectedKeywords[a] == keyWord;
                }
                if (a + 1 < expectedKeywords.length) {
                    expectedKeywordsString.append(", ");
                }
            }
        }

        if (!isKeywordToken || !isExpectedKeyword) {
            String msg = String.format("Invalid KEYWORD. Expected %s KEYWORD value. Got %s value: %s",
                    expectedKeywordsString, tokenizer.tokenType(), getCurrentTokenValue());
            throw new InvalidParameterException(msg);
        }
    }

    private void verifyIdentifierOrThrowError() {
        if (!tokenizer.hasMoreTokens()) {
            throw new InvalidParameterException("Invalid end of tokens. Expected IDENTIFIER.");
        }

        if (TokenType.valueOf(tokenizer.tokenType()) != TokenType.IDENTIFIER) {
            String msg = String.format("Invalid IDENTIFIER. Expected an IDENTIFIER value. Got %s value: %s",
                    tokenizer.tokenType(), getCurrentTokenValue());
            throw new InvalidParameterException(msg);
        }
    }

    private void verifySymbolOrThrowError(Symbol[] expectedSymbols) {
        if (!tokenizer.hasMoreTokens()) {
            throw new InvalidParameterException("Invalid end of tokens. Expected SYMBOL.");
        }

        boolean isSymbolToken = true;
        boolean isExpectedSymbol = false;
        StringBuilder expectedSymbolsString = new StringBuilder();

        if (TokenType.valueOf(tokenizer.tokenType()) != TokenType.SYMBOL) {
            isSymbolToken = false;
        }

        if (isSymbolToken) {
            Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
            for (int a = 0; a < expectedSymbols.length; a++) {
                expectedSymbolsString.append(expectedSymbols[a].toString());
                if (!isExpectedSymbol) {
                    isExpectedSymbol = expectedSymbols[a] == symbol;
                }
                if (a + 1 < expectedSymbols.length) {
                    expectedSymbolsString.append(", ");
                }
            }
        }

        if (!isSymbolToken || !isExpectedSymbol) {
            String msg = String.format("Invalid SYMBOL. Expected %s SYMBOL value. Got %s value: %s",
                    expectedSymbolsString, tokenizer.tokenType(), getCurrentTokenValue());
            throw new InvalidParameterException(msg);
        }
    }

    private String getCurrentTokenValue() {
        TokenType type = TokenType.valueOf(tokenizer.tokenType());

        return switch (type) {
            case KEYWORD -> KeyWord.fromValue(tokenizer.keyWord()).toString();
            case SYMBOL -> Symbol.fromValue(String.valueOf(tokenizer.symbol())).toString();
            case IDENTIFIER -> tokenizer.identifier();
            case INT_CONST -> String.valueOf(tokenizer.intVal());
            case STRING_CONST -> tokenizer.stringVal();
        };
    }

    private boolean isClassVarDecKeyword(String keyword) {
        KeyWord keyWord = KeyWord.fromValue(keyword);

        return keyWord == KeyWord.FIELD || keyWord == KeyWord.STATIC;
    }

    private boolean isSubroutineType(String value) {
        KeyWord keyWord = KeyWord.fromValue(value);
        return keyWord == KeyWord.CONSTRUCTOR || keyWord == KeyWord.FUNCTION || keyWord == KeyWord.METHOD;
    }

    private void writeKeywordElement(Element parentElement) {
        Element childElement = document.createElement(TerminalTag.KEYWORD.toString());
        childElement.setTextContent(" " + tokenizer.keyWord() + " ");
        parentElement.appendChild(childElement);
    }

    private void writeIdentifierElement(Element parentElement) {
        Element childElement = document.createElement(TerminalTag.IDENTIFIER.toString());
        childElement.setTextContent(" " + tokenizer.identifier() + " ");
        parentElement.appendChild(childElement);
    }

    private void writeSymbolElement(Element parentElement) {
        Element childElement = document.createElement(TerminalTag.SYMBOL.toString());
        childElement.setTextContent(" " + tokenizer.symbol() + " ");
        parentElement.appendChild(childElement);
    }

    private void writeToFile() throws FileNotFoundException, TransformerException {
        OutputStream outputStream = new FileOutputStream(outputFilePath);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(outputStream);
        transformer.transform(domSource, streamResult);
    }
}
