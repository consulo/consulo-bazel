package org.jetbrains.bazel.languages.bazelrc.flags;

import consulo.util.lang.Pair;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

// consulo: upstream BazelFlag.kt. KnownFlags uses Flag unqualified, so the public class Flag needs its own file name.
// Inside this class the nested Boolean, Integer and Double shadow java.lang; only primitives are used here.
public abstract sealed class Flag {
    public abstract String getName();

    public static final class Unknown extends Flag {
        private final String myName;

        public Unknown(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class Boolean extends Flag {
        private final String myName;

        public Boolean(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class TriState extends Flag {
        private final String myName;

        public TriState(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class Integer extends Flag {
        private final String myName;

        public Integer(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class Path extends Flag {
        private final String myName;

        public Path(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class Double extends Flag {
        private final String myName;

        public Double(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class Duration extends Flag {
        private final String myName;

        public Duration(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class Str extends Flag {
        private final String myName;

        public Str(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class Label extends Flag {
        private final String myName;

        public Label(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    public static final class OneOf extends Flag {
        private final String myName;

        public OneOf(String name) {
            myName = name;
        }

        @Override
        public String getName() {
            return myName;
        }
    }

    // consulo: equals/hashCode/toString of the Kotlin data classes above, shared by all subclasses
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return getName().equals(((Flag) o).getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name=" + getName() + ")";
    }

    /**
     * Lazily load and return the @Option() annotation associated with this value, if any.
     */
    // consulo: the Kotlin property delegate LazyExtension is replaced by LazyValue
    private final Supplier<Option> myOption = LazyValue.notNull(() -> {
        Field field = DECLARED_FIELDS_MAP.get().get(getName());
        Option option = field != null ? field.getDeclaredAnnotation(Option.class) : null;
        return option != null ? option : createOption(getName());
    });

    public Option getOption() {
        return myOption.get();
    }

    // companion object

    static final Supplier<Map<String, Field>> DECLARED_FIELDS_MAP = LazyValue.notNull(() -> {
        Map<String, Field> result = new LinkedHashMap<>();
        for (Field it : KnownFlags.class.getDeclaredFields()) {
            if (it.getDeclaredAnnotation(Option.class) != null) {
                // consulo: keyed by the flag name, which upstream always equals the field name; in Java, KnownFlags fields
                // named after Java keywords (long, short, null) cannot keep that name
                result.put(flagName(it), it);
            }
        }
        // consulo: Kotlin associateBy keeps insertion order
        return Collections.unmodifiableMap(result);
    });

    // consulo: see DECLARED_FIELDS_MAP
    private static String flagName(Field field) {
        try {
            return field.get(null) instanceof Flag flag ? flag.getName() : field.getName();
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Supplier<Map<String, Flag>> ALL_FLAGS = LazyValue.notNull(() -> {
        Map<String, Flag> result = new LinkedHashMap<>();
        for (Field it : DECLARED_FIELDS_MAP.get().values()) {
            Option op = it.getDeclaredAnnotation(Option.class);
            if (op == null) {
                continue;
            }
            // consulo: KnownFlags fields are static, Field.get(null) instead of Field.get(KnownFlags)
            Object value;
            try {
                value = it.get(null);
            }
            catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            if (!(value instanceof Flag flag)) {
                continue;
            }
            for (Pair<String, Flag> name : knownFlagNames(Pair.create(flag, op))) {
                result.put(name.getFirst(), name.getSecond());
            }
        }
        // consulo: Kotlin toMap keeps insertion order
        return Collections.unmodifiableMap(result);
    });

    private static final Supplier<Set<String>> ALL_COMMANDS = LazyValue.notNull(() -> {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, Flag> x : ALL_FLAGS.get().entrySet()) {
            Collections.addAll(result, x.getValue().getOption().commands());
        }
        return Collections.unmodifiableSet(result);
    });

    public static Set<String> commands() {
        return ALL_COMMANDS.get();
    }

    public static @Nullable Flag byName(String name) {
        return ALL_FLAGS.get().get(name);
    }

    public static Map<String, Flag> all() {
        return ALL_FLAGS.get();
    }

    // consulo: upstream top-level function of BazelFlag.kt
    static List<Pair<String, Flag>> knownFlagNames(Pair<Flag, Option> pair) {
        Flag flag = pair.getFirst();
        Option option = pair.getSecond();
        List<String> names = Stream.of(option.name(), option.oldName()).filter(it -> !it.isEmpty()).toList();

        if (flag instanceof Flag.Boolean) {
            names = names.stream().flatMap(it -> Stream.of("--" + it, "--no" + it)).toList();
        }
        else if (flag instanceof Flag.TriState) {
            names = names.stream().flatMap(it -> Stream.of("--" + it, "--no" + it)).toList();
        }
        else {
            names = names.stream().map(it -> "--" + it).toList();
        }

        List<Pair<String, Flag>> result = new ArrayList<>();
        for (String it : names) {
            result.add(Pair.create(it, flag));
        }
        return result;
    }

    // consulo: upstream LazyExtension (a Kotlin property delegate) is not needed, LazyValue is used instead

    // consulo: Kotlin instantiates the annotation class (Option(o.name)); Java needs an implementation with the defaults
    private static Option createOption(String name) {
        return new Option() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Option.class;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public char abbrev() {
                return '\0';
            }

            @Override
            public String help() {
                return "";
            }

            @Override
            public String valueHelp() {
                return "";
            }

            @Override
            public String defaultValue() {
                return "";
            }

            @Override
            public OptionEffectTag[] effectTags() {
                return new OptionEffectTag[0];
            }

            @Override
            public OptionMetadataTag[] metadataTags() {
                return new OptionMetadataTag[0];
            }

            @Override
            public boolean allowMultiple() {
                return false;
            }

            @Override
            public String[] expandsTo() {
                return new String[0];
            }

            @Override
            public String oldName() {
                return "";
            }

            @Override
            public String[] commands() {
                return new String[0];
            }
        };
    }
}
