package dynfs.core;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import dynfs.core.store.DynSpaceFactory;
import dynfs.core.store.DynSpaceLoader;

public final class DynFileSystem<Space extends DynSpace<Space>> extends FileSystem {

    //
    // Configuration: File System Provider

    private final DynFileSystemProvider provider;

    @Override
    public DynFileSystemProvider provider() {
        return provider;
    }

    //
    // Configuration: Domain

    private final String domain;

    public String domain() {
        return domain;
    }

    //
    // Configuration: DynStore

    private final Space store;

    public Space getStore() {
        return store;
    }

    //
    // State: Status

    @Override
    public boolean isOpen() {
        return getStore().status().isOpen();
    }

    @Override
    public void close() throws IOException {
        getStore().close();
    }

    //
    // Construction: Factory

    private DynFileSystem(DynFileSystemProvider provider, String domain, Space store) {
        this.provider = provider;
        this.domain = domain;
        this.store = store;
    }

    static <Space extends DynSpace<Space>> DynFileSystem<Space> newFileSystem(DynFileSystemProvider provider,
            String domain, DynSpaceFactory<Space> storeFactory, Map<String, ?> env) throws IOException {
        Space store = storeFactory.createStore(env);
        return new DynFileSystem<>(provider, domain, store);
    }

    static <Space extends DynSpace<Space>> DynFileSystem<Space> loadFileSystem(DynFileSystemProvider provider,
            String domain, DynSpaceLoader<? extends Space> storeLoader) throws IOException {
        Space store = storeLoader.loadStore();
        return new DynFileSystem<>(provider, domain, store);
    }

    //
    // Interface: Read-Only Property

    @Override
    public boolean isReadOnly() {
        return store.isReadOnly();
    }

    //
    // Interface: Paths

    @Override
    public String getSeparator() {
        return DynRoute.PATH_SEPARATOR;
    }

    @Override
    public DynPath getPath(String first, String... more) {
        return DynPath.newPath(this, first, more);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return DynPath.getPathMatcher(syntaxAndPattern);
    }

    //
    // Interface: Root Directories

    public DynPath getRootDirectory() {
        return getPath(DynRoute.ROOT_PATH_STRING);
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Arrays.asList(getRootDirectory());
    }

    //
    // Interface: File Stores

    @Override
    public Iterable<FileStore> getFileStores() {
        return Arrays.asList(getStore());
    }

    //
    // Interface: Supported File Attributes

    @Override
    public Set<String> supportedFileAttributeViews() {
        return store.supportedFileAttributeViewsByName();
    }

    //
    // Interface: UserPrincipal Support (Unsupported)

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // NOTE: Future feature?
        throw new UnsupportedOperationException("Users and groups are not supported");
    }

    //
    // Interface: WatchService Support (Unsupported)

    // NOTE: Check access control if implemented

    @Override
    public WatchService newWatchService() throws IOException {
        // NOTE: Future feature?
        throw new UnsupportedOperationException("Watch services are not supported");
    }

    public WatchKey register(WatchService watcher, DynRoute route, Kind<?>[] events) throws IOException {
        // NOTE: Future feature?
        // NOTE: Delegate to WatchService
        throw new UnsupportedOperationException("Watch services are not supported");
    }

    public WatchKey register(WatchService watcher, DynRoute route, Kind<?>[] events, Modifier[] modifiers)
            throws IOException {
        // NOTE: Future feature?
        // NOTE: Delegate to WatchService
        throw new UnsupportedOperationException("Watch services are not supported");
    }

    //
    // Interface: Route Resolution

    public final ResolutionResult<Space> resolve(DynRoute route) throws IOException {
        return getStore().resolve(route);
    }

    public final ResolutionResult<Space> resolve(DynRoute route, boolean followLinks) throws IOException {
        return getStore().resolve(route, followLinks);
    }

}
