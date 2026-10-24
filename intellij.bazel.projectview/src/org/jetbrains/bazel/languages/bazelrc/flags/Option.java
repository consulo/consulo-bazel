package org.jetbrains.bazel.languages.bazelrc.flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// consulo: upstream Annotations.kt. KnownFlags uses Option, OptionEffectTag and OptionMetadataTag unqualified, so each is a
// top-level public type: the enums live in OptionEffectTag.java and OptionMetadataTag.java

/**
 * An annotation modelled by https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/common/options/Option.java
 * <p>
 * The intent is to be able to root IDE Features from a database of values annotated with this.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Option {
    /**
     * The name of the option ("--name").
     */
    String name();

    /**
     * The single-character abbreviation of the option ("-a").
     */
    char abbrev() default '\0';

    /**
     * A help string for the usage information. Note that this should be in plain text (no HTML tags,
     * for example).
     */
    String help() default "";

    /**
     * A short text string to describe the type of the expected value. E.g., <code>regex</code>. This
     * is ignored for boolean, tristate, boolean_or_enum, and void options.
     */
    String valueHelp() default "";

    String defaultValue() default "";

    /**
     * Tag about the intent or effect of this option. Unless this option is a no-op (and the reason
     * for this should be documented) all options should have some effect, so this needs to have at
     * least one value, and as many as apply.
     *
     * <p>No option should list NO_OP or UNKNOWN with other effects listed, but all other combinations
     * are allowed.
     */
    OptionEffectTag[] effectTags() default {};

    /**
     * Tag about the option itself, not its effect, such as option state (experimental) or intended
     * use (a value that isn't a flag but is used internally, for example, is "internal")
     *
     * <p>If one or more of the OptionMetadataTag values apply, please include, but otherwise, this
     * list can be left blank.
     *
     * <p>Hidden or internal options must be UNDOCUMENTED (set in {@code #documentationCategory()}).
     */
    OptionMetadataTag[] metadataTags() default {};

    /**
     * A boolean value indicating whether the option type should be allowed to occur multiple times in
     * a single arg list.
     *
     * <p>If the option can occur multiple times, then the attribute value <em>must</em> be a list
     * type {@code List<T>}, and the result type of the converter for this option must either match
     * the parameter {@code T} or {@code List<T>}. In the latter case the individual lists are
     * concatenated to form the full options value.
     *
     * <p>The {@link #defaultValue()} field of the annotation is ignored for repeatable flags and the
     * default value will be the empty list.
     */

    boolean allowMultiple() default false;

    /**
     * If the option is actually an abbreviation for other options, this field will contain the
     * strings to expand this option into. The original option is dropped and the replacement used in
     * its stead. It is recommended that such an option be of type {@link Void}.
     *
     * <p>An expanded option overrides previously specified options of the same name, even if it is
     * explicitly specified. This is the original behavior and can be surprising if the user is not
     * aware of it, which has led to several requests to change this behavior. This was discussed in
     * the blaze team and it was decided that it is not a strong enough case to change the behavior.
     */
    String[] expandsTo() default {};

    /**
     * The old name for this option. If an option has a name "foo" and an old name "bar", --foo=baz
     * and --bar=baz will be equivalent. If the old name is used, a warning will be printed indicating
     * that the old name is deprecated and the new name should be used.
     */
    String oldName() default "";

    /**
     * The list of commands this option applies to
     */
    String[] commands() default {};
}
