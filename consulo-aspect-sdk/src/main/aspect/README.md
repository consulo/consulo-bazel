# Vendored IntelliJ aspect sources

The Starlark sources in this directory, and the protos in `../protobuf/sdk/proto`, are copied verbatim from our fork
[consulo/bazel-aspects](https://github.com/consulo/bazel-aspects) of
[JetBrains/intellij-aspect](https://github.com/JetBrains/intellij-aspect) at commit
`326afd3c208ce570ef86bb920df1da7e9fecfde9`: the commit the Bazel plugin pins in `MODULE.bazel`
(`git_override` of `intellij_aspect_sdk`). Always vendor the commit that `MODULE.bazel` pins, so the aspect output
matches what the ported plugin code reads.

Our fork adds Rust on top of that commit (fork commit `edbec1d`, "Merge intellij-aspect 326afd3c", over `2ee9e50`
"Add the Rust module"): `modules/rust_info.bzl`, `RustInfo` in `common/provider.bzl`, `rust_info` in the `MODULES` of
`intellij/modules.bzl`, the `RustCrateInfo` / `RustToolchainInfo` / `RustCrateLocation` messages and the
`rust_crate_info = 1000` field in `ide_info.proto`, and `Rules.RUST` in the SDK library (`aspectchoice.kt`). Vendor
the fork commit that merged the pinned upstream commit, not upstream itself, or the Rust module is lost.

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
# checkout of consulo/bazel-aspects at the commit pinned in MODULE.bazel
# (sync the fork with JetBrains/intellij-aspect first if it does not have that commit yet)
UPSTREAM=/path/to/bazel-aspects
cd consulo-aspect-sdk/src/main/aspect
rm -f common/*.bzl common/third_party/*.bzl intellij/*.bzl modules/*.bzl
cp "$UPSTREAM"/common/*.bzl common/
cp "$UPSTREAM"/common/third_party/*.bzl common/third_party/
cp "$UPSTREAM"/intellij/*.bzl intellij/
cp "$UPSTREAM"/modules/*.bzl modules/
cp "$UPSTREAM"/sdk/proto/aspect.proto "$UPSTREAM"/sdk/proto/common.proto "$UPSTREAM"/sdk/proto/ide_info.proto \
  ../protobuf/sdk/proto/
```

Then:

1. Check upstream's `BUILD`, `common/BUILD`, `intellij/BUILD`, `modules/BUILD` and `private/rules/aspect_lib.bzl`
   for changed globs or new packages in `archive_ide`, and update this directory and the `zip` task in
   `consulo-aspect-sdk/pom.xml` accordingly. New protos in `sdk/proto` are compiled by `protobuf-maven-plugin`
   without pom changes.
2. Update the commit hash above.
3. Port changes of upstream `sdk/lib/*.kt` and `private/lib/*.kt` into
   `consulo-aspect-sdk/src/main/java/com/intellij/aspect/lib` (`git diff <old> <new> -- sdk private/lib` in the fork).
4. Regenerate the bundled aspect definitions (see below).

## Bundled aspect definitions (upstream `aspect_config`)

Upstream generates, with `private/rules/aspect_config.bzl` from `config/BUILD`, one `AspectConfiguration` text proto
(`sdk/proto/aspect.proto`) per aspect and bundles it into the SDK jar (`sdk/lib/BUILD`, `java_resource_jar`).
`com.intellij.aspect.lib.Definition` (upstream `INTELLIJ_ASPECT` / `INTELLIJ_CC_DEBUG_ASPECT` of `sdk/lib/definition.kt`)
reads them. We keep hand-written equivalents in `src/main/resources/config`:

| Resource                      | Upstream target                    | `location`                                     | `modules`                              |
|-------------------------------|------------------------------------|------------------------------------------------|----------------------------------------|
| `aspect_ide.textproto`        | `//config:aspect_ide`              | `aspect.bzl%intellij_aspect`                   | `MODULES` + `rules_proto_info`         |
| `cc_debug_aspect.textproto`   | `//config:cc_debug_aspect`         | `cc_debug_aspect.bzl%intellij_cc_debug_aspect` | `cc_debug_info`, `platform_info`       |

`MODULES` is the list in `intellij/modules.bzl`. Check `config/BUILD` for changed or new `aspect_config` targets
(a new one bundled into the jar also needs a constant in `Definition` and an entry in `Config.writeAspectConfig`).

The `file` of each definition is `config/<location file>`, the `name` is the location name. For every module, take
its `module = intellij_module.define(...)` call in `modules/<file>.bzl` and write a `modules` entry with its
`file = "..."` and its `rulesets = [...]` (none when the argument is missing), keeping the order of the
`modules` list of the `aspect_config` target: `AspectDefinition.modulesFor` returns modules in this order, and that
is the order of the `modules` attribute of the generated aspect file.

A script that regenerates both files (run in `src/main/aspect`, `UPSTREAM` as above):

```bash
gen() { # <aspect file> <aspect name> <module file>...
  local file=$1 name=$2; shift 2
  printf '# consulo: hand-written equivalent of upstream //config:%s.textproto (private/rules/aspect_config.bzl),\n' "$OUT"
  printf '# see src/main/aspect/README.md\nfile: "%s"\nname: "%s"\n' "$file" "$name"
  for m in "$@"; do
    printf 'modules {\n  file: "%s"\n' "$m"
    awk '/^module = /,/^\)/' "$UPSTREAM/modules/$m.bzl" | grep -oP 'rulesets = \[\K[^]]*' | tr ',' '\n' \
      | grep -oP '"\K[^"]+' | while read -r r; do printf '  rulesets: "%s"\n' "$r"; done
    printf '}\n'
  done
}
files=$(sed -n '/^MODULES = \[/,/^\]/p' "$UPSTREAM"/intellij/modules.bzl | grep -oP '^\s+\K\w+(?=,)' | while read -r v; do
  grep -oP "load\(\"//modules:\K\w+(?=\.bzl\", $v = \"module\"\))" "$UPSTREAM"/intellij/modules.bzl; done)
OUT=aspect_ide gen config/aspect.bzl intellij_aspect $files rules_proto_info > ../resources/config/aspect_ide.textproto
OUT=cc_debug_aspect gen config/cc_debug_aspect.bzl intellij_cc_debug_aspect cc_debug_info platform_info \
  > ../resources/config/cc_debug_aspect.textproto
```
