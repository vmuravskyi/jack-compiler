package main.project_10;

public final class XMLUtil {

    private XMLUtil() {
    }

    /**
     * Escapes XML special chars in token values.
     */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Maps tokenizer token types to the XML tag names used by the project.
     */
    public static String tokenTag(TokenType type) {
        return switch (type) {
            case KEYWORD -> "keyword";
            case SYMBOL -> "symbol";
            case IDENTIFIER -> "identifier";
            case INT_CONST -> "integerConstant";
            case STRING_CONST -> "stringConstant";
            default -> throw new IllegalArgumentException("Unknown token type: " + type);
        };
    }

}
