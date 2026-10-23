# Consulo port of the Bazel plugin

This repository is a fork of [JetBrains/hirschgarten](https://github.com/JetBrains/hirschgarten) (`main`).
The goal is to port the Bazel plugin to Consulo **in Java**, while still merging upstream changes regularly.

This document describes the rules for porting and the procedure for merging upstream.

## Core idea

Every ported file stays **at the same path, with the same name**. Only the extension changes (`.kt` -> `.java`).

- The upstream file `x/y/Foo.kt` always corresponds to our `x/y/Foo.java`.
- Every upstream change to `Foo.kt` turns into a merge conflict or a diff line that points directly at `Foo.java`.
- Files that are **not ported yet stay untouched** in the tree (Kotlin, not compiled by Maven). They merge
  cleanly, cause no noise, and are ready when we get to them. The `.kt` files remaining in the tree are the
  porting backlog.

## Remotes

```bash
git remote add upstream https://github.com/JetBrains/hirschgarten.git
git fetch upstream
```

- `origin` is `consulo/consulo-bazel`, our fork.
- `upstream` is `JetBrains/hirschgarten`. We track `upstream/main`, never release branches like `262`.

We always **merge** upstream. Never rebase onto it. The last merged upstream commit is then always:

```bash
git merge-base HEAD upstream/main
```

## Porting rules

### Paths and names

- Keep the directory, the package (`org.jetbrains.bazel...`) and the class name exactly as upstream.
- Do not move sources into `src/main/java`. Upstream source folders are compiled where they are.
- **Upstream modules share packages** (25 split packages, e.g. `org.jetbrains.bazel.config` lives in commons,
  backend and core). IntelliJ allows that, JPMS does not. Therefore the upstream core modules are compiled
  together as **one** Java module, `consulo.bazel.base`, by the Consulo-only Maven module `consulo-bazel-base/`:
  - its `pom.xml` adds each ported upstream `src/` folder with `build-helper-maven-plugin` (`add-source`);
  - its `src/main/java` holds `module-info.java` and all Consulo-only classes (`consulo.bazel.*` packages);
  - its `src/main/resources` holds the Consulo-only resources (`LOCALIZE-LIB`, `ICON-LIB`).
  Upstream resource folders are not used.
- Upstream directories contain only upstream files (converted to Java). No `pom.xml` or `module-info.java` is
  added there; upstream build files (`BUILD.bazel`, `.iml`) stay untouched.
- Exception: upstream's generated protobuf code (`protobuf/src/main/gen`) is its own Maven module
  (`protobuf/pom.xml`, `protobuf/src/main/gen/module-info.java`).
- Language integrations (`java/`, `golang/`, `python/`, `clion/`) go to the consumer plugins. They also share
  packages with core (e.g. `org.jetbrains.bazel.config`); this needs a decision when the first one is ported.
- The Consulo plugin descriptor lives in the Consulo-only module `consulo-plugin/`.
  Upstream's `intellij.bazel.plugin/resources/META-INF/plugin.xml` is left untouched.
- Upstream `<extensions>` XML (for example `intellij.bazel.languages.xml`) is not used. Consulo registers
  extensions with `@ExtensionImpl` on the class, and file types through a Consulo-only `FileTypeFactory`.

### Strings, icons, lexers

- `*Bundle.properties` becomes a YAML file in `consulo-bazel-base/src/main/resources/LOCALIZE-LIB/en_US/consulo.bazel.<module>.<Name>Localize.yaml`
  (generated class `consulo.bazel.<module>.localize.<Name>Localize`). The texts are copied verbatim; only the
  key style stays as upstream. `XxxBundle.message("a.b.c")` calls become `XxxLocalize.aBC()`.
  The upstream `XxxBundle.kt` and `.properties` files are deleted.
- Icons go to `consulo-bazel-base/src/main/resources/ICON-LIB/{light,dark}/consulo.bazel.<module>.<Name>IconGroup/` (generated class
  `consulo.bazel.<module>.icon.<Name>IconGroup`). The upstream `XxxIcons.kt` object is kept as a Java class
  that delegates to the generated icon group.
- JFlex lexers are generated at build time from upstream's `.flex` files (`generate-lexers`). An empty
  `<Name>.flex.idea` marker next to each `.flex` selects the IntelliJ-compatible skeleton. The upstream
  pre-generated `_XxxLexer.java` files are deleted, and the `.flex` imports are switched to Consulo packages.

### Two commits per ported file group

1. **Rename commit**: `git mv Foo.kt Foo.java` with the content unchanged.
   Message: `port: rename <module> sources to .java`
2. **Conversion commit**: rewrite the content in Java.
   Message: `port: convert <module> to Java`

The rename commit does not help merges. Git only compares end points, and a rewritten file is not detected
as a rename. It does keep the history: `git log --follow Foo.java` continues into upstream's Kotlin history.
The build may be broken between the two commits; that is fine.

### Java conversion rules

- Follow Consulo code style: 4-space indent, `else`/`catch` on a new line, `my` field prefix, no `var`,
  no `final` parameters, interface types on the left.
- `@NullMarked` on every `module-info.java`, and only `org.jspecify.annotations.Nullable` where needed.
- Kotlin `object` becomes a final class with a private constructor and static members, or a Consulo service
  if it holds state.
- `companion object` members become static members.
- `data class` becomes a `record` when it is immutable and not extended; otherwise a class with
  `equals`/`hashCode`.
- `suspend` / `Flow` / coroutine scopes become `CompletableFuture` chains (see the Consulo async rules).
  This is the biggest semantic change, so keep the method names and structure as close to upstream as
  possible so diffs stay portable.
- Keep method order and names as in the Kotlin file. Future upstream diffs are then easy to locate in the
  Java file.
- IntelliJ API -> Consulo API mapping is done in place. Do not move logic between files during conversion.
- Kotlin allows several top-level declarations per file; Java does not. Extra public declarations become
  `public static` nested classes of the file's main class (for example `StarlarkLanguage.StarlarkFileType`),
  package-private ones stay package-private top-level classes in the same file. Never move them to new files.
- Kotlin platform types (`node.firstChildNode.firstChildNode` without `?.`) become
  `Objects.requireNonNull(...)`: same runtime behavior, and the build enforces null safety (NullAway).
- Package-private `@ExtensionImpl` classes need an explicit `public` no-arg constructor.
- `com.intellij.model.psi.PsiSymbolReference` (IntelliJ's symbol API) does not exist in Consulo. Upstream uses it
  only for references to non-PSI targets (Bazel flags, built-in rules). `getOwnReferences()` returns
  `Collection<PsiReference>` and the PSI element merges them into `getReferences()`. The targets become light PSI
  elements with a documentation provider (when `intellij.bazel.core` is ported).
- Optional services (`serviceOrNull<X>()`) become `@ExtensionAPI` interfaces with a static nullable `getInstance()`.
- Hard-coded user-visible strings that upstream never put in a bundle are still moved to the YAML file
  (no `LocalizeValue.localizeTODO` in committed code).

### Files we never port

Some upstream code depends on APIs that Consulo does not have (for example CLion's closed-source
`com.jetbrains.cidr.*`). Such files are **left as `.kt` and never converted**. Their logic is reimplemented
elsewhere and noted in [Replaced files](#replaced-files).

## Merge procedure

### 1. Review what changed upstream (before merging)

```bash
git fetch upstream
BASE=$(git merge-base HEAD upstream/main)

# all changed Kotlin files since the last merge, with renames detected
git diff --name-status -M "$BASE" upstream/main -- '*.kt'

# the actual change of a single file, to port into its .java
git diff "$BASE" upstream/main -- path/to/Foo.kt
```

Split the list into **ported** files (a `.java` exists at the same path) and **unported** files (still
`.kt` in our tree). Only ported files need manual work.

### 2. Merge

```bash
git merge upstream/main
```

### 3. Resolve, case by case

| Upstream change | What git does | What to do |
|---|---|---|
| `M` on an **unported** `Foo.kt` | Merges cleanly | Nothing |
| `M` on a **ported** `Foo.kt` | Modify/delete conflict on `Foo.kt` (status `DU`) | Port the diff from step 1 into `Foo.java`, then `git rm path/Foo.kt` |
| `A` new `Bar.kt` in a **ported** module | Merges cleanly. **Silent!** | Convert it with the same two-commit rule (it can go in the merge or right after) |
| `A` new `Bar.kt` in an **unported** module | Merges cleanly | Nothing; it is backlog |
| `D` of a **ported** `Foo.kt` | Merges cleanly, our `Foo.java` stays. **Silent!** | `git rm path/Foo.java` and fix references |
| `R` `Old.kt` -> `New.kt` of a **ported** file | Conflict or clean add | `git mv Old.java New.java`, port content changes, remove `New.kt` |
| Changes to upstream `BUILD.bazel`, `.iml`, `MODULE.bazel` | Merges cleanly | Check whether `pom.xml` / `module-info.java` need a matching dependency change |
| Changes to a [replaced file](#replaced-files) | Merges cleanly (we kept the `.kt`) | Review the diff and port the logic into the replacement |

List the conflicts:

```bash
git status --short | grep -E '^(DU|UD|UU|AA|DD) '
```

Find the silent cases after merging (using the `BASE` from step 1):

```bash
# new upstream .kt files inside modules that are already ported
git diff --name-only --diff-filter=A "$BASE" upstream/main -- '*.kt'

# upstream .kt files deleted whose .java we still have
git diff --name-only --diff-filter=D "$BASE" upstream/main -- '*.kt' \
  | sed 's/\.kt$/.java/' | xargs -r ls 2>/dev/null
```

### 4. Verify and commit

- Build with Maven. Never commit a merge that does not compile, except across the two port commits.
- Commit message: `merge upstream/main <short-sha>` plus a short list of manually ported files.
- Merge often, at least weekly. Upstream `main` gets several commits per day, and small merges are much
  easier to port.

## Module status

| Upstream module | Consulo plugin / module | Status |
|---|---|---|
| `protobuf` (generated) | `consulo.bazel.protobuf` | builds (BES v1 gRPC package excluded) |
| aspect SDK protos (JetBrains/intellij-aspect) | `consulo.bazel.aspect.sdk` (`consulo-aspect-sdk/`) | builds |
| `intellij.bazel.languages` (Starlark, `.bazelrc`, query, `.bazelversion`) | `consulo.bazel.base` | converted, builds |
| `intellij.bazel.commons` | `consulo.bazel.base` | not started |
| `intellij.bazel.projectview` (needs commons) | `consulo.bazel.base` | not started |
| `intellij.bazel.backend` (runner, BEP, query, sync) | `consulo.bazel.base` | not started |
| `intellij.bazel.core` | `consulo.bazel.base` | not started |
| `java/*`, `kotlin/*` | `bazel` module in consulo-java / consulo-kotlin | not started |
| `golang/*` | `bazel` module in consulo-google-go | not started |
| `python/*` | `bazel` module in consulo-python | not started |
| `clion/*` | `bazel` module in incubating-consulo-cpp | not started |

Order: `languages` -> `commons` -> `projectview` -> `backend` -> `core` -> language integrations.

## Replaced files

Upstream files that are **not converted** but reimplemented elsewhere. On every merge, review their diffs
and port the logic into the replacement.

| Upstream file | Reason | Replacement |
|---|---|---|
| `intellij.bazel.languages/resources/intellij.bazel.languages.xml` | Consulo registers extensions with annotations | `@ExtensionImpl` on each class, `consulo.bazel.languages.BazelLanguagesFileTypeFactory` for file types |
| `intellij.bazel.languages/resources/colorSchemes/Starlark{Default,Darcula}.xml` | Consulo color schemes are extended in code | `consulo.bazel.languages.Starlark{Default,Darcula}ColorSchemeExtender` |
| `intellij.bazel.languages/resources/messages/*.properties` (deleted) | Consulo localization | `resources/LOCALIZE-LIB/en_US/*.yaml` |

Expected entries: the `clion/*` files that import `com.jetbrains.cidr.*` (compiler resolver, OC workspace,
CIDR execution and debugger). They are replaced by code in `incubating-consulo-cpp/bazel`, built on
`CPreprocessorOptions`, `CCompileSettings`, `SdkDialect` and consulo-dap.

## Aspect

The Bazel aspect lives in a separate repository, [JetBrains/intellij-aspect](https://github.com/JetBrains/intellij-aspect),
pinned in `MODULE.bazel`. We plan to fork it the same way (as `consulo/consulo-aspect`), with these rules:

- New languages (Zig, D, Rust, .NET) are new files in `modules/`.
- Our new fields in `ide_info.proto` use field numbers **1000 and up**, so they never clash with upstream.
- Bug fixes are also sent upstream as pull requests, so our diff shrinks over time.
