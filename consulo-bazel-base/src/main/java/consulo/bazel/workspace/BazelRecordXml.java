/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.bazel.workspace;

import org.jdom.Element;
import org.jetbrains.bsp.protocol.OutputLocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes records to XML and reads them back, driven by the declared (generic) types of the record components.
 * <p>
 * Supported: {@code String}, {@code boolean}, {@code int}, {@code long}, {@code double} (and their wrappers), enums, records,
 * sealed interfaces implemented by records, {@code List}/{@code Set}/{@code Collection}/{@code Map} of supported types and
 * {@link OutputLocation.OutputRoot}. Anything else (IDE objects such as {@code VirtualFile}, {@code FileType}, {@code Module},
 * paths, arbitrary classes) is rejected with an {@link IllegalArgumentException}, so a type that cannot be restored is never
 * written. A {@code null} component is an absent child element.
 * <p>
 * Collections are read back as unmodifiable {@code ArrayList}/{@code LinkedHashSet}/{@code LinkedHashMap} (the upstream
 * entities hold read-only Kotlin collections). Only public records of exported packages can be used: their accessors and
 * canonical constructor are called by reflection, no {@code opens} is needed.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public final class BazelRecordXml {
    private static final String VALUE = "value";
    private static final String TYPE = "type";
    private static final String NULL = "null";
    private static final String ITEM = "item";
    private static final String ENTRY = "entry";
    private static final String KEY = "key";

    private record RecordInfo(RecordComponent[] components, Constructor<?> constructor) {
    }

    private static final ClassValue<RecordInfo> RECORD_INFO = new ClassValue<>() {
        @Override
        protected RecordInfo computeValue(Class<?> type) {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] parameterTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                parameterTypes[i] = components[i].getType();
            }
            try {
                return new RecordInfo(components, type.getDeclaredConstructor(parameterTypes));
            }
            catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("No canonical constructor of " + type.getName(), e);
            }
        }
    };

    private BazelRecordXml() {
    }

    /**
     * Writes the components of {@code value} as child elements of {@code element}.
     */
    public static void writeRecord(Record value, Element element) {
        RecordInfo info = recordInfo(value.getClass());
        for (RecordComponent component : info.components()) {
            @Nullable Object componentValue;
            try {
                componentValue = component.getAccessor().invoke(value);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException("Cannot read " + value.getClass().getName() + "." + component.getName(), e);
            }
            if (componentValue == null) {
                continue;
            }
            Element child = new Element(component.getName());
            writeValue(component.getGenericType(), componentValue, child);
            element.addContent(child);
        }
    }

    public static <T extends Record> T readRecord(Class<T> type, Element element) {
        RecordInfo info = recordInfo(type);
        RecordComponent[] components = info.components();
        @Nullable Object[] arguments = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            Element child = element.getChild(component.getName());
            @Nullable Object componentValue = child == null ? null : readValue(component.getGenericType(), child);
            if (componentValue == null && component.getType().isPrimitive()) {
                throw new IllegalArgumentException("Missing " + type.getName() + "." + component.getName());
            }
            arguments[i] = componentValue;
        }
        try {
            return type.cast(info.constructor().newInstance(arguments));
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot create " + type.getName(), e);
        }
    }

    /**
     * Writes a value of the declared type {@code type} into {@code element} (attributes and children).
     */
    public static void writeValue(Type type, Object value, Element element) {
        Class<?> raw = rawClass(type);
        if (raw == String.class) {
            element.setAttribute(VALUE, (String) value);
        }
        else if (isScalar(raw)) {
            element.setAttribute(VALUE, String.valueOf(value));
        }
        else if (raw.isEnum()) {
            element.setAttribute(VALUE, ((Enum<?>) value).name());
        }
        else if (raw == OutputLocation.OutputRoot.class) {
            writeValue(listOf(String.class), ((OutputLocation.OutputRoot) value).getSegments(), element);
        }
        else if (Map.class.isAssignableFrom(raw)) {
            Type keyType = typeArgument(type, 0);
            Type valueType = typeArgument(type, 1);
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Element entryElement = new Element(ENTRY);
                entryElement.addContent(writeItem(KEY, keyType, entry.getKey()));
                entryElement.addContent(writeItem(VALUE, valueType, entry.getValue()));
                element.addContent(entryElement);
            }
        }
        else if (Collection.class.isAssignableFrom(raw)) {
            Type itemType = typeArgument(type, 0);
            for (Object item : (Collection<?>) value) {
                element.addContent(writeItem(ITEM, itemType, item));
            }
        }
        else if (raw.isRecord()) {
            writeRecord((Record) value, element);
        }
        else if (raw.isSealed() && value instanceof Record record) {
            Class<?> concrete = permittedSubclass(raw, value.getClass().getSimpleName());
            if (concrete != value.getClass()) {
                throw new IllegalArgumentException(value.getClass().getName() + " is not a permitted subclass of " + raw.getName());
            }
            element.setAttribute(TYPE, value.getClass().getSimpleName());
            writeRecord(record, element);
        }
        else {
            throw new IllegalArgumentException("Bazel data of type " + type.getTypeName() + " (" + value.getClass().getName() +
                ") cannot be persisted");
        }
    }

    public static @Nullable Object readValue(Type type, Element element) {
        Class<?> raw = rawClass(type);
        if (raw == String.class) {
            return requiredValue(element);
        }
        else if (raw == boolean.class || raw == Boolean.class) {
            return Boolean.parseBoolean(requiredValue(element));
        }
        else if (raw == int.class || raw == Integer.class) {
            return Integer.parseInt(requiredValue(element));
        }
        else if (raw == long.class || raw == Long.class) {
            return Long.parseLong(requiredValue(element));
        }
        else if (raw == double.class || raw == Double.class) {
            return Double.parseDouble(requiredValue(element));
        }
        else if (raw.isEnum()) {
            return enumValue(raw, requiredValue(element));
        }
        else if (raw == OutputLocation.OutputRoot.class) {
            List<String> segments = new ArrayList<>();
            for (Element item : element.getChildren(ITEM)) {
                segments.add(requiredValue(item));
            }
            return OutputLocation.OutputRoot.of(List.copyOf(segments));
        }
        else if (Map.class.isAssignableFrom(raw)) {
            Type keyType = typeArgument(type, 0);
            Type valueType = typeArgument(type, 1);
            Map<Object, Object> map = new LinkedHashMap<>();
            for (Element entry : element.getChildren(ENTRY)) {
                Element key = entry.getChild(KEY);
                Element entryValue = entry.getChild(VALUE);
                if (key == null || entryValue == null) {
                    throw new IllegalArgumentException("Malformed map entry");
                }
                map.put(readItem(keyType, key), readItem(valueType, entryValue));
            }
            return Collections.unmodifiableMap(map);
        }
        else if (Collection.class.isAssignableFrom(raw)) {
            Type itemType = typeArgument(type, 0);
            Collection<Object> collection = Set.class.isAssignableFrom(raw) ? new LinkedHashSet<>() : new ArrayList<>();
            for (Element item : element.getChildren(ITEM)) {
                collection.add(readItem(itemType, item));
            }
            if (collection instanceof Set<Object> set) {
                return Collections.unmodifiableSet(set);
            }
            return Collections.unmodifiableList((List<Object>) collection);
        }
        else if (raw.isRecord()) {
            return readRecord(raw.asSubclass(Record.class), element);
        }
        else if (raw.isSealed()) {
            String typeName = element.getAttributeValue(TYPE);
            if (typeName == null) {
                throw new IllegalArgumentException("Missing type of " + raw.getName());
            }
            Class<?> concrete = permittedSubclass(raw, typeName);
            if (concrete == null || !concrete.isRecord()) {
                throw new IllegalArgumentException("Unknown type " + typeName + " of " + raw.getName());
            }
            return readRecord(concrete.asSubclass(Record.class), element);
        }
        throw new IllegalArgumentException("Bazel data of type " + type.getTypeName() + " cannot be restored");
    }

    private static RecordInfo recordInfo(Class<?> type) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException(type.getName() + " is not a record");
        }
        return RECORD_INFO.get(type);
    }

    private static Element writeItem(String name, Type type, @Nullable Object value) {
        Element item = new Element(name);
        if (value == null) {
            item.setAttribute(NULL, "true");
        }
        else {
            writeValue(type, value, item);
        }
        return item;
    }

    private static @Nullable Object readItem(Type type, Element item) {
        if ("true".equals(item.getAttributeValue(NULL))) {
            return null;
        }
        return readValue(type, item);
    }

    private static String requiredValue(Element element) {
        String value = element.getAttributeValue(VALUE);
        if (value == null) {
            throw new IllegalArgumentException("Missing value of " + element.getName());
        }
        return value;
    }

    private static boolean isScalar(Class<?> raw) {
        return raw == boolean.class || raw == Boolean.class ||
            raw == int.class || raw == Integer.class ||
            raw == long.class || raw == Long.class ||
            raw == double.class || raw == Double.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }

    private static @Nullable Class<?> permittedSubclass(Class<?> sealed, String simpleName) {
        Class<?>[] permitted = sealed.getPermittedSubclasses();
        if (permitted == null) {
            return null;
        }
        for (Class<?> it : permitted) {
            if (it.getSimpleName().equals(simpleName)) {
                return it;
            }
        }
        return null;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> cls) {
            return cls;
        }
        if (type instanceof ParameterizedType parameterized) {
            return rawClass(parameterized.getRawType());
        }
        if (type instanceof WildcardType wildcard) {
            return rawClass(wildcard.getUpperBounds()[0]);
        }
        throw new IllegalArgumentException("Bazel data of type " + type.getTypeName() + " cannot be persisted");
    }

    private static Type typeArgument(Type type, int index) {
        if (type instanceof ParameterizedType parameterized) {
            Type argument = parameterized.getActualTypeArguments()[index];
            if (argument instanceof WildcardType wildcard) {
                return wildcard.getUpperBounds()[0];
            }
            return argument;
        }
        throw new IllegalArgumentException("Raw collection type " + type.getTypeName() + " cannot be persisted");
    }

    private static Type listOf(Class<?> itemType) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{itemType};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public @Nullable Type getOwnerType() {
                return null;
            }
        };
    }
}
