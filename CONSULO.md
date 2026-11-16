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
- **Protobuf runtime comes from the platform.** The generated code needs the full `protobuf-java` (descriptors,
  `TextFormat`). The Consulo platform must ship `protobuf-java` instead of `protobuf-javalite` (same module name
  `com.google.protobuf`; the full runtime also runs lite code). Bundling our own copy is impossible: both are
  automatic modules named `com.google.protobuf`. All modules use `protobuf-java` with `provided` scope, and
  `consulo-aspect-sdk` overrides the platform's `liteOnly=true` for its generated code.
- **gRPC comes from the platform too.** The platform ships `grpc-api`, `grpc-stub`, `grpc-protobuf` (+ `-lite`) and
  `proto-google-common-protos` (managed by `arch.managment`, `${grpc.version}`); all of them are `provided` here, the
  plugin bundles none. Until the published `arch.managment` has `${grpc.version}`, the poms keep explicit versions (TODO).
- The plugin packaging (`maven-consulo-plugin`) bundles **direct** dependencies of `consulo-plugin/pom.xml` only:
  every bundled library must be listed there.
- The Maven build cache ignores plugin-configuration-only changes in a pom; touch an input file of the module to
  force a rebuild.
- Language integrations (`java/`, `kotlin/`, `golang/`, `python/`, `clion/`) are **optional libraries of the
  `consulo.bazel` plugin** (decision of the project owner), like consulo-dotenv's language parts: one Maven jar module
  per integration (`consulo-bazel-java`, ...) compiling the upstream source folders, with
  `src/main/resources/META-INF/plugin-requires.xml` naming the required plugin (e.g. `consulo.java`). `consulo-plugin`
  depends on it with `runtime` scope; the packager writes `lib/<jar>.requires`, so the jar is loaded only when that
  plugin is installed; `plugin.xml` declares `<depends optional="true">consulo.java</depends>`. The jar is its own JPMS
  module next to `consulo.bazel.base`, so upstream files that reuse a core package get an integration-specific
  `package` line (kept at the upstream path, listed in [Renamed files](#renamed-files)).
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
- Consulo's annotation processor cannot handle **nested** `@ServiceImpl`/`@ExtensionImpl` classes. A public one gets its
  own file with the upstream fully qualified name (e.g. `org.jetbrains.bazel.commons.LanguageClassService`), a
  package-private one becomes a package-private top-level class in the same file.
- Kotlin `suspend` functions return `CompletableFuture`; async code uses Consulo's coroutine library
  (`consulo.util.concurrent.coroutine`). `BazelCoroutineService.start/startAsync` take
  `Supplier<CompletableFuture<...>>`; `startAsync` returns `BazelCoroutineService.Deferred` (lazy start supported).
- `<registryKey>` entries become defaults in the Consulo-only `consulo.bazel.commons.BazelRegistryKeys`.
- `com.intellij.model.psi.PsiSymbolReference` (IntelliJ's symbol API) does not exist in Consulo. Upstream uses it
  only for references to non-PSI targets (Bazel flags, built-in rules). `getOwnReferences()` returns
  `Collection<PsiReference>` and the PSI element merges them into `getReferences()`. The targets become light PSI
  elements with a documentation provider (when `intellij.bazel.core` is ported).
- Optional services (`serviceOrNull<X>()`) become `@ExtensionAPI` interfaces with a static nullable `getInstance()`.
- **Telemetry is not ported.** `BspTracer.measure(description, body)` only runs the body; `bspTracer`, `bspMeter`
  and `bspScope` do not exist, and code using them drops those lines.
- **EEL is not ported.** Environment and process access go through Consulo APIs instead of
  `com.intellij.platform.eel`.
- **Usage statistics (FUS) are not ported**, like telemetry: the `fus/*` collectors are deleted and calls to them
  are dropped with `// consulo: usage statistics are not ported`.
- IntelliJ-only sync infrastructure without a Consulo counterpart is dropped with a `// consulo:` comment:
  trusted projects (every project is synced), `IncompleteDependenciesService` (phased sync runs without the incomplete
  state), `SaveAndSyncHandler.scheduleProjectSave`. `withBackgroundProgress` -> `Task.Backgroundable.queue`,
  `suspendScanningAndIndexingThenExecute` -> `DumbService.suspendIndexingAndRun`,
  `withDisabledAutoSave` -> `SaveAndSyncHandler.block/unblockSaveOnFrameDeactivation`.
- Kotlin file-level declarations live in a class named after the file, which may be lowercase (`project`, `util`,
  `configuration`, `extensions`). A local variable with the same name hides the class: write the class fully
  qualified there (e.g. `org.jetbrains.bazel.config.project.getRootDir(project)`).
- **OS, environment and system info always go through `consulo.platform.Platform`**
  (`Platform.current().os().isWindows()`, `.environmentVariables()`, `Platform.current().user()`, ...), never
  `SystemInfo`, `SystemInfoRt`, `OS.CURRENT` or `System.getenv()`.
- Hard-coded user-visible strings that upstream never put in a bundle are still moved to the YAML file
  (no `LocalizeValue.localizeTODO` in committed code).

### Project model: classic API instead of the IntelliJ workspace model

Upstream builds the project structure with the IntelliJ workspace model (entities, `MutableEntityStorage`,
`WorkspaceFileIndexContributor`). Consulo has no workspace model, so this part is **rewritten on the classic API**
(decision of the project owner). Code that touches it diverges from upstream on purpose; port upstream changes to it
by hand.

- `MutableEntityStorage` (the sync "diff") -> Consulo-only `consulo.bazel.workspace.BazelModelBuilder`: wraps
  `ModifiableModuleModel`, the module `ModifiableRootModel`s and the project `LibraryTable.ModifiableModel`.
  Modules/libraries created through it are owned by Bazel; `commit()` applies everything with
  `ModifiableModelCommitter.multiCommit` and removes the Bazel-owned modules/libraries of the previous sync that were not
  created again (upstream: `replaceBySource(BazelEntitySource)`). `dispose()` discards a failed/cancelled sync.
- Ownership is persisted by `consulo.bazel.workspace.BazelProjectModelService` (workspace file).
- Upstream custom entities (`workspacemodel/entities`, `CcToolchainCompilerInfoEntity`, ...) become plain records with
  the upstream names. Their data is attached with `BazelModelBuilder.putProjectData/putModuleData/putLibraryData` and
  read through `BazelProjectModelService` (accessors like `extensions.getBazelModuleExtension(module)`). The data is
  persisted, as the IntelliJ workspace model caches the upstream entities:
  - module data in the Consulo-only module extension `consulo.bazel.workspace.BazelModuleExtension` (id `bazel`, system
    only: not editable in the module settings), enabled and filled by `BazelModelBuilder.commit()` and saved with the module;
  - project data and library data (by library name) in `consulo.bazel.workspace.BazelProjectDataStorage`
    (`.consulo/bazelProjectData.xml`, not roamed).
  Every stored type needs a `consulo.bazel.workspace.BazelDataSerializer` (`@ExtensionAPI`), registered by the library
  that owns the type (base: `consulo.bazel.workspace.serializer`, Go: `consulo.bazel.go.workspace`, C/C++:
  `consulo.bazel.cpp.workspace`). Records of strings, primitives, enums, collections, other records and `OutputLocation`
  extend `BazelRecordDataSerializer` (`BazelRecordXml`, reflection over public records of exported packages, no `opens`);
  types holding IDE objects get a hand-written serializer (`CcWorkspaceModelSerializer` writes file type and compiler
  kind ids and rebuilds the model through its API; `GoPackageEntitiesSerializer` writes the entity records and rebuilds the
  lookup maps). A `data` element without a serializer (its language plugin is not installed) is kept as unknown XML and
  written back. The XML carries a format version, the plugin version and a `complete` flag (false when a value had no
  serializer or could not be written or read); a mismatch or an error drops the data and the project resyncs.
  The data is restored when the module and the storage service load, before any reader, so a restore needs no roots
  change. `BazelModelBuilder.commit()` publishes the project and library data before its roots change.
- `VirtualFileUrl` -> VFS url `String`; `VirtualFileUrlManager` is dropped.
- `WorkspaceFileIndexContributor` -> `IndexableSetContributor` (indexable roots) + `DirectoryIndexExcludePolicy`
  (excluded roots). Upstream's "content but not indexable" project root (`CONTENT_NON_INDEXABLE`) has no classic
  equivalent (TODO).
- The generated workspace entity code (`intellij.bazel.backend/gen`) is deleted.
- `BazelWorkspaceImporter.import` is `doImport` (`import` is a Java keyword).
- JVM source roots: upstream marks a source root which is not a jar with `packageMatchesDirectory = false` (experimental
  IntelliJ property: the package of a file comes from its `package` statement). Consulo has neither this property nor a
  package prefix, so `SourceRootBuilder.toPackageRoot` moves a root whose directory ends with the declared package up
  to the package root (`.../com/example/app` -> `src/main/java`; decision of the project owner). TODO: layouts where the
  directory does not match the package.
- The importers fill `BazelModelBuilder` on background threads; the classic modifiable models need read access there
  (`getRootModel` takes it itself, the JVM entity writer runs in a read action).

### Core: project opening, build and platform gaps

- **Opening a Bazel project.** IntelliJ's `ProjectOpenProcessor` is internal in Consulo, so
  `flow/open/BazelProjectOpenProcessor` is a `ModuleImportProvider`: opening a Bazel workspace goes through Consulo's
  import flow, and `process(...)` records the Bazel store descriptor and runs the upstream open callback after the
  project is opened. Upstream marks a Bazel project with its project store (`ProjectStorePathCustomizer` ->
  `BazelProjectStoreDescriptor`); Consulo keeps project files in `.consulo`, so `BazelProjectStoreDescriptor` is a plain
  class persisted by the Consulo-only `consulo.bazel.core.BazelProjectStoreService` (`bazel.xml`), and
  `BazelProjectStorePathCustomizer` only computes it. `project.isBazelProject` = a descriptor is stored.
- `closeAndReopenAsBazelProject` stores the descriptor and calls `ProjectManager.reloadProject` (no `OpenProjectTask`).
- Not registered (no Consulo API), the classes are kept and marked `TODO consulo`: `BazelUnlinkedProjectAware`
  (`ExternalSystemUnlinkedProjectAware`), `BazelTrustedProjectsLocator` (trusted projects), `CounterPlatformProjectConfigurator`
  (`DirectoryProjectConfigurator`), and the workspace-model `registerProjectRoot` call in
  `OpenBazelProjectAndSyncStartupActivity` (`InitProjectActivity` -> first `PostStartupActivity`).
- **Build.** IntelliJ's `ProjectTaskRunner` does not exist: `buildTask/BazelProjectTaskRunner` is a Consulo
  `CompilerRunner` that builds the targets of the affected modules; `ProjectTaskContext` is the `CompileContext`.
- `ModuleEntity` in core APIs (`ModuleTargetService`, `TargetsToBuildProvider`) is the classic `Module`; indexes over the
  entity storage are `CachedValue`s that depend on `ProjectRootManager`.
- Startup resync: upstream resyncs when the workspace model was not loaded from its cache; here, when the persisted Bazel
  data was not restored complete and of the current plugin version (`BazelProjectModelService.isLoadedFromCache()`), or
  has no `BazelProjectDirectoriesEntity`.
- Documentation: the Consulo-only bridge `consulo.bazel.documentation` also has `DocumentationSymbol`,
  `PsiDocumentationTargetProvider`, `DocumentationLinkHandler` and `LinkResolveResult`; references to symbols resolve
  to `BazelDocumentationElement`. One `BazelDocumentationProviderBase` subclass per language
  (`consulo.bazel.core.{Starlark,Bazelrc,BazelQuery,BazelQueryFlags}DocumentationProvider`).
- Notification groups: `consulo.bazel.core.BazelNotificationGroupContributor`. Icons: `consulo.bazel.core.BazelPluginIconGroup`
  (copied from `intellij.bazel.resource/resources/icons`), wrapped by upstream `BazelPluginIcons`.
- Run configuration fragments (`SettingsEditorFragment`, `RunConfigurationFragmentedEditor`, ...) do not exist in Consulo:
  a minimal Consulo-only bridge with the same names lives in `consulo.bazel.execution.ui`.
- Kotlin UI DSL panels are rebuilt with Swing and `FormBuilder`, keeping row order and bindings.
- The new IntelliJ Search Everywhere API (`SeLabelProvider*`, split mode only) is deleted; the legacy contributor is ported.
- `multiverse/` (IntelliJ code insight contexts) is deleted.

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
| `protobuf` (generated) | `consulo.bazel.protobuf` | builds (incl. the BES v1 gRPC API) |
| aspect SDK protos (JetBrains/intellij-aspect) | `consulo.bazel.aspect.sdk` (`consulo-aspect-sdk/`) | builds |
| `intellij.bazel.languages` (Starlark, `.bazelrc`, query, `.bazelversion`) | `consulo.bazel.base` | converted, builds |
| `intellij.bazel.commons` | `consulo.bazel.base` | converted, builds |
| `intellij.bazel.projectview` (needs commons) | `consulo.bazel.base` | converted, builds |
| `intellij.bazel.backend` (runner, BEP, query, sync) | `consulo.bazel.base` | converted, builds (project model on the classic API) |
| `intellij.bazel.core`, `intellij.bazel.bazelisk` | `consulo.bazel.base` | converted, builds, sync tested (C++ project) |
| `misc/intellij.bazel.coverage` | `consulo.bazel.base` (package `org.jetbrains.bazel.run.coverage`) | converted onto `consulo.execution.coverage`, Run with Coverage tested (java_test); the LCOV report is read by the Consulo-only `consulo.bazel.execution.coverage.LcovSerializationUtils` (IntelliJ's LCOV reader is not in Consulo); `CoverageLoadErrorReporter` and `FailedCoverageLoadingResult` are missing, load problems are only logged |
| `java/intellij.bazel.java.{common,sync,core,coverage}` | `consulo.bazel.java` (`consulo-bazel-java/`, optional library, needs consulo.java) | converted, builds |
| `java/intellij.bazel.java.profiler` | `consulo.bazel.java` (package `com.intellij.bazel.java.profiler`) | converted onto the platform profiler API (`consulo.execution.profiler`) and consulo-java's JFR profiler, javac-checked; only `ProfilerRunWithScriptPathExtension` is registered. Not registered (`TODO consulo`): `BazelJavaProfilerStarterExtension` (consulo-java's `JavaProfilerStarter` has no extension point), `BazelJFRConfigurationExtension` (consulo-java's JFR extension, options and process are not exported), `BazelAsyncProfilerConfigurationExtension` (no async-profiler in Consulo) |
| `kotlin/*` | optional library, needs consulo-kotlin | not started |
| `golang/*` | `consulo.bazel.go` (`consulo-bazel-go/`, optional library, needs consulo.google.go) | converted, builds, sync, import resolution and run tested (needs GoPackage/GoImportResolver of consulo-google-go; Delve debugging through consulo-native-dev, its path mapping waits for a consulo-google-go API; Go coverage report parser) |
| `python/backend`, `python/intellij.bazel.python.common` | `consulo.bazel.python` (`consulo-bazel-python/`, optional library, needs consulo.python) | converted, builds, sync and import resolution tested; packaging API, module renderer, canonical path provider and stub files missing in consulo-python |
| `clion/*` | `consulo.bazel.cpp` (`consulo-bazel-cpp/`, optional library, needs consulo.cpp from consulo/consulo-cpp) | converted, builds, sync tested; per-file options via `CFileOptionsContributor`, debug via consulo-native-dev; no compiler detection or test frameworks |
| `rust/intellij.bazel.rust.sync` (Consulo-only, see [Rust](#rust)) | `consulo.bazel.rust` (`consulo-bazel-rust/`, optional library, needs consulo.rust) | restored and written, javac-checked; aspect output verified with Bazel 9.3.2; not yet built with Maven or tested in the IDE |

Order: `languages` -> `commons` -> `projectview` -> `backend` -> `core` -> language integrations.

### Rust

Upstream has no Rust support any more (removed in `4699fc65a` "[maintenance] remove rust support"; it was BSP-server only,
the IDE side lived in intellij-rust). `rust/intellij.bazel.rust.sync` is therefore **Consulo-only code**, not an upstream
folder, and is not touched by merges:

- `org.jetbrains.bazel.rust.protocol` and `org.jetbrains.bazel.rust.sync` are restored from `4699fc65a^`
  (`protocol/.../bsp/protocol/Rust*.kt` and `server/server/.../sync/languages/rust/*.kt`), ported faithfully and adapted to the
  aspect-based sync: `RustLanguagePlugin` reads `RustCrateInfo` (incl. `cfg_options` and the crate's toolchain) of our aspect
  fork into the snapshot data `RustModule` / `RustToolchain`, and `Module` stands in for the removed BSP server model.
- `org.jetbrains.bazel.rust.workspace` is new: `RustBazelWorkspaceImporter` (+`Factory`) creates a `.rust-workspace` module
  whose content roots are the Bazel packages of the crates, and attaches the crates to consulo-rust as an external Cargo project
  (`CargoProjectsService.attachExternalCargoProject`): `BazelRustWorkspace` computes upstream's `RustWorkspaceResult` from the
  snapshot and translates it into a `CargoWorkspaceData` plus an `ExternalRustToolchain` (rustc and sysroot of the crate's
  Rustc action, resolved against the execution root). consulo-rust does not persist external projects, so
  `RustCargoProjectStartupActivity` attaches the project again from the persisted snapshot when the project is opened.
- The `rust_info` aspect module only exists in our fork (see [Aspect](#aspect)).

## Replaced files

Upstream files that are **not converted** but reimplemented elsewhere. On every merge, review their diffs
and port the logic into the replacement.

| Upstream file | Reason | Replacement |
|---|---|---|
| `intellij.bazel.languages/resources/intellij.bazel.languages.xml` | Consulo registers extensions with annotations | `@ExtensionImpl` on each class, `consulo.bazel.languages.BazelLanguagesFileTypeFactory` for file types |
| `intellij.bazel.languages/resources/colorSchemes/Starlark{Default,Darcula}.xml` | Consulo color schemes are extended in code | `consulo.bazel.languages.Starlark{Default,Darcula}ColorSchemeExtender` |
| `intellij.bazel.languages/resources/messages/*.properties` (deleted) | Consulo localization | `resources/LOCALIZE-LIB/en_US/*.yaml` |

### Renamed files

Java needs every public top-level class in a file of its own name. Where upstream Kotlin files declare public
classes under other names, the Java file is renamed. Upstream diffs of the left column are ported into the right one.

| Upstream file | Java file(s) |
|---|---|
| `intellij.bazel.projectview/.../bazelrc/flags/Annotations.kt` | `Option.java`, `OptionEffectTag.java`, `OptionMetadataTag.java` |
| `intellij.bazel.projectview/.../bazelrc/flags/BazelFlag.kt` | `Flag.java` |
| `intellij.bazel.projectview/.../projectview/imports/Import.kt` | `ImportFactory.java` |
| `intellij.bazel.commons/.../commons/LanguageClass.kt` (the `LanguageClassService` part) | `LanguageClass.java` + `LanguageClassService.java` |
| `intellij.bazel.commons/.../sync/workspace/snapshot/WorkspaceSnapshot.kt` | `WorkspaceTargetKey.java`, `WorkspaceAspectIds.java`, `WorkspaceConfigurationId.java`, `WorkspaceConfiguration.java`, `WorkspaceConfigurationSummary.java`, `WorkspaceConfigurationFragment.java` |
| `intellij.bazel.backend/.../sync/workspace/snapshot/InMemoryFileToTargetMap.kt` (the `FileToTargetMap` interface) | `InMemoryFileToTargetMap.java` + `FileToTargetMap.java` |
| `intellij.bazel.core/.../buildTask/BazelProjectTaskRunner.kt` | `BazelProjectTaskRunner.java`, `AdditionalProjectTaskRunnerProvider.java`, `AdditionalProjectTask.java`, `TargetsToBuildProvider.java`, `DefaultTargetsToBuildProvider.java` |
| `intellij.bazel.core/.../ui/projectTree/BazelTreeStructureController.kt` (the `BazelTreeNodeType` enum) | `BazelTreeStructureController.java` + `BazelTreeNodeType.java` |
| `intellij.bazel.core/.../languages/starlark/bazel/BazelGlobalFunctions.kt` (the extension point) | `BazelGlobalFunctions.java` + `StarlarkGlobalFunctionProvider.java` |
| `intellij.bazel.core/.../run/state/GenericRunState.kt` | `GenericRunState.java`, `AbstractGenericRunState.java`, `GenericRunConfigurationState.java` |
| `intellij.bazel.core/.../run/state/GenericTestState.kt` | `GenericTestState.java`, `AbstractGenericTestState.java` |
| `intellij.bazel.core/.../startup/BazelStartupActivityTracker.kt` (the service) | `BazelStartupActivityTracker.java` + `BspConfigurationTrackerService.java` |
| `intellij.bazel.core/.../sync/action/ResyncFilesAction.kt` (`XmlRegistered`) | `ResyncFilesAction.java` + `ResyncFilesActionXmlRegistered.java` |
| `intellij.bazel.commons/.../settings/bazel/BazelApplicationSettings.kt` | `BazelApplicationSettings.java` + `BazelApplicationSettingsService.java` |
| `intellij.bazel.core/.../ui/status/BazelFileStatusDecorator.kt` (the refresher service) | `BazelFileStatusDecorator.java` + `BazelFileStatusRefresher.java` |
| `intellij.bazel.core/.../ui/widgets/BazelJumpToBuildFileAction.kt` (`XmlRegistered`) | `BazelJumpToBuildFileAction.java` + `BazelJumpToBuildFileActionXmlRegistered.java` |
| `intellij.bazel.core/.../ui/widgets/LabelSearchEverywhereContributor.kt` (`Factory`) | `LabelSearchEverywhereContributor.java` + `LabelSearchEverywhereContributorFactory.java` |
| `intellij.bazel.core/.../workspace/fileEvents/BazelFileEventProcessor.kt` | `BazelFileEventProcessor.java`, `BazelFileEventProcessorResult.java`, `DefaultBazelFileEventProcessor.java` |
| `java/intellij.bazel.java.sync/.../importer/JavaBazelWorkspaceImporter.kt` (`Factory`) | `JavaBazelWorkspaceImporter.java` + `JavaBazelWorkspaceImporterFactory.java` |
| `java/intellij.bazel.java.sync/.../importer/ScalaBazelWorkspaceImporter.kt` (`Factory`) | `ScalaBazelWorkspaceImporter.java` + `ScalaBazelWorkspaceImporterFactory.java` |
| `java/intellij.bazel.java.core/.../fastbuild/ToolchainInfoSyncHook.kt` (the service) | `ToolchainInfoSyncHook.java` + `JvmToolchainInfoService.java` |
| `java/intellij.bazel.java.core/.../jvm/run/JvmRunHandler.kt` (the provider) | `JvmRunHandler.java` + `JvmRunHandlerProvider.java` |
| `java/intellij.bazel.java.core/.../jvm/run/JvmTestHandler.kt` (the provider) | `JvmTestHandler.java` + `JvmTestHandlerProvider.java` |
| `python/backend/.../BazelPythonWorkspaceImporter.kt` (`Factory`) | `BazelPythonWorkspaceImporter.java` + `BazelPythonWorkspaceImporterFactory.java` |
| `python/intellij.bazel.python.common/.../run/PythonBazelRunHandler.kt` (the provider) | `PythonBazelRunHandler.java` + `PythonBazelRunHandlerProvider.java` |
| `python/intellij.bazel.python.common/.../run/PythonBazelTestHandler.kt` (the provider) | `PythonBazelTestHandler.java` + `PythonBazelTestHandlerProvider.java` |
| `golang/intellij.bazel.golang.common/.../sync/GoWorkspaceImporter.kt` (`Factory`) | `GoWorkspaceImporter.java` + `GoWorkspaceImporterFactory.java` |
| `golang/intellij.bazel.golang.common/.../debug/BazelGoRunHandler.kt` (the provider) | `BazelGoRunHandler.java` + `BazelGoRunHandlerProvider.java` |
| `golang/intellij.bazel.golang.common/.../debug/BazelGoTestHandler.kt` (the provider) | `BazelGoTestHandler.java` + `BazelGoTestHandlerProvider.java` |
| `clion/intellij.bazel.clion.common/.../workspace/CcWorkspaceImporter.kt` (`Factory`, data classes) | `CcWorkspaceImporter.java`, `CcWorkspaceImporterFactory.java`, `CcCompilerInfo.java`, `CcResolveConfiguration.java` |
| `clion/intellij.bazel.clion.core/.../run/CcRunHandler.kt` (the providers) | `CcRunHandler.java`, `CcRunHandlerProvider.java`, `CcTestHandlerProvider.java` |

`KnownFlags.kt` is generated upstream and was converted mechanically: re-run the conversion when it changes
(fields named after Java keywords - `long`, `null`, `short` - get a trailing underscore).

Expected entries: the `clion/*` files that import `com.jetbrains.cidr.*` (compiler resolver, OC workspace,
CIDR execution and debugger). In `consulo.bazel.cpp` they are mapped onto consulo-cpp (github.com/consulo/consulo-cpp:
`CCompileSettings`, `SdkDialect`, consulo-dap), with `TODO consulo` where consulo-cpp has no counterpart yet.

## Aspect

The Bazel aspect lives in a separate repository, [JetBrains/intellij-aspect](https://github.com/JetBrains/intellij-aspect),
pinned in `MODULE.bazel` (`git_override` of `intellij_aspect_sdk`; that upstream file stays unchanged). Our fork is
[consulo/bazel-aspects](https://github.com/consulo/bazel-aspects). `consulo-aspect-sdk` vendors its Starlark
sources and protos at the commit `MODULE.bazel` pins and ports its SDK library to Java; the procedure is in
`consulo-aspect-sdk/src/main/aspect/README.md`.

The fork publishes the module as **`consulo_aspect`** (upstream: `intellij_aspect`; the aspect symbol
`//config:aspect.bzl%intellij_aspect` and its output are the same). Sync supports both: if the root module of the
workspace has a `bazel_dep` on `consulo_aspect` (preferred) or `intellij_aspect`, the aspect of that module is used
(`@@<canonical>//config:aspect.bzl%intellij_aspect`) and nothing is materialized; otherwise the aspect is materialized
into `.bazelbsp` as upstream does. Consulo-only `consulo.bazel.aspect.BazelAspectModule` detects it from the Bzlmod
repo mapping; `BazelBspAspectsManager.deployIntelliJAspect`/`resolveAspectLabel` use it.

Rules for our changes in the fork:

- New languages (Zig, D, Rust, .NET) are new files in `modules/`.
- Rust: `modules/rust_info.bzl` (fork commits `2ee9e50`, merged with upstream `326afd3c` in `edbec1d`) reads the crate from
  rules_rust's `CrateInfo` and the toolchain from the crate's own `Rustc` action instead of toolchain resolution, so a
  workspace without Rust does not download the Rust toolchain. `consulo-aspect-sdk` vendors the fork commit.
- .NET: `modules/dotnet_info.bzl` (fork commit `da9dd3a`, on top of `edbec1d`) reads the assembly from rules_dotnet's
  `DotnetAssemblyCompileInfo` / `DotnetAssemblyRuntimeInfo` / `DotnetBinaryInfo`, the framework reference assemblies
  from the target's targeting pack and the `dotnet` executable from the target's own `CSharpCompile` / `FSharpCompile`
  action instead of toolchain resolution, so a workspace without .NET does not download the .NET SDK.
  `consulo-aspect-sdk` vendors the fork commit.
- Our new fields in `ide_info.proto` use field numbers **1000 and up**, so they never clash with upstream.
- Bug fixes are also sent upstream as pull requests, so our diff shrinks over time.
