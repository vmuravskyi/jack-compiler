package main.project_11;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private static class Entry {

        final String type;
        final Kind kind;
        final int index;

        Entry(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    private final Map<String, Entry> table = new HashMap<>();

    private int staticIndex = 0;
    private int fieldIndex = 0;
    private int argIndex = 0;
    private int varIndex = 0;

    /**
     * Constructor / initializer: Creates a new symbol table.
     */
    public SymbolTable() {
    }

    /**
     * Empties the symbol table, and resets the four indexes to 0. Call when starting to compile a
     * new scope (class or subroutine).
     */
    public void reset() {
        table.clear();
        staticIndex = 0;
        fieldIndex = 0;
        argIndex = 0;
        varIndex = 0;
    }

    /**
     * Defines a new identifier of the given name, type, and kind. Assigns it the next index for
     * that kind.
     */
    public void define(String name, String type, Kind kind) {
        if (kind == Kind.NONE) {
            throw new IllegalArgumentException("Cannot define kind NONE for " + name);
        }

        int idx = nextIndex(kind);
        table.put(name, new Entry(type, kind, idx));
        bumpIndex(kind);
    }

    /**
     * Returns the number of variables of the given kind already defined in the table.
     */
    public int varCount(Kind kind) {
        switch (kind) {
            case STATIC:
                return staticIndex;
            case FIELD:
                return fieldIndex;
            case ARG:
                return argIndex;
            case VAR:
                return varIndex;
            default:
                return 0;
        }
    }

    /**
     * Returns the kind of the named identifier, or NONE if not found.
     */
    public Kind kindOf(String name) {
        Entry e = table.get(name);
        return (e == null) ? Kind.NONE : e.kind;
    }

    /**
     * Returns the type of the named identifier, or null if not found.
     */
    public String typeOf(String name) {
        Entry e = table.get(name);
        return (e == null) ? null : e.type;
    }

    /**
     * Returns the index of the named identifier, or -1 if not found.
     */
    public int indexOf(String name) {
        Entry e = table.get(name);
        return (e == null) ? -1 : e.index;
    }

    // -------- internal helpers --------

    private int nextIndex(Kind kind) {
        switch (kind) {
            case STATIC:
                return staticIndex;
            case FIELD:
                return fieldIndex;
            case ARG:
                return argIndex;
            case VAR:
                return varIndex;
            default:
                throw new IllegalStateException("Unexpected kind: " + kind);
        }
    }

    private void bumpIndex(Kind kind) {
        switch (kind) {
            case STATIC:
                staticIndex++;
                break;
            case FIELD:
                fieldIndex++;
                break;
            case ARG:
                argIndex++;
                break;
            case VAR:
                varIndex++;
                break;
            default:
                throw new IllegalStateException("Unexpected kind: " + kind);
        }
    }

}
