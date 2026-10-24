# Vendored IntelliJ aspect sources

The Starlark sources in this directory are copied verbatim from
[JetBrains/intellij-aspect](https://github.com/JetBrains/intellij-aspect) at commit
`5b08cdf0f02b0a6d4eae9bc96cc7b64a4ac3cd48` (the commit pinned by the Bazel plugin in `MODULE.bazel`).

They are the inputs of upstream's `//:archive_ide` target (`pkg_zip` of the `aspect_lib` targets `//common`,
`//intellij` and `//modules`). The Maven build (`maven-antrun-plugin`, `generate-resources`) zips this directory
into `archive_ide.zip` at the root of the `consulo-aspect-sdk` jar, which `com.intellij.aspect.lib.Deploy`
extracts into the user's workspace. This `README.md` is not part of the archive.

Layout (identical to the `pkg_zip` output):

| Directory            | Upstream glob                         |
|----------------------|---------------------------------------|
| `common/`            | `common/*.bzl`, `common/third_party/*.bzl` |
| `intellij/`          | `intellij/*.bzl`                      |
| `modules/`           | `modules/*.bzl`                       |

The `BUILD` files are **not** the upstream ones: `aspect_lib` (`private/rules/aspect_lib.bzl`) replaces each
package's `BUILD` with a generated file containing only `# generated BUILD file` (no trailing newline).
They are kept here with exactly that content.

## Updating

```bash
UPSTREAM=/path/to/intellij-aspect   # checkout of the new pinned commit
cd consulo-aspect-sdk/src/main/aspect
rm -f common/*.bzl common/third_party/*.bzl intellij/*.bzl modules/*.bzl
cp "$UPSTREAM"/common/*.bzl common/
cp "$UPSTREAM"/common/third_party/*.bzl common/third_party/
cp "$UPSTREAM"/intellij/*.bzl intellij/
cp "$UPSTREAM"/modules/*.bzl modules/
```

Then:

1. Check upstream's `BUILD`, `common/BUILD`, `intellij/BUILD`, `modules/BUILD` and `private/rules/aspect_lib.bzl`
   for changed globs or new packages in `archive_ide`, and update this directory and the `zip` task in
   `consulo-aspect-sdk/pom.xml` accordingly.
2. Update the commit hash above.
3. Port changes of upstream `sdk/lib/*.kt` into `consulo-aspect-sdk/src/main/java/com/intellij/aspect/lib`.
4. Regenerate `Modules.java` (see below).

## `Modules.java` (upstream `kt_aspect_enum`)

Upstream generates the Kotlin enum `com.intellij.aspect.lib.Modules` at build time with
`private/rules/kt_aspect_enum.bzl` from `sdk/lib/BUILD`:
`kt_aspect_enum(modules = MODULES + [rules_proto_info], ...)`, where `MODULES` is the list in `config/aspect.bzl`.
We keep a hand-written Java equivalent in `src/main/java/com/intellij/aspect/lib/Modules.java`.

To regenerate it, take the entries of `MODULES` in `config/aspect.bzl` **in order**, then append
`rules_proto_info`. For every entry, read its `module = intellij_module.define(...)` call in `modules/<file>.bzl`
and write one enum constant:

```java
<FILE_UPPER_CASE>("<file>", List.of(Rules.<ruleset>, ...)),
```

`file` is the `file = "..."` argument, the constant name is `file.upper()`, and the rule sets are the
`rulesets = [...]` argument (empty `List.of()` when it is missing). The order of the constants matters:
`AspectChoice.modulesForRules` returns modules in this order, and that is the order of `MODULES` in the
generated `config/aspect.bzl`.

A quick listing of the inputs:

```bash
cd "$UPSTREAM"
sed -n '/^MODULES = \[/,/^\]/p' config/aspect.bzl
for f in modules/*.bzl; do awk '/^module = /,/^\)/' "$f" | grep -E 'file =|rulesets ='; done
```
