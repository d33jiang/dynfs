package dynfs.core;

import java.nio.file.attribute.FileStoreAttributeView;

public class DynSpaceAttributeView implements FileStoreAttributeView {

    //
    // Configuration: DynSpace

    private final DynSpace<?> store;

    //
    // Construction

    DynSpaceAttributeView(DynSpace<?> store) {
        this.store = store;
    }

    //
    // Interface: Attribute Access, Name

    @Override
    public String name() {
        return store.name();
    }

}
