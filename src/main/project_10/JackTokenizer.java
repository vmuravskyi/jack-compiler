package main.project_10;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JackTokenizer {

    private static final Set<String> KEYWORDS;

    static {
        Set<String> kw = new HashSet<>(Arrays.asList(
            "class", "constructor", "function", "method", "field", "static",
            "var", "int", "char", "boolean", "void", "true", "false", "null", "this",
            "let", "do", "if", "else", "while", "return"
        ));
        KEYWORDS = Collections.unmodifiableSet(kw);
    }

    // Jack symbol set (single-character tokens)
    private static final String SYMBOLS = "{}()[].,;+-*/&|<>=~";

    private final String input;
    private int pos = 0;

    private String currentToken = null;
    private TokenType currentType = null;

    public JackTokenizer(Path jackFile) throws IOException {
        byte[] bytes = Files.readAllBytes(jackFile);
        this.input = new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Are there more tokens in the input?
     */
    public boolean hasMoreTokens() {
        skipIgnorables();
        return pos < input.length();
    }

    /**
     * Advances to the next token and makes it the current token.
     */
    public void advance() {
        skipIgnorables();
        if (pos >= input.length()) {
            currentToken = null;
            currentType = null;
            return;
        }

        char c = input.charAt(pos);

        // String constant
        if (c == '"') {
            pos++; // skip opening "
            int start = pos;
            while (pos < input.length()) {
                char ch = input.charAt(pos);
                if (ch == '"') {
                    break;
                }
                if (ch == '\n' || ch == '\r') {
                    throw new IllegalStateException("Unterminated string constant");
                }
                pos++;
            }
            if (pos >= input.length()) {
                throw new IllegalStateException("Unterminated string constant");
            }
            currentToken = input.substring(start, pos); // without quotes
            currentType = TokenType.STRING_CONST;
            pos++; // skip closing "
            return;
        }

        // Symbol
        if (isSymbolChar(c)) {
            currentToken = String.valueOf(c);
            currentType = TokenType.SYMBOL;
            pos++;
            return;
        }

        // Integer constant
        if (Character.isDigit(c)) {
            int start = pos;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
            currentToken = input.substring(start, pos);
            currentType = TokenType.INT_CONST;
            return;
        }

        // Identifier or keyword
        if (isIdentifierStart(c)) {
            int start = pos;
            while (pos < input.length() && isIdentifierPart(input.charAt(pos))) {
                pos++;
            }
            currentToken = input.substring(start, pos);
            if (KEYWORDS.contains(currentToken)) {
                currentType = TokenType.KEYWORD;
            } else {
                currentType = TokenType.IDENTIFIER;
            }
            return;
        }

        throw new IllegalStateException("Unexpected character at pos " + pos + ": '" + c + "'");
    }

    public TokenType tokenType() {
        ensureCurrent();
        return currentType;
    }

    public String keyword() {
        ensureType(TokenType.KEYWORD);
        return currentToken;
    }

    public char symbol() {
        ensureType(TokenType.SYMBOL);
        return currentToken.charAt(0);
    }

    public String identifier() {
        ensureType(TokenType.IDENTIFIER);
        return currentToken;
    }

    public int intVal() {
        ensureType(TokenType.INT_CONST);
        return Integer.parseInt(currentToken);
    }

    public String stringVal() {
        ensureType(TokenType.STRING_CONST);
        return currentToken;
    }

    /**
     * Convenience: raw token string (for engine logic).
     */
    public String token() {
        ensureCurrent();
        return currentToken;
    }

    // ----------------- helpers -----------------

    private void ensureCurrent() {
        if (currentType == null || currentToken == null) {
            throw new IllegalStateException("No current token (did you call advance()?)");
        }
    }

    private void ensureType(TokenType t) {
        ensureCurrent();
        if (currentType != t) {
            throw new IllegalStateException(
                "Expected " + t + " but got " + currentType + " token=" + currentToken);
        }
    }

    private boolean isSymbolChar(char c) {
        return SYMBOLS.indexOf(c) >= 0;
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Skips whitespace and comments. Handles: - // line comment - /* block comment *\/ - /** API
     * comment *\/
     */
    private void skipIgnorables() {
        while (pos < input.length()) {
            char c = input.charAt(pos);

            // whitespace
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            // comments?
            if (c == '/' && pos + 1 < input.length()) {
                char n = input.charAt(pos + 1);

                // line comment //
                if (n == '/') {
                    pos += 2;
                    while (pos < input.length()) {
                        char ch = input.charAt(pos);
                        if (ch == '\n' || ch == '\r') {
                            break;
                        }
                        pos++;
                    }
                    continue;
                }

                // block comment /* ... */
                if (n == '*') {
                    pos += 2;
                    while (pos + 1 < input.length()) {
                        if (input.charAt(pos) == '*' && input.charAt(pos + 1) == '/') {
                            pos += 2;
                            break;
                        }
                        pos++;
                    }
                    continue;
                }
            }

            // nothing to skip
            break;
        }
    }

}
