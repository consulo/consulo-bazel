package org.jetbrains.bazel.languages.bazelquery.functions;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public abstract sealed class BazelQueryFunction permits BazelQueryFunction.SimpleFunction {
    public abstract String getName();

    public abstract String getDescription();

    public abstract List<StandardFunctionArgument> getArguments();

    public abstract @Nullable VarArgFunctionArgument getVarArguments();

    public abstract String getExampleUsage();

    // consulo: the properties are named like the record components (name(), type(), description()) of the implementations
    public sealed interface BazelQueryFunctionArgument permits StandardFunctionArgument, VarArgFunctionArgument {
        String name();

        String type();

        String description();
    }

    public record StandardFunctionArgument(
        String name,
        String type,
        boolean optional,
        String description
    ) implements BazelQueryFunctionArgument {
    }

    public record VarArgFunctionArgument(
        String name,
        String type,
        String description
    ) implements BazelQueryFunctionArgument {
    }

    public static final class SimpleFunction extends BazelQueryFunction {
        private final String myName;
        private final String myDescription;
        private final String myExampleUsage;
        private final List<StandardFunctionArgument> myArguments;
        private final @Nullable VarArgFunctionArgument myVarArguments;

        public SimpleFunction(
            String name,
            String description,
            String exampleUsage,
            List<StandardFunctionArgument> arguments,
            @Nullable VarArgFunctionArgument varArguments
        ) {
            myName = name;
            myDescription = description;
            myExampleUsage = exampleUsage;
            myArguments = arguments;
            myVarArguments = varArguments;
        }

        // consulo: upstream default arguments
        public SimpleFunction(
            String name,
            String description,
            String exampleUsage,
            List<StandardFunctionArgument> arguments
        ) {
            this(name, description, exampleUsage, arguments, null);
        }

        public SimpleFunction(
            String name,
            String description,
            String exampleUsage
        ) {
            this(name, description, exampleUsage, List.of(), null);
        }

        @Override
        public String getName() {
            return myName;
        }

        @Override
        public String getDescription() {
            return myDescription;
        }

        @Override
        public String getExampleUsage() {
            return myExampleUsage;
        }

        @Override
        public List<StandardFunctionArgument> getArguments() {
            return myArguments;
        }

        @Override
        public @Nullable VarArgFunctionArgument getVarArguments() {
            return myVarArguments;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SimpleFunction that)) {
                return false;
            }
            return myName.equals(that.myName) &&
                myDescription.equals(that.myDescription) &&
                myExampleUsage.equals(that.myExampleUsage) &&
                myArguments.equals(that.myArguments) &&
                Objects.equals(myVarArguments, that.myVarArguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myName, myDescription, myExampleUsage, myArguments, myVarArguments);
        }

        @Override
        public String toString() {
            return "SimpleFunction(name=" + myName +
                ", description=" + myDescription +
                ", exampleUsage=" + myExampleUsage +
                ", arguments=" + myArguments +
                ", varArguments=" + myVarArguments + ")";
        }
    }

    private static final List<BazelQueryFunction> KNOWN_FUNCTIONS =
        List.of(
            new SimpleFunction(
                "allpaths",
                "Computes all paths between two sets of targets.",
                "allpaths(//foo:bar, //baz:qux)",
                List.of(
                    new StandardFunctionArgument("start", "expr", false, "Set of starting points."),
                    new StandardFunctionArgument("end", "expr", false, "Set of ending points.")
                )
            ),
            new SimpleFunction(
                "allrdeps",
                "Computes all reverse dependencies in the universe set.",
                "allrdeps(//foo:bar, 2)",
                List.of(
                    new StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
                    new StandardFunctionArgument("depth", "int", true, "Maximum depth for reverse dependencies.")
                )
            ),
            new SimpleFunction(
                "attr",
                "Filters targets by rule attribute values using regular expressions.",
                "attr(\"tags\", \"[feature]\", deps(//foo:bar))",
                List.of(
                    new StandardFunctionArgument("name", "word", false, "The attribute name."),
                    new StandardFunctionArgument("pattern", "word", false, "Regular expression for attribute values."),
                    new StandardFunctionArgument("input", "expr", false, "The input set of targets.")
                )
            ),
            new SimpleFunction(
                "buildfiles",
                "Returns the BUILD files that define the packages for a given set of targets.",
                "buildfiles(deps(//foo:bar))",
                List.of(
                    new StandardFunctionArgument("expr", "expr", false, "The input set of targets.")
                )
            ),
            new SimpleFunction(
                "deps",
                "Computes the transitive closure of dependencies of a given set of targets.",
                "deps(//target:foo) or deps(//target:foo, 2)",
                List.of(
                    new StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
                    new StandardFunctionArgument("depth", "int", true, "Maximum depth of dependency graph traversal.")
                )
            ),
            new SimpleFunction(
                "filter",
                "Filters a set of targets whose labels match a given pattern.",
                "filter(\"//foo\", deps(//bar:baz))",
                List.of(
                    new StandardFunctionArgument("pattern", "word", false, "Regular expression for the target name."),
                    new StandardFunctionArgument("input", "expr", false, "The input set of targets to filter.")
                )
            ),
            new SimpleFunction(
                "kind",
                "Filters a set of targets by their kind (type).",
                "kind(\"source file\", deps(//foo:bar))",
                List.of(
                    new StandardFunctionArgument("pattern", "word", false, "Regular expression for the target type."),
                    new StandardFunctionArgument("input", "expr", false, "The input set of targets.")
                )
            ),
            new SimpleFunction(
                "labels",
                "Returns the set of targets specified in a given attribute of type label.",
                "labels(\"srcs\", deps(//foo:bar))",
                List.of(
                    new StandardFunctionArgument("attribute", "word", false, "The name of the attribute."),
                    new StandardFunctionArgument("input", "expr", false, "The input set of targets.")
                )
            ),
            new SimpleFunction(
                "loadfiles",
                "Returns the Starlark (.bzl) files needed to load the packages of a given set of targets.",
                "loadfiles(deps(//foo:bar))",
                List.of(
                    new StandardFunctionArgument("expr", "expr", false, "The input set of targets.")
                )
            ),
            new SimpleFunction(
                "rbuildfiles",
                "Returns the BUILD files that transitively depend on given path fragments.",
                "rbuildfiles(foo/BUILD, bar/file.bzl)",
                List.of(new StandardFunctionArgument("path", "word", false, "A list of path fragments (variable number of arguments).")),
                new VarArgFunctionArgument(
                    "path",
                    "word",
                    "A list of path fragments (variable number of arguments)."
                )
            ),
            new SimpleFunction(
                "rdeps",
                "Computes reverse dependencies of a set of targets within a universe set.",
                "rdeps(//foo/..., //bar:baz, 1)",
                List.of(
                    new StandardFunctionArgument("universe", "expr", false, "The universe set."),
                    new StandardFunctionArgument("input", "expr", false, "The input set."),
                    new StandardFunctionArgument("depth", "int", true, "Maximum depth for reverse dependencies.")
                )
            ),
            new SimpleFunction(
                "same_pkg_direct_rdeps",
                "Returns the set of targets in the same package that directly depend on the input set.",
                "same_pkg_direct_rdeps(//foo:bar)",
                List.of(
                    new StandardFunctionArgument("expr", "expr", false, "The input set of targets.")
                )
            ),
            new SimpleFunction(
                "siblings",
                "Returns the set of targets in the same package as the input set of targets.",
                "siblings(//foo:bar)",
                List.of(
                    new StandardFunctionArgument("expr", "expr", false, "The input set of targets.")
                )
            ),
            new SimpleFunction(
                "some",
                "Selects at most k targets arbitrarily from the input set.",
                "some(deps(//foo:bar), 2)",
                List.of(
                    new StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
                    new StandardFunctionArgument("count", "int", true, "The maximum number of targets to select.")
                )
            ),
            new SimpleFunction(
                "somepath",
                "Computes a single path between two sets of targets.",
                "somepath(//foo:bar, //baz:qux)",
                List.of(
                    new StandardFunctionArgument("start", "expr", false, "Set of starting points."),
                    new StandardFunctionArgument("end", "expr", false, "Set of ending points.")
                )
            ),
            new SimpleFunction(
                "tests",
                "Returns all test targets in the input set, expanding test_suite rules.",
                "tests(deps(//foo:bar))",
                List.of(
                    new StandardFunctionArgument("expr", "expr", false, "The input set of targets.")
                )
            ),
            new SimpleFunction(
                "visible",
                "Filters a set of targets based on the visibility to a predicate target set.",
                "visible(//foo, deps(//bar:qux))",
                List.of(
                    new StandardFunctionArgument("predicate", "expr", false, "The predicate target set."),
                    new StandardFunctionArgument("input", "expr", false, "The input set of targets.")
                )
            )
        );

    public static @Nullable BazelQueryFunction byName(String name) {
        return KNOWN_FUNCTIONS.stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public static List<BazelQueryFunction> getAll() {
        return KNOWN_FUNCTIONS;
    }
}
