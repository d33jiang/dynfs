package dynfs.template;

public class ClosedAllocatorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClosedAllocatorException(Allocator<?, ?> allocator) {
        super("Allocator has been closed");
    }

}
