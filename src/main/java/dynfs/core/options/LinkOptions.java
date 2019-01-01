package dynfs.core.options;

import java.nio.file.LinkOption;

public class LinkOptions {

    public boolean nofollowLinks = false;

    LinkOptions() {}

    public static LinkOptions parse(LinkOption[] options) {
        LinkOptions result = new LinkOptions();

        for (LinkOption option : options) {
            if (option == LinkOption.NOFOLLOW_LINKS) {
                result.nofollowLinks = true;
            } else if (option == null) {
                throw new NullPointerException("LinkOption is null");
            }
        }

        return result;
    }
}
