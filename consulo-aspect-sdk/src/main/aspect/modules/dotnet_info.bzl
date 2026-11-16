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
# Consulo fork: .NET (rules_dotnet) support.

load("@rules_dotnet//dotnet/private:providers.bzl", "DotnetAssemblyCompileInfo", "DotnetAssemblyRuntimeInfo", "DotnetBinaryInfo", "DotnetTargetingPackInfo")
load("//common:artifact_location.bzl", "artifact_location")
load("//common:common.bzl", "intellij_common")
load("//common:dependencies.bzl", "intellij_deps")
load("//common:output_groups.bzl", "intellij_output_groups")
load("//common:provider.bzl", "intellij_provider")
load(":module.bzl", "intellij_module")

# Dependencies (besides `deps`, which are collected for every target) that contribute assemblies to a .NET target.
COMPILE_TIME_DEPS = ["exports"]

# Consulo fork: like modules/rust_info.bzl, the module must not request `@rules_dotnet//dotnet:toolchain_type`
# itself: a toolchain type of the aspect is resolved for every target the aspect is applied to, so every workspace
# depending on the aspect module (and through it on rules_dotnet) would download the .NET SDK. The dotnet executable
# is read from the compile action of the assembly instead, its toolchain is the one the target itself resolved.
COMPILE_MNEMONICS = ["CSharpCompile", "FSharpCompile", "VBCompile"]

# rule kind prefix -> language
LANGUAGES = {
    "csharp_": "csharp",
    "fsharp_": "fsharp",
    "vb_": "vb",
}

def _is_compile_rule(ctx):
    # rules_dotnet compile rules (csharp_*, fsharp_*) resolve a targeting pack, NuGet packages (`import_library`,
    # `import_dll`) and the assemblies of targeting packs only forward prebuilt assemblies
    return hasattr(ctx.rule.attr, "_targeting_pack") and hasattr(ctx.rule.attr, "_target_framework")

def _language(kind):
    for prefix, language in LANGUAGES.items():
        if kind.startswith(prefix):
            return language

    return ""

def _compile_action(target):
    for action in getattr(target, "actions", []):
        if action.mnemonic in COMPILE_MNEMONICS:
            return action

    return None

def _dotnet_path(action):
    """Returns the dotnet executable of a compile action: `<compiler wrapper> <dotnet> <csc.dll|fsc.dll> @<params>`."""
    if action == None:
        return None

    argv = action.argv or []
    if len(argv) < 2:
        return None

    return argv[1]

def _dependency_toolchain(ctx, dotnet_path):
    """Returns the dotnet executable already computed for a dependency built with the same SDK."""
    for dep in intellij_common.attr_as_label_list(ctx, "deps") + intellij_common.attr_as_label_list(ctx, "exports"):
        value = intellij_module.lookup_target(dep, intellij_provider.DotNetInfo)
        if value != None and value.dotnet_path == dotnet_path:
            return value.dotnet

    return None

def _toolchain_info(ctx, action):
    """Returns (dotnet File, SDK files to materialize) of a compile action."""
    dotnet_path = _dotnet_path(action)
    if not dotnet_path:
        return None, []

    # flattening the action inputs is linear in the transitive dependencies, do it once per SDK
    dotnet = _dependency_toolchain(ctx, dotnet_path)
    if dotnet != None:
        return dotnet, []

    sdk_dir = dotnet_path.rpartition("/")[0]

    dotnet = None
    files = []
    for file in action.inputs.to_list():
        if file.path == dotnet_path:
            dotnet = file
        elif sdk_dir and file.path.startswith(sdk_dir + "/"):
            files.append(file)

    if dotnet == None:
        return None, []

    return dotnet, [dotnet] + files

def _target_framework(ctx, assembly):
    # The resolved target framework is the value of the `_target_framework` build setting (a skylib
    # BuildSettingInfo). The aspect does not load bazel_skylib, the materialized aspect can only load repositories it
    # remaps (see sdk/lib/aspectchoice.kt). rules_dotnet writes the assembly to `<name>/<tfm>/<assembly>.dll`.
    if assembly != None:
        tfm = assembly.dirname.rpartition("/")[2]
        if tfm:
            return tfm

    target_frameworks = intellij_common.attr_as_string_list(ctx, "target_frameworks")
    return target_frameworks[0] if len(target_frameworks) == 1 else ""

def _framework_references(ctx):
    targeting_pack = getattr(ctx.rule.attr, "_targeting_pack", None)

    # attributes with a transition are lists
    if type(targeting_pack) == "list":
        targeting_pack = targeting_pack[0] if targeting_pack else None

    if targeting_pack == None or DotnetTargetingPackInfo not in targeting_pack:
        return []

    refs = []
    for compile_info in targeting_pack[DotnetTargetingPackInfo].assembly_compile_infos:
        refs.extend(compile_info.refs)

    return refs

def _package_references(ctx):
    """Returns the reference assemblies of direct dependencies that are not built by rules_dotnet compile rules."""
    refs = []
    for dep in intellij_common.attr_as_label_list(ctx, "deps") + intellij_common.attr_as_label_list(ctx, "exports"):
        if DotnetAssemblyCompileInfo not in dep:
            continue

        # compile rules have IDE info of their own and are dependencies of the target
        if intellij_module.lookup_target(dep, intellij_provider.DotNetInfo) != None:
            continue

        compile_info = dep[DotnetAssemblyCompileInfo]
        for file in compile_info.refs + compile_info.exports:
            if file not in refs:
                refs.append(file)

    return refs

def _implementation(target, ctx, attr):
    if DotnetAssemblyCompileInfo not in target or not _is_compile_rule(ctx):
        return None

    # assemblies built for the exec platform are tools (e.g. analyzers or code generators)
    if intellij_common.is_exec_configuration(ctx):
        return None

    compile_info = target[DotnetAssemblyCompileInfo]
    runtime_info = target[DotnetAssemblyRuntimeInfo] if DotnetAssemblyRuntimeInfo in target else None
    binary_info = target[DotnetBinaryInfo] if DotnetBinaryInfo in target else None

    assembly = None
    if binary_info != None:
        assembly = binary_info.dll
    elif runtime_info != None and runtime_info.libs:
        assembly = runtime_info.libs[0]

    pdb = runtime_info.pdbs[0] if runtime_info != None and runtime_info.pdbs else None

    dotnet, toolchain_files = _toolchain_info(ctx, _compile_action(target))

    sources = [f for f in getattr(ctx.rule.files, "srcs", [])]
    framework_references = _framework_references(ctx)
    package_references = _package_references(ctx)

    kind = ctx.rule.kind

    dotnet_struct = intellij_common.struct(
        assembly_name = compile_info.name,
        language = _language(kind),
        target_framework = _target_framework(ctx, assembly),
        sources = artifact_location.from_files(sources),
        framework_references = artifact_location.from_files(framework_references),
        package_references = artifact_location.from_files(package_references),
        defines = intellij_common.attr_as_string_list(ctx, "defines"),
        language_version = intellij_common.attr_as_str(ctx, "langversion"),
        nullable = intellij_common.attr_as_str(ctx, "nullable"),
        allow_unsafe_blocks = getattr(ctx.rule.attr, "allow_unsafe_blocks", False) == True,
        assembly = artifact_location.from_file(assembly),
        pdb = artifact_location.from_file(pdb),
        internals_visible_to = compile_info.internals_visible_to,
        project_sdk = compile_info.project_sdk or "",
        executable = binary_info != None or kind.endswith("_binary") or kind.endswith("_test"),
        dotnet = artifact_location.from_file(dotnet),
    )

    # the built assembly (the IDE resolves dependencies between targets from it), the reference assemblies the
    # target is compiled against, generated or external sources (e.g. the NUnit shim of `csharp_nunit_test`) and the
    # hermetic .NET SDK of the dotnet executable (reported once per SDK)
    outputs = [f for f in [assembly, pdb] if f != None]
    outputs.extend(framework_references)
    outputs.extend(package_references)
    outputs.extend(sources)
    outputs.extend(toolchain_files)

    return intellij_module.result(
        value = dotnet_struct,
        cross_target_internal_value = struct(
            dotnet = dotnet,
            dotnet_path = None if dotnet == None else dotnet.path,
        ),
        dependencies = {
            intellij_deps.COMPILE_TIME: intellij_deps.collect(ctx, COMPILE_TIME_DEPS),
        },
        outputs = intellij_output_groups.from_files(outputs),
    )

_aspect = intellij_module.aspect(
    provider = intellij_provider.DotNetInfo,
    implementation = _implementation,
    field = "dotnet_assembly_info",
)

module = intellij_module.define(
    file = "dotnet_info",
    aspect = _aspect,
    rulesets = ["DOTNET"],
)
