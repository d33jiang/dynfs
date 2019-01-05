package dynfs.debug;

import gnu.trove.stack.TByteStack;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TByteArrayStack;
import gnu.trove.stack.array.TIntArrayStack;

public interface Dumpable {

    //
    // Support Structure: Builder

    public static final class DumpBuilder {

        //
        // Constant: Prefix Configuration

        private static final String HEAD_PREFIX = "> ";

        //
        // Constant: Indentation Configuration

        private static final int BODY_INDENT_SIZE = 4;
        private static final int DUMPABLE_PUSH_SIZE = 3;

        //
        // State: Dump String

        private final StringBuilder sb;

        //
        // State: Indentation Record

        private final TIntStack indentDepths;
        private final TByteStack isGenerated;

        //
        // State: Last Character Optional

        private boolean truncateLastCharacterOnBuild;

        //
        // Construction

        private DumpBuilder() {
            this.sb = new StringBuilder();
            this.indentDepths = new TIntArrayStack();
            this.isGenerated = new TByteArrayStack();

            indentDepths.push(HEAD_PREFIX.length());
            isGenerated.push(Byte.MIN_VALUE);

            truncateLastCharacterOnBuild = true;
        }

        private DumpBuilder(Dumpable d) {
            this();
            loadDumpable(d);
        }

        //
        // Support: Indentation Stack Manipulation

        private void __pushIndent(int size, boolean isGenerated) {
            this.indentDepths.push(indentDepths.peek() + size);
            this.isGenerated.push((byte) (isGenerated ? -1 : 0));
        }

        private void __popIndent(boolean isGenerated) {
            if ((this.isGenerated.peek() < 0) != isGenerated)
                throw new IllegalStateException(
                        "There is a mismatch between generated indent levels and custom indent levels");

            this.indentDepths.pop();
            this.isGenerated.pop();
        }

        private void pushGeneratedIndent(int size) {
            __pushIndent(size, true);
        }

        private void popGeneratedIndent() {
            __popIndent(true);
        }

        //
        // Support: Raw Write, Newlines

        private void __putNewline() {
            sb.append(System.lineSeparator());
        }

        //
        // Support: Raw Write, Indentation

        private void __putIndent(int size) {
            for (int i = 0; i < size; i++)
                sb.append(' ');
        }

        //
        // Support: Indentation, Body

        private void open() {
            pushGeneratedIndent(BODY_INDENT_SIZE);
        }

        private void close() {
            popGeneratedIndent();
        }

        //
        // Support: NL Write, Head Mark

        private void mark(String tag) {
            __putIndent(indentDepths.peek() - HEAD_PREFIX.length());
            sb.append(HEAD_PREFIX);
            sb.append(tag);
            __putNewline();
            truncateLastCharacterOnBuild = true;
        }

        //
        // Interface: NL Write, Indentation

        public void insertIndent() {
            __putIndent(indentDepths.peek());
            truncateLastCharacterOnBuild = false;
        }

        //
        // Interface: Core Write, Content

        public void write(char c) {
            sb.append(c);
            truncateLastCharacterOnBuild = false;
        }

        public void write(String str) {
            sb.append(str);
            truncateLastCharacterOnBuild = false;
        }

        //
        // Interface: Core Write, Newline

        public void newline() {
            __putNewline();
            truncateLastCharacterOnBuild = true;
        }

        //
        // Interface: NL Write, Content

        public void writeLine(String line) {
            insertIndent();
            write(line);
            __putNewline();
            truncateLastCharacterOnBuild = true;
        }

        public void writeLine(Object o) {
            writeLine(o.toString());
        }

        public void writeLine(String... lines) {
            for (String line : lines) {
                writeLine(line);
            }
        }

        //
        // Interface: Nested Dumps

        public void nest(Dumpable nested) {
            pushGeneratedIndent(DUMPABLE_PUSH_SIZE);
            loadDumpable(nested);
            popGeneratedIndent();
        }

        public void nest(Dumpable... nested) {
            pushGeneratedIndent(DUMPABLE_PUSH_SIZE);
            for (Dumpable d : nested) {
                loadDumpable(d);
            }
            popGeneratedIndent();
        }

        //
        // Interface: Custom Indentation

        public void pushIndent(int size) {
            __pushIndent(size, false);
        }

        public void popIndent() {
            __popIndent(false);
        }

        //
        // Support: Load Dump

        private void loadDumpable(Dumpable d) {
            mark(d.__getDumpTag());
            open();
            d.__dump(this);
            close();
        }

        public String build() {
            if (truncateLastCharacterOnBuild) {
                return sb.substring(0, sb.length() - 1);
            }

            return sb.toString();
        }

    }

    //
    // Implementation: Dump

    public default DumpBuilder dump() {
        return new DumpBuilder(this);
    }

    //
    // Interface Implementation: Dump Tag

    public default String __getDumpTag() {
        return this.getClass().getName();
    }

    //
    // Interface Implementation: Dump Body

    public void __dump(DumpBuilder db);

}
