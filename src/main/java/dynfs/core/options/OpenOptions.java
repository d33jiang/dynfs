package dynfs.core.options;

import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;

public final class OpenOptions {

    //
    // Configuration: Internal Data

    public boolean read = false;
    public boolean write = false;

    public boolean append = false;
    public boolean truncateExisting = false;

    public boolean create = false;
    public boolean createNew = false;

    public boolean deleteOnClose = false;

    public boolean sparse = false;

    public boolean sync = false;
    public boolean dsync = false;

    public boolean nofollowLinks = false;

    //
    // Construction: Factory

    private OpenOptions() {}

    public static OpenOptions parse(Iterable<? extends OpenOption> options) {
        OpenOptions result = new OpenOptions();

        for (OpenOption option : options) {
            if (option == StandardOpenOption.READ) {
                result.read = true;
            } else if (option == StandardOpenOption.WRITE) {
                result.write = true;
            } else if (option == StandardOpenOption.APPEND) {
                result.append = true;
            } else if (option == StandardOpenOption.TRUNCATE_EXISTING) {
                result.truncateExisting = true;
            } else if (option == StandardOpenOption.CREATE) {
                result.create = true;
            } else if (option == StandardOpenOption.CREATE_NEW) {
                result.createNew = true;
            } else if (option == StandardOpenOption.DELETE_ON_CLOSE) {
                result.deleteOnClose = true;
            } else if (option == StandardOpenOption.SPARSE) {
                result.sparse = true;
            } else if (option == StandardOpenOption.SYNC) {
                result.sync = true;
            } else if (option == StandardOpenOption.DSYNC) {
                result.dsync = true;
            } else if (option == LinkOption.NOFOLLOW_LINKS) {
                result.nofollowLinks = true;
            } else if (option == null) {
                throw new NullPointerException("null OpenOption encountered");
            } else {
                throw new IllegalArgumentException("OpenOption " + option + " is unavailable");
            }
        }

        if (result.append && result.read)
            throw new IllegalArgumentException(
                    "Cannot open file with both StandardOpenOption.APPEND and StandardOpenOption.READ");

        if (result.append && result.truncateExisting)
            throw new IllegalArgumentException(
                    "Cannot open file with both StandardOpenOption.APPEND and StandardOpenOption.TRUNCATE_EXISTING");

        return result;
    }

}
