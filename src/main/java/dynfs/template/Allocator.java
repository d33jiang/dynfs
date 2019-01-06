package dynfs.template;

import java.io.Closeable;
import java.io.IOException;

public interface Allocator<Owner, Resource> extends Closeable {

    //
    // Interface: Resource Management

    public Iterable<Resource> allocate(Owner f, int size) throws IOException;

    public void free(Owner f, Iterable<Resource> resources);

    //
    // Interface: Close

    @Override
    public void close() throws IOException;

    public boolean isClosed();

}
