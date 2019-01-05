package dynfs.core;

import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

public final class DynSpaceType {

    //
    // Configuration: Locality

    public final Locality locality;

    public static enum Locality {
        LOCAL("local"),
        REMOTE("remote");

        private final String name;

        private Locality(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    //
    // Configuration: Storage Medium

    public final Storage storage;

    public static enum Storage {
        MEMORY("memory"),
        DATABASE("database"),
        FILE_SYSTEM("fileSystem");

        private final String name;

        private Storage(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    //
    // Construction

    public DynSpaceType(Locality locality, Storage storage) {
        this.locality = locality;
        this.storage = storage;
    }

    //
    // Interface Implementation: Type String

    public String toTypeString() {
        return ImmutableList.of(locality, storage).stream()
                .map(Object::toString)
                .collect(Collectors.joining("."));
    }

}
