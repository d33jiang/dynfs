package dynfs.core.options;

import java.nio.file.LinkOption;

public final class LinkOptions {

    //
    // Configuration: Internal Data

    public boolean nofollowLinks = false;

    //
    // Construction: Factory

    private LinkOptions() {}

    public static LinkOptions newInstance(boolean nofollowLinks) {
        LinkOptions instance = new LinkOptions();
        instance.nofollowLinks = nofollowLinks;
        return instance;
    }

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
