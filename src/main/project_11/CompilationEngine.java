package main.project_11;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CompilationEngine {

    private final JackTokenizer tokenizer;
    private final VMWriter vm;

    // Two scopes:
    private final SymbolTable classTable = new SymbolTable();
    private final SymbolTable subTable = new SymbolTable();

    private String className = "";
    private String subroutineName = "";
    private String subroutineType = ""; // "constructor" | "function" | "method"

    private int ifCounter = 0;
    private int whileCounter = 0;

    private static final Set<Character> OPS = new HashSet<>();

    static {
        for (char c : new char[]{'+', '-', '*', '/', '&', '|', '<', '>', '='}) {
            OPS.add(c);
        }
    }

    public CompilationEngine(JackTokenizer tokenizer, VMWriter vm) throws IOException {
        this.tokenizer = tokenizer;
        this.vm = vm;
        // Prime tokenizer
        this.tokenizer.advance();
    }

    // ------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------

    public void compileClass() throws IOException {
        classTable.reset();

        eatKeyword("class");
        className = eatIdentifier();

        eatSymbol('{');

        while (isKeyword("static") || isKeyword("field")) {
            compileClassVarDec();
        }

        while (isKeyword("constructor") || isKeyword("function") || isKeyword("method")) {
            compileSubroutine();
        }

        eatSymbol('}');
    }

    // ------------------------------------------------------------
    // Class-level declarations (populate class symbol table)
    // ------------------------------------------------------------

    public void compileClassVarDec() throws IOException {
        Kind kind;
        if (isKeyword("static")) {
            eatKeyword("static");
            kind = Kind.STATIC;
        } else {
            eatKeyword("field");
            kind = Kind.FIELD;
        }

        String type = parseType();
        String name = eatIdentifier();
        classTable.define(name, type, kind);

        while (isSymbol(',')) {
            eatSymbol(',');
            name = eatIdentifier();
            classTable.define(name, type, kind);
        }

        eatSymbol(';');
    }

    // ------------------------------------------------------------
    // Subroutines
    // ------------------------------------------------------------

    public void compileSubroutine() throws IOException {
        subTable.reset();
        ifCounter = 0;
        whileCounter = 0;

        if (isKeyword("constructor")) {
            subroutineType = "constructor";
        } else if (isKeyword("function")) {
            subroutineType = "function";
        } else {
            subroutineType = "method";
        }

        eatKeyword(subroutineType);

        // return type: void or type
        if (isKeyword("void")) {
            eatKeyword("void");
        } else {
            parseType();
        }

        subroutineName = eatIdentifier();

        // If method: arg0 is this
        if ("method".equals(subroutineType)) {
            subTable.define("this", className, Kind.ARG);
        }

        eatSymbol('(');
        compileParameterList();
        eatSymbol(')');

        compileSubroutineBody();
    }

    public void compileParameterList() throws IOException {
        if (isSymbol(')')) {
            return; // empty
        }

        String type = parseType();
        String name = eatIdentifier();
        subTable.define(name, type, Kind.ARG);

        while (isSymbol(',')) {
            eatSymbol(',');
            type = parseType();
            name = eatIdentifier();
            subTable.define(name, type, Kind.ARG);
        }
    }

    public void compileSubroutineBody() throws IOException {
        eatSymbol('{');

        while (isKeyword("var")) {
            compileVarDec();
        }

        int nLocals = subTable.varCount(Kind.VAR);
        vm.writeFunction(className + "." + subroutineName, nLocals);

        // method setup: align this
        if ("method".equals(subroutineType)) {
            vm.writePush(Segment.ARGUMENT, 0);
            vm.writePop(Segment.POINTER, 0);
        }

        // constructor setup: allocate fields and set this
        if ("constructor".equals(subroutineType)) {
            int nFields = classTable.varCount(Kind.FIELD);
            vm.writePush(Segment.CONSTANT, nFields);
            vm.writeCall("Memory.alloc", 1);
            vm.writePop(Segment.POINTER, 0);
        }

        compileStatements();

        eatSymbol('}');
    }

    public void compileVarDec() throws IOException {
        eatKeyword("var");

        String type = parseType();
        String name = eatIdentifier();
        subTable.define(name, type, Kind.VAR);

        while (isSymbol(',')) {
            eatSymbol(',');
            name = eatIdentifier();
            subTable.define(name, type, Kind.VAR);
        }

        eatSymbol(';');
    }

    // ------------------------------------------------------------
    // Statements
    // ------------------------------------------------------------

    public void compileStatements() throws IOException {
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
    }

    public void compileLet() throws IOException {
        eatKeyword("let");

        String varName = eatIdentifier();
        boolean isArray = false;

        if (isSymbol('[')) {
            isArray = true;
            eatSymbol('[');

            // push base address
            pushVar(varName);

            // push index
            compileExpression();

            eatSymbol(']');

            // base + index
            vm.writeArithmetic("add");
        }

        eatSymbol('=');
        compileExpression();
        eatSymbol(';');

        if (isArray) {
            // stack: address, value
            vm.writePop(Segment.TEMP, 0);     // value -> temp0
            vm.writePop(Segment.POINTER, 1);  // address -> that
            vm.writePush(Segment.TEMP, 0);
            vm.writePop(Segment.THAT, 0);
        } else {
            popVar(varName);
        }
    }

    public void compileIf() throws IOException {
        eatKeyword("if");

        int id = ifCounter++;
        String falseLabel = "IF_FALSE" + id;
        String endLabel = "IF_END" + id;

        eatSymbol('(');
        compileExpression();
        eatSymbol(')');

        // if NOT condition -> jump false
        vm.writeArithmetic("not");
        vm.writeIf(falseLabel);

        eatSymbol('{');
        compileStatements();
        eatSymbol('}');

        if (isKeyword("else")) {
            vm.writeGoto(endLabel);
            vm.writeLabel(falseLabel);

            eatKeyword("else");
            eatSymbol('{');
            compileStatements();
            eatSymbol('}');

            vm.writeLabel(endLabel);
        } else {
            vm.writeLabel(falseLabel);
        }
    }

    public void compileWhile() throws IOException {
        eatKeyword("while");

        int id = whileCounter++;
        String expLabel = "WHILE_EXP" + id;
        String endLabel = "WHILE_END" + id;

        vm.writeLabel(expLabel);

        eatSymbol('(');
        compileExpression();
        eatSymbol(')');

        vm.writeArithmetic("not");
        vm.writeIf(endLabel);

        eatSymbol('{');
        compileStatements();
        eatSymbol('}');

        vm.writeGoto(expLabel);
        vm.writeLabel(endLabel);
    }

    public void compileDo() throws IOException {
        eatKeyword("do");

        // subroutineCall starts with an identifier
        String first = eatIdentifier();
        compileSubroutineCallAfterFirst(first);

        eatSymbol(';');

        // discard return value
        vm.writePop(Segment.TEMP, 0);
    }

    public void compileReturn() throws IOException {
        eatKeyword("return");

        if (!isSymbol(';')) {
            compileExpression();
        } else {
            // void return convention
            vm.writePush(Segment.CONSTANT, 0);
        }

        eatSymbol(';');
        vm.writeReturn();
    }

    // ------------------------------------------------------------
    // Expressions
    // ------------------------------------------------------------

    public void compileExpression() throws IOException {
        compileTerm();

        while (tokenizer.tokenType() == TokenType.SYMBOL && OPS.contains(tokenizer.symbol())) {
            char op = tokenizer.symbol();
            tokenizer.advance();
            compileTerm();
            writeOp(op);
        }
    }

    public void compileTerm() throws IOException {
        TokenType tt = tokenizer.tokenType();

        if (tt == TokenType.INT_CONST) {
            vm.writePush(Segment.CONSTANT, tokenizer.intVal());
            tokenizer.advance();
            return;
        }

        if (tt == TokenType.STRING_CONST) {
            writeStringConstant(tokenizer.stringVal());
            tokenizer.advance();
            return;
        }

        if (tt == TokenType.KEYWORD) {
            String kw = tokenizer.keyword();
            if ("true".equals(kw)) {
                vm.writePush(Segment.CONSTANT, 1);
                vm.writeArithmetic("neg"); // -> -1
                tokenizer.advance();
                return;
            }
            if ("false".equals(kw) || "null".equals(kw)) {
                vm.writePush(Segment.CONSTANT, 0);
                tokenizer.advance();
                return;
            }
            if ("this".equals(kw)) {
                vm.writePush(Segment.POINTER, 0);
                tokenizer.advance();
                return;
            }
        }

        if (tt == TokenType.SYMBOL && isSymbol('(')) {
            eatSymbol('(');
            compileExpression();
            eatSymbol(')');
            return;
        }

        if (tt == TokenType.SYMBOL && (isSymbol('-') || isSymbol('~'))) {
            char unary = tokenizer.symbol();
            tokenizer.advance();
            compileTerm();
            if (unary == '-') {
                vm.writeArithmetic("neg");
            } else {
                vm.writeArithmetic("not");
            }
            return;
        }

        if (tt == TokenType.IDENTIFIER) {
            String name = tokenizer.identifier();
            tokenizer.advance();

            // varName[expression]
            if (isSymbol('[')) {
                eatSymbol('[');

                pushVar(name);       // base
                compileExpression(); // index

                eatSymbol(']');

                vm.writeArithmetic("add");
                vm.writePop(Segment.POINTER, 1);
                vm.writePush(Segment.THAT, 0);
                return;
            }

            // subroutineCall
            if (isSymbol('(') || isSymbol('.')) {
                compileSubroutineCallAfterFirst(name);
                return;
            }

            // simple varName
            pushVar(name);
            return;
        }

        throw new IllegalStateException(
            "Unexpected token in term: " + tokenizer.tokenType() + " " + tokenizer.token());
    }

    /**
     * (expression (',' expression)*)? Returns number of expressions pushed (nArgs).
     */
    public int compileExpressionList() throws IOException {
        int count = 0;

        if (isSymbol(')')) {
            return 0;
        }

        compileExpression();
        count++;

        while (isSymbol(',')) {
            eatSymbol(',');
            compileExpression();
            count++;
        }

        return count;
    }

    // ------------------------------------------------------------
    // Subroutine calls (handled from doStatement and term)
    // ------------------------------------------------------------

    private void compileSubroutineCallAfterFirst(String firstName) throws IOException {
        // subroutineName '(' expressionList ')'
        if (isSymbol('(')) {
            // method on current object: push this
            vm.writePush(Segment.POINTER, 0);

            eatSymbol('(');
            int nArgs = compileExpressionList();
            eatSymbol(')');

            vm.writeCall(className + "." + firstName, nArgs + 1);
            return;
        }

        // (className|varName) '.' subroutineName '(' expressionList ')'
        if (isSymbol('.')) {
            eatSymbol('.');
            String secondName = eatIdentifier(); // subroutineName

            boolean isVar = resolveKind(firstName) != Kind.NONE;

            if (isVar) {
                // method call on object variable
                String type = resolveType(firstName);
                pushVar(firstName); // object ref as arg0

                eatSymbol('(');
                int nArgs = compileExpressionList();
                eatSymbol(')');

                vm.writeCall(type + "." + secondName, nArgs + 1);
            } else {
                // function/constructor call on class
                eatSymbol('(');
                int nArgs = compileExpressionList();
                eatSymbol(')');

                vm.writeCall(firstName + "." + secondName, nArgs);
            }
            return;
        }

        throw new IllegalStateException(
            "Expected '(' or '.' after identifier in subroutine call, got: "
                + tokenizer.tokenType() + " " + tokenizer.token());
    }

    // ------------------------------------------------------------
    // Helpers: VM ops, strings, vars, types, token eating
    // ------------------------------------------------------------

    private void writeOp(char op) throws IOException {
        switch (op) {
            case '+':
                vm.writeArithmetic("add");
                break;
            case '-':
                vm.writeArithmetic("sub");
                break;
            case '&':
                vm.writeArithmetic("and");
                break;
            case '|':
                vm.writeArithmetic("or");
                break;
            case '<':
                vm.writeArithmetic("lt");
                break;
            case '>':
                vm.writeArithmetic("gt");
                break;
            case '=':
                vm.writeArithmetic("eq");
                break;
            case '*':
                vm.writeCall("Math.multiply", 2);
                break;
            case '/':
                vm.writeCall("Math.divide", 2);
                break;
            default:
                throw new IllegalArgumentException("Unknown operator: " + op);
        }
    }

    private void writeStringConstant(String s) throws IOException {
        // push length; call String.new 1
        vm.writePush(Segment.CONSTANT, s.length());
        vm.writeCall("String.new", 1);

        // for each char: push ascii; call String.appendChar 2
        for (int i = 0; i < s.length(); i++) {
            vm.writePush(Segment.CONSTANT, (int) s.charAt(i));
            vm.writeCall("String.appendChar", 2);
        }
    }

    private void pushVar(String name) throws IOException {
        Kind k = resolveKind(name);
        int idx = resolveIndex(name);

        vm.writePush(segmentOf(k), idx);
    }

    private void popVar(String name) throws IOException {
        Kind k = resolveKind(name);
        int idx = resolveIndex(name);

        vm.writePop(segmentOf(k), idx);
    }

    private Segment segmentOf(Kind kind) {
        switch (kind) {
            case STATIC:
                return Segment.STATIC;
            case FIELD:
                return Segment.THIS;
            case ARG:
                return Segment.ARGUMENT;
            case VAR:
                return Segment.LOCAL;
            default:
                throw new IllegalStateException("No VM segment for kind: " + kind);
        }
    }

    private Kind resolveKind(String name) {
        Kind k = subTable.kindOf(name);
        if (k != Kind.NONE) {
            return k;
        }
        return classTable.kindOf(name);
    }

    private String resolveType(String name) {
        String t = subTable.typeOf(name);
        if (t != null) {
            return t;
        }
        return classTable.typeOf(name);
    }

    private int resolveIndex(String name) {
        int idx = subTable.indexOf(name);
        if (idx >= 0) {
            return idx;
        }
        idx = classTable.indexOf(name);
        if (idx >= 0) {
            return idx;
        }
        throw new IllegalStateException("Unknown identifier (not in symbol tables): " + name);
    }

    private String parseType() throws IOException {
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            String kw = tokenizer.keyword();
            if ("int".equals(kw) || "char".equals(kw) || "boolean".equals(kw)) {
                tokenizer.advance();
                return kw;
            }
        }
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            String t = tokenizer.identifier();
            tokenizer.advance();
            return t;
        }
        throw new IllegalStateException(
            "Expected type but got: " + tokenizer.tokenType() + " " + tokenizer.token());
    }

    private boolean isKeyword(String kw) {
        return tokenizer.tokenType() == TokenType.KEYWORD && kw.equals(tokenizer.keyword());
    }

    private boolean isSymbol(char c) {
        return tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == c;
    }

    private void eatKeyword(String expected) throws IOException {
        if (!isKeyword(expected)) {
            throw new IllegalStateException("Expected keyword '" + expected + "' but got: "
                + tokenizer.tokenType() + " " + tokenizer.token());
        }
        tokenizer.advance();
    }

    private void eatSymbol(char expected) throws IOException {
        if (!isSymbol(expected)) {
            throw new IllegalStateException("Expected symbol '" + expected + "' but got: "
                + tokenizer.tokenType() + " " + tokenizer.token());
        }
        tokenizer.advance();
    }

    private String eatIdentifier() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Expected identifier but got: "
                + tokenizer.tokenType() + " " + tokenizer.token());
        }
        String name = tokenizer.identifier();
        tokenizer.advance();
        return name;
    }

}
