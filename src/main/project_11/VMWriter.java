package main.project_11;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VMWriter implements Closeable {

    private final BufferedWriter out;

    /**
     * Creates a new output .vm file / stream, and prepares it for writing.
     */
    public VMWriter(Path outVmFile) throws IOException {
        this.out = Files.newBufferedWriter(outVmFile, StandardCharsets.UTF_8);
    }

    public void writePush(Segment segment, int index) throws IOException {
        out.write("push " + segment.vmName() + " " + index + "\n");
    }

    public void writePop(Segment segment, int index) throws IOException {
        out.write("pop " + segment.vmName() + " " + index + "\n");
    }

    /**
     * command is one of: add, sub, neg, eq, gt, lt, and, or, not
     */
    public void writeArithmetic(String command) throws IOException {
        out.write(command + "\n");
    }

    public void writeLabel(String label) throws IOException {
        out.write("label " + label + "\n");
    }

    public void writeGoto(String label) throws IOException {
        out.write("goto " + label + "\n");
    }

    public void writeIf(String label) throws IOException {
        out.write("if-goto " + label + "\n");
    }

    public void writeCall(String name, int nArgs) throws IOException {
        out.write("call " + name + " " + nArgs + "\n");
    }

    public void writeFunction(String name, int nVars) throws IOException {
        out.write("function " + name + " " + nVars + "\n");
    }

    public void writeReturn() throws IOException {
        out.write("return\n");
    }

    @Override
    public void close() throws IOException {
        out.flush();
        out.close();
    }

}
