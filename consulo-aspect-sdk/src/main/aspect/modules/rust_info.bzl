# Copyright 2023-2025 JetBrains s.r.o.
# Copyright 2026 consulo.io
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Consulo fork: Rust (rules_rust) support.
# Derived from: https://github.com/JetBrains/hirschgarten/blob/24c400195/server/resources/aspects/rules/rust/rust_info.bzl
# (the Bazel BSP aspect; removed upstream in 4699fc65a "[maintenance] remove rust support").

load("@rules_rust//rust:rust_common.bzl", "BuildInfo", "CrateInfo", "TestCrateInfo")
load("//common:artifact_location.bzl", "artifact_location")
load("//common:common.bzl", "intellij_common")
load("//common:dependencies.bzl", "intellij_deps")
load("//common:output_groups.bzl", "intellij_output_groups")
load("//common:provider.bzl", "intellij_provider")
load(":module.bzl", "intellij_module")

# This is supposed to be enum, but Starlark does not support enums.
# See ide_info.proto:RustCrateLocation.
WORKSPACE_DIR = 0
EXEC_ROOT = 1

# Dependencies (besides `deps`, which are collected for every target) that contribute crates to a Rust target.
COMPILE_TIME_DEPS = ["proc_macro_deps"]

# Consulo fork: the module must not request `@rules_rust//rust:toolchain_type` itself. A toolchain type of the aspect
# (even an optional one) is resolved for every target the aspect is applied to, so every workspace depending on the
# aspect module (and through it on rules_rust, which registers its default toolchains) downloaded the rustc toolchain
# (~700 MB), with or without Rust code. The toolchain data is read from the `Rustc` action of the crate instead, its
# toolchain is the one the crate itself resolved. `ctx.rule.toolchains` does not help: without toolchain aspects
# (disabled up to Bazel 9, see intellij_common.aspect) it only exposes the label of the resolved toolchain.
RUSTC_MNEMONIC = "Rustc"

def _crate_info(target):
    if CrateInfo in target:
        return target[CrateInfo]

    # rust_test, rust_static_library and rust_shared_library do not advertise their crate as CrateInfo
    # (they must not be depended on), it is wrapped in TestCrateInfo instead.
    if TestCrateInfo in target:
        return target[TestCrateInfo].crate

    return None

def _build_info(target, ctx):
    if BuildInfo in target:
        return target[BuildInfo]

    # the BuildInfo of a crate is provided by its cargo_build_script dependency
    for dep in intellij_common.attr_as_label_list(ctx, "deps"):
        if BuildInfo in dep:
            return dep[BuildInfo]

    return None

def _collect_proc_macro_artifacts(crate_info):
    # The original aspect filtered the outputs of the crate's actions by the toolchain's `dylib_ext`. The output of a
    # proc-macro crate is that dylib, so no toolchain is needed.
    if crate_info.type != "proc-macro" or crate_info.output == None:
        return []

    return [crate_info.output]

def _rustc_action(target):
    for action in getattr(target, "actions", []):
        if action.mnemonic == RUSTC_MNEMONIC:
            return action

    return None

def _rustc_path(argv):
    """Returns the rustc executable of a Rustc action, the action runs it through rules_rust's process wrapper."""
    if "--" in argv:
        index = argv.index("--")
        return argv[index + 1] if index + 1 < len(argv) else None

    # bootstrap targets (`*_without_process_wrapper`) run rustc directly
    return argv[0] if argv else None

def _flag_value(argv, prefix):
    value = None
    for arg in argv:
        if arg.startswith(prefix):
            value = arg[len(prefix):]

    return value

def _cfg_options(argv):
    """Returns the `--cfg` options of a rustc command line, including the `feature="..."` ones."""
    cfgs = []
    for index, arg in enumerate(argv):
        if arg == "--cfg" and index + 1 < len(argv):
            cfgs.append(argv[index + 1])
        elif arg.startswith("--cfg="):
            cfgs.append(arg[len("--cfg="):])

    return cfgs

def _sysroot_of(argv, rustc_path):
    # rules_rust passes --sysroot when the toolchain generates its sysroot (the default)
    sysroot = _flag_value(argv, "--sysroot=")
    if sysroot:
        return sysroot

    # otherwise rustc is <sysroot>/bin/rustc of the toolchain repository
    parts = rustc_path.split("/")
    if len(parts) >= 3 and parts[-2] == "bin":
        return "/".join(parts[:-2])

    return None

def _dependency_toolchain(ctx, sysroot):
    """Returns the toolchain already computed for a dependency built with the same sysroot."""
    for dep in intellij_common.attr_as_label_list(ctx, "deps") + intellij_common.attr_as_label_list(ctx, "proc_macro_deps"):
        value = intellij_module.lookup_target(dep, intellij_provider.RustInfo)
        if value != None and value.sysroot == sysroot:
            return value.toolchain

    return None

def _toolchain_info(ctx, action):
    """Returns (toolchain struct, sysroot, toolchain files to materialize) of a Rustc action."""
    if action == None:
        return None, None, []

    argv = action.argv or []
    rustc_path = _rustc_path(argv)
    if not rustc_path:
        return None, None, []

    sysroot = _sysroot_of(argv, rustc_path)

    # flattening the action inputs is linear in the transitive dependencies, do it once per toolchain
    toolchain = _dependency_toolchain(ctx, sysroot)
    if toolchain != None:
        return toolchain, sysroot, []

    rustc = None
    cargo = None
    files = []
    for file in action.inputs.to_list():
        if file.path == rustc_path:
            rustc = file
        if sysroot and file.path.startswith(sysroot + "/"):
            files.append(file)
            if file.path in [sysroot + "/bin/cargo", sysroot + "/bin/cargo.exe"]:
                cargo = file

    if rustc != None and rustc not in files:
        files.append(rustc)

    toolchain = intellij_common.struct(
        rustc = artifact_location.from_file(rustc),
        cargo = artifact_location.from_file(cargo),
        sysroot = sysroot,
        target_triple = _flag_value(argv, "--target="),
        # `version` and `default_edition` are fields of rules_rust's ToolchainInfo, which is only available to rules
        # resolving the toolchain type (see RUSTC_MNEMONIC), the IDE asks rustc for its version
    )

    return toolchain, sysroot, files

def _implementation(target, ctx, attr):
    crate_info = _crate_info(target)
    if crate_info == None:
        return None

    # Crates built for the exec platform are tools (e.g. rules_rust's own tools or build script dependencies), except
    # proc macros: a crate depending on a proc macro reaches it in the exec configuration, and its dylib is what
    # expands the macros.
    if intellij_common.is_exec_configuration(ctx) and crate_info.type != "proc-macro":
        return None

    rustc_action = _rustc_action(target)
    toolchain, sysroot, toolchain_files = _toolchain_info(ctx, rustc_action)

    build_info = _build_info(target, ctx)

    crate_root_path = crate_info.root.path

    def is_same_crate(dep):
        dep_crate_info = _crate_info(dep)
        if dep_crate_info == None:
            return False

        return dep_crate_info.root.path == crate_root_path

    crate_is_from_workspace = not crate_info.root.path.startswith("external/")
    crate_is_generated = not crate_info.root.is_source
    crate_is_in_exec_root = not crate_is_from_workspace or crate_is_generated

    deps = [
        _crate_info(dep).root.path
        for dep in (intellij_common.attr_as_label_list(ctx, "deps") + intellij_common.attr_as_label_list(ctx, "proc_macro_deps"))
        if not is_same_crate(dep) and _crate_info(dep) != None
    ]

    proc_macro_artifacts = _collect_proc_macro_artifacts(crate_info)
    proc_macro_artifacts_paths = [artifact.path for artifact in proc_macro_artifacts]

    out_dir = None if build_info == None or build_info.out_dir == None else build_info.out_dir

    # To obtain crate root file, find directory corresponding to
    # `crate_location` and concatenate it with `crate_id` (relative crate root
    # file path); this has be done in the IDE
    # (see rules_rust/tools/rust_analyzer/rust_project.rs:write_rust_project).
    rust_crate_struct = intellij_common.struct(
        # The `crate-id` field must be unique. The deduplication has to be done
        # in the IDE
        # (see rules_rust/tools/rust_analyzer/aquery.rs:consolidate_crate_specs).
        crate_id = crate_info.root.path,
        location = EXEC_ROOT if crate_is_in_exec_root else WORKSPACE_DIR,
        from_workspace = crate_is_from_workspace,
        name = crate_info.name,
        kind = crate_info.type,
        edition = crate_info.edition,
        out_dir = "" if out_dir == None else out_dir.path,
        crate_features = intellij_common.attr_as_string_list(ctx, "crate_features"),
        dependencies_crate_ids = deps,
        crate_root = crate_info.root.path,
        version = intellij_common.attr_as_str(ctx, "version"),
        proc_macro_artifacts = proc_macro_artifacts_paths,
        toolchain = toolchain,
        cfg_options = [] if rustc_action == None else _cfg_options(rustc_action.argv or []),
    )

    # proc macros are expanded by the IDE from their built dylib, a build script output directory is included by the
    # crate sources (`include!(concat!(env!("OUT_DIR"), ...))`) and a generated crate root is a source of the crate
    outputs = list(proc_macro_artifacts)
    if out_dir != None:
        outputs.append(out_dir)
    if crate_is_generated:
        outputs.append(crate_info.root)

    # rustc, its libraries and the standard library rlibs of the toolchain sysroot (generated by rules_rust), so the
    # IDE can run rustc of the sysroot (version, target information); reported once per toolchain
    outputs.extend(toolchain_files)

    return intellij_module.result(
        value = rust_crate_struct,
        cross_target_internal_value = None if toolchain == None else struct(toolchain = toolchain, sysroot = sysroot),
        dependencies = {
            intellij_deps.COMPILE_TIME: intellij_deps.collect(ctx, COMPILE_TIME_DEPS),
        },
        outputs = intellij_output_groups.from_files(outputs),
    )

_aspect = intellij_module.aspect(
    provider = intellij_provider.RustInfo,
    implementation = _implementation,
    field = "rust_crate_info",
)

module = intellij_module.define(
    file = "rust_info",
    aspect = _aspect,
    rulesets = ["RUST"],
)
