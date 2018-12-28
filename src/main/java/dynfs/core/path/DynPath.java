package dynfs.core.path;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import dynfs.core.DynFileSystem;
import dynfs.core.DynFileSystemProvider;

public final class DynPath implements Path {

    //
    // Constant: Root Path String

    public static final String ROOT_PATH_STRING = "/";

    //
    // Constant: Path Separator

    public static final String PATH_SEPARATOR = "/";

    //
    // Constant: Special Path Names

    public static final String PATH_CURDIR = ".";
    public static final String PATH_PARENT = "..";

    //
    // Field: File System

    private final DynFileSystem<?> fs;

    @Override
    public DynFileSystem<?> getFileSystem() {
        return fs;
    }

    //
    // Field: Route

    private final DynRoute route;

    public DynRoute route() {
        return route;
    }

    //
    // Construction

    private DynPath(DynFileSystem<?> fs, DynRoute route) {
        this.fs = fs;
        this.route = route;
    }

    public static DynPath newPathFromUri(DynFileSystem<?> fs, URI uri) {
        // NOTE: null-check?
        return new DynPath(fs, DynRoute.fromUri(uri));
    }

    public static DynPath newPath(DynFileSystem<?> fs, String first, String... more) {
        // NOTE: null-check?
        return new DynPath(fs, DynRoute.fromPathNames(first, more));
    }

    //
    // Implementation: Path Matcher (Unsupported)

    public static PathMatcher getPathMatcher(String syntaxAndPattern) {
        if (syntaxAndPattern == null)
            throw new NullPointerException("syntaxAndPattern is null");

        // TODO: Future feature?
        throw new UnsupportedOperationException("PathMatchers are unavailable for DynFileSystemPaths");
    }

    //
    // Helper: Derived Path

    private DynPath derivePath(DynRoute route) {
        return route == null ? null : new DynPath(fs, route);
    }

    //
    // Interface Delegation: DynRoute

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
        return derivePath(route.getName(index));
    }

    @Override
    public DynPath getFileName() {
        return derivePath(route.getFileName());
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

        List<String> newPath = new ArrayList<>(route.getNameCount() + other.getNameCount());
        newPath.addAll(route.path());
        for (Path name : other) {
            newPath.add(name.toString());
        }

        return derivePath(DynRoute.fromPathNameList(isAbsolute(), newPath));
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
        return derivePath(route.relativize(DynRoute.fromPathNames(other)));
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
        return route.toUri(fs.domain());
    }

    @Override
    public DynPath toAbsolutePath() {
        return derivePath(route.toAbsoluteRoute());
    }

    @Override
    public DynPath toRealPath(LinkOption... options) throws IOException {
        return derivePath(route.toRealRoute(fs, options));
    }

    @Override
    public File toFile() {
        return route.toDynNode(fs);
    }

    //
    // Interface: WatchService

    @Override
    public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
        return fs.register(watcher, route, events);
    }

    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        return fs.register(watcher, route, events, modifiers);
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
    // Interface: Comparable<Path>

    @Override
    public int compareTo(Path other) {
        if (other == null)
            throw new NullPointerException("other is null");
        if (!(other instanceof DynPath))
            throw new IllegalArgumentException("other corresponds to another FileSystemProvider");

        DynPath p = (DynPath) other;
        return route.compareTo(p.route);
    }

}
