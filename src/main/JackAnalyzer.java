package main;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JackAnalyzer {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: JackAnalyzer <source>");
            System.err.println(
                "  where <source> is either Xxx.jack or a directory containing .jack files");
            System.exit(1);
        }

        Path source = Paths.get(args[0]);

        try {
            if (Files.isDirectory(source)) {
                List<Path> jackFiles = listJackFiles(source);
                for (Path jackFile : jackFiles) {
                    compileOne(jackFile);
                }
            } else {
                if (!source.toString().toLowerCase().endsWith(".jack")) {
                    throw new IllegalArgumentException(
                        "Input file must have .jack extension: " + source);
                }
                compileOne(source);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void compileOne(Path jackFile) throws IOException {
        Path outXml = outputXmlPathFor(jackFile);

        JackTokenizer tokenizer = new JackTokenizer(jackFile);
        try (CompilationEngine engine = new CompilationEngine(tokenizer, outXml)) {
            engine.compileClass();
        }

        System.out.println("Wrote: " + outXml);
    }

    private static Path outputXmlPathFor(Path jackFile) {
        String name = jackFile.getFileName().toString();
        String base = name.substring(0, name.length() - ".jack".length());
        return jackFile.getParent().resolve(base + ".xml");
    }

    private static List<Path> listJackFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.jack")) {
            for (Path p : ds) {
                files.add(p);
            }
        }
        files.sort(Comparator.comparing(Path::toString));
        return files;
    }

}
