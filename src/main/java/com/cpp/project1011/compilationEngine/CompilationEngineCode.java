package com.cpp.project1011.compilationEngine;

import com.cpp.project1011.jackTokenizer.JackTokenizer;
import com.cpp.project1011.jackTokenizer.KeyWord;
import com.cpp.project1011.jackTokenizer.Symbol;
import com.cpp.project1011.jackTokenizer.TokenType;
import com.cpp.project1011.symbolTable.Segment;
import com.cpp.project1011.symbolTable.SymbolKind;
import com.cpp.project1011.symbolTable.SymbolTable;
import com.cpp.project1011.symbolTable.SymbolTableImpl;
import com.cpp.project1011.vmWriter.Command;
import com.cpp.project1011.vmWriter.VMWriter;
import com.cpp.project1011.vmWriter.VMWriterImpl;

import java.io.IOException;
import java.security.InvalidParameterException;

public class CompilationEngineCode implements CompilationEngine {
    private final JackTokenizer tokenizer;
    private final SymbolTable symbolTable;
    private final VMWriter vmWriter;

    private String className;
    private String currentSubroutineType;
    private String currentSubroutineName;
    private int labelCounter;

    public CompilationEngineCode(String outputFilePath, JackTokenizer tokenizer) throws IOException {
        this.tokenizer = tokenizer;
        this.symbolTable = new SymbolTableImpl();
        this.vmWriter = new VMWriterImpl(outputFilePath);
        this.labelCounter = 0;
    }

    @Override
    public void compileClass() {
        // 1. 'class' keyword
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.CLASS});
        tokenizer.advance();

        // 2. class name
        verifyIdentifierOrThrowError();
        className = tokenizer.identifier();
        tokenizer.advance();

        // 3. class start symbol '{'
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_START});
        tokenizer.advance();

        // 4. class variables or subroutine variables declaration
        while (tokenizer.hasMoreTokens()) {
            TokenType type = TokenType.valueOf(tokenizer.tokenType());

            switch (type) {
                // 4A. class or subroutine start
                case KEYWORD: {
                    verifyKeywordOrThrowError(new KeyWord[]{KeyWord.FIELD, KeyWord.STATIC,
                            KeyWord.CONSTRUCTOR, KeyWord.METHOD, KeyWord.FUNCTION});
                    // 4A-A. class start
                    if (isClassVarDecKeyword(tokenizer.keyWord())) {
                        compileClassVarDec();
                    }
                    // 4A-B. subroutine start
                    else if (isSubroutineType(tokenizer.keyWord())) {
                        compileSubroutine();
                    }
                }
                break;

                // 4B. class or subroutine end
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});
                }
                break;

                default:
                    throw new InvalidParameterException("Invalid token type. Expected KEYWORD OR SYMBOL. Got token type: "
                            + tokenizer.tokenType());
            }

            tokenizer.advance();
        }

        vmWriter.close();
    }

    @Override
    public void compileClassVarDec() {
        // 1. class variables types
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.FIELD, KeyWord.STATIC});
        String kind = tokenizer.keyWord().toUpperCase();
        tokenizer.advance();

        // 2. variable type
        String type = readCurrentType();
        tokenizer.advance();

        // 3. variable names separated by commas ',' until ';'
        while (tokenizer.hasMoreTokens()) {
            TokenType tokenType = TokenType.valueOf(tokenizer.tokenType());

            switch (tokenType) {
                // 3A. adding variable declaration to symbol table
                case IDENTIFIER: {
                    symbolTable.define(tokenizer.identifier(), type, kind);
                }
                break;

                // 3B. moving to next variable or ending class variables declaration
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.COMMA, Symbol.SEMICOLON});
                    // 3B-A. ending class variables declaration
                    if (Symbol.fromValue(String.valueOf(tokenizer.symbol())) == Symbol.SEMICOLON) {
                        return;
                    }
                }
                break;

                default:
                    throw new InvalidParameterException("Invalid token type. Expected IDENTIFIER or SYMBOL. Got token type: "
                            + tokenizer.tokenType());
            }

            tokenizer.advance();
        }
    }

    @Override
    public void compileSubroutine() {
        // 1. declaring constructor, method, or function (subroutine)
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.CONSTRUCTOR, KeyWord.METHOD, KeyWord.FUNCTION});
        currentSubroutineType = tokenizer.keyWord();
        tokenizer.advance();

        // 2. skip return type (keyword or class identifier)
        tokenizer.advance();

        // 3. saving subroutine, constructor, or function name
        verifyIdentifierOrThrowError();
        currentSubroutineName = tokenizer.identifier();
        tokenizer.advance();

        // 4. resetting subroutine symbol table
        symbolTable.startSubroutine();

        // 5. adding THIS argument in case of a method (subroutine)
        if (currentSubroutineType.equals(KeyWord.METHOD.toString())) {
            symbolTable.define(KeyWord.THIS.toString(), className, SymbolKind.ARG.toString());
        }

        // 6. subroutine parameters start symbol '('
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START});
        tokenizer.advance();

        // 7. compiling subroutine parameters list
        compileParameterList();

        // 8. subroutine parameters end symbol ')'
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
        tokenizer.advance();

        // 9. compiling subroutine body
        compileSubroutineBody();
    }

    @Override
    public void compileParameterList() {
        // 1. verifying if subroutine has parameters
        if (TokenType.valueOf(tokenizer.tokenType()) == TokenType.SYMBOL
                && Symbol.fromValue(String.valueOf(tokenizer.symbol())) == Symbol.PARENTHESES_END) {
            return;
        }

        // 2. adding first parameter
        String type = readCurrentType();
        tokenizer.advance();
        verifyIdentifierOrThrowError();
        symbolTable.define(tokenizer.identifier(), type, SymbolKind.ARG.toString());
        tokenizer.advance();

        // 3. adding remaining parameters
        while (tokenizer.hasMoreTokens()) {
            // 3A. verifying if subroutine have more parameters
            verifySymbolOrThrowError(new Symbol[]{Symbol.COMMA, Symbol.PARENTHESES_END});
            if (Symbol.fromValue(String.valueOf(tokenizer.symbol())) == Symbol.PARENTHESES_END) {
                return;
            }

            // 3B. skipping symbol ','
            tokenizer.advance(); // skip ','

            // 3C. adding parameter
            type = readCurrentType();
            tokenizer.advance();
            verifyIdentifierOrThrowError();
            symbolTable.define(tokenizer.identifier(), type, SymbolKind.ARG.toString());
            tokenizer.advance();
        }
    }

    @Override
    public void compileVarDec() {
        // 1. verifying if variable is declared
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.VAR});
        tokenizer.advance();

        // 2. storing current variable type
        String type = readCurrentType();
        tokenizer.advance();

        // 3. compiling variables separated by comma ','
        while (tokenizer.hasMoreTokens()) {
            TokenType tokenType = TokenType.valueOf(tokenizer.tokenType());

            switch (tokenType) {
                // 3A. adding variable to the symbol table
                case IDENTIFIER: {
                    symbolTable.define(tokenizer.identifier(), type, SymbolKind.VAR.toString());
                }
                break;

                // 3B. verifying if there are more variables declared
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.COMMA, Symbol.SEMICOLON});
                    if (Symbol.fromValue(String.valueOf(tokenizer.symbol())) == Symbol.SEMICOLON) {
                        return;
                    }
                }
                break;

                default:
                    throw new InvalidParameterException("Invalid token type. Expected IDENTIFIER or SYMBOL. " +
                            "Got token type: " + tokenizer.tokenType());
            }

            tokenizer.advance();
        }
    }

    @Override
    public void compileStatements() {
        // 1. comping statements
        while (tokenizer.hasMoreTokens()) {
            TokenType type = TokenType.valueOf(tokenizer.tokenType());

            switch (type) {
                // 1A. verifying statement types
                case KEYWORD: {
                    KeyWord keyWord = KeyWord.fromValue(tokenizer.keyWord());
                    switch (keyWord) {
                        // 1A-A. compiling let statement
                        case LET -> compileLet();
                        // 1A-B. compiling do statement
                        case DO -> compileDo();
                        // 1A-C. compiling return statement
                        case RETURN -> compileReturn();
                        // 1A-D. compiling while statement
                        case WHILE -> compileWhile();
                        // 1A-E. compiling if & else statement
                        case IF -> {
                            compileIf();
                            // 1A-F. already advanced past closing symbol '}'
                            continue;
                        }
                        default ->
                                throw new InvalidParameterException("Invalid token type. Expected LET, DO, RETURN, or IF. " +
                                        "Got token type: " + tokenizer.tokenType() + " with value: " + tokenizer.keyWord());
                    }
                }
                break;

                // 1B. verifying and ending statement compilation if symbol '}' has encountered
                case SYMBOL: {
                    verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});
                    return;
                }

                default:
                    throw new InvalidParameterException("Invalid token type. Expected KEYWORD or SYMBOL. Got token type: " +
                            tokenizer.tokenType());
            }

            tokenizer.advance();
        }
    }

    @Override
    public void compileDo() {
        // 1. verifying if it's a do statement
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.DO});
        tokenizer.advance();

        // 2. storing the identifier of do statement
        verifyIdentifierOrThrowError();
        String name = tokenizer.identifier();
        tokenizer.advance();

        // 3. verifying if parameters has started or if a class method is called
        verifySymbolOrThrowError(new Symbol[]{Symbol.PERIOD, Symbol.PARENTHESES_START});
        Symbol separator = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
        tokenizer.advance();

        // 4. if a class method is called
        if (separator == Symbol.PERIOD) {
            // 4A. storing method name
            verifyIdentifierOrThrowError();
            String subroutineName = tokenizer.identifier();
            tokenizer.advance();

            // 4B. verifying arguments and called kind
            String callerKind = symbolTable.kindOf(name);
            int nExtraArgs = 0;
            String callTarget;

            if (!callerKind.equals(SymbolKind.NONE.toString())) {
                // 4C. name is an object variable: push it as 'this' for the method
                pushIdentifier(name);
                nExtraArgs = 1;
                callTarget = symbolTable.typeOf(name) + "." + subroutineName;
            } else {
                // 4D. name is a class name: static function call
                callTarget = name + "." + subroutineName;
            }

            verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START});
            tokenizer.advance();
            int nArgs = compileExpressionListImpl();
            vmWriter.writeCall(callTarget, nArgs + nExtraArgs);
        } else {
            // 4E. implicit 'this' call: subroutineName(args) within same class
            vmWriter.writePush(Segment.POINTER.toString(), 0);
            int nArgs = compileExpressionListImpl();
            vmWriter.writeCall(className + "." + name, nArgs + 1);
        }

        // 5. parameters have ended with symbol ')'
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
        tokenizer.advance();

        // 6. discard return value from 'do' call
        vmWriter.writePop(Segment.TEMP.toString(), 0);

        // 7. verifying if the do statement has properly ended
        verifySymbolOrThrowError(new Symbol[]{Symbol.SEMICOLON});
    }

    @Override
    public void compileLet() {
        // 1. verifying if it is a let statement
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.LET});
        tokenizer.advance();

        // 2. saving variable name
        verifyIdentifierOrThrowError();
        String varName = tokenizer.identifier();
        tokenizer.advance();

        boolean isArrayAccess = false;

        // 3. checking for optional array index
        if (TokenType.valueOf(tokenizer.tokenType()) == TokenType.SYMBOL
                && Symbol.fromValue(String.valueOf(tokenizer.symbol())) == Symbol.SQUARE_START) {
            isArrayAccess = true;
            // 3A. skipping array access starting symbol '['
            tokenizer.advance();

            // 3B. compiling base address
            pushIdentifier(varName);
            // 3C. compiling array index
            compileExpression();
            // 3D. completing array access (base + index)
            vmWriter.writeArithmetic(Command.add.toString());

            // 3E. verifying array access has finished
            verifySymbolOrThrowError(new Symbol[]{Symbol.SQUARE_END});
            tokenizer.advance();
        }

        // 4. verifying if assignment operator exists '='
        verifySymbolOrThrowError(new Symbol[]{Symbol.EQUAL});
        tokenizer.advance();

        // 5. right-hand side expression
        compileExpression();

        // 6. assign result
        if (isArrayAccess) {
            // target address is on stack below the value: store via THAT
            vmWriter.writePop(Segment.TEMP.toString(), 0);    // save value
            vmWriter.writePop(Segment.POINTER.toString(), 1); // set THAT = target address
            vmWriter.writePush(Segment.TEMP.toString(), 0);   // restore value
            vmWriter.writePop(Segment.THAT.toString(), 0);    // store value at THAT[0]
        } else {
            popIdentifier(varName);
        }

        // 7. verifying if the let statement has properly ended
        verifySymbolOrThrowError(new Symbol[]{Symbol.SEMICOLON});
    }

    @Override
    public void compileWhile() {
        // 1. creating new while statement start and ending labels
        int localLabel = labelCounter++;
        String whileExpLabel = Label.WHILE_EXP.toString() + localLabel;
        String whileEndLabel = Label.WHILE_END.toString() + localLabel;

        vmWriter.writeLabel(whileExpLabel);

        // 2. verifying if it's a while statement
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.WHILE});
        tokenizer.advance();

        // 3. verifying while statement expressions have started '('
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START});
        tokenizer.advance();

        // 4. condition: negate so that if-goto exits the loop
        compileExpression();
        vmWriter.writeArithmetic(Command.not.toString());
        vmWriter.writeIf(whileEndLabel);

        // 5. verifying while statement expressions have ended ')'
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
        tokenizer.advance();

        // 6. while statement body has started '{'
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_START});
        tokenizer.advance();

        // 7. loop body
        compileStatements();

        vmWriter.writeGoto(whileExpLabel);

        // 8. verifying if the while statement has properly ended
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});

        vmWriter.writeLabel(whileEndLabel);
    }

    @Override
    public void compileReturn() {
        // 1. verifying if return statement has started
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.RETURN});
        tokenizer.advance();

        // 2. optional return expression
        boolean isSemicolon = TokenType.valueOf(tokenizer.tokenType()) == TokenType.SYMBOL
                && Symbol.fromValue(String.valueOf(tokenizer.symbol())) == Symbol.SEMICOLON;

        // 2A. void subroutines push 0 as dummy
        if (isSemicolon) {
            vmWriter.writePush(Segment.CONST.toString(), 0);
        }
        // 2B. return statement expressions
        else {
            compileExpression();
        }

        vmWriter.writeReturn();

        // 3. verifying if the return statement has properly ended
        verifySymbolOrThrowError(new Symbol[]{Symbol.SEMICOLON});
    }

    @Override
    public void compileIf() {
        // 1. creating new if statement start and ending labels
        int localLabel = labelCounter++;
        String ifFalseLabel = Label.IF_FALSE.toString() + localLabel;
        String ifEndLabel = Label.IF_END.toString() + localLabel;

        // 2. verifying if it's an if statement
        verifyKeywordOrThrowError(new KeyWord[]{KeyWord.IF});
        tokenizer.advance();

        // 3. verifying if statement expressions have started '('
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START});
        tokenizer.advance();

        // 4. condition: negate and branch to false
        compileExpression();
        vmWriter.writeArithmetic(Command.not.toString());
        vmWriter.writeIf(ifFalseLabel);

        // 5. verifying if statement expressions have ended ')'
        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
        tokenizer.advance();

        // 6. compiling if statement body
        compileIfElseBody();

        // 7. check for optional 'else' branch
        if (TokenType.valueOf(tokenizer.tokenType()) == TokenType.KEYWORD
                && KeyWord.fromValue(tokenizer.keyWord()) == KeyWord.ELSE) {
            vmWriter.writeGoto(ifEndLabel);
            vmWriter.writeLabel(ifFalseLabel);

            // 7A. skip 'else' identifier
            tokenizer.advance();

            // 7B. compiling else statement body
            compileIfElseBody();

            vmWriter.writeLabel(ifEndLabel);
        } else {
            vmWriter.writeLabel(ifFalseLabel);
        }
    }

    @Override
    public void compileExpression() {
        // 1. compiling first term
        compileTerm();

        // 2. compiling the rest of the terms
        while (tokenizer.hasMoreTokens() && isOperatorSymbol()) {
            String op = String.valueOf(tokenizer.symbol());
            tokenizer.advance();
            compileTerm();
            writeOperator(op);
        }
    }

    @Override
    public void compileTerm() {
        TokenType tokenType = TokenType.valueOf(tokenizer.tokenType());

        // 1. branching between different kinds of terms
        switch (tokenType) {
            // integer constant: push directly
            case INT_CONST: {
                vmWriter.writePush(Segment.CONST.toString(), tokenizer.intVal());
                tokenizer.advance();
            }
            break;

            // string constant: construct via OS String class
            case STRING_CONST: {
                String str = tokenizer.stringVal();
                vmWriter.writePush(Segment.CONST.toString(), str.length());
                vmWriter.writeCall(Label.STRING_NEW.toString(), 1);
                for (char c : str.toCharArray()) {
                    vmWriter.writePush(Segment.CONST.toString(), c);
                    vmWriter.writeCall(Label.STRING_APPEND_CHAR.toString(), 2);
                }
                tokenizer.advance();
            }
            break;

            // keyword constant: true, false, null, this
            case KEYWORD: {
                verifyKeywordOrThrowError(new KeyWord[]{KeyWord.TRUE, KeyWord.FALSE, KeyWord.NULL, KeyWord.THIS});
                KeyWord kw = KeyWord.fromValue(tokenizer.keyWord());
                switch (kw) {
                    case TRUE -> {
                        vmWriter.writePush(Segment.CONST.toString(), 0);
                        vmWriter.writeArithmetic(Command.not.toString()); // ~0 == -1 (true)
                    }
                    case FALSE, NULL -> vmWriter.writePush(Segment.CONST.toString(), 0);
                    case THIS -> vmWriter.writePush(Segment.POINTER.toString(), 0);
                }
                tokenizer.advance();
            }
            break;

            // identifier: variable, array access, or subroutine call
            case IDENTIFIER: {
                String name = tokenizer.identifier();
                tokenizer.advance();

                if (TokenType.valueOf(tokenizer.tokenType()) == TokenType.SYMBOL) {
                    Symbol peekSymbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));

                    if (peekSymbol == Symbol.SQUARE_START) {
                        // array access: varName[expression]
                        tokenizer.advance(); // skip '['
                        pushIdentifier(name);
                        compileExpression();
                        vmWriter.writeArithmetic(Command.add.toString());
                        verifySymbolOrThrowError(new Symbol[]{Symbol.SQUARE_END});
                        tokenizer.advance();
                        vmWriter.writePop(Segment.POINTER.toString(), 1);
                        vmWriter.writePush(Segment.THAT.toString(), 0);

                    } else if (peekSymbol == Symbol.PARENTHESES_START) {
                        // implicit method call on 'this': subroutineName(args)
                        tokenizer.advance(); // skip '('
                        vmWriter.writePush(Segment.POINTER.toString(), 0);
                        int nArgs = compileExpressionListImpl();
                        vmWriter.writeCall(className + "." + name, nArgs + 1);
                        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
                        tokenizer.advance();

                    } else if (peekSymbol == Symbol.PERIOD) {
                        // qualified call: name.subroutineName(args)
                        tokenizer.advance(); // skip '.'
                        verifyIdentifierOrThrowError();
                        String subroutineName = tokenizer.identifier();
                        tokenizer.advance();

                        String callerKind = symbolTable.kindOf(name);
                        int nExtraArgs = 0;
                        String callTarget;

                        if (!callerKind.equals(SymbolKind.NONE.toString())) {
                            pushIdentifier(name);
                            nExtraArgs = 1;
                            callTarget = symbolTable.typeOf(name) + "." + subroutineName;
                        } else {
                            callTarget = name + "." + subroutineName;
                        }

                        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_START});
                        tokenizer.advance();
                        int nArgs = compileExpressionListImpl();
                        vmWriter.writeCall(callTarget, nArgs + nExtraArgs);
                        verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
                        tokenizer.advance();

                    } else {
                        // plain variable reference
                        pushIdentifier(name);
                    }
                } else {
                    pushIdentifier(name);
                }
            }
            break;

            // symbol: grouped expression '(expr)' or unary op '-' / '~'
            case SYMBOL: {
                Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));

                if (symbol == Symbol.PARENTHESES_START) {
                    tokenizer.advance(); // skip '('
                    compileExpression();
                    verifySymbolOrThrowError(new Symbol[]{Symbol.PARENTHESES_END});
                    tokenizer.advance();
                } else if (symbol == Symbol.MINUS) {
                    tokenizer.advance();
                    compileTerm();
                    vmWriter.writeArithmetic(Command.neg.toString());
                } else if (symbol == Symbol.TILDE) {
                    tokenizer.advance();
                    compileTerm();
                    vmWriter.writeArithmetic(Command.not.toString());
                } else {
                    throw new InvalidParameterException("Invalid SYMBOL in term. Expected '(', '-', or '~'. " +
                            "Got: " + tokenizer.symbol());
                }
            }
            break;

            default:
                throw new InvalidParameterException("Invalid token type in term. Expected INT_CONST, STRING_CONST, " +
                        "KEYWORD, IDENTIFIER, or SYMBOL. Got: " + tokenizer.tokenType());
        }
    }

    @Override
    public void compileExpressionList() {
        compileExpressionListImpl();
    }

    private void compileSubroutineBody() {
        // 1. '{'
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_START});
        tokenizer.advance();

        // 2. varDec* — populates symbol table; no VM output yet
        while (TokenType.valueOf(tokenizer.tokenType()) == TokenType.KEYWORD
                && KeyWord.fromValue(tokenizer.keyWord()) == KeyWord.VAR) {
            compileVarDec();
            tokenizer.advance(); // advance past ';'
        }

        // 3. now that locals are counted, emit function declaration
        int nLocals = symbolTable.varCount(SymbolKind.VAR.toString());
        vmWriter.writeFunction(className + "." + currentSubroutineName, nLocals);

        // 4. subroutine-type-specific preamble
        if (currentSubroutineType.equals(KeyWord.CONSTRUCTOR.toString())) {
            int nFields = symbolTable.varCount(SymbolKind.FIELD.toString());
            vmWriter.writePush(Segment.CONST.toString(), nFields);
            vmWriter.writeCall(Label.MEMORY_ALLOC.toString(), 1);
            vmWriter.writePop(Segment.POINTER.toString(), 0); // anchor THIS to new object
        } else if (currentSubroutineType.equals(KeyWord.METHOD.toString())) {
            vmWriter.writePush(Segment.ARG.toString(), 0);    // arg 0 is the hidden 'this' ref
            vmWriter.writePop(Segment.POINTER.toString(), 0); // anchor THIS
        }

        // 5. statements
        compileStatements();

        // 6. '}' — leave on '}' for compileClass loop to advance
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});
    }

    private void compileIfElseBody() {
        // '{' statements '}'  — used by both if and else branches
        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_START});
        tokenizer.advance();

        compileStatements();

        verifySymbolOrThrowError(new Symbol[]{Symbol.CURLY_END});
        tokenizer.advance(); // advance past '}' so caller can peek next token
    }

    // Returns expression count; always call this internally to avoid field aliasing.
    private int compileExpressionListImpl() {
        int count = 0;

        if (TokenType.valueOf(tokenizer.tokenType()) == TokenType.SYMBOL
                && Symbol.fromValue(String.valueOf(tokenizer.symbol())) == Symbol.PARENTHESES_END) {
            return 0;
        }

        compileExpression();
        count++;

        while (tokenizer.hasMoreTokens()) {
            verifySymbolOrThrowError(new Symbol[]{Symbol.COMMA, Symbol.PARENTHESES_END});
            if (Symbol.fromValue(String.valueOf(tokenizer.symbol())) == Symbol.PARENTHESES_END) {
                return count;
            }
            tokenizer.advance(); // skip ','
            compileExpression();
            count++;
        }

        return count;
    }

    private void pushIdentifier(String name) {
        vmWriter.writePush(kindToSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name));
    }

    private void popIdentifier(String name) {
        vmWriter.writePop(kindToSegment(symbolTable.kindOf(name)), symbolTable.indexOf(name));
    }

    private String kindToSegment(String kind) {
        SymbolKind symbolKind = SymbolKind.valueOf(kind);
        return switch (symbolKind) {
            case STATIC -> Segment.STATIC.toString();
            case FIELD -> Segment.THIS.toString();
            case ARG -> Segment.ARG.toString();
            case VAR -> Segment.LOCAL.toString();
            default -> throw new IllegalArgumentException("Cannot map symbol kind to segment: " + kind);
        };
    }

    private void writeOperator(String op) {
        Symbol symbol = Symbol.fromValue(op);

        switch (symbol) {
            case Symbol.PLUS -> vmWriter.writeArithmetic(Command.add.toString());
            case Symbol.MINUS -> vmWriter.writeArithmetic(Command.sub.toString());
            case Symbol.STAR -> vmWriter.writeCall(Label.MATH_MULTIPLY.toString(), 2);
            case Symbol.SLASH -> vmWriter.writeCall(Label.MATH_DIVIDE.toString(), 2);
            case Symbol.AMPERSAND -> vmWriter.writeArithmetic(Command.and.toString());
            case Symbol.BAR -> vmWriter.writeArithmetic(Command.or.toString());
            case Symbol.LESS_THAN -> vmWriter.writeArithmetic(Command.lt.toString());
            case Symbol.GREATER_THAN -> vmWriter.writeArithmetic(Command.gt.toString());
            case Symbol.EQUAL -> vmWriter.writeArithmetic(Command.eq.toString());
            default -> throw new InvalidParameterException("Unknown binary operator: " + op);
        }
    }

    private String readCurrentType() {
        TokenType type = TokenType.valueOf(tokenizer.tokenType());
        if (type == TokenType.KEYWORD) {
            return tokenizer.keyWord();
        } else if (type == TokenType.IDENTIFIER) {
            return tokenizer.identifier();
        }
        throw new InvalidParameterException("Expected type KEYWORD or IDENTIFIER. Got: " + tokenizer.tokenType());
    }

    private boolean isOperatorSymbol() {
        if (TokenType.valueOf(tokenizer.tokenType()) != TokenType.SYMBOL) {
            return false;
        }
        Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
        return symbol == Symbol.PLUS || symbol == Symbol.MINUS || symbol == Symbol.STAR
                || symbol == Symbol.SLASH || symbol == Symbol.AMPERSAND || symbol == Symbol.BAR
                || symbol == Symbol.LESS_THAN || symbol == Symbol.GREATER_THAN || symbol == Symbol.EQUAL;
    }

    private boolean isClassVarDecKeyword(String keyword) {
        KeyWord keyWord = KeyWord.fromValue(keyword);
        return keyWord == KeyWord.FIELD || keyWord == KeyWord.STATIC;
    }

    private boolean isSubroutineType(String value) {
        KeyWord keyWord = KeyWord.fromValue(value);
        return keyWord == KeyWord.CONSTRUCTOR || keyWord == KeyWord.FUNCTION || keyWord == KeyWord.METHOD;
    }

    private void verifyKeywordOrThrowError(KeyWord[] expectedKeywords) {
        if (!tokenizer.hasMoreTokens()) {
            throw new InvalidParameterException("Unexpected end of tokens. Expected KEYWORD.");
        }

        boolean isKeywordToken = TokenType.valueOf(tokenizer.tokenType()) == TokenType.KEYWORD;
        boolean isExpectedKeyword = false;
        StringBuilder expectedKeywordsString = new StringBuilder();

        if (isKeywordToken) {
            KeyWord keyWord = KeyWord.fromValue(tokenizer.keyWord());
            for (int i = 0; i < expectedKeywords.length; i++) {
                expectedKeywordsString.append(expectedKeywords[i].toString());
                if (!isExpectedKeyword) {
                    isExpectedKeyword = expectedKeywords[i] == keyWord;
                }
                if (i + 1 < expectedKeywords.length) {
                    expectedKeywordsString.append(", ");
                }
            }
        }

        if (!isKeywordToken || !isExpectedKeyword) {
            String msg = String.format("Invalid KEYWORD. Expected %s. Got %s: %s",
                    expectedKeywordsString, tokenizer.tokenType(), getCurrentTokenValue());
            throw new InvalidParameterException(msg);
        }
    }

    private void verifyIdentifierOrThrowError() {
        if (!tokenizer.hasMoreTokens()) {
            throw new InvalidParameterException("Unexpected end of tokens. Expected IDENTIFIER.");
        }
        if (TokenType.valueOf(tokenizer.tokenType()) != TokenType.IDENTIFIER) {
            String msg = String.format("Invalid IDENTIFIER. Got %s: %s",
                    tokenizer.tokenType(), getCurrentTokenValue());
            throw new InvalidParameterException(msg);
        }
    }

    private void verifySymbolOrThrowError(Symbol[] expectedSymbols) {
        if (!tokenizer.hasMoreTokens()) {
            throw new InvalidParameterException("Unexpected end of tokens. Expected SYMBOL.");
        }

        boolean isSymbolToken = TokenType.valueOf(tokenizer.tokenType()) == TokenType.SYMBOL;
        boolean isExpectedSymbol = false;
        StringBuilder expectedSymbolsString = new StringBuilder();

        if (isSymbolToken) {
            Symbol symbol = Symbol.fromValue(String.valueOf(tokenizer.symbol()));
            for (int i = 0; i < expectedSymbols.length; i++) {
                expectedSymbolsString.append(expectedSymbols[i].toString());
                if (!isExpectedSymbol) {
                    isExpectedSymbol = expectedSymbols[i] == symbol;
                }
                if (i + 1 < expectedSymbols.length) {
                    expectedSymbolsString.append(", ");
                }
            }
        }

        if (!isSymbolToken || !isExpectedSymbol) {
            String msg = String.format("Invalid SYMBOL. Expected %s. Got %s: %s",
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
}
