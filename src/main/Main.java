package main;

public class Main {

    public static void main(String[] args) {
        // Example input:
        // args[0] = "projects/10/Square"
        // args[0] = "projects/10/Square/Main.jack"

        if (args.length != 1) {
            System.err.println("Usage: java Main <source>");
            System.err.println("  <source> = .jack file OR directory containing .jack files");
            System.exit(1);
        }

        // Delegate everything to JackAnalyzer
        JackAnalyzer.main(args);
    }
}
