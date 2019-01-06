package dynfs.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DynPath implements Path {

    //
    // Configuration: DynFileSystem

    private final DynFileSystem<?> fs;

    @Override
    public DynFileSystem<?> getFileSystem() {
        return fs;
    }

    private void throwIfNullFileSystem() {
        if (fs == null)
            throw new ClosedFileSystemException();
    }

    //
    // Configuration: Domain

    private final String domain;

    public String domain() {
        return domain;
    }

    //
    // Configuration: Route

    private final DynRoute route;

    public DynRoute route() {
        return route;
    }

    //
    // Construction: Factory

    private DynPath(DynFileSystem<?> fs, String domain, DynRoute route) {
        this.fs = fs;
        this.domain = domain;
        this.route = route;
    }

    static DynPath newPathFromUri(DynFileSystem<?> fs, URI uri) {
        return new DynPath(fs, uri.getHost(), DynRoute.fromUri(uri));
    }

    public static DynPath newPath(DynFileSystem<?> fs, String first, String... more) {
        if (more == null)
            throw new NullPointerException("more is null");

        return new DynPath(fs, fs.domain(), DynRoute.fromRouteNames(first, more));
    }

    public static DynPath newPath(DynFileSystem<?> fs, DynRoute route) {
        if (route == null)
            throw new NullPointerException("route is null");

        return new DynPath(fs, fs.domain(), route);
    }

    //
    // Core Support: Comparable<Path>

    @Override
    public int compareTo(Path other) {
        if (other == null)
            throw new NullPointerException("other is null");
        if (!(other instanceof DynPath))
            throw new ClassCastException("other is not a DynPath");

        DynPath p = (DynPath) other;
        return route.compareTo(p.route);
    }

    //
    // Support: Path Derivation

    private DynPath derivePath(DynRoute route) {
        return route == null ? null : new DynPath(fs, domain, route);
    }

    //
    // Interface: Path Queries
    // Delegation to DynRoute

    @Override
    public boolean isAbsolute() {
        return route.isAbsolute();
    }

    @Override
    public int getNameCount() {
        return route.getNameCount();
    }

    @Override
    public DynPath subpath(int beginIndex, int endIndex) {
        return derivePath(route.subroute(beginIndex, endIndex));
    }

    @Override
    public DynPath getRoot() {
        return derivePath(route.getRoot());
    }

    @Override
    public DynPath getName(int index) {
        return derivePath(route.getNameRoute(index));
    }

    @Override
    public DynPath getFileName() {
        return derivePath(route.getFileNameRoute());
    }

    @Override
    public DynPath getParent() {
        return derivePath(route.getParent());
    }

    @Override
    public boolean startsWith(Path other) {
        if (other == null)
            throw new NullPointerException("other is null");
        if (!(other instanceof DynPath))
            return false;

        DynPath p = (DynPath) other;
        return route.startsWith(p.route);
    }

    @Override
    public boolean startsWith(String other) {
        return route.startsWith(other);
    }

    @Override
    public boolean endsWith(Path other) {
        if (other == null)
            throw new NullPointerException("other is null");
        if (!(other instanceof DynPath))
            return false;

        DynPath p = (DynPath) other;
        return route.endsWith(p.route);
    }

    @Override
    public boolean endsWith(String other) {
        return route.endsWith(other);
    }

    @Override
    public DynPath normalize() {
        return derivePath(route.normalize());
    }

    public DynPath resolve(DynPath other) {
        return derivePath(route.resolve(other.route));
    }

    @Override
    public Path resolve(Path other) {
        if (other == null)
            throw new NullPointerException("other is null");
        if (other.isAbsolute())
            return other;
        if (other instanceof DynPath)
            return resolve((DynPath) other);

        List<String> newRoute = new ArrayList<>(route.getNameCount() + other.getNameCount());
        newRoute.addAll(route.names());
        for (Path name : other) {
            newRoute.add(name.toString());
        }

        return derivePath(DynRoute.fromRouteNameList(isAbsolute(), newRoute));
    }

    @Override
    public DynPath resolve(String other) {
        return derivePath(route.resolve(other));
    }

    public DynPath resolveSibling(DynPath other) {
        return derivePath(route.resolveSibling(other.route));
    }

    @Override
    public Path resolveSibling(Path other) {
        if (other == null)
            throw new NullPointerException("other is null");
        if (other.isAbsolute())
            return other;
        if (other instanceof DynPath)
            return resolveSibling((DynPath) other);

        DynPath parent = getParent();
        if (parent == null)
            return other;

        return parent.resolve(other);
    }

    @Override
    public DynPath resolveSibling(String other) {
        return derivePath(route.resolveSibling(other));
    }

    public DynPath relativize(DynPath other) {
        return derivePath(route.relativize(other.route));
    }

    private DynPath relativize(String other) {
        return derivePath(route.relativize(other));
    }

    @Override
    public DynPath relativize(Path other) {
        if (other == null)
            throw new NullPointerException("other is null");
        if (isAbsolute() != other.isAbsolute())
            throw new IllegalArgumentException("Exactly one of this and other has a root component");
        if (other instanceof DynPath)
            return relativize((DynPath) other);

        return relativize(other.toString());
    }

    @Override
    public URI toUri() {
        return route.toUri(domain);
    }

    @Override
    public DynPath toAbsolutePath() {
        return derivePath(route.toAbsoluteRoute());
    }

    @Override
    public DynPath toRealPath(LinkOption... options) throws IOException {
        throwIfNullFileSystem();
        return derivePath(route.toRealRoute(fs, options));
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException("DynPath is not interoperable with the java.io.File class");
    }

    public DynNode<?, ?> toDynNode() throws IOException {
        throwIfNullFileSystem();
        return route.lookup(fs);
    }

    //
    // Interface: Iterable<Path>

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            private int index = 0;

            @Override
            public Path next() {
                return getName(index++);
            }

            @Override
            public boolean hasNext() {
                return index < getNameCount();
            }
        };
    }

    //
    // Interface: Watchable
    // Delegation to DynFileSystem

    @Override
    public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
        throwIfNullFileSystem();
        return fs.register(watcher, route, events);
    }

    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        throwIfNullFileSystem();
        return fs.register(watcher, route, events, modifiers);
    }

    //
    // Implementation: Path Matcher (Future)

    public static PathMatcher getPathMatcher(String syntaxAndPattern) {
        if (syntaxAndPattern == null)
            throw new NullPointerException("syntaxAndPattern is null");

        // TODO: Future feature?
        throw new UnsupportedOperationException("PathMatchers are unavailable for DynFileSystemPaths");
    }

}
