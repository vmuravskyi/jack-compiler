package main.project_10;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class TokenizerToXML {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: TokenizerToXML <Xxx.jack>");
            System.exit(1);
        }
        Path jackFile = Paths.get(args[0]);
        if (!jackFile.toString().toLowerCase().endsWith(".jack")) {
            throw new IllegalArgumentException("Input must be a .jack file");
        }

        String name = jackFile.getFileName().toString();
        String base = name.substring(0, name.length() - ".jack".length());
        Path outXml = jackFile.getParent().resolve(base + "T.xml");

        JackTokenizer tokenizer = new JackTokenizer(jackFile);

        try (BufferedWriter out = Files.newBufferedWriter(outXml, StandardCharsets.UTF_8)) {
            out.write("<tokens>\n");

            while (tokenizer.hasMoreTokens()) {
                tokenizer.advance();
                writeTokenLine(out, tokenizer);
            }

            out.write("</tokens>\n");
        }

        System.out.println("Wrote: " + outXml);
    }

    private static void writeTokenLine(BufferedWriter out, JackTokenizer t) throws IOException {
        TokenType type = t.tokenType();
        String tag = XMLUtil.tokenTag(type);

        String val;
        switch (type) {
            case KEYWORD:
                val = t.keyword();
                break;
            case SYMBOL:
                val = String.valueOf(t.symbol());
                break;
            case IDENTIFIER:
                val = t.identifier();
                break;
            case INT_CONST:
                val = String.valueOf(t.intVal());
                break;
            case STRING_CONST:
                val = t.stringVal();
                break;
            default:
                throw new IllegalStateException("Unknown token type: " + type);
        }

        out.write("<" + tag + "> " + XMLUtil.escape(val) + " </" + tag + ">\n");
    }

}
