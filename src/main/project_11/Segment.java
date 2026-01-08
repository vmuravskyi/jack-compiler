package main.project_11;

public enum Segment {
    CONSTANT("constant"),
    ARGUMENT("argument"),
    LOCAL("local"),
    STATIC("static"),
    THIS("this"),
    THAT("that"),
    POINTER("pointer"),
    TEMP("temp");

    private final String vmName;

    Segment(String vmName) {
        this.vmName = vmName;
    }

    public String vmName() {
        return vmName;
    }

}
