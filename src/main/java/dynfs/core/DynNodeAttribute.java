package dynfs.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DynNodeAttribute {

    // NOTE: Store DynNodeAttribute type as field? (Future feature?)

    //
    // Static Support Structure: Associated DynNodeAttribute.View

    public static final class View {

        //
        // Configuration: Name

        private final String name;

        public String name() {
            return name;
        }

        //
        // Construction: Managed Instances

        private View(String name) {
            this.name = name;
        }

        // TODO: Implementation
    }

    //
    // Configuration: Lookup String

    private final String lookupString;

    public String lookupString() {
        return lookupString;
    }

    //
    // Configuration: Key

    private final String key;

    public String key() {
        return key;
    }

    //
    // Configuration: View

    private final View view;

    public View view() {
        return view;
    }

    //
    // Configuration: Name

    private final String name;

    public String name() {
        return name;
    }

    //
    // Construction: Managed Instances

    // TODO: Dynamically structured heuristic map implementation?
    private static final Map<String, DynNodeAttribute> registeredAttributes = new HashMap<>();

    private DynNodeAttribute(String lookupString, String key) {
        this.lookupString = lookupString;
        this.key = key;

        // TODO: Implementation
        this.view = null;
        this.name = null;
    }

    private static final Pattern ILLEGAL_VIEW_ATTRIBUTE_PATTERN = Pattern.compile("[,:]");

    public static DynNodeAttribute register(String view, String attribute) {
        {
            Matcher verifier = ILLEGAL_VIEW_ATTRIBUTE_PATTERN.matcher(view);
            if (verifier.matches()) {
                throw new IllegalArgumentException("Invalid view name");
            }

            verifier.reset(attribute);
            if (verifier.matches()) {
                throw new IllegalArgumentException("Invalid attribute name");
            }
        }

        return registeredAttributes.computeIfAbsent(view + ":" + attribute,
                k -> new DynNodeAttribute(view + ":" + attribute, view + ":" + attribute));
    }

    private static DynNodeAttribute put(String attribute) {
        return registeredAttributes.computeIfAbsent(attribute, k -> new DynNodeAttribute(attribute, attribute));
    }

    private static void put(String attribute, DynNodeAttribute instance) {
        if (instance == null) {
            registeredAttributes.remove(attribute);
        } else {
            registeredAttributes.put(attribute, new DynNodeAttribute(attribute, instance.key));
        }
    }

    //
    // Core Support: Conversion to String

    @Override
    public String toString() {
        return lookupString();
    }

    //
    // Static Support Structure: Base Attributes

    public static final class Base {

        //
        // Constant: View

        // TODO: Implementation
        public static final View VIEW = null;

        //
        // Constant: Attributes

        public static final DynNodeAttribute NAME;

        public static final DynNodeAttribute SIZE;

        public static final DynNodeAttribute CREATION_TIME;
        public static final DynNodeAttribute LAST_MODIFIED_TIME;
        public static final DynNodeAttribute LAST_ACCESS_TIME;

        static {
            NAME = put("name");

            SIZE = put("size");

            CREATION_TIME = put("creationTime");
            LAST_MODIFIED_TIME = put("lastModifiedTime");
            LAST_ACCESS_TIME = put("lastAccessTime");

            put("createTime", CREATION_TIME);

            put("basic:name", NAME);
            put("basic:size", SIZE);
            put("basic:createTime", CREATION_TIME);
            put("basic:lastModifiedTime", LAST_MODIFIED_TIME);
            put("basic:lastAccessTime", LAST_ACCESS_TIME);
        }
    }

    //
    // Interface Implementation: Parse from String

    public static DynNodeAttribute parse(String attribute) {
        DynNodeAttribute attr = registeredAttributes.get(attribute);

        if (attr == null)
            throw new IllegalArgumentException("No DynNodeAttribute is registered as [" + attribute + "]");

        return attr;
    }

    private static final Pattern ATTRIBUTES_STRING_SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");

    public static Set<DynNodeAttribute> parseToSet(String attributes) {
        String[] attrNames = ATTRIBUTES_STRING_SPLIT_PATTERN.split(attributes);
        Set<DynNodeAttribute> result = new HashSet<>();

        for (String attrName : attrNames) {
            DynNodeAttribute attr = registeredAttributes.get(attrName);

            if (attr == null)
                throw new IllegalArgumentException("No DynNodeAttribute is registered as [" + attrName + "]");

            result.add(attr);
        }

        return result;
    }

    public static Set<View> getDynNodeAttributeViews(Set<DynNodeAttribute> attributes) {
        return attributes.stream().map(DynNodeAttribute::view).collect(Collectors.toSet());
    }

}
