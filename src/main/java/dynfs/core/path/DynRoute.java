package dynfs.core.path;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import dynfs.core.DynFileSystem;
import dynfs.core.DynFileSystemProvider;
import dynfs.core.DynNode;
import dynfs.core.DynSpace;
import dynfs.core.ResolutionResult;
import dynfs.core.options.LinkOptions;

public final class DynRoute implements Iterable<String>, Comparable<DynRoute> {

    // TODO: null-checks?

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
    // Configuration: Absolute Property

    private final boolean isAbsolute;

    public boolean isAbsolute() {
        return isAbsolute;
    }

    //
    // Configuration: Path, i.e. Route Names

    // TODO: Change to "names"?
    private final List<String> path;

    public List<String> path() {
        return path;
    }

    public String getName(int index) {
        return path.get(index);
    }

    public String getFileName() {
        if (getNameCount() == 0) {
            return null;
        } else {
            return getName(getNameCount() - 1);
        }
    }

    //
    // Configuration: Query

    private final String query;

    public String query() {
        return query;
    }

    //
    // Configuration: Fragment

    private final String fragment;

    public String fragment() {
        return fragment;
    }

    //
    // Construction: Factory

    private DynRoute(List<String> path) {
        this(true, path);
    }

    private DynRoute(boolean isAbsolute, List<String> path) {
        this(isAbsolute, path, null);
    }

    private DynRoute(boolean isAbsolute, List<String> path, String query) {
        this(isAbsolute, path, query, null);
    }

    private DynRoute(boolean isAbsolute, List<String> path, String query, String fragment) {
        if (path == null)
            path = ImmutableList.of();

        this.isAbsolute = isAbsolute;
        this.path = Collections.unmodifiableList(path);
        this.query = query;
        this.fragment = fragment;
    }

    private static List<String> decomposePathString(String first, String... more) {
        Stream<String> pathStream = Stream.concat(Stream.<String>builder().add(first).build(), Arrays.stream(more));
        List<String> pathComponents = pathStream.flatMap(ps -> Arrays.stream(ps.split(PATH_SEPARATOR)))
                .collect(Collectors.toCollection(ArrayList<String>::new));
        return pathComponents;
    }

    static DynRoute fromPathNames(String first, String... more) {
        // NOTE: null-check!
        boolean isAbsolute = first.startsWith(PATH_SEPARATOR);
        if (isAbsolute) {
            first = first.substring(1);
        }

        List<String> path = decomposePathString(first, more);
        return new DynRoute(isAbsolute, path);
    }

    static DynRoute fromUri(URI uri) {
        List<String> path = decomposePathString(uri.getPath());
        return new DynRoute(true, path, uri.getQuery(), uri.getFragment());
    }

    // TODO: package-private?
    public static DynRoute fromPathNameList(boolean isAbsolute, List<String> path) {
        // NOTE: null-check?
        return new DynRoute(isAbsolute, new ArrayList<>(path));
    }

    //
    // Core Support: Comparable<DynRoute>

    @Override
    public int compareTo(DynRoute other) {
        if (other == null)
            throw new NullPointerException("other is null");

        if (isAbsolute() != other.isAbsolute())
            return isAbsolute() ? -1 : 1;

        List<String> tp = path;
        List<String> op = other.path;

        for (int i = 0; i < tp.size() && i < op.size(); i++) {
            if (!tp.get(i).equals(op.get(i)))
                return tp.get(i).compareTo(op.get(i));
        }

        return tp.size() - op.size();
    }

    //
    // Core Support: Conversion to String

    @Override
    public String toString() {
        return path.stream().collect(Collectors.joining(isAbsolute ? ROOT_PATH_STRING : "", PATH_SEPARATOR, ""));
    }

    //
    // Interface: Name Count

    public int getNameCount() {
        return path.size();
    }

    //
    // Support: Route Derivation

    private DynRoute deriveRoute(boolean isAbsolute, List<String> path) {
        return new DynRoute(isAbsolute, path, query(), fragment());
    }

    //
    // Implementation: Subroute

    public DynRoute subroute(int beginIndex, int endIndex) {
        if (beginIndex < 0)
            throw new IllegalArgumentException("beginIndex must be nonnegative");
        if (endIndex > getNameCount())
            throw new IllegalArgumentException("endIndex must be at most " + getNameCount());
        if (endIndex < beginIndex)
            throw new IllegalArgumentException("endIndex must be at least beginIndex");

        return subrouteImpl(beginIndex, endIndex);
    }

    private DynRoute subrouteImpl(int beginIndex, int endIndex) {
        boolean isAbsolute = isAbsolute() && beginIndex == 0;
        return subrouteImpl(beginIndex, endIndex, isAbsolute);
    }

    private DynRoute subrouteImpl(int beginIndex, int endIndex, boolean isAbsolute) {
        return deriveRoute(isAbsolute, path.subList(beginIndex, endIndex));
    }

    //
    // Implementation: Root Route

    public DynRoute getRoot() {
        if (isAbsolute()) {
            return deriveRoute(true, null);
        } else {
            return null;
        }
    }

    //
    // Implementation: Name Route by Index

    public DynRoute getNameRoute(int index) {
        if (getNameCount() == 0)
            throw new IllegalStateException("getName cannot be invoked on an empty route");
        if (index < 0)
            throw new IllegalArgumentException("index must be nonnegative");
        if (index >= getNameCount())
            throw new IllegalArgumentException("index must be less than " + getNameCount());

        return getNameRouteImpl(index);
    }

    private DynRoute getNameRouteImpl(int index) {
        return subrouteImpl(index, index + 1, false);
    }

    //
    // Implementation: File Name Route

    public DynRoute getFileNameRoute() {
        if (getNameCount() == 0) {
            return null;
        } else {
            return getNameRouteImpl(getNameCount() - 1);
        }
    }

    //
    // Implementation: Parent Route

    public DynRoute getParent() {
        if (getNameCount() == 0) {
            return getRoot();
        } else {
            return subrouteImpl(0, getNameCount() - 1);
        }
    }

    //
    // Implementation: Query, Starts With
    public boolean startsWith(DynRoute other) {
        if (other == null)
            throw new NullPointerException("other is null");

        if (isAbsolute() != other.isAbsolute())
            return false;
        if (getNameCount() < other.getNameCount())
            return false;

        List<String> tp = path.subList(0, other.getNameCount());
        List<String> op = other.path;

        return tp.equals(op);
    }

    public boolean startsWith(String other) {
        if (other == null)
            throw new NullPointerException("other is null");

        return startsWith(DynRoute.fromPathNames(other));
    }

    //
    // Implementation: Query, Ends With

    public boolean endsWith(DynRoute other) {
        if (other == null)
            throw new NullPointerException("other is null");

        if (getNameCount() < other.getNameCount()) {
            return false;
        } else if (getNameCount() == other.getNameCount()) {
            if (other.isAbsolute() && !isAbsolute())
                return false;
        } else {
            if (other.isAbsolute())
                return false;
        }

        List<String> tp = path.subList(getNameCount() - other.getNameCount(), getNameCount());
        List<String> op = other.path;

        return tp.equals(op);
    }

    public boolean endsWith(String other) {
        if (other == null)
            throw new NullPointerException("other is null");

        return endsWith(DynRoute.fromPathNames(other));
    }

    //
    // Implementation: Route Normalization

    // TODO: Cache normalized route if normalize used often enough?
    // (Re-normalization after small modifications would then be inexpensive)
    // NOTE: If possible, do not depend on normalize!!!
    public DynRoute normalize() {
        LinkedList<String> newPath = new LinkedList<>(path);

        ListIterator<String> iter = newPath.listIterator();
        while (iter.hasNext()) {
            String cur = iter.next();
            if (cur.equals(PATH_CURDIR)) {
                iter.remove();
            } else if (!cur.equals(PATH_PARENT) && iter.hasNext()) {
                if (iter.next().equals(PATH_PARENT)) {
                    iter.remove();
                    iter.previous();
                    iter.remove();
                    if (iter.hasPrevious()) {
                        iter.previous();
                    }
                } else {
                    iter.previous();
                }
            }
        }

        if (isAbsolute()) {
            while (!newPath.isEmpty() && newPath.getFirst().equals(PATH_PARENT)) {
                newPath.removeFirst();
            }
        }

        return deriveRoute(isAbsolute(), new ArrayList<>(newPath));
    }

    //
    // Implementation: Route Resolution

    public DynRoute resolve(DynRoute other) {
        if (other.isAbsolute()) {
            return other;
        }

        List<String> newPath = new ArrayList<>(path.size() + other.getNameCount());
        newPath.addAll(path);
        newPath.addAll(other.path);

        return deriveRoute(isAbsolute(), newPath);
    }

    public DynRoute resolve(String other) {
        return resolve(DynRoute.fromPathNames(other));
    }

    //
    // Implementation: Route Sibling Resolution

    public DynRoute resolveSibling(DynRoute other) {
        if (other.isAbsolute())
            return other;

        DynRoute parent = getParent();
        if (parent == null)
            return other;

        return parent.resolve(other);
    }

    public DynRoute resolveSibling(String other) {
        return resolveSibling(DynRoute.fromPathNames(other));
    }

    //
    // Implementation: Route Relativization

    public DynRoute relativize(DynRoute other) {
        if (isAbsolute() != other.isAbsolute())
            throw new IllegalArgumentException("Exactly one of this and other has a root component");

        DynRoute src = this.normalize();
        DynRoute dst = other.normalize();

        if (src.getNameCount() == 0)
            return dst;
        if (PATH_PARENT.equals(src.getName(0)))
            throw new IllegalArgumentException("Cannot return to . from this");

        int commonNames = 0;
        while (commonNames < src.getNameCount() && commonNames < dst.getNameCount()) {
            if (src.getNameRoute(commonNames).equals(dst.getNameRoute(commonNames)))
                commonNames++;
        }

        int numBacktracks = src.getNameCount() - commonNames;

        List<String> relativePath = new LinkedList<>();
        for (int i = 0; i < numBacktracks; i++) {
            relativePath.add(PATH_PARENT);
        }
        for (int i = commonNames; i < dst.getNameCount(); i++) {
            relativePath.add(dst.getName(i));
        }

        return DynRoute.fromPathNameList(false, relativePath);
    }

    public DynRoute relativize(String other) {
        return relativize(DynRoute.fromPathNames(other));
    }

    //
    // Implementation: Conversion to URI

    public URI toUri(String domain) {
        try {
            return new URI(DynFileSystemProvider.URI_SCHEME, domain,
                    path().stream().collect(Collectors.joining(PATH_SEPARATOR, PATH_SEPARATOR, null)), query(),
                    fragment());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    //
    // Implementation: Query, Absolute Route

    public DynRoute toAbsoluteRoute() {
        if (isAbsolute()) {
            return this;
        } else {
            return deriveRoute(true, path);
        }
    }

    //
    // Implementation: Route Resolution by File System

    public DynRoute toRealRoute(DynFileSystem<?> fs, LinkOption... options) throws IOException {
        return lookup(fs, options).getRoute();
    }

    //
    // Interface Implementation: DynNode Lookup by FileSystem

    public <Space extends DynSpace<Space>> DynNode<Space, ?> lookup(DynFileSystem<Space> fs, LinkOption... options)
            throws IOException {
        if (fs == null)
            throw new NullPointerException("fs is null");

        LinkOptions linkOptions = LinkOptions.parse(options);

        ResolutionResult<Space> resolution = fs.resolve(this, !linkOptions.nofollowLinks);
        if (resolution.isSuccess()) {
            return resolution.node();
        } else {
            resolution.throwException();
            return null;
        }
    }

    //
    // Interface: Iterable<String>

    @Override
    public Iterator<String> iterator() {
        return path.iterator();
    }

}
