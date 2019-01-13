package dynfs.core.io;

import java.io.IOException;

import dynfs.core.DynFileSystem;
import dynfs.core.DynFileSystemProvider;
import dynfs.core.DynPath;
import dynfs.core.DynRoute;
import dynfs.core.util.ProviderUtil;
import dynfs.dynlm.LMSpace;

public final class DirectoryIO {

    //
    // Construction

    private DirectoryIO() {}

    //
    // Directory Construction

    public static void createDirectory(DynFileSystem<LMSpace> fs, DynRoute route) throws IOException {
        DynPath childPath = DynPath.newPath(fs, route);
        provider().createDirectory(childPath);
    }

    // TODO: ...

    //
    // Alias: Provider

    private static DynFileSystemProvider provider() {
        return ProviderUtil.provider();
    }

}
