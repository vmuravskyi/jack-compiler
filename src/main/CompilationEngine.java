package main;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class CompilationEngine implements Closeable {

    private final JackTokenizer tokenizer;
    private final BufferedWriter out;
    private int indent = 0;

    private static final Set<Character> OPS = new HashSet<>();

    static {
        for (char c : new char[]{'+', '-', '*', '/', '&', '|', '<', '>', '='}) {
            OPS.add(c);
        }
    }

    public CompilationEngine(JackTokenizer tokenizer, Path outputXml) throws IOException {
        this.tokenizer = tokenizer;
        this.out = Files.newBufferedWriter(outputXml, StandardCharsets.UTF_8);
        // Initialize tokenizer current token
        this.tokenizer.advance();
    }

    // -------- public API (project 10) --------

    public void compileClass() throws IOException {
        openTag("class");

        eatKeyword("class");
        eatIdentifier();      // className
        eatSymbol('{');

        while (isKeyword("static") || isKeyword("field")) {
            compileClassVarDec();
        }

        while (isKeyword("constructor") || isKeyword("function") || isKeyword("method")) {
            compileSubroutine();
        }

        eatSymbol('}');
        closeTag("class");
        out.flush();
    }

    public void compileClassVarDec() throws IOException {
        openTag("classVarDec");

        // 'static' | 'field'
        if (isKeyword("static")) {
            eatKeyword("static");
        } else {
            eatKeyword("field");
        }

        compileType();     // type
        eatIdentifier();   // varName

        while (isSymbol(',')) {
            eatSymbol(',');
            eatIdentifier();
        }

        eatSymbol(';');
        closeTag("classVarDec");
    }

    public void compileSubroutine() throws IOException {
        openTag("subroutineDec");

        // ('constructor'|'function'|'method')
        if (isKeyword("constructor")) {
            eatKeyword("constructor");
        } else if (isKeyword("function")) {
            eatKeyword("function");
        } else {
            eatKeyword("method");
        }

        // ('void'|type)
        if (isKeyword("void")) {
            eatKeyword("void");
        } else {
            compileType();
        }

        eatIdentifier(); // subroutineName
        eatSymbol('(');
        compileParameterList();
        eatSymbol(')');

        compileSubroutineBody();

        closeTag("subroutineDec");
    }

    public void compileParameterList() throws IOException {
        openTag("parameterList");

        // ((type varName) (',' type varName)*)?
        if (!isSymbol(')')) {
            compileType();
            eatIdentifier();
            while (isSymbol(',')) {
                eatSymbol(',');
                compileType();
                eatIdentifier();
            }
        }

        closeTag("parameterList");
    }

    public void compileSubroutineBody() throws IOException {
        openTag("subroutineBody");

        eatSymbol('{');

        while (isKeyword("var")) {
            compileVarDec();
        }

        compileStatements();

        eatSymbol('}');
        closeTag("subroutineBody");
    }

    public void compileVarDec() throws IOException {
        openTag("varDec");

        eatKeyword("var");
        compileType();
        eatIdentifier();

        while (isSymbol(',')) {
            eatSymbol(',');
            eatIdentifier();
        }

        eatSymbol(';');
        closeTag("varDec");
    }

    public void compileStatements() throws IOException {
        openTag("statements");

        while (tokenizer.tokenType() == TokenType.KEYWORD) {
            String kw = tokenizer.keyword();
            if ("let".equals(kw)) {
                compileLet();
            } else if ("if".equals(kw)) {
                compileIf();
            } else if ("while".equals(kw)) {
                compileWhile();
            } else if ("do".equals(kw)) {
                compileDo();
            } else if ("return".equals(kw)) {
                compileReturn();
            } else {
                break;
            }
        }

        closeTag("statements");
    }

    public void compileLet() throws IOException {
        openTag("letStatement");

        eatKeyword("let");
        eatIdentifier(); // varName

        // ('[' expression ']')?
        if (isSymbol('[')) {
            eatSymbol('[');
            compileExpression();
            eatSymbol(']');
        }

        eatSymbol('=');
        compileExpression();
        eatSymbol(';');

        closeTag("letStatement");
    }

    public void compileIf() throws IOException {
        openTag("ifStatement");

        eatKeyword("if");
        eatSymbol('(');
        compileExpression();
        eatSymbol(')');
        eatSymbol('{');
        compileStatements();
        eatSymbol('}');

        // ('else' '{' statements '}')?
        if (isKeyword("else")) {
            eatKeyword("else");
            eatSymbol('{');
            compileStatements();
            eatSymbol('}');
        }

        closeTag("ifStatement");
    }

    public void compileWhile() throws IOException {
        openTag("whileStatement");

        eatKeyword("while");
        eatSymbol('(');
        compileExpression();
        eatSymbol(')');
        eatSymbol('{');
        compileStatements();
        eatSymbol('}');

        closeTag("whileStatement");
    }

    public void compileDo() throws IOException {
        openTag("doStatement");

        eatKeyword("do");
        compileSubroutineCall();  // no <subroutineCall> tag in output
        eatSymbol(';');

        closeTag("doStatement");
    }

    public void compileReturn() throws IOException {
        openTag("returnStatement");

        eatKeyword("return");

        // expression?
        if (!isSymbol(';')) {
            compileExpression();
        }

        eatSymbol(';');
        closeTag("returnStatement");
    }

    public void compileExpression() throws IOException {
        openTag("expression");

        compileTerm();

        while (tokenizer.tokenType() == TokenType.SYMBOL && OPS.contains(tokenizer.symbol())) {
            writeCurrentTokenAndAdvance(); // op
            compileTerm();
        }

        closeTag("expression");
    }

    public void compileTerm() throws IOException {
        openTag("term");

        TokenType tt = tokenizer.tokenType();

        if (tt == TokenType.INT_CONST) {
            writeCurrentTokenAndAdvance();
        } else if (tt == TokenType.STRING_CONST) {
            writeCurrentTokenAndAdvance();
        } else if (tt == TokenType.KEYWORD && isKeywordConstant(tokenizer.keyword())) {
            writeCurrentTokenAndAdvance();
        } else if (tt == TokenType.SYMBOL && isSymbol('(')) {
            eatSymbol('(');
            compileExpression();
            eatSymbol(')');
        } else if (tt == TokenType.SYMBOL && (isSymbol('-') || isSymbol('~'))) {
            // unaryOp term
            writeCurrentTokenAndAdvance();
            compileTerm();
        } else if (tt == TokenType.IDENTIFIER) {
            // varName | varName[expression] | subroutineCall
            writeCurrentTokenAndAdvance(); // identifier

            if (tokenizer.tokenType() == TokenType.SYMBOL && isSymbol('[')) {
                eatSymbol('[');
                compileExpression();
                eatSymbol(']');
            } else if (tokenizer.tokenType() == TokenType.SYMBOL && (isSymbol('(') || isSymbol(
                '.'))) {
                // subroutine call continuation (we already wrote the first identifier)
                if (isSymbol('.')) {
                    eatSymbol('.');
                    eatIdentifier(); // subroutineName
                }
                eatSymbol('(');
                compileExpressionList();
                eatSymbol(')');
            }
            // else simple varName term (done)
        } else {
            throw new IllegalStateException(
                "Unexpected token in term: " + tokenizer.tokenType() + " " + tokenizer.token());
        }

        closeTag("term");
    }

    /**
     * Returns number of expressions in list (useful in project 11).
     */
    public int compileExpressionList() throws IOException {
        openTag("expressionList");

        int count = 0;

        // (expression (',' expression)*)?
        if (!isSymbol(')')) {
            compileExpression();
            count++;

            while (isSymbol(',')) {
                eatSymbol(',');
                compileExpression();
                count++;
            }
        }

        closeTag("expressionList");
        return count;
    }

    // -------- Closeable --------

    @Override
    public void close() throws IOException {
        out.close();
    }

    // -------- Internal helpers --------

    private void compileType() throws IOException {
        // type: 'int'|'char'|'boolean'|className(identifier)
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            String kw = tokenizer.keyword();
            if ("int".equals(kw) || "char".equals(kw) || "boolean".equals(kw)) {
                writeCurrentTokenAndAdvance();
                return;
            }
        }
        // className (identifier)
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            writeCurrentTokenAndAdvance();
            return;
        }
        throw new IllegalStateException(
            "Expected type but got " + tokenizer.tokenType() + " " + tokenizer.token());
    }

    /**
     * subroutineCall: subroutineName '(' expressionList ')' (className|varName) '.' subroutineName
     * '(' expressionList ')'
     * <p>
     * Note: No <subroutineCall> tag per project spec.
     */
    private void compileSubroutineCall() throws IOException {
        // first identifier already current
        eatIdentifier(); // could be className|varName|subroutineName

        if (isSymbol('.')) {
            eatSymbol('.');
            eatIdentifier(); // subroutineName
        }

        eatSymbol('(');
        compileExpressionList();
        eatSymbol(')');
    }

    private boolean isKeyword(String kw) {
        return tokenizer.tokenType() == TokenType.KEYWORD && kw.equals(tokenizer.keyword());
    }

    private boolean isSymbol(char c) {
        return tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == c;
    }

    private boolean isKeywordConstant(String kw) {
        return "true".equals(kw) || "false".equals(kw) || "null".equals(kw) || "this".equals(kw);
    }

    private void eatKeyword(String expected) throws IOException {
        if (!isKeyword(expected)) {
            throw new IllegalStateException(
                "Expected keyword '" + expected + "' but got " + tokenizer.tokenType() + " "
                    + tokenizer.token());
        }
        writeCurrentTokenAndAdvance();
    }

    private void eatSymbol(char expected) throws IOException {
        if (!isSymbol(expected)) {
            throw new IllegalStateException(
                "Expected symbol '" + expected + "' but got " + tokenizer.tokenType() + " "
                    + tokenizer.token());
        }
        writeCurrentTokenAndAdvance();
    }

    private void eatIdentifier() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new IllegalStateException(
                "Expected identifier but got " + tokenizer.tokenType() + " " + tokenizer.token());
        }
        writeCurrentTokenAndAdvance();
    }

    private void writeCurrentTokenAndAdvance() throws IOException {
        TokenType type = tokenizer.tokenType();
        String tag = XMLUtil.tokenTag(type);

        String val;
        switch (type) {
            case KEYWORD:
                val = tokenizer.keyword();
                break;
            case SYMBOL:
                val = String.valueOf(tokenizer.symbol());
                break;
            case IDENTIFIER:
                val = tokenizer.identifier();
                break;
            case INT_CONST:
                val = String.valueOf(tokenizer.intVal());
                break;
            case STRING_CONST:
                val = tokenizer.stringVal();
                break;
            default:
                throw new IllegalStateException("Unknown token type: " + type);
        }

        writeIndent();
        out.write("<" + tag + "> " + XMLUtil.escape(val) + " </" + tag + ">\n");

        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        } else {
            // Move to "no current token" state if finished
            tokenizer.advance();
        }
    }

    private void openTag(String tag) throws IOException {
        writeIndent();
        out.write("<" + tag + ">\n");
        indent++;
    }

    private void closeTag(String tag) throws IOException {
        indent--;
        writeIndent();
        out.write("</" + tag + ">\n");
    }

    private void writeIndent() throws IOException {
        for (int i = 0; i < indent; i++) {
            out.write("  ");
        }
    }

}
