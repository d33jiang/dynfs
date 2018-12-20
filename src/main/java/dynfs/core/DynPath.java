package dynfs.core;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DynPath implements Path {

	static final String PATH_SEPARATOR = "/";

	private static final String PATH_CURDIR = ".";
	private static final String PATH_PARENT = "..";

	static final String ROOT_PATH_STRING = "/";

	//
	// File System

	private final DynFileSystem fs;

	@Override
	public DynFileSystem getFileSystem() {
		return fs;
	}

	//
	// Internal Data

	private final DynPathBody body;

	public DynPathBody body() {
		return body;
	}

	public static final class DynPathBody {
		private final boolean isAbsolute;
		private final List<String> path;
		private final String query;
		private final String fragment;

		public boolean isAbsolute() {
			return isAbsolute;
		}

		public List<String> path() {
			return Collections.unmodifiableList(path);
		}

		public int pathSize() {
			return path.size();
		}

		public String query() {
			return query;
		}

		public String fragment() {
			return fragment;
		}

		private DynPathBody(List<String> path) {
			this(true, path);
		}

		private DynPathBody(boolean isAbsolute, List<String> path) {
			this(isAbsolute, path, null);
		}

		private DynPathBody(boolean isAbsolute, List<String> path, String query) {
			this(isAbsolute, path, query, null);
		}

		private DynPathBody(boolean isAbsolute, List<String> path, String query, String fragment) {
			if (path == null)
				path = Arrays.asList();

			this.isAbsolute = isAbsolute;
			this.path = new ArrayList<>(path);
			this.query = query;
			this.fragment = fragment;
		}
	}

	@Override
	public boolean isAbsolute() {
		return body.isAbsolute();
	}

	private List<String> __path() {
		return body.path();
	}

	private String __query() {
		return body.query();
	}

	private String __fragment() {
		return body.fragment();
	}

	//
	// Construction

	private DynPath(DynFileSystem fs, DynPathBody body) {
		this.fs = fs;
		this.body = body;
	}

	static DynPath _newPathFromUri(DynFileSystem fs, URI uri) {
		List<String> path = decomposePathString(uri.getPath());
		DynPathBody body = new DynPathBody(true, path, uri.getQuery(), uri.getFragment());
		return new DynPath(fs, body);
	}

	private static DynPath __newPathFromUnitList(DynFileSystem fs, boolean isAbsolute, List<String> path) {
		DynPathBody body = new DynPathBody(isAbsolute, path);
		return new DynPath(fs, body);
	}

	static DynPath _newPath(DynFileSystem fs, String first, String... more) {
		boolean isAbsolute = first.startsWith(PATH_SEPARATOR);
		if (isAbsolute) {
			first = first.substring(1);
		}

		List<String> pd = decomposePathString(first, more);
		return __newPathFromUnitList(fs, isAbsolute, pd);
	}

	private static List<String> decomposePathString(String first, String... more) {
		Stream<String> pathStream = Stream.concat(Stream.<String>builder().add(first).build(), Arrays.stream(more));
		List<String> pathComponents = pathStream.flatMap(ps -> Arrays.stream(ps.split(PATH_SEPARATOR)))
				.collect(Collectors.toCollection(ArrayList<String>::new));
		return pathComponents;
	}

	//
	// Path Matcher

	public static PathMatcher getPathMatcher(String syntaxAndPattern) {
		// TODO: Future feature?
		throw new UnsupportedOperationException("PathMatchers are unavailable for DynFileSystemPaths");
	}

	//
	// Path Queries

	@Override
	public int getNameCount() {
		return __path().size();
	}

	private DynPath __subpath(int beginIndex, int endIndex, boolean isAbsolute) {
		DynPathBody newBody = new DynPathBody(isAbsolute, __path().subList(beginIndex, endIndex), __query(),
				__fragment());
		return new DynPath(fs, newBody);
	}

	private DynPath __subpath(int beginIndex, int endIndex) {
		boolean isAbsolute = isAbsolute() && beginIndex == 0;
		return __subpath(beginIndex, endIndex, isAbsolute);
	}

	@Override
	public DynPath subpath(int beginIndex, int endIndex) {
		if (beginIndex < 0)
			throw new IllegalArgumentException("beginIndex must be nonnegative");
		if (endIndex > getNameCount())
			throw new IllegalArgumentException("endIndex must be at most " + getNameCount());
		if (endIndex < beginIndex)
			throw new IllegalArgumentException("endIndex must be at least beginIndex");

		return __subpath(beginIndex, endIndex);
	}

	@Override
	public DynPath getRoot() {
		if (isAbsolute()) {
			return __subpath(0, 0, true);
		} else {
			return null;
		}
	}

	private DynPath __getName(int index) {
		return __subpath(index, index + 1, false);
	}

	@Override
	public DynPath getName(int index) {
		if (getNameCount() == 0)
			throw new IllegalStateException("getName cannot be invoked on an empty path");
		if (index < 0)
			throw new IllegalArgumentException("index must be nonnegative");
		if (index >= getNameCount())
			throw new IllegalArgumentException("index must be less than " + getNameCount());

		return __getName(index);
	}

	@Override
	public DynPath getFileName() {
		if (getNameCount() == 0) {
			return null;
		} else {
			return __getName(getNameCount() - 1);
		}
	}

	@Override
	public DynPath getParent() {
		if (getNameCount() == 0) {
			return getRoot();
		} else {
			return subpath(0, getNameCount() - 1);
		}
	}

	@Override
	public boolean startsWith(Path other) {
		if (__path() == null)
			throw new NullPointerException("other is null");
		if (!(other instanceof DynPath))
			return false;

		DynPath p = (DynPath) other;
		if (isAbsolute() != p.isAbsolute())
			return false;
		if (getNameCount() < p.getNameCount())
			return false;

		List<String> tp = __path();
		List<String> op = p.__path();

		for (int i = 0; i < op.size(); i++) {
			if (!op.get(i).equals(tp.get(i)))
				return false;
		}
		return true;
	}

	@Override
	public boolean startsWith(String other) {
		return startsWith(_newPath(fs, other));
	}

	@Override
	public boolean endsWith(Path other) {
		if (__path() == null)
			throw new NullPointerException("other is null");
		if (!(other instanceof DynPath))
			return false;

		DynPath p = (DynPath) other;
		if (getNameCount() < p.getNameCount()) {
			return false;
		} else if (getNameCount() == p.getNameCount()) {
			if (p.isAbsolute() && !isAbsolute())
				return false;
		} else {
			if (p.isAbsolute())
				return false;
		}

		List<String> tp = __path();
		List<String> op = p.__path();

		for (int i = op.size() - 1, j = tp.size() - 1; i >= 0; i--, j--) {
			if (!op.get(i).equals(tp.get(j)))
				return false;
		}
		return true;
	}

	@Override
	public boolean endsWith(String other) {
		return endsWith(_newPath(fs, other));
	}

	//
	// Path Manipulations

	@Override
	public DynPath normalize() {
		List<String> newPath = new LinkedList<>(__path());
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
				} else {
					iter.previous();
				}
			}
		}

		if (isAbsolute()) {
			while (newPath.get(0).equals(PATH_PARENT)) {
				newPath.remove(0);
			}
		}
		return __newPathFromUnitList(fs, isAbsolute(), new ArrayList<>(newPath));
	}

	@Override
	public Path resolve(Path other) {
		if (other.isAbsolute()) {
			return other;
		}

		List<String> newPath = new ArrayList<>(__path().size() + other.getNameCount());
		newPath.addAll(__path());
		for (Path name : other) {
			newPath.add(name.toString());
		}

		return __newPathFromUnitList(fs, isAbsolute(), newPath);
	}

	public DynPath resolve(DynPath other) {
		return (DynPath) resolve((Path) other);
	}

	@Override
	public DynPath resolve(String other) {
		return resolve(_newPath(fs, other));
	}

	@Override
	public Path resolveSibling(Path other) {
		if (other.isAbsolute())
			return other;

		DynPath parent = getParent();
		if (parent == null)
			return other;

		return parent.resolve(other);
	}

	public DynPath resolveSibling(DynPath other) {
		return (DynPath) resolveSibling((Path) other);
	}

	@Override
	public DynPath resolveSibling(String other) {
		return resolveSibling(_newPath(fs, other));
	}

	@Override
	public DynPath relativize(Path other) {
		// TODO Implementation: Method
		return null;
	}

	//
	// Path Conversions

	@Override
	public URI toUri() {
		try {
			return new URI(DynFileSystemProvider.URI_SCHEME, getFileSystem().domain(),
					__path().stream().collect(Collectors.joining(PATH_SEPARATOR)), __query(), __fragment());
		} catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public DynPath toAbsolutePath() {
		if (isAbsolute())
			return this;
		return fs.getRootDirectory().resolve(this);
	}

	//
	// Path-File Queries

	@Override
	public DynPath toRealPath(LinkOption... options) throws IOException {
		// TODO Implementation: Method
		return null;
	}

	@Override
	public File toFile() {
		// TODO Implementation: Method
		return null;
	}

	//
	// Interface: WatchService

	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
		return fs._register(watcher, events);
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		return fs._register(watcher, events, modifiers);
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
		if (!(other instanceof DynPath))
			throw new IllegalArgumentException("other corresponds to another FileSystemProvider");

		DynPath p = (DynPath) other;

		if (isAbsolute() != other.isAbsolute())
			return isAbsolute() ? -1 : 1;

		List<String> tp = __path();
		List<String> op = p.__path();
		for (int i = 0; i < tp.size() && i < op.size(); i++) {
			if (!tp.get(i).equals(op.get(i)))
				return tp.get(i).compareTo(op.get(i));
		}
		return tp.size() - op.size();
	}

}
