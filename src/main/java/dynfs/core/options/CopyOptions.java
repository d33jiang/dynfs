package dynfs.core.options;

import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;

public class CopyOptions {

    public boolean atomicMove = false;
    public boolean copyAttributes = false;
    public boolean replaceExisting = false;

    public boolean nofollowLinks = false;

    CopyOptions() {}

    public static CopyOptions parse(CopyOption[] options) {
        CopyOptions result = new CopyOptions();

        for (CopyOption option : options) {
            if (option == StandardCopyOption.ATOMIC_MOVE) {
                result.atomicMove = true;
            } else if (option == StandardCopyOption.COPY_ATTRIBUTES) {
                result.copyAttributes = true;
            } else if (option == StandardCopyOption.REPLACE_EXISTING) {
                result.replaceExisting = true;
            } else if (option == LinkOption.NOFOLLOW_LINKS) {
                result.nofollowLinks = true;
            } else if (option == null) {
                throw new NullPointerException("null CopyOption encountered");
            } else {
                throw new IllegalArgumentException("CopyOption " + option + " is unavailable");
            }
        }

        return result;
    }

    public LinkOptions getLinkOptions() {
        LinkOptions linkOptions = new LinkOptions();
        linkOptions.nofollowLinks = this.nofollowLinks;
        return linkOptions;
    }

}
