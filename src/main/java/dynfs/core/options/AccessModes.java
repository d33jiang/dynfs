package dynfs.core.options;

import java.nio.file.AccessMode;

public class AccessModes {

    public boolean read = false;
    public boolean write = false;
    public boolean execute = false;

    private AccessModes() {}

    public static AccessModes parse(AccessMode[] modes) {
        AccessModes result = new AccessModes();

        for (AccessMode mode : modes) {
            if (mode == AccessMode.READ) {
                result.read = true;
            } else if (mode == AccessMode.WRITE) {
                result.write = true;
            } else if (mode == AccessMode.EXECUTE) {
                result.execute = true;
            } else if (mode == null) {
                throw new NullPointerException("null CopyOption encountered");
            } else {
                throw new IllegalArgumentException("CopyOption " + mode + " is unavailable");
            }
        }

        return result;
    }

}
