package dynfs.core.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import dynfs.core.DynFileSystemProvider;
import dynfs.core.DynRoute;
import dynfs.core.util.ProviderUtil;

public class TestBase {

    //
    // ProviderUtil Constants

    private static final String S_ROOT = ProviderUtil.S_ROOT;
    private static final DynRoute R_ROOT = ProviderUtil.R_ROOT;

    protected static final String sRoot() {
        return S_ROOT;
    }

    protected static final DynRoute rRoot() {
        return R_ROOT;
    }

    //
    // Construction

    protected TestBase() {}

    //
    // Provider Access

    protected static final DynFileSystemProvider provider() {
        return ProviderUtil.provider();
    }

    //
    // Private Method Access

    @SuppressWarnings("unchecked")
    protected static final <Instance, Return> Return invoke(Instance o, String method, Object... args)
            throws SecurityException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {

        Class<?>[] parameterTypes = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
        Method m = o.getClass().getDeclaredMethod(method, parameterTypes);
        m.setAccessible(true);

        return (Return) m.invoke(o, args);
    }

}
