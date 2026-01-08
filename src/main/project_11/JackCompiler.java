package main.project_11;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JackCompiler {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: JackCompiler <source>");
            System.err.println(
                "  <source> is either Xxx.jack or a directory containing .jack files");
            System.exit(1);
        }

        Path source = Paths.get(args[0]);

        try {
            if (Files.isDirectory(source)) {
                for (Path jackFile : listJackFiles(source)) {
                    compileOne(jackFile);
                }
            } else {
                if (!source.toString().toLowerCase().endsWith(".jack")) {
                    throw new IllegalArgumentException("Input file must be .jack: " + source);
                }
                compileOne(source);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void compileOne(Path jackFile) throws IOException {
        Path outVm = outputVmPathFor(jackFile);

        JackTokenizer tokenizer = new JackTokenizer(jackFile);
        try (VMWriter vm = new VMWriter(outVm)) {
            CompilationEngine engine = new CompilationEngine(tokenizer, vm);
            engine.compileClass();
        }

        System.out.println("Wrote: " + outVm);
    }

    private static Path outputVmPathFor(Path jackFile) {
        String name = jackFile.getFileName().toString();
        String base = name.substring(0, name.length() - ".jack".length());
        return jackFile.getParent().resolve(base + ".vm");
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
