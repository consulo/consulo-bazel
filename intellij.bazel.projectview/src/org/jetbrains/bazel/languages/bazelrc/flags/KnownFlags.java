package org.jetbrains.bazel.languages.bazelrc.flags;

// consulo: converted mechanically from upstream KnownFlags.kt; fields named after Java keywords get a trailing underscore
final class KnownFlags {
    private KnownFlags() {
    }

    @Option(
        name = "action_cache",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Dump action cache content.",
        valueHelp = "a boolean",
        commands = {"dump"}
    )
    public static final Flag.Boolean actionCache = new Flag.Boolean("actionCache");

    @Option(
        name = "action_cache_store_output_metadata",
        oldName = "experimental_action_cache_store_output_metadata",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "no-op",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean actionCacheStoreOutputMetadata = new Flag.Boolean("actionCacheStoreOutputMetadata");

    @Option(
        name = "action_env",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "\n" +
            "      Specifies the set of environment variables available to actions with target configuration. Variables can be\n" +
            "      either specified by name, in which case the value will be taken from the invocation environment, or by the\n" +
            "      name=value pair which sets the value independent of the invocation environment. This option can be used\n" +
            "      multiple times; for options given for the same variable, the latest wins, options for different variables\n" +
            "      accumulate.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment with an optional value part",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown actionEnv = new Flag.Unknown("actionEnv");

    @Option(
        name = "adb",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      adb binary to use for the 'mobile-install' command. If unspecified, the one in the Android SDK specified by\n" +
            "      the --android_sdk_channel command line option (or  the default SDK if --android_sdk_channel is not specified)\n" +
            "      is used.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"mobile-install"}
    )
    public static final Flag.Str adb = new Flag.Str("adb");

    @Option(
        name = "adb_arg",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "Extra arguments to pass to adb. Usually used to designate a device to install to.",
        valueHelp = "a string",
        commands = {"mobile-install"}
    )
    public static final Flag.Str adbArg = new Flag.Str("adbArg");

    @Option(
        name = "all",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      Fetches all external repositories necessary for building any target or repository. This is the default if no\n" +
            "      other flags and arguments are provided. Only works when --enable_bzlmod is on.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"fetch"}
    )
    public static final Flag.Boolean all = new Flag.Boolean("all");

    @Option(
        name = "allow_analysis_cache_discard",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      If discarding the analysis cache due to a change in the build system, setting this option to false will cause\n" +
            "      bazel to exit, rather than continuing with the build. This option has no effect when 'discard_analysis_cache'\n" +
            "      is also set.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean allowAnalysisCacheDiscard = new Flag.Boolean("allowAnalysisCacheDiscard");

    @Option(
        name = "allow_analysis_failures",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, an analysis failure of a rule target results in the target's propagation of an instance of\n" +
            "      AnalysisFailureInfo containing the error description, instead of resulting in a build failure.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean allowAnalysisFailures = new Flag.Boolean("allowAnalysisFailures");

    @Option(
        name = "allow_local_tests",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "If true, Bazel will allow local tests to run.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean allowLocalTests = new Flag.Boolean("allowLocalTests");

    @Option(
        name = "allow_unresolved_symlinks",
        oldName = "experimental_allow_unresolved_symlinks",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If enabled, Bazel allows the use of ctx.action.declare_symlink() and the use of ctx.actions.symlink() without\n" +
            "      a target file, thus allowing the creation of unresolved symlinks. Unresolved symlinks inside tree artifacts\n" +
            "      are not currently supported.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean allowUnresolvedSymlinks = new Flag.Boolean("allowUnresolvedSymlinks");

    @Option(
        name = "allow_yanked_versions",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Specified the module versions in the form of `<module1>@<version1>,<module2>@<version2>` that will be allowed\n" +
            "      in the resolved dependency graph even if they are declared yanked in the registry where they come from (if\n" +
            "      they are not coming from a NonRegistryOverride). Otherwise, yanked versions will cause the resolution to\n" +
            "      fail. You can also define allowed yanked version with the `BZLMOD_ALLOW_YANKED_VERSIONS` environment\n" +
            "      variable. You can disable this check by using the keyword 'all' (not recommended).\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str allowYankedVersions = new Flag.Str("allowYankedVersions");

    @Option(
        name = "allowed_cpu_values",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Allowed values for the --cpu flag.",
        valueHelp = "comma-separated set of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown allowedCpuValues = new Flag.Unknown("allowedCpuValues");

    @Option(
        name = "allowed_local_actions_regex",
        help = "\n" +
            "      A regex whitelist for action types which may be run locally. If unset, all actions are allowed to execute\n" +
            "      locally\n" +
            "    ",
        valueHelp = "a valid Java regular expression",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown allowedLocalActionsRegex = new Flag.Unknown("allowedLocalActionsRegex");

    @Option(
        name = "always_profile_slow_operations",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "Whether profiling slow operations is always turned on",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean alwaysProfileSlowOperations = new Flag.Boolean("alwaysProfileSlowOperations");

    @Option(
        name = "analysis_testing_deps_limit",
        defaultValue = "2000",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Sets the maximum number of transitive dependencies through a rule attribute with a for_analysis_testing\n" +
            "      configuration transition. Exceeding this limit will result in a rule error.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer analysisTestingDepsLimit = new Flag.Integer("analysisTestingDepsLimit");

    @Option(
        name = "analyze",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Execute the loading/analysis phase; this is the usual behaviour. Specifying --noanalyzecauses the build to\n" +
            "      stop before starting the loading/analysis phase, just doing target pattern parsing and returning zero iff\n" +
            "      that completed successfully; this mode is useful for testing.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean analyze = new Flag.Boolean("analyze");

    @Option(
        name = "android_compiler",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        help = "The Android target compiler.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str androidCompiler = new Flag.Str("androidCompiler");

    @Option(
        name = "android_cpu",
        defaultValue = "",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str androidCpu = new Flag.Str("androidCpu");

    @Option(
        name = "android_crosstool_top",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str androidCrosstoolTop = new Flag.Str("androidCrosstoolTop");

    @Option(
        name = "android_databinding_use_androidx",
        defaultValue = "true",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Generate AndroidX-compatible data-binding files. This is only used with databinding v2. This flag is a no-op.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean androidDatabindingUseAndroidx = new Flag.Boolean("androidDatabindingUseAndroidx");

    @Option(
        name = "android_databinding_use_v3_4_args",
        defaultValue = "true",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Use android databinding v2 with 3.4.0 argument. This flag is a no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean androidDatabindingUseV3_4Args = new Flag.Boolean("androidDatabindingUseV3_4Args");

    @Option(
        name = "android_dynamic_mode",
        defaultValue = "off",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Determines whether C++ deps of Android rules will be linked dynamically when a cc_binary does not explicitly\n" +
            "      create a shared library. 'default' means bazel will choose whether to link dynamically.  'fully' means all\n" +
            "      libraries will be linked dynamically. 'off' means that all libraries will be linked in mostly static mode.\n" +
            "    ",
        valueHelp = "off, default or fully",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf androidDynamicMode = new Flag.OneOf("androidDynamicMode");

    @Option(
        name = "android_fixed_resource_neverlinking",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If true, resources will properly not get propagated through neverlinked libraries. Otherwise, the old\n" +
            "      behavior of propagating those resources if no resource-related attributes are specified in the neverlink\n" +
            "      library will be preserved.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean androidFixedResourceNeverlinking = new Flag.Boolean("androidFixedResourceNeverlinking");

    @Option(
        name = "android_grte_top",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str androidGrteTop = new Flag.Str("androidGrteTop");

    @Option(
        name = "android_manifest_merger",
        defaultValue = "android",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        help = "\n" +
            "      Selects the manifest merger to use for android_binary rules. Flag to help thetransition to the Android\n" +
            "      manifest merger from the legacy merger.\n" +
            "    ",
        valueHelp = "legacy, android or force_android",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf androidManifestMerger = new Flag.OneOf("androidManifestMerger");

    @Option(
        name = "android_manifest_merger_order",
        defaultValue = "alphabetical",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Sets the order of manifests passed to the manifest merger for Android binaries. ALPHABETICAL means manifests\n" +
            "      are sorted by path relative to the execroot. ALPHABETICAL_BY_CONFIGURATION means manifests are sorted by\n" +
            "      paths relative to the configuration directory within the output directory. DEPENDENCY means manifests are\n" +
            "      ordered with each library's manifest coming before the manifests of its dependencies.\n" +
            "    ",
        valueHelp = "alphabetical, alphabetical_by_configuration or dependency",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf androidManifestMergerOrder = new Flag.OneOf("androidManifestMergerOrder");

    @Option(
        name = "android_migration_tag_check",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      If enabled, strict usage of the Starlark migration tag is enabled for android rules. Prefer using\n" +
            "      --incompatible_disable_native_android_rules.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean androidMigrationTagCheck = new Flag.Boolean("androidMigrationTagCheck");

    @Option(
        name = "android_platforms",
        defaultValue = "",
        effectTags = {
            OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        help = "\n" +
            "      Sets the platforms that android_binary targets use. If multiple platforms are specified, then the binary is a\n" +
            "      fat APKs, which contains native binaries for each specified target platform.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label androidPlatforms = new Flag.Label("androidPlatforms");

    @Option(
        name = "android_resource_shrinking",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Enables resource shrinking for android_binary APKs that use ProGuard.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean androidResourceShrinking = new Flag.Boolean("androidResourceShrinking");

    @Option(
        name = "android_sdk",
        defaultValue = "",
        help = "No-op",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str androidSdk = new Flag.Str("androidSdk");

    @Option(
        name = "announce",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated. No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean announce = new Flag.Boolean("announce");

    @Option(
        name = "announce_rc",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Whether to announce rc options.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean announceRc = new Flag.Boolean("announceRc");

    @Option(
        name = "apk_signing_method",
        defaultValue = "v1_v2",
        effectTags = {
            OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "Implementation to use to sign APKs",
        valueHelp = "v1, v2, v1_v2 or v4",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf apkSigningMethod = new Flag.OneOf("apkSigningMethod");

    @Option(
        name = "apple_crosstool_top",
        defaultValue = "@bazel_tools//tools/cpp:toolchain",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.CHANGES_INPUTS},
        help = "The label of the crosstool package to be used in Apple and Objc rules and their dependencies.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label appleCrosstoolTop = new Flag.Label("appleCrosstoolTop");

    @Option(
        name = "apple_generate_dsym",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES},
        help = "Whether to generate debug symbol(.dSYM) file(s).",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean appleGenerateDsym = new Flag.Boolean("appleGenerateDsym");

    @Option(
        name = "apple_platform_type",
        defaultValue = "macos",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      Don't set this value from the command line - it is derived from other flags and configuration transitions\n" +
            "      derived from rule attributes\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str applePlatformType = new Flag.Str("applePlatformType");

    @Option(
        name = "apple_platforms",
        defaultValue = "",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Comma-separated list of platforms to use when building Apple binaries.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label applePlatforms = new Flag.Label("applePlatforms");

    @Option(
        name = "apple_split_cpu",
        defaultValue = "",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      Don't set this value from the command line - it is derived from other flags and configuration transitions\n" +
            "      derived from rule attributes\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str appleSplitCpu = new Flag.Str("appleSplitCpu");

    @Option(
        name = "archived_tree_artifact_mnemonics_filter",
        defaultValue = "-.*",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Regex filter for mnemonics of actions for which we should create archived tree artifacts. This option is a\n" +
            "      no-op for actions which do not generate tree artifacts.\n" +
            "    ",
        valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown archivedTreeArtifactMnemonicsFilter = new Flag.Unknown("archivedTreeArtifactMnemonicsFilter");

    @Option(
        name = "aspect_deps",
        defaultValue = "conservative",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "\n" +
            "      How to resolve aspect dependencies when the output format is one of {xml,proto,record}. 'off' means no aspect\n" +
            "      dependencies are resolved, 'conservative' (the default) means all declared aspect dependencies are added\n" +
            "      regardless of whether they are given the rule class of direct dependencies, 'precise' means that only those\n" +
            "      aspects are added that are possibly active given the rule class of the direct dependencies. Note that precise\n" +
            "      mode requires loading other packages to evaluate a single target thus making it slower than the other modes.\n" +
            "      Also note that even precise mode is not completely precise: the decision whether to compute an aspect is\n" +
            "      decided in the analysis phase, which is not run during 'bazel query'.\n" +
            "    ",
        valueHelp = "off, conservative or precise",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.OneOf aspectDeps = new Flag.OneOf("aspectDeps");

    @Option(
        name = "aspects",
        allowMultiple = true,
        help = "\n" +
            "      Comma-separated list of aspects to be applied to top-level targets. In the list, if aspect some_aspect\n" +
            "      specifies required aspect providers via required_aspect_providers, some_aspect will run after every aspect\n" +
            "      that was mentioned before it in the aspects list whose advertised providers satisfy some_aspect required\n" +
            "      aspect providers. Moreover, some_aspect will run after all its required aspects specified by requires\n" +
            "      attribute. some_aspect will then have access to the values of those aspects' providers.\n" +
            "      <bzl-file-label>%<aspect_name>, for example '//tools:my_def.bzl%my_aspect', where 'my_aspect' is a top-level\n" +
            "      value from a file tools/my_def.bzl\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown aspects = new Flag.Unknown("aspects");

    @Option(
        name = "aspects_parameters",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Specifies the values of the command-line aspects parameters. Each parameter value is specified via\n" +
            "      <param_name>=<param_value>, for example 'my_param=my_val' where 'my_param' is a parameter of some aspect in\n" +
            "      --aspects list or required by an aspect in the list. This option can be used multiple times. However, it is\n" +
            "      not allowed to assign values to the same parameter more than once.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown aspectsParameters = new Flag.Unknown("aspectsParameters");

    @Option(
        name = "async",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      If true, output cleaning is asynchronous. When this command completes, it will be safe to execute new\n" +
            "      commands in the same client, even though the deletion may continue in the background.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"clean"}
    )
    public static final Flag.Boolean async = new Flag.Boolean("async");

    @Option(
        name = "attempt_to_print_relative_paths",
        oldName = "experimental_ui_attempt_to_print_relative_paths",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      When printing the location part of messages, attempt to use a path relative to the workspace directory or one\n" +
            "      of the directories specified by --package_path.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean attemptToPrintRelativePaths = new Flag.Boolean("attemptToPrintRelativePaths");

    @Option(
        name = "auto_cpu_environment_group",
        defaultValue = "",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "No-op",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str autoCpuEnvironmentGroup = new Flag.Str("autoCpuEnvironmentGroup");

    @Option(
        name = "auto_output_filter",
        defaultValue = "none",
        help = "\n" +
            "      If --output_filter is not specified, then the value for this option is used create a filter automatically.\n" +
            "      Allowed values are 'none' (filter nothing / show everything), 'all' (filter everything / show nothing),\n" +
            "      'packages' (include output from rules in packages mentioned on the Blaze command line), and 'subpackages'\n" +
            "      (like 'packages', but also include subpackages). For the 'packages' and 'subpackages' values //java/foo and\n" +
            "      //javatests/foo are treated as one package)'.\n" +
            "    ",
        valueHelp = "none, all, packages or subpackages",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf autoOutputFilter = new Flag.OneOf("autoOutputFilter");

    @Option(
        name = "autodetect_server_javabase",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      When --noautodetect_server_javabase is passed, Bazel does not fall back to the local JDK for running the\n" +
            "      bazel server and instead exits.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean autodetectServerJavabase = new Flag.Boolean("autodetectServerJavabase");

    @Option(
        name = "base_module",
        defaultValue = "<root>",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Specify a module relative to which the specified target repos will be interpreted.",
        valueHelp = "\n" +
            "      \"<root>\" for the root module; <module>@<version> for a specific version of a module; <module> for\n" +
            "      all versions of a module; @<name> for a repo with the given apparent name; or @@<name> for a repo\n" +
            "      with the given canonical name\n" +
            "    ",
        commands = {"mod"}
    )
    public static final Flag.Unknown baseModule = new Flag.Unknown("baseModule");

    @Option(
        name = "batch",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.DEPRECATED},
        help = "\n" +
            "      If set, Bazel will be run as just a client process without a server, instead of in the standard client/server\n" +
            "      mode. This is deprecated and will be removed, please prefer shutting down the server explicitly if you wish\n" +
            "      to avoid lingering servers.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean batch = new Flag.Boolean("batch");

    @Option(
        name = "batch_cpu_scheduling",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Only on Linux; use 'batch' CPU scheduling for Blaze. This policy is useful for workloads that are\n" +
            "      non-interactive, but do not want to lower their nice value. See 'man 2 sched_setscheduler'. If false, then\n" +
            "      Bazel does not perform a system call.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean batchCpuScheduling = new Flag.Boolean("batchCpuScheduling");

    @Option(
        name = "bazelrc",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      The location of the user .bazelrc file containing default values of Bazel options. /dev/null indicates that\n" +
            "      all further `--bazelrc`s will be ignored, which is useful to disable the search for a user rc file, e.g. in\n" +
            "      release builds.\n" +
            "      This option can also be specified multiple times.\n" +
            "      E.g. with `--bazelrc=x.rc --bazelrc=y.rc --bazelrc=/dev/null --bazelrc=z.rc`,\n" +
            "        1) x.rc and y.rc are read.\n" +
            "        2) z.rc is ignored due to the prior /dev/null.\n" +
            "      If unspecified, Bazel uses the first .bazelrc file it finds in the following two locations: the workspace\n" +
            "      directory, then the user's home directory.\n" +
            "      Note: command line options will always supersede any option in bazelrc.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"startup"}
    )
    public static final Flag.Str bazelrc = new Flag.Str("bazelrc");

    @Option(
        name = "bep_maximum_open_remote_upload_files",
        defaultValue = "-1",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Maximum number of open files allowed during BEP artifact upload.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer bepMaximumOpenRemoteUploadFiles = new Flag.Integer("bepMaximumOpenRemoteUploadFiles");

    @Option(
        name = "bes_backend",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specifies the build event service (BES) backend endpoint in the form [SCHEME://]HOST[:PORT]. The default is\n" +
            "      to disable BES uploads. Supported schemes are grpc and grpcs (grpc with TLS enabled). If no scheme is\n" +
            "      provided, Bazel assumes grpcs.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str besBackend = new Flag.Str("besBackend");

    @Option(
        name = "bes_best_effort",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean besBestEffort = new Flag.Boolean("besBestEffort");

    @Option(
        name = "bes_check_preceding_lifecycle_events",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Sets the field check_preceding_lifecycle_events_present on PublishBuildToolEventStreamRequest which tells BES\n" +
            "      to check whether it previously received InvocationAttemptStarted and BuildEnqueued events matching the\n" +
            "      current tool event.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean besCheckPrecedingLifecycleEvents = new Flag.Boolean("besCheckPrecedingLifecycleEvents");

    @Option(
        name = "bes_header",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specify a header in NAME=VALUE form that will be included in BES requests. Multiple headers can be passed by\n" +
            "      specifying the flag multiple times. Multiple values for the same name will be converted to a comma-separated\n" +
            "      list.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown besHeader = new Flag.Unknown("besHeader");

    @Option(
        name = "bes_instance_name",
        oldName = "project_id",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Specifies the instance name under which the BES will persist uploaded BEP. Defaults to null.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str besInstanceName = new Flag.Str("besInstanceName");

    @Option(
        name = "bes_keywords",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specifies a list of notification keywords to be added the default set of keywords published to BES\n" +
            "      (\"command_name=<command_name> \", \"protocol_name=BEP\"). Defaults to none.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown besKeywords = new Flag.Unknown("besKeywords");

    @Option(
        name = "bes_lifecycle_events",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Specifies whether to publish BES lifecycle events. (defaults to 'true').",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean besLifecycleEvents = new Flag.Boolean("besLifecycleEvents");

    @Option(
        name = "bes_oom_finish_upload_timeout",
        defaultValue = "10m",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      Specifies how long bazel should wait for the BES/BEP upload to complete while OOMing. This flag ensures\n" +
            "      termination when the JVM is severely GC thrashing and cannot make progress on any user thread.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration besOomFinishUploadTimeout = new Flag.Duration("besOomFinishUploadTimeout");

    @Option(
        name = "bes_outerr_buffer_size",
        defaultValue = "10240",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specifies the maximal size of stdout or stderr to be buffered in BEP, before it is reported as a progress\n" +
            "      event. Individual writes are still reported in a single event, even if larger than the specified value up to\n" +
            "      --bes_outerr_chunk_size.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer besOuterrBufferSize = new Flag.Integer("besOuterrBufferSize");

    @Option(
        name = "bes_outerr_chunk_size",
        defaultValue = "1048576",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Specifies the maximal size of stdout or stderr to be sent to BEP in a single message.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer besOuterrChunkSize = new Flag.Integer("besOuterrChunkSize");

    @Option(
        name = "bes_proxy",
        help = "\n" +
            "      Connect to the Build Event Service through a proxy. Currently this flag can only be used to configure a Unix\n" +
            "      domain socket (unix:/path/to/socket).\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str besProxy = new Flag.Str("besProxy");

    @Option(
        name = "bes_results_url",
        defaultValue = "",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Specifies the base URL where a user can view the information streamed to the BES backend. Bazel will output\n" +
            "      the URL appended by the invocation id to the terminal.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str besResultsUrl = new Flag.Str("besResultsUrl");

    @Option(
        name = "bes_system_keywords",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specifies a list of notification keywords to be included directly, without the \"user_keyword=\" prefix\n" +
            "      included for keywords supplied via --bes_keywords. Intended for Build service operators that set\n" +
            "      --bes_lifecycle_events=false and include keywords when calling PublishLifecycleEvent. Build service operators\n" +
            "      using this flag should prevent users from overriding the flag value.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown besSystemKeywords = new Flag.Unknown("besSystemKeywords");

    @Option(
        name = "bes_timeout",
        defaultValue = "0s",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specifies how long bazel should wait for the BES/BEP upload to complete after the build and tests have\n" +
            "      finished. A valid timeout is a natural number followed by a unit: Days (d), hours (h), minutes (m), seconds\n" +
            "      (s), and milliseconds (ms). The default value is '0' which means that there is no timeout.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration besTimeout = new Flag.Duration("besTimeout");

    @Option(
        name = "bes_upload_mode",
        defaultValue = "wait_for_upload_complete",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      Specifies whether the Build Event Service upload should block the build completion or should end the\n" +
            "      invocation immediately and finish the upload in the background. Either 'wait_for_upload_complete' (default),\n" +
            "      'nowait_for_upload_complete', or 'fully_async'.\n" +
            "    ",
        valueHelp = "wait_for_upload_complete, nowait_for_upload_complete or fully_async",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf besUploadMode = new Flag.OneOf("besUploadMode");

    @Option(
        name = "binary_path",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "The absolute path of the bazel binary.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str binaryPath = new Flag.Str("binaryPath");

    @Option(
        name = "block_for_lock",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      When --noblock_for_lock is passed, Bazel does not wait for a running command to complete, but instead exits\n" +
            "      immediately.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean blockForLock = new Flag.Boolean("blockForLock");

    @Option(
        name = "break_build_on_parallel_dex2oat_failure",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true dex2oat action failures will cause the build to break instead of executing dex2oat during test\n" +
            "      runtime.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean breakBuildOnParallelDex2oatFailure = new Flag.Boolean("breakBuildOnParallelDex2oatFailure");

    @Option(
        name = "build",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Execute the build; this is the usual behaviour. Specifying --nobuild causes the build to stop before\n" +
            "      executing the build actions, returning zero iff the package loading and analysis phases completed\n" +
            "      successfully; this mode is useful for testing those phases.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean build = new Flag.Boolean("build");

    @Option(
        name = "build_event_binary_file",
        oldName = "experimental_build_event_binary_file",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If non-empty, write a varint delimited binary representation of representation of the build event protocol to\n" +
            "      that file. This option implies --bes_upload_mode=wait_for_upload_complete.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str buildEventBinaryFile = new Flag.Str("buildEventBinaryFile");

    @Option(
        name = "build_event_binary_file_path_conversion",
        oldName = "experimental_build_event_binary_file_path_conversion",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Convert paths in the binary file representation of the build event protocol to more globally valid URIs\n" +
            "      whenever possible; if disabled, the file:// uri scheme will always be used\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean buildEventBinaryFilePathConversion = new Flag.Boolean("buildEventBinaryFilePathConversion");

    @Option(
        name = "build_event_binary_file_upload_mode",
        defaultValue = "wait_for_upload_complete",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      Specifies whether the Build Event Service upload for --build_event_binary_file should block the build\n" +
            "      completion or should end the invocation immediately and finish the upload in the background. Either\n" +
            "      'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 'fully_async'.\n" +
            "    ",
        valueHelp = "wait_for_upload_complete, nowait_for_upload_complete or fully_async",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf buildEventBinaryFileUploadMode = new Flag.OneOf("buildEventBinaryFileUploadMode");

    @Option(
        name = "build_event_json_file",
        oldName = "experimental_build_event_json_file",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If non-empty, write a JSON serialisation of the build event protocol to that file. This option implies\n" +
            "      --bes_upload_mode=wait_for_upload_complete.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str buildEventJsonFile = new Flag.Str("buildEventJsonFile");

    @Option(
        name = "build_event_json_file_path_conversion",
        oldName = "experimental_build_event_json_file_path_conversion",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Convert paths in the json file representation of the build event protocol to more globally valid URIs\n" +
            "      whenever possible; if disabled, the file:// uri scheme will always be used\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean buildEventJsonFilePathConversion = new Flag.Boolean("buildEventJsonFilePathConversion");

    @Option(
        name = "build_event_json_file_upload_mode",
        defaultValue = "wait_for_upload_complete",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      Specifies whether the Build Event Service upload for --build_event_json_file should block the build\n" +
            "      completion or should end the invocation immediately and finish the upload in the background. Either\n" +
            "      'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 'fully_async'.\n" +
            "    ",
        valueHelp = "wait_for_upload_complete, nowait_for_upload_complete or fully_async",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf buildEventJsonFileUploadMode = new Flag.OneOf("buildEventJsonFileUploadMode");

    @Option(
        name = "build_event_max_named_set_of_file_entries",
        defaultValue = "5000",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      The maximum number of entries for a single named_set_of_files event; values smaller than 2 are ignored and no\n" +
            "      event splitting is performed. This is intended for limiting the maximum event size in the build event\n" +
            "      protocol, although it does not directly control event size. The total event size is a function of the\n" +
            "      structure of the set as well as the file and uri lengths, which may in turn depend on the hash function.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer buildEventMaxNamedSetOfFileEntries = new Flag.Integer("buildEventMaxNamedSetOfFileEntries");

    @Option(
        name = "build_event_publish_all_actions",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Whether all actions should be published.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean buildEventPublishAllActions = new Flag.Boolean("buildEventPublishAllActions");

    @Option(
        name = "build_event_text_file",
        oldName = "experimental_build_event_text_file",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "If non-empty, write a textual representation of the build event protocol to that file",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str buildEventTextFile = new Flag.Str("buildEventTextFile");

    @Option(
        name = "build_event_text_file_path_conversion",
        oldName = "experimental_build_event_text_file_path_conversion",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Convert paths in the text file representation of the build event protocol to more globally valid URIs\n" +
            "      whenever possible; if disabled, the file:// uri scheme will always be used\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean buildEventTextFilePathConversion = new Flag.Boolean("buildEventTextFilePathConversion");

    @Option(
        name = "build_event_text_file_upload_mode",
        defaultValue = "wait_for_upload_complete",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      Specifies whether the Build Event Service upload for --build_event_text_file should block the build\n" +
            "      completion or should end the invocation immediately and finish the upload in the background. Either\n" +
            "      'wait_for_upload_complete' (default), 'nowait_for_upload_complete', or 'fully_async'.\n" +
            "    ",
        valueHelp = "wait_for_upload_complete, nowait_for_upload_complete or fully_async",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf buildEventTextFileUploadMode = new Flag.OneOf("buildEventTextFileUploadMode");

    @Option(
        name = "build_event_upload_max_retries",
        oldName = "experimental_build_event_upload_max_retries",
        defaultValue = "4",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "The maximum number of times Bazel should retry uploading a build event.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer buildEventUploadMaxRetries = new Flag.Integer("buildEventUploadMaxRetries");

    @Option(
        name = "build_manual_tests",
        defaultValue = "false",
        help = "\n" +
            "      Forces test targets tagged 'manual' to be built. 'manual' tests are excluded from processing. This option\n" +
            "      forces them to be built (but not executed).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean buildManualTests = new Flag.Boolean("buildManualTests");

    @Option(
        name = "build_metadata",
        allowMultiple = true,
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Custom key-value string pairs to supply in a build event.",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown buildMetadata = new Flag.Unknown("buildMetadata");

    @Option(
        name = "build_python_zip",
        defaultValue = "auto",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Build python executable zip; on on Windows, off on other platforms",
        valueHelp = "a tri-state (auto, yes, no)",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.TriState buildPythonZip = new Flag.TriState("buildPythonZip");

    @Option(
        name = "build_request_id",
        defaultValue = "",
        effectTags = {OptionEffectTag.BAZEL_MONITORING, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "Unique string identifier for the build being run.",
        valueHelp = "An optionally prefixed UUID. The last 36 characters will be verified as a UUID.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown buildRequestId = new Flag.Unknown("buildRequestId");

    @Option(
        name = "build_runfile_links",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If true, build runfiles symlink forests for all targets.  If false, write them only when required by a local\n" +
            "      action, test or run command.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean buildRunfileLinks = new Flag.Boolean("buildRunfileLinks");

    @Option(
        name = "build_runfile_manifests",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If true, write runfiles manifests for all targets. If false, omit them. Local tests will fail to run when\n" +
            "      false.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean buildRunfileManifests = new Flag.Boolean("buildRunfileManifests");

    @Option(
        name = "build_tag_filters",
        defaultValue = "",
        help = "\n" +
            "      Specifies a comma-separated list of tags. Each tag can be optionally preceded with '-' to specify excluded\n" +
            "      tags. Only those targets will be built that contain at least one included tag and do not contain any excluded\n" +
            "      tags. This option does not affect the set of tests executed with the 'test' command; those are be governed by\n" +
            "      the test filtering options, for example '--test_tag_filters'\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown buildTagFilters = new Flag.Unknown("buildTagFilters");

    @Option(
        name = "build_test_dwp",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If enabled, when building C++ tests statically and with fission the .dwp file  for the test binary will be\n" +
            "      automatically built as well.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean buildTestDwp = new Flag.Boolean("buildTestDwp");

    @Option(
        name = "build_tests_only",
        defaultValue = "false",
        help = "\n" +
            "      If specified, only *_test and test_suite rules will be built and other targets specified on the command line\n" +
            "      will be ignored. By default everything that was requested will be built.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean buildTestsOnly = new Flag.Boolean("buildTestsOnly");

    @Option(
        name = "bytecode_optimization_pass_actions",
        defaultValue = "1",
        help = "Do not use.",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer bytecodeOptimizationPassActions = new Flag.Integer("bytecodeOptimizationPassActions");

    @Option(
        name = "cache_computed_file_digests",
        defaultValue = "50000",
        help = "\n" +
            "      If greater than 0, configures Bazel to cache file digests in memory based on their metadata instead of\n" +
            "      recomputing the digests from disk every time they are needed. Setting this to 0 ensures correctness because\n" +
            "      not all file changes can be noted from file metadata. When not 0, the number indicates the size of the cache\n" +
            "      as the number of file digests to be cached.\n" +
            "    ",
        valueHelp = "a long integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown cacheComputedFileDigests = new Flag.Unknown("cacheComputedFileDigests");

    @Option(
        name = "cache_test_results",
        abbrev = 't',
        defaultValue = "auto",
        help = "\n" +
            "      If set to 'auto', Bazel reruns a test if and only if: (1) Bazel detects changes in the test or its\n" +
            "      dependencies, (2) the test is marked as external, (3) multiple test runs were requested with --runs_per_test,\n" +
            "      or(4) the test previously failed. If set to 'yes', Bazel caches all test results except for tests marked as\n" +
            "      external. If set to 'no', Bazel does not cache any test results.\n" +
            "    ",
        valueHelp = "a tri-state (auto, yes, no)",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.TriState cacheTestResults = new Flag.TriState("cacheTestResults");

    @Option(
        name = "canonicalize_policy",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Output the canonical policy, after expansion and filtering. To keep the output clean, the canonicalized\n" +
            "      command arguments will NOT be shown when this option is set to true. Note that the command specified by\n" +
            "      --for_command affects the filtered policy, and if none is specified, the default command is 'build'.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"canonicalize-flags"}
    )
    public static final Flag.Boolean canonicalizePolicy = new Flag.Boolean("canonicalizePolicy");

    @Option(
        name = "catalyst_cpus",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Comma-separated list of architectures for which to build Apple Catalyst binaries.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown catalystCpus = new Flag.Unknown("catalystCpus");

    @Option(
        name = "cc_output_directory_tag",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Specifies a suffix to be added to the configuration directory.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str ccOutputDirectoryTag = new Flag.Str("ccOutputDirectoryTag");

    @Option(
        name = "cc_proto_library_header_suffixes",
        defaultValue = ".pb.h",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Sets the suffixes of header files that a cc_proto_library creates.",
        valueHelp = "comma-separated set of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown ccProtoLibraryHeaderSuffixes = new Flag.Unknown("ccProtoLibraryHeaderSuffixes");

    @Option(
        name = "cc_proto_library_source_suffixes",
        defaultValue = ".pb.cc",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Sets the suffixes of source files that a cc_proto_library creates.",
        valueHelp = "comma-separated set of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown ccProtoLibrarySourceSuffixes = new Flag.Unknown("ccProtoLibrarySourceSuffixes");

    @Option(
        name = "charset",
        defaultValue = "utf8",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Chooses the character set to use for the tree. Only affects text output. Valid values are \"utf8\" or \"ascii\".\n" +
            "      Default is \"utf8\"\n" +
            "    ",
        valueHelp = "utf8 or ascii",
        commands = {"mod"}
    )
    public static final Flag.OneOf charset = new Flag.OneOf("charset");

    @Option(
        name = "check_bazel_compatibility",
        defaultValue = "error",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Check bazel version compatibility of Bazel modules. Valid values are `error` to escalate it to a resolution\n" +
            "      failure, `off` to disable the check, or `warning` to print a warning when mismatch detected.\n" +
            "    ",
        valueHelp = "error, warning or off",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf checkBazelCompatibility = new Flag.OneOf("checkBazelCompatibility");

    @Option(
        name = "check_bzl_visibility",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "If disabled, .bzl load visibility errors are demoted to warnings.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean checkBzlVisibility = new Flag.Boolean("checkBzlVisibility");

    @Option(
        name = "check_direct_dependencies",
        defaultValue = "warning",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Check if the direct `bazel_dep` dependencies declared in the root module are the same versions you get in the\n" +
            "      resolved dependency graph. Valid values are `off` to disable the check, `warning` to print a warning when\n" +
            "      mismatch detected or `error` to escalate it to a resolution failure.\n" +
            "    ",
        valueHelp = "off, warning or error",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf checkDirectDependencies = new Flag.OneOf("checkDirectDependencies");

    @Option(
        name = "check_fileset_dependencies_recursively",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean checkFilesetDependenciesRecursively = new Flag.Boolean("checkFilesetDependenciesRecursively");

    @Option(
        name = "check_licenses",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "\n" +
            "      Check that licensing constraints imposed by dependent packages do not conflict with distribution modes of the\n" +
            "      targets being built. By default, licenses are not checked.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean checkLicenses = new Flag.Boolean("checkLicenses");

    @Option(
        name = "check_tests_up_to_date",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Don't run tests, just check if they are up-to-date.  If all tests results are up-to-date, the testing\n" +
            "      completes successfully.  If any test needs to be built or executed, an error is reported and the testing\n" +
            "      fails.  This option implies --check_up_to_date behavior.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean checkTestsUpToDate = new Flag.Boolean("checkTestsUpToDate");

    @Option(
        name = "check_up_to_date",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Don't perform the build, just check if it is up-to-date.  If all targets are up-to-date, the build completes\n" +
            "      successfully.  If any step needs to be executed an error is reported and the build fails.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean checkUpToDate = new Flag.Boolean("checkUpToDate");

    @Option(
        name = "check_visibility",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "If disabled, visibility errors in target dependencies are demoted to warnings.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean checkVisibility = new Flag.Boolean("checkVisibility");

    @Option(
        name = "client_cwd",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "A system-generated parameter which specifies the client's working directory",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path clientCwd = new Flag.Path("clientCwd");

    @Option(
        name = "client_debug",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      If true, log debug information from the client to stderr. Changing this option will not cause the server to\n" +
            "      restart.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean clientDebug = new Flag.Boolean("clientDebug");

    @Option(
        name = "client_env",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "A system-generated parameter which specifies the client's environment",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown clientEnv = new Flag.Unknown("clientEnv");

    @Option(
        name = "collapse_duplicate_defines",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "no-op",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean collapseDuplicateDefines = new Flag.Boolean("collapseDuplicateDefines");

    @Option(
        name = "collect_code_coverage",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If specified, Bazel will instrument code (using offline instrumentation where possible) and will collect\n" +
            "      coverage information during tests. Only targets that  match --instrumentation_filter will be affected.\n" +
            "      Usually this option should  not be specified directly - 'bazel coverage' command should be used instead.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean collectCodeCoverage = new Flag.Boolean("collectCodeCoverage");

    @Option(
        name = "color",
        defaultValue = "auto",
        help = "Use terminal controls to colorize output.",
        valueHelp = "yes, no or auto",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf color = new Flag.OneOf("color");

    @Option(
        name = "combined_report",
        defaultValue = "none",
        help = "Specifies desired cumulative coverage report type. At this point only LCOV is supported.",
        valueHelp = "none or lcov",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf combinedReport = new Flag.OneOf("combinedReport");

    @Option(
        name = "command_port",
        defaultValue = "0",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "Port to start up the gRPC command server on. If 0, let the kernel choose.",
        valueHelp = "an integer",
        commands = {"startup"}
    )
    public static final Flag.Integer commandPort = new Flag.Integer("commandPort");

    @Option(
        name = "command_wait_time",
        defaultValue = "0",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "The time in ms a command had to wait on a busy Bazel server process.",
        valueHelp = "a long integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown commandWaitTime = new Flag.Unknown("commandWaitTime");

    @Option(
        name = "compilation_mode",
        abbrev = 'c',
        defaultValue = "fastbuild",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES},
        help = "Specify the mode the binary will be built in. Values: 'fastbuild', 'dbg', 'opt'.",
        valueHelp = "fastbuild, dbg or opt",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf compilationMode = new Flag.OneOf("compilationMode");

    @Option(
        name = "compile_one_dependency",
        defaultValue = "false",
        help = "\n" +
            "      Compile a single dependency of the argument files. This is useful for syntax checking source files in IDEs,\n" +
            "      for example, by rebuilding a single target that depends on the source file to detect errors as early as\n" +
            "      possible in the edit/build/test cycle. This argument affects the way all non-flag arguments are interpreted;\n" +
            "      instead of being targets to build they are source filenames.  For each source filename an arbitrary target\n" +
            "      that depends on it will be built.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean compileOneDependency = new Flag.Boolean("compileOneDependency");

    @Option(
        name = "compiler",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        help = "The C++ compiler to use for compiling the target.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str compiler = new Flag.Str("compiler");

    @Option(
        name = "config",
        allowMultiple = true,
        help = "\n" +
            "      Selects additional config sections from the rc files; for every <command>, it also pulls in the options from\n" +
            "      <command>:<config> if such a section exists; if this section doesn't exist in any .rc file, Blaze fails with\n" +
            "      an error. The config sections and flag combinations they are equivalent to are located in the tools/*.blazerc\n" +
            "      config files.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str config = new Flag.Str("config");

    @Option(
        name = "configure",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      Only fetches repositories marked as 'configure' for system-configuration purpose. Only works when\n" +
            "      --enable_bzlmod is on.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"fetch", "sync"}
    )
    public static final Flag.Boolean configure = new Flag.Boolean("configure");

    @Option(
        name = "conlyopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional option to pass to gcc when compiling C source files.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str conlyopt = new Flag.Str("conlyopt");

    @Option(
        name = "connect_timeout_secs",
        defaultValue = "30",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "The amount of time the client waits for each attempt to connect to the server",
        valueHelp = "an integer",
        commands = {"startup"}
    )
    public static final Flag.Integer connectTimeoutSecs = new Flag.Integer("connectTimeoutSecs");

    @Option(
        name = "consistent_labels",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      If enabled, every query command emits labels as if by the Starlark <code>str</code> function applied to a\n" +
            "      <code>Label</code> instance. This is useful for tools that need to match the output of different query\n" +
            "      commands and/or labels emitted by rules. If not enabled, output formatters are free to emit apparent\n" +
            "      repository names (relative to the main repository) instead to make the output more readable.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean consistentLabels = new Flag.Boolean("consistentLabels");

    @Option(
        name = "copt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional options to pass to gcc.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str copt = new Flag.Str("copt");

    @Option(
        name = "coverage_output_generator",
        defaultValue = "@bazel_tools//tools/test:lcov_merger",
        effectTags = {
            OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "\n" +
            "      Location of the binary that is used to postprocess raw coverage reports. This must currently be a filegroup\n" +
            "      that contains a single file, the binary. Defaults to '//tools/test:lcov_merger'.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label coverageOutputGenerator = new Flag.Label("coverageOutputGenerator");

    @Option(
        name = "coverage_report_generator",
        defaultValue = "@bazel_tools//tools/test:coverage_report_generator",
        effectTags = {
            OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "\n" +
            "      Location of the binary that is used to generate coverage reports. This must currently be a filegroup that\n" +
            "      contains a single file, the binary. Defaults to '//tools/test:coverage_report_generator'.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label coverageReportGenerator = new Flag.Label("coverageReportGenerator");

    @Option(
        name = "coverage_support",
        defaultValue = "@bazel_tools//tools/test:coverage_support",
        effectTags = {
            OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "\n" +
            "      Location of support files that are required on the inputs of every test action that collects code coverage.\n" +
            "      Defaults to '//tools/test:coverage_support'.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label coverageSupport = new Flag.Label("coverageSupport");

    @Option(
        name = "cpu",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "The target CPU.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str cpu = new Flag.Str("cpu");

    @Option(
        name = "credential_helper",
        oldName = "experimental_credential_helper",
        allowMultiple = true,
        help = "\n" +
            "      Configures a credential helper conforming to the <a\n" +
            "      href=\"https://github.com/EngFlow/credential-helper-spec\">Credential Helper Specification</a> to use for\n" +
            "      retrieving authorization credentials for  repository fetching, remote caching and execution, and the build\n" +
            "      event service.\n" +
            "      \n" +
            "      Credentials supplied by a helper take precedence over credentials supplied by `--google_default_credentials`,\n" +
            "      `--google_credentials`, a `.netrc` file, or the auth parameter to `repository_ctx.download()` and\n" +
            "      `repository_ctx.download_and_extract()`.\n" +
            "      \n" +
            "      May be specified multiple times to set up multiple helpers.\n" +
            "      \n" +
            "      See https://blog.engflow.com/2023/10/09/configuring-bazels-credential-helper/ for instructions.\n" +
            "    ",
        valueHelp = "\n" +
            "      Path to a credential helper. It may be absolute, relative to the PATH environment variable, or\n" +
            "      %workspace%-relative. The path be optionally prefixed by a scope  followed by an '='. The scope is\n" +
            "      a domain name, optionally with a single leading '*' wildcard component. A helper applies to URIs\n" +
            "      matching its scope, with more specific scopes preferred. If a helper has no scope, it applies to\n" +
            "      every URI.\n" +
            "    ",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown credentialHelper = new Flag.Unknown("credentialHelper");

    @Option(
        name = "credential_helper_cache_duration",
        oldName = "experimental_credential_helper_cache_duration",
        defaultValue = "30m",
        help = "\n" +
            "      The default duration for which credentials supplied by a credential helper are cached if the helper does not\n" +
            "      provide when the credentials expire.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration credentialHelperCacheDuration = new Flag.Duration("credentialHelperCacheDuration");

    @Option(
        name = "credential_helper_timeout",
        oldName = "experimental_credential_helper_timeout",
        defaultValue = "10s",
        help = "\n" +
            "      Configures the timeout for a credential helper.\n" +
            "      \n" +
            "      Credential helpers failing to respond within this timeout will fail the invocation.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration credentialHelperTimeout = new Flag.Duration("credentialHelperTimeout");

    @Option(
        name = "crosstool_top",
        defaultValue = "@bazel_tools//tools/cpp:toolchain",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op flag. Will be removed in a future release.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label crosstoolTop = new Flag.Label("crosstoolTop");

    @Option(
        name = "cs_fdo_absolute_path",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Use CSFDO profile information to optimize compilation. Specify the absolute path name of the zip file\n" +
            "      containing the profile file, a raw or an indexed LLVM profile file.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str csFdoAbsolutePath = new Flag.Str("csFdoAbsolutePath");

    @Option(
        name = "cs_fdo_instrument",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Generate binaries with context sensitive FDO instrumentation. With Clang/LLVM compiler, it also accepts the\n" +
            "      directory name under which the raw profile file(s) will be dumped at runtime.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str csFdoInstrument = new Flag.Str("csFdoInstrument");

    @Option(
        name = "cs_fdo_profile",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "The cs_fdo_profile representing the context sensitive profile to be used for optimization.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label csFdoProfile = new Flag.Label("csFdoProfile");

    @Option(
        name = "curses",
        defaultValue = "auto",
        help = "Use terminal cursor controls to minimize scrolling output.",
        valueHelp = "yes, no or auto",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf curses = new Flag.OneOf("curses");

    @Option(
        name = "custom_malloc",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specifies a custom malloc implementation. This setting overrides malloc attributes in build rules.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label customMalloc = new Flag.Label("customMalloc");

    @Option(
        name = "cxxopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional option to pass to gcc when compiling C++ source files.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str cxxopt = new Flag.Str("cxxopt");

    @Option(
        name = "cycles",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Points out dependency cycles inside the displayed tree, which are normally ignored by default.",
        valueHelp = "a boolean",
        commands = {"mod"}
    )
    public static final Flag.Boolean cycles = new Flag.Boolean("cycles");

    @Option(
        name = "debug_app",
        effectTags = {OptionEffectTag.EXECUTION},
        expandsTo = {"--start=DEBUG"},
        help = "Whether to wait for the debugger before starting the app.",
        valueHelp = "",
        commands = {"mobile-install"}
    )
    public static final Flag.Unknown debugApp = new Flag.Unknown("debugApp");

    @Option(
        name = "debug_spawn_scheduler",
        oldName = "experimental_debug_spawn_scheduler",
        defaultValue = "false",
        help = "",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean debugSpawnScheduler = new Flag.Boolean("debugSpawnScheduler");

    @Option(
        name = "default_override",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "",
        valueHelp = "blazerc option override",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown defaultOverride = new Flag.Unknown("defaultOverride");

    @Option(
        name = "default_system_javabase",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "\n" +
            "      The root of the user's local JDK install, to be used as the default target javabase and as a fall-back\n" +
            "      host_javabase. This is not the embedded JDK.\n" +
            "    ",
        valueHelp = "a path",
        commands = {"startup"}
    )
    public static final Flag.Path defaultSystemJavabase = new Flag.Path("defaultSystemJavabase");

    @Option(
        name = "default_test_resources",
        allowMultiple = true,
        help = "\n" +
            "      Override the default resources amount for tests. The expected format is <resource>=<value>. If a single\n" +
            "      positive number is specified as <value> it will override the default resources for all test sizes. If 4\n" +
            "      comma-separated numbers are specified, they will override the resource amount for respectively the small,\n" +
            "      medium, large, enormous test sizes. Values can also be HOST_RAM/HOST_CPU, optionally followed by [-|*]<float>\n" +
            "      (eg. memory=HOST_RAM*.1,HOST_RAM*.2,HOST_RAM*.3,HOST_RAM*.4). The default test resources specified by this\n" +
            "      flag are overridden by explicit resources specified in tags.\n" +
            "    ",
        valueHelp = "a resource name followed by equal and 1 float or 4 float, e.g memory=10,30,60,100",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown defaultTestResources = new Flag.Unknown("defaultTestResources");

    @Option(
        name = "default_visibility",
        defaultValue = "private",
        help = "Default visibility for packages that don't set it explicitly ('public' or 'private').",
        valueHelp = "default visibility",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Unknown defaultVisibility = new Flag.Unknown("defaultVisibility");

    @Option(
        name = "defer_param_files",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "This option is deprecated and has no effect and will be removed in the future.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean deferParamFiles = new Flag.Boolean("deferParamFiles");

    @Option(
        name = "define",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Each --define option specifies an assignment for a build variable. In case of multiple values for a variable,\n" +
            "      the last one wins.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown define = new Flag.Unknown("define");

    @Option(
        name = "deleted_packages",
        allowMultiple = true,
        help = "\n" +
            "      A comma-separated list of names of packages which the build system will consider non-existent, even if they\n" +
            "      are visible somewhere on the package path.\n" +
            "      Use this option when deleting a subpackage 'x/y' of an existing package 'x'.  For example, after deleting\n" +
            "      x/y/BUILD in your client, the build system may complain if it encounters a label '//x:y/z' if that is still\n" +
            "      provided by another package_path entry.  Specifying --deleted_packages x/y avoids this problem.\n" +
            "    ",
        valueHelp = "comma-separated list of package names",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Unknown deletedPackages = new Flag.Unknown("deletedPackages");

    @Option(
        name = "depth",
        defaultValue = "-1",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Maximum display depth of the dependency tree. A depth of 1 displays the direct dependencies, for example. For\n" +
            "      tree, path and all_paths it defaults to Integer.MAX_VALUE, while for deps and explain it defaults to 1 (only\n" +
            "      displays direct deps of the root besides the target leaves and their parents).\n" +
            "      \n" +
            "    ",
        valueHelp = "an integer",
        commands = {"mod"}
    )
    public static final Flag.Integer depth = new Flag.Integer("depth");

    @Option(
        name = "desugar_for_android",
        oldName = "experimental_desugar_for_android",
        defaultValue = "true",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        help = "Whether to desugar Java 8 bytecode before dexing.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean desugarForAndroid = new Flag.Boolean("desugarForAndroid");

    @Option(
        name = "desugar_java8_libs",
        oldName = "experimental_desugar_java8_libs",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Whether to include supported Java 8 libraries in apps for legacy devices.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean desugarJava8Libs = new Flag.Boolean("desugarJava8Libs");

    @Option(
        name = "device",
        defaultValue = "",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "The adb device serial number. If not specified, the first device will be used.",
        valueHelp = "a string",
        commands = {"mobile-install"}
    )
    public static final Flag.Str device = new Flag.Str("device");

    @Option(
        name = "device_debug_entitlements",
        defaultValue = "true",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      If set, and compilation mode is not 'opt', objc apps will include debug entitlements when signing.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean deviceDebugEntitlements = new Flag.Boolean("deviceDebugEntitlements");

    @Option(
        name = "dexopts_supported_in_dexmerger",
        defaultValue = "--minimal-main-dex,--set-max-idx-number",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "dx flags supported in tool that merges dex archives into final classes.dex files.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown dexoptsSupportedInDexmerger = new Flag.Unknown("dexoptsSupportedInDexmerger");

    @Option(
        name = "dexopts_supported_in_dexsharder",
        defaultValue = "--minimal-main-dex",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "dx flags supported in tool that groups classes for inclusion in final .dex files.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown dexoptsSupportedInDexsharder = new Flag.Unknown("dexoptsSupportedInDexsharder");

    @Option(
        name = "dexopts_supported_in_incremental_dexing",
        defaultValue = "--no-optimize,--no-locals",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "dx flags supported when converting Jars to dex archives incrementally.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown dexoptsSupportedInIncrementalDexing = new Flag.Unknown("dexoptsSupportedInIncrementalDexing");

    @Option(
        name = "digest_function",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "The hash function to use when computing file digests.",
        valueHelp = "hash function",
        commands = {"startup"}
    )
    public static final Flag.Unknown digestFunction = new Flag.Unknown("digestFunction");

    @Option(
        name = "directory_creation_cache",
        defaultValue = "maximumSize=100000",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Describes the cache used to store known regular directories as they're created. Parent directories of output\n" +
            "      files are created on-demand during action execution.\n" +
            "    ",
        valueHelp = "Converts to a CaffeineSpec, or null if the input is empty",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown directoryCreationCache = new Flag.Unknown("directoryCreationCache");

    @Option(
        name = "disallow_strict_deps_for_jpl",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "No-op, kept only for backwards compatibility.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean disallowStrictDepsForJpl = new Flag.Boolean("disallowStrictDepsForJpl");

    @Option(
        name = "discard_actions_after_execution",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "This option is deprecated and has no effect.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean discardActionsAfterExecution = new Flag.Boolean("discardActionsAfterExecution");

    @Option(
        name = "discard_analysis_cache",
        defaultValue = "false",
        help = "\n" +
            "      Discard the analysis cache immediately after the analysis phase completes. Reduces memory usage by ~10%, but\n" +
            "      makes further incremental builds slower.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean discardAnalysisCache = new Flag.Boolean("discardAnalysisCache");

    @Option(
        name = "disk_cache",
        help = "\n" +
            "      A path to a directory where Bazel can read and write actions and action outputs. If the directory does not\n" +
            "      exist, it will be created.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path diskCache = new Flag.Path("diskCache");

    @Option(
        name = "distdir",
        oldName = "experimental_distdir",
        allowMultiple = true,
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "Additional places to search for archives before accessing the network to download them.",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path distdir = new Flag.Path("distdir");

    @Option(
        name = "downloader_config",
        help = "\n" +
            "      Specify a file to configure the remote downloader with. This file consists of lines, each of which starts\n" +
            "      with a directive (`allow`, `block` or `rewrite`) followed by either a host name (for `allow` and `block`) or\n" +
            "      two patterns, one to match against, and one to use as a substitute URL, with back-references starting from\n" +
            "      `$1`. It is possible for multiple `rewrite` directives for the same URL to be give, and in this case\n" +
            "      multiple URLs will be returned.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path downloaderConfig = new Flag.Path("downloaderConfig");

    @Option(
        name = "dump",
        abbrev = 'd',
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      output full profile data dump either in human-readable 'text' format or script-friendly 'raw' format.\n" +
            "    ",
        valueHelp = "text or raw",
        commands = {"analyze-profile"}
    )
    public static final Flag.OneOf dump = new Flag.OneOf("dump");

    @Option(
        name = "dump_all",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "If set, dump all known configurations instead of just the ids.",
        valueHelp = "a boolean",
        commands = {"config"}
    )
    public static final Flag.Boolean dumpAll = new Flag.Boolean("dumpAll");

    @Option(
        name = "dynamic_local_execution_delay",
        oldName = "experimental_local_execution_delay",
        defaultValue = "1000",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      How many milliseconds should local execution be delayed, if remote execution was faster during a build at\n" +
            "      least once?\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer dynamicLocalExecutionDelay = new Flag.Integer("dynamicLocalExecutionDelay");

    @Option(
        name = "dynamic_local_strategy",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      The local strategies, in order, to use for the given mnemonic - the first applicable strategy is used. For\n" +
            "      example, `worker,sandboxed` runs actions that support persistent workers using the worker strategy, and all\n" +
            "      others using the sandboxed strategy. If no mnemonic is given, the list of strategies is used as the fallback\n" +
            "      for all mnemonics. The default fallback list is `worker,sandboxed`, or`worker,sandboxed,standalone` if\n" +
            "      `experimental_local_lockfree_output` is set. Takes [mnemonic=]local_strategy[,local_strategy,...]\n" +
            "    ",
        valueHelp = "a '[name=]value1[,..,valueN]' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown dynamicLocalStrategy = new Flag.Unknown("dynamicLocalStrategy");

    @Option(
        name = "dynamic_mode",
        defaultValue = "default",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Determines whether C++ binaries will be linked dynamically.  'default' means Bazel will choose whether to\n" +
            "      link dynamically.  'fully' means all libraries will be linked dynamically. 'off' means that all libraries\n" +
            "      will be linked in mostly static mode.\n" +
            "    ",
        valueHelp = "off, default or fully",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf dynamicMode = new Flag.OneOf("dynamicMode");

    @Option(
        name = "dynamic_remote_strategy",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      The remote strategies, in order, to use for the given mnemonic - the first applicable strategy is used. If no\n" +
            "      mnemonic is given, the list of strategies is used as the fallback for all mnemonics. The default fallback\n" +
            "      list is `remote`, so this flag usually does not need to be set explicitly. Takes\n" +
            "      [mnemonic=]remote_strategy[,remote_strategy,...]\n" +
            "    ",
        valueHelp = "a '[name=]value1[,..,valueN]' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown dynamicRemoteStrategy = new Flag.Unknown("dynamicRemoteStrategy");

    @Option(
        name = "emacs",
        defaultValue = "false",
        help = "\n" +
            "      A system-generated parameter which is true iff EMACS=t or INSIDE_EMACS is set in the environment of the\n" +
            "      client.  This option controls certain display features.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean emacs = new Flag.Boolean("emacs");

    @Option(
        name = "embed_label",
        defaultValue = "",
        help = "Embed source control revision or release label in binary",
        valueHelp = "a one-line string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown embedLabel = new Flag.Unknown("embedLabel");

    @Option(
        name = "emit_script_path_in_exec_request",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If true, emits the ExecRequest with --script_path file value and script contents instead of writing the\n" +
            "      script.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"run"}
    )
    public static final Flag.Boolean emitScriptPathInExecRequest = new Flag.Boolean("emitScriptPathInExecRequest");

    @Option(
        name = "enable_bzlmod",
        oldName = "experimental_enable_bzlmod",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If true, enables the Bzlmod dependency management system, taking precedence over WORKSPACE. See\n" +
            "      https://bazel.build/docs/bzlmod for more information.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean enableBzlmod = new Flag.Boolean("enableBzlmod");

    @Option(
        name = "enable_fdo_profile_absolute_path",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated. No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean enableFdoProfileAbsolutePath = new Flag.Boolean("enableFdoProfileAbsolutePath");

    @Option(
        name = "enable_platform_specific_config",
        defaultValue = "false",
        help = "\n" +
            "      If true, Bazel picks up host-OS-specific config lines from bazelrc files. For example, if the host OS is\n" +
            "      Linux and you run bazel build, Bazel picks up lines starting with build:linux. Supported OS identifiers are\n" +
            "      linux, macos, windows, freebsd, and openbsd. Enabling this flag is equivalent to using --config=linux on\n" +
            "      Linux, --config=windows on Windows, etc.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean enablePlatformSpecificConfig = new Flag.Boolean("enablePlatformSpecificConfig");

    @Option(
        name = "enable_propeller_optimize_absolute_paths",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "If set, any use of absolute paths for propeller optimize will raise an error.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean enablePropellerOptimizeAbsolutePaths = new Flag.Boolean("enablePropellerOptimizeAbsolutePaths");

    @Option(
        name = "enable_remaining_fdo_absolute_paths",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "If set, any use of absolute paths for FDO will raise an error.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean enableRemainingFdoAbsolutePaths = new Flag.Boolean("enableRemainingFdoAbsolutePaths");

    @Option(
        name = "enable_runfiles",
        defaultValue = "auto",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Enable runfiles symlink tree; By default, it's off on Windows, on on other platforms.",
        valueHelp = "a tri-state (auto, yes, no)",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.TriState enableRunfiles = new Flag.TriState("enableRunfiles");

    @Option(
        name = "enable_workspace",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If true, enables the legacy WORKSPACE system for external dependencies. See\n" +
            "      https://bazel.build/external/overview for more information.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean enableWorkspace = new Flag.Boolean("enableWorkspace");

    @Option(
        name = "enforce_constraints",
        oldName = "experimental_enforce_constraints",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "\n" +
            "      Checks the environments each target is compatible with and reports errors if any target has dependencies that\n" +
            "      don't support the same environments\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean enforceConstraints = new Flag.Boolean("enforceConstraints");

    @Option(
        name = "enforce_proguard_file_extension",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      If enabled, requires that ProGuard configuration files outside of third_party/ use the *.pgcfg file extension.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean enforceProguardFileExtension = new Flag.Boolean("enforceProguardFileExtension");

    @Option(
        name = "enforce_project_configs",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, interactive builds may only pass the --scl_config build flag; they may not use any other build\n" +
            "      flags. --scl_config must be set to an officially suported project configuration. Supported configurations are\n" +
            "      defined in the target's PROJECT.scl, which can be found by walking up the target's packagge path. See\n" +
            "      b/324126745.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean enforceProjectConfigs = new Flag.Boolean("enforceProjectConfigs");

    @Option(
        name = "enforce_transitive_configs_for_config_feature_flag",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean enforceTransitiveConfigsForConfigFeatureFlag = new Flag.Boolean("enforceTransitiveConfigsForConfigFeatureFlag");

    @Option(
        name = "execution_log_binary_file",
        help = "\n" +
            "      Log the executed spawns into this file as length-delimited SpawnExec protos, according to\n" +
            "      src/main/protobuf/spawn.proto. Prefer --execution_log_compact_file, which is significantly smaller and\n" +
            "      cheaper to produce. Related flags: --execution_log_compact_file (compact format; mutually exclusive),\n" +
            "      --execution_log_json_file (text JSON format; mutually exclusive), --execution_log_sort (whether to sort the\n" +
            "      execution log), --subcommands (for displaying subcommands in terminal output).\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Path executionLogBinaryFile = new Flag.Path("executionLogBinaryFile");

    @Option(
        name = "execution_log_compact_file",
        oldName = "experimental_execution_log_compact_file",
        help = "\n" +
            "      Log the executed spawns into this file as length-delimited ExecLogEntry protos, according to\n" +
            "      src/main/protobuf/spawn.proto. The entire file is zstd compressed. Related flags: --execution_log_binary_file\n" +
            "      (binary protobuf format; mutually exclusive), --execution_log_json_file (text JSON format; mutually\n" +
            "      exclusive), --subcommands (for displaying subcommands in terminal output).\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Path executionLogCompactFile = new Flag.Path("executionLogCompactFile");

    @Option(
        name = "execution_log_json_file",
        help = "\n" +
            "      Log the executed spawns into this file as newline-delimited JSON representations of SpawnExec protos,\n" +
            "      according to src/main/protobuf/spawn.proto. Prefer --execution_log_compact_file, which is significantly\n" +
            "      smaller and cheaper to produce. Related flags: --execution_log_compact_file (compact format; mutually\n" +
            "      exclusive), --execution_log_binary_file (binary protobuf format; mutually exclusive), --execution_log_sort\n" +
            "      (whether to sort the execution log), --subcommands (for displaying subcommands in terminal output).\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Path executionLogJsonFile = new Flag.Path("executionLogJsonFile");

    @Option(
        name = "execution_log_sort",
        defaultValue = "true",
        help = "\n" +
            "      Whether to sort the execution log, making it easier to compare logs across invocations. Set to false to avoid\n" +
            "      potentially significant CPU and memory usage at the end of the invocation, at the cost of producing the log\n" +
            "      in nondeterministic execution order. Only applies to the binary and JSON formats; the compact format is never\n" +
            "      sorted.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean executionLogSort = new Flag.Boolean("executionLogSort");

    @Option(
        name = "expand_test_suites",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Expand test_suite targets into their constituent tests before analysis. When this flag is turned on (the\n" +
            "      default), negative target patterns will apply to the tests belonging to the test suite, otherwise they will\n" +
            "      not. Turning off this flag is useful when top-level aspects are applied at command line: then they can\n" +
            "      analyze test_suite targets.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean expandTestSuites = new Flag.Boolean("expandTestSuites");

    @Option(
        name = "experimental_action_listener",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Deprecated in favor of aspects. Use action_listener to attach an extra_action to existing build actions.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label experimentalActionListener = new Flag.Label("experimentalActionListener");

    @Option(
        name = "experimental_action_resource_set",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalActionResourceSet = new Flag.Boolean("experimentalActionResourceSet");

    @Option(
        name = "experimental_add_exec_constraints_to_targets",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      List of comma-separated regular expressions, each optionally prefixed by - (negative expression), assigned\n" +
            "      (=) to a list of comma-separated constraint value targets. If a target matches no negative expression and at\n" +
            "      least one positive expression its toolchain resolution will be performed as if it had declared the constraint\n" +
            "      values as execution constraints. Example: //demo,-test=@platforms//cpus:x86_64 will add 'x86_64' to any\n" +
            "      target under //demo except for those whose name contains 'test'.\n" +
            "    ",
        valueHelp = "a '<RegexFilter>=<label1>[,<label2>,...]' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalAddExecConstraintsToTargets = new Flag.Unknown("experimentalAddExecConstraintsToTargets");

    @Option(
        name = "experimental_add_test_support_to_compile_time_deps",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Flag to help transition away from adding test support libraries to the compile-time deps of Java test rules.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAddTestSupportToCompileTimeDeps = new Flag.Boolean("experimentalAddTestSupportToCompileTimeDeps");

    @Option(
        name = "experimental_allow_android_library_deps_without_srcs",
        defaultValue = "false",
        help = "No-op. Kept here for backwards compatibility.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAllowAndroidLibraryDepsWithoutSrcs = new Flag.Boolean("experimentalAllowAndroidLibraryDepsWithoutSrcs");

    @Option(
        name = "experimental_allow_project_files",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL, OptionMetadataTag.HIDDEN},
        help = "Enable processing of +<file> parameters.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalAllowProjectFiles = new Flag.Boolean("experimentalAllowProjectFiles");

    @Option(
        name = "experimental_allow_runtime_deps_on_neverlink",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "No-op, kept only for backwards compatibility",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAllowRuntimeDepsOnNeverlink = new Flag.Boolean("experimentalAllowRuntimeDepsOnNeverlink");

    @Option(
        name = "experimental_always_filter_duplicate_classes_from_android_test",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled and the android_test defines a binary_under_test, the class filterering applied to the test's\n" +
            "      deploy jar will always filter duplicate classes based solely on matching class and package name, ignoring\n" +
            "      hash values.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAlwaysFilterDuplicateClassesFromAndroidTest =
        new Flag.Boolean("experimentalAlwaysFilterDuplicateClassesFromAndroidTest");

    @Option(
        name = "experimental_android_assume_minsdkversion",
        defaultValue = "false",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      When enabled, the minSdkVersion is parsed from the merged AndroidManifest and used to instruct Proguard on\n" +
            "      valid Android build versions.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidAssumeMinsdkversion = new Flag.Boolean("experimentalAndroidAssumeMinsdkversion");

    @Option(
        name = "experimental_android_compress_java_resources",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Compress Java resources in APKs",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidCompressJavaResources = new Flag.Boolean("experimentalAndroidCompressJavaResources");

    @Option(
        name = "experimental_android_databinding_v2",
        defaultValue = "true",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Use android databinding v2. This flag is a no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidDatabindingV2 = new Flag.Boolean("experimentalAndroidDatabindingV2");

    @Option(
        name = "experimental_android_library_exports_manifest_default",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "The default value of the exports_manifest attribute on android_library.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidLibraryExportsManifestDefault = new Flag.Boolean("experimentalAndroidLibraryExportsManifestDefault");

    @Option(
        name = "experimental_android_resource_cycle_shrinking",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Enables more shrinking of code and resources by instructing AAPT2 to emit conditional Proguard keep rules.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidResourceCycleShrinking = new Flag.Boolean("experimentalAndroidResourceCycleShrinking");

    @Option(
        name = "experimental_android_resource_name_obfuscation",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Enables obfuscation of resource names within android_binary APKs.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidResourceNameObfuscation = new Flag.Boolean("experimentalAndroidResourceNameObfuscation");

    @Option(
        name = "experimental_android_resource_path_shortening",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Enables shortening of resource file paths within android_binary APKs.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidResourcePathShortening = new Flag.Boolean("experimentalAndroidResourcePathShortening");

    @Option(
        name = "experimental_android_resource_shrinking",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Enables resource shrinking for android_binary APKs that use ProGuard.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidResourceShrinking = new Flag.Boolean("experimentalAndroidResourceShrinking");

    @Option(
        name = "experimental_android_rewrite_dexes_with_rex",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "use rex tool to rewrite dex files",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidRewriteDexesWithRex = new Flag.Boolean("experimentalAndroidRewriteDexesWithRex");

    @Option(
        name = "experimental_android_use_parallel_dex2oat",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Use dex2oat in parallel to possibly speed up android_test.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAndroidUseParallelDex2oat = new Flag.Boolean("experimentalAndroidUseParallelDex2oat");

    @Option(
        name = "experimental_announce_profile_path",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalAnnounceProfilePath = new Flag.Boolean("experimentalAnnounceProfilePath");

    @Option(
        name = "experimental_aquery_dump_after_build_format",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Writes the state of Skyframe (which includes previous invocations on this blaze instance as well) after a\n" +
            "      build. Output is streamed remotely unless local output is requested with\n" +
            "      --experimental_aquery_dump_after_build_output_file.  Does not honor aquery flags for --include_*, but uses\n" +
            "      the same defaults, except for --include_commandline=false. Possible output formats:\n" +
            "      proto|streamed_proto|textproto|jsonproto. Using this will disable Skymeld.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalAqueryDumpAfterBuildFormat = new Flag.Str("experimentalAqueryDumpAfterBuildFormat");

    @Option(
        name = "experimental_aquery_dump_after_build_output_file",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specify the output file for the aquery dump after a build. Use in conjunction with\n" +
            "      --experimental_aquery_dump_after_build_format. The path provided is relative to Bazel's output base, unless\n" +
            "      it's an absolute path. Using this will disable Skymeld.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Path experimentalAqueryDumpAfterBuildOutputFile = new Flag.Path("experimentalAqueryDumpAfterBuildOutputFile");

    @Option(
        name = "experimental_async_execution",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, Bazel is allowed to run action in a virtual thread. The number of actions in flight is still\n" +
            "      capped with --jobs.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalAsyncExecution = new Flag.Boolean("experimentalAsyncExecution");

    @Option(
        name = "experimental_bep_target_summary",
        defaultValue = "false",
        help = "Whether to publish TargetSummary events.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalBepTargetSummary = new Flag.Boolean("experimentalBepTargetSummary");

    @Option(
        name = "experimental_build_event_expand_filesets",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "If true, expand Filesets in the BEP when presenting output files.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalBuildEventExpandFilesets = new Flag.Boolean("experimentalBuildEventExpandFilesets");

    @Option(
        name = "experimental_build_event_fully_resolve_fileset_symlinks",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If true, fully resolve relative Fileset symlinks in the BEP when presenting output files. Requires\n" +
            "      --experimental_build_event_expand_filesets.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalBuildEventFullyResolveFilesetSymlinks = new Flag.Boolean("experimentalBuildEventFullyResolveFilesetSymlinks");

    @Option(
        name = "experimental_build_event_output_group_mode",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specify how an output group's files will be represented in TargetComplete/AspectComplete BEP events. Values\n" +
            "      are an assignment of an output group name to one of 'NAMED_SET_OF_FILES_ONLY', 'INLINE_ONLY', or 'BOTH'. The\n" +
            "      default value is 'NAMED_SET_OF_FILES_ONLY'. If an output group is repeated, the final value to appear is\n" +
            "      used. The default value sets the mode for coverage artifacts to BOTH:\n" +
            "      --experimental_build_event_output_group_mode=baseline.lcov=both\n" +
            "    ",
        valueHelp = "an output group name followed by an OutputGroupFileMode, e.g. default=both",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalBuildEventOutputGroupMode = new Flag.Unknown("experimentalBuildEventOutputGroupMode");

    @Option(
        name = "experimental_build_event_upload_retry_minimum_delay",
        defaultValue = "1s",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "Initial, minimum delay for exponential backoff retries when BEP upload fails. (exponent: 1.6)",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration experimentalBuildEventUploadRetryMinimumDelay = new Flag.Duration("experimentalBuildEventUploadRetryMinimumDelay");

    @Option(
        name = "experimental_build_event_upload_strategy",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Selects how to upload artifacts referenced in the build event protocol.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str experimentalBuildEventUploadStrategy = new Flag.Str("experimentalBuildEventUploadStrategy");

    @Option(
        name = "experimental_build_transitive_python_runfiles",
        oldName = "incompatible_build_transitive_python_runfiles",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalBuildTransitivePythonRunfiles = new Flag.Boolean("experimentalBuildTransitivePythonRunfiles");

    @Option(
        name = "experimental_builtins_bzl_path",
        defaultValue = "%bundled%",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      This flag tells Bazel how to find the \"@_builtins\" .bzl files that govern how predeclared symbols for BUILD\n" +
            "      and .bzl files are defined. This flag is only intended for Bazel developers, to help when writing @_builtins\n" +
            "      .bzl code. Ordinarily this value is set to \"%bundled%\", which means to use the builtins_bzl/ directory\n" +
            "      packaged in the Bazel binary. However, it can be set to the path (relative to the root of the current\n" +
            "      workspace) of an alternate builtins_bzl/ directory, such as one in a Bazel source tree workspace. A literal\n" +
            "      value of \"%workspace%\" is equivalent to the relative package path of builtins_bzl/ within a Bazel source\n" +
            "      tree; this should only be used when running Bazel within its own source tree. Finally, a value of the empty\n" +
            "      string disables the builtins injection mechanism entirely.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str experimentalBuiltinsBzlPath = new Flag.Str("experimentalBuiltinsBzlPath");

    @Option(
        name = "experimental_builtins_dummy",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Enables an internal dummy symbol used to test builtins injection.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalBuiltinsDummy = new Flag.Boolean("experimentalBuiltinsDummy");

    @Option(
        name = "experimental_builtins_injection_override",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      A comma-separated list of symbol names prefixed by a + or - character, indicating which symbols from\n" +
            "      `@_builtins//:exports.bzl` to inject, overriding their default injection status. Precisely, this works as\n" +
            "      follows. Each dict key of `exported_toplevels` or `exported_rules` has the form `foo`, `+foo`, or `-foo`. The\n" +
            "      first two forms mean it gets injected by default, while the last form means it does not get injected by\n" +
            "      default. In the first case (unprefixed), the default is absolute and cannot be overridden. Otherwise, we then\n" +
            "      consult this options list, and if we see foo occur here, we take the prefix of its last occurrence and use\n" +
            "      that to decide whether or not to inject. It is a no-op to specify an unknown symbol, or to attempt to not\n" +
            "      inject a symbol that occurs unprefixed in a dict key.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalBuiltinsInjectionOverride = new Flag.Unknown("experimentalBuiltinsInjectionOverride");

    @Option(
        name = "experimental_bytecode_optimizers",
        defaultValue = "Proguard",
        help = "Do not use.",
        valueHelp = "a comma-separated list of keys optionally followed by '=' and a label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalBytecodeOptimizers = new Flag.Unknown("experimentalBytecodeOptimizers");

    @Option(
        name = "experimental_bzl_visibility",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled, adds a `visibility()` function that .bzl files may call during top-level evaluation to set their\n" +
            "      visibility for the purpose of load() statements.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalBzlVisibility = new Flag.Boolean("experimentalBzlVisibility");

    @Option(
        name = "experimental_cancel_concurrent_tests",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, then Blaze will cancel concurrently running tests on the first successful run. This is only useful\n" +
            "      in combination with --runs_per_test_detects_flakes.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCancelConcurrentTests = new Flag.Boolean("experimentalCancelConcurrentTests");

    @Option(
        name = "experimental_cc_implementation_deps",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If enabled, cc_library targets can use attribute `implementation_deps`.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCcImplementationDeps = new Flag.Boolean("experimentalCcImplementationDeps");

    @Option(
        name = "experimental_cc_shared_library",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set to true, rule attributes and Starlark API methods needed for the rule cc_shared_library will be\n" +
            "      available\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCcSharedLibrary = new Flag.Boolean("experimentalCcSharedLibrary");

    @Option(
        name = "experimental_cc_skylark_api_enabled_packages",
        defaultValue = "",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Passes list of packages that can use the C++ Starlark API. Don't enable this flag yet, we will be making\n" +
            "      breaking changes.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalCcSkylarkApiEnabledPackages = new Flag.Unknown("experimentalCcSkylarkApiEnabledPackages");

    @Option(
        name = "experimental_cc_static_library",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set to true, rule attributes and Starlark API methods needed for the rule cc_static_library will be\n" +
            "      available\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCcStaticLibrary = new Flag.Boolean("experimentalCcStaticLibrary");

    @Option(
        name = "experimental_cgroup_parent",
        effectTags = {OptionEffectTag.BAZEL_MONITORING, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The cgroup where to start the bazel server as an absolute path. The server process will be started in the\n" +
            "      specified cgroup for each supported controller. For example, if the value of this flag is /build/bazel and\n" +
            "      the cpu and memory controllers are mounted respectively on /sys/fs/cgroup/cpu and /sys/fs/cgroup/memory, the\n" +
            "      server will be started in the cgroups /sys/fs/cgroup/cpu/build/bazel and /sys/fs/cgroup/memory/build/bazel.It\n" +
            "      is not an error if the specified cgroup is not writable for one or more of the controllers. This options does\n" +
            "      not have any effect on platforms that do not support cgroups.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"startup"}
    )
    public static final Flag.Str experimentalCgroupParent = new Flag.Str("experimentalCgroupParent");

    @Option(
        name = "experimental_check_desugar_deps",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Whether to double-check correct desugaring at Android binary level.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCheckDesugarDeps = new Flag.Boolean("experimentalCheckDesugarDeps");

    @Option(
        name = "experimental_check_external_repository_files",
        defaultValue = "true",
        help = "\n" +
            "      Check for modifications to files in external repositories. Consider setting this flag to false if you don't\n" +
            "      expect these files to change outside of bazel since it will speed up subsequent runs as they won't have to\n" +
            "      check a previous run's cache.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCheckExternalRepositoryFiles = new Flag.Boolean("experimentalCheckExternalRepositoryFiles");

    @Option(
        name = "experimental_check_output_files",
        defaultValue = "true",
        help = "\n" +
            "      Check for modifications made to the output files of a build. Consider setting this flag to false if you don't\n" +
            "      expect these files to change outside of bazel since it will speed up subsequent runs as they won't have to\n" +
            "      check a previous run's cache.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCheckOutputFiles = new Flag.Boolean("experimentalCheckOutputFiles");

    @Option(
        name = "experimental_circuit_breaker_strategy",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Specifies the strategy for the circuit breaker to use. Available strategies are \"failure\". On invalid value\n" +
            "      for the option the behavior same as the option is not set.\n" +
            "    ",
        valueHelp = "failure",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalCircuitBreakerStrategy = new Flag.Unknown("experimentalCircuitBreakerStrategy");

    @Option(
        name = "experimental_collect_code_coverage_for_generated_files",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If specified, Bazel will also generate collect coverage information for generated files.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCollectCodeCoverageForGeneratedFiles = new Flag.Boolean("experimentalCollectCodeCoverageForGeneratedFiles");

    @Option(
        name = "experimental_collect_load_average_in_profiler",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "If enabled, the profiler collects the system's overall load average.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCollectLoadAverageInProfiler = new Flag.Boolean("experimentalCollectLoadAverageInProfiler");

    @Option(
        name = "experimental_collect_local_action_metrics",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCollectLocalActionMetrics = new Flag.Boolean("experimentalCollectLocalActionMetrics");

    @Option(
        name = "experimental_collect_local_sandbox_action_metrics",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCollectLocalSandboxActionMetrics = new Flag.Boolean("experimentalCollectLocalSandboxActionMetrics");

    @Option(
        name = "experimental_collect_pressure_stall_indicators",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "If enabled, the profiler collects the Linux PSI data.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCollectPressureStallIndicators = new Flag.Boolean("experimentalCollectPressureStallIndicators");

    @Option(
        name = "experimental_collect_resource_estimation",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "If enabled, the profiler collects CPU and memory usage estimation for local actions.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCollectResourceEstimation = new Flag.Boolean("experimentalCollectResourceEstimation");

    @Option(
        name = "experimental_collect_skyframe_counts_in_profiler",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      If enabled, the profiler collects SkyFunction counts in the Skyframe graph over time for key function types,\n" +
            "      like configured targets and action executions. May have a performance hit as this visits the ENTIRE Skyframe\n" +
            "      graph at every profiling time unit. Do not use this flag with performance-critical measurements.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCollectSkyframeCountsInProfiler = new Flag.Boolean("experimentalCollectSkyframeCountsInProfiler");

    @Option(
        name = "experimental_collect_system_network_usage",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "If enabled, the profiler collects the system's network usage.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCollectSystemNetworkUsage = new Flag.Boolean("experimentalCollectSystemNetworkUsage");

    @Option(
        name = "experimental_collect_worker_data_in_profiler",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "If enabled, the profiler collects worker's aggregated resource data.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalCollectWorkerDataInProfiler = new Flag.Boolean("experimentalCollectWorkerDataInProfiler");

    @Option(
        name = "experimental_command_profile",
        help = "\n" +
            "      Records a Java Flight Recorder profile for the duration of the command. One of the supported profiling event\n" +
            "      types (cpu, wall, alloc or lock) must be given as an argument. The profile is written to a file named after\n" +
            "      the event type under the output base directory. The syntax and semantics of this flag might change in the\n" +
            "      future to support additional profile types or output formats; use at your own risk.\n" +
            "    ",
        valueHelp = "cpu, wall, alloc or lock",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf experimentalCommandProfile = new Flag.OneOf("experimentalCommandProfile");

    @Option(
        name = "experimental_convenience_symlinks",
        defaultValue = "normal",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      This flag controls how the convenience symlinks (the symlinks that appear in the workspace after the build)\n" +
            "      will be managed. Possible values:\n" +
            "        normal (default): Each kind of convenience symlink will be created or deleted, as determined by the build.\n" +
            "        clean: All symlinks will be unconditionally deleted.\n" +
            "        ignore: Symlinks will not be created or cleaned up.\n" +
            "        log_only: Generate log messages as if 'normal' were passed, but don't actually perform any filesystem\n" +
            "      operations (useful for tools).\n" +
            "      Note that only symlinks whose names are generated by the current value of --symlink_prefix can be affected;\n" +
            "      if the prefix changes, any pre-existing symlinks will be left alone.\n" +
            "    ",
        valueHelp = "normal, clean, ignore or log_only",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalConvenienceSymlinks = new Flag.OneOf("experimentalConvenienceSymlinks");

    @Option(
        name = "experimental_convenience_symlinks_bep_event",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      This flag controls whether or not we will post the build eventConvenienceSymlinksIdentified to the\n" +
            "      BuildEventProtocol. If the value is true, the BuildEventProtocol will have an entry for\n" +
            "      convenienceSymlinksIdentified, listing all of the convenience symlinks created in your workspace. If false,\n" +
            "      then the convenienceSymlinksIdentified entry in the BuildEventProtocol will be empty.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalConvenienceSymlinksBepEvent = new Flag.Boolean("experimentalConvenienceSymlinksBepEvent");

    @Option(
        name = "experimental_correct_runfiles_middleman_paths",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If set, the path of runfiles middlemen represents the real path of the runfiles tree.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCorrectRunfilesMiddlemanPaths = new Flag.Boolean("experimentalCorrectRunfilesMiddlemanPaths");

    @Option(
        name = "experimental_cpp_compile_resource_estimation",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If enabled, will estimate precise resource usage for local execution of CppCompileAction.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCppCompileResourceEstimation = new Flag.Boolean("experimentalCppCompileResourceEstimation");

    @Option(
        name = "experimental_cpp_modules",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Enables experimental C++20 modules support. Use it with `module_interfaces` attribute on `cc_binary` and\n" +
            "      `cc_library`. While the support is behind the experimental flag, there are no guarantees about incompatible\n" +
            "      changes to it or even keeping the support in the future. Consider those risks when using it.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCppModules = new Flag.Boolean("experimentalCppModules");

    @Option(
        name = "experimental_cpu_load_scheduling",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Enables the experimental local execution scheduling based on CPU load, not estimation of actions one by one.\n" +
            "      Experimental scheduling have showed the large benefit on a large local builds on a powerful machines with the\n" +
            "      large number of cores. Reccommended to use with --local_resources=cpu=HOST_CPUS\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalCpuLoadScheduling = new Flag.Boolean("experimentalCpuLoadScheduling");

    @Option(
        name = "experimental_cpu_load_scheduling_window_size",
        defaultValue = "5000ms",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The size of window during experimental scheduling of action based on CPU load. Make sense to define only when\n" +
            "      flag --experimental_cpu_load_scheduling is enabled.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Duration experimentalCpuLoadSchedulingWindowSize = new Flag.Duration("experimentalCpuLoadSchedulingWindowSize");

    @Option(
        name = "experimental_debug_selects_always_succeed",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      When set, select functions with no matching clause will return an empty value, instead of failing. This is to\n" +
            "      help use cquery diagnose failures in select.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalDebugSelectsAlwaysSucceed = new Flag.Boolean("experimentalDebugSelectsAlwaysSucceed");

    @Option(
        name = "experimental_disable_external_package",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set to true, the auto-generated //external package will not be available anymore. Bazel will still be\n" +
            "      unable to parse the file 'external/BUILD', but globs reaching into external/ from the unnamed package will\n" +
            "      work.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalDisableExternalPackage = new Flag.Boolean("experimentalDisableExternalPackage");

    @Option(
        name = "experimental_disable_instrumentation_manifest_merge",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Disables manifest merging when an android_binary has instruments set (i.e. is used for instrumentation\n" +
            "      testing).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalDisableInstrumentationManifestMerge = new Flag.Boolean("experimentalDisableInstrumentationManifestMerge");

    @Option(
        name = "experimental_disallow_legacy_java_toolchain_flags",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled, disallow legacy Java toolchain flags (--javabase, --host_javabase, --java_toolchain,\n" +
            "      --host_java_toolchain) and require the use of --platforms instead; see #7849\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalDisallowLegacyJavaToolchainFlags = new Flag.Boolean("experimentalDisallowLegacyJavaToolchainFlags");

    @Option(
        name = "experimental_disk_cache_gc_idle_delay",
        defaultValue = "5m",
        help = "\n" +
            "      How long the server must remain idle before a garbage collection of the disk cache occurs. To specify the\n" +
            "      garbage collection policy, set --experimental_disk_cache_gc_max_size and/or\n" +
            "      --experimental_disk_cache_gc_max_age.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration experimentalDiskCacheGcIdleDelay = new Flag.Duration("experimentalDiskCacheGcIdleDelay");

    @Option(
        name = "experimental_disk_cache_gc_max_age",
        defaultValue = "0",
        help = "\n" +
            "      If set to a positive value, the disk cache will be periodically garbage collected to remove entries older\n" +
            "      than this age. If set in conjunction with --experimental_disk_cache_gc_max_size, both criteria are applied.\n" +
            "      Garbage collection occurrs in the background once the server has become idle, as determined by the\n" +
            "      --experimental_disk_cache_gc_idle_delay flag.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration experimentalDiskCacheGcMaxAge = new Flag.Duration("experimentalDiskCacheGcMaxAge");

    @Option(
        name = "experimental_disk_cache_gc_max_size",
        defaultValue = "0",
        help = "\n" +
            "      If set to a positive value, the disk cache will be periodically garbage collected to stay under this size. If\n" +
            "      set in conjunction with --experimental_disk_cache_gc_max_age, both criteria are applied. Garbage collection\n" +
            "      occurrs in the background once the server has become idle, as determined by the\n" +
            "      --experimental_disk_cache_gc_idle_delay flag.\n" +
            "    ",
        valueHelp = "a size in bytes, optionally followed by a K, M, G or T multiplier",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalDiskCacheGcMaxSize = new Flag.Unknown("experimentalDiskCacheGcMaxSize");

    @Option(
        name = "experimental_docker_image",
        defaultValue = "",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Specify a Docker image name (e.g. \"ubuntu:latest\") that should be used to execute a sandboxed action when\n" +
            "      using the docker strategy and the action itself doesn't already have a container-image attribute in its\n" +
            "      remote_execution_properties in the platform description. The value of this flag is passed verbatim to 'docker\n" +
            "      run', so it supports the same syntax and mechanisms as Docker itself.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalDockerImage = new Flag.Str("experimentalDockerImage");

    @Option(
        name = "experimental_docker_privileged",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If enabled, Bazel will pass the --privileged flag to 'docker run' when running actions. This might be\n" +
            "      required by your build, but it might also result in reduced hermeticity.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalDockerPrivileged = new Flag.Boolean("experimentalDockerPrivileged");

    @Option(
        name = "experimental_docker_use_customized_images",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If enabled, injects the uid and gid of the current user into the Docker image before using it. This is\n" +
            "      required if your build / tests depend on the user having a name and home directory inside the container. This\n" +
            "      is on by default, but you can disable it in case the automatic image customization feature doesn't work in\n" +
            "      your case or you know that you don't need it.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalDockerUseCustomizedImages = new Flag.Boolean("experimentalDockerUseCustomizedImages");

    @Option(
        name = "experimental_docker_verbose",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "If enabled, Bazel will print more verbose messages about the Docker sandbox strategy.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalDockerVerbose = new Flag.Boolean("experimentalDockerVerbose");

    @Option(
        name = "experimental_dormant_deps",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "       If set to true, attr.label(materializer=), attr(for_dependency_resolution=), attr.dormant_label(),\n" +
            "      attr.dormant_label_list() and rule(for_dependency_resolution=) are allowed.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalDormantDeps = new Flag.Boolean("experimentalDormantDeps");

    @Option(
        name = "experimental_downloader_config",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Specify a file to configure the remote downloader with. This file consists of lines, each of which starts\n" +
            "      with a directive (`allow`, `block` or `rewrite`) followed by either a host name (for `allow` and `block`) or\n" +
            "      two patterns, one to match against, and one to use as a substitute URL, with back-references starting from\n" +
            "      `$1`. It is possible for multiple `rewrite` directives for the same URL to be give, and in this case\n" +
            "      multiple URLs will be returned.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path experimentalDownloaderConfig = new Flag.Path("experimentalDownloaderConfig");

    @Option(
        name = "experimental_dynamic_exclude_tools",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      When set, targets that are build \"for tool\" are not subject to dynamic execution. Such targets are extremely\n" +
            "      unlikely to be built incrementally and thus not worth spending local cycles on.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalDynamicExcludeTools = new Flag.Boolean("experimentalDynamicExcludeTools");

    @Option(
        name = "experimental_dynamic_ignore_local_signals",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Takes a list of OS signal numbers. If a local branch of dynamic execution gets killed with any of these\n" +
            "      signals, the remote branch will be allowed to finish instead. For persistent workers, this only affects\n" +
            "      signals that kill the worker process.\n" +
            "    ",
        valueHelp = "a comma-separated list of signal numbers",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalDynamicIgnoreLocalSignals = new Flag.Unknown("experimentalDynamicIgnoreLocalSignals");

    @Option(
        name = "experimental_dynamic_local_load_factor",
        defaultValue = "0",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Controls how much load from dynamic execution to put on the local machine. This flag adjusts how many actions\n" +
            "      in dynamic execution we will schedule concurrently. It is based on the number of CPUs Blaze thinks is\n" +
            "      available, which can be controlled with the --local_cpu_resources flag.\n" +
            "      If this flag is 0, all actions are scheduled locally immediately. If > 0, the amount of actions scheduled\n" +
            "      locally is limited by the number of CPUs available. If < 1, the load factor is used to reduce the number of\n" +
            "      locally scheduled actions when the number of actions waiting to schedule is high. This lessens the load on\n" +
            "      the local machine in the clean build case, where the local machine does not contribute much.\n" +
            "    ",
        valueHelp = "a double",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Double experimentalDynamicLocalLoadFactor = new Flag.Double("experimentalDynamicLocalLoadFactor");

    @Option(
        name = "experimental_dynamic_slow_remote_time",
        defaultValue = "0",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      If >0, the time a dynamically run action must run remote-only before we prioritize its local execution to\n" +
            "      avoid remote timeouts. This may hide some problems on the remote execution system. Do not turn this on\n" +
            "      without monitoring of remote execution issues.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Duration experimentalDynamicSlowRemoteTime = new Flag.Duration("experimentalDynamicSlowRemoteTime");

    @Option(
        name = "experimental_enable_android_migration_apis",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "If set to true, enables the APIs required to support the Android Starlark migration.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalEnableAndroidMigrationApis = new Flag.Boolean("experimentalEnableAndroidMigrationApis");

    @Option(
        name = "experimental_enable_aspect_hints",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalEnableAspectHints = new Flag.Boolean("experimentalEnableAspectHints");

    @Option(
        name = "experimental_enable_critical_path_profiling",
        defaultValue = "true",
        help = "\n" +
            "      If set (the default), critical path profiling is enabled for the execution phase. This has a slight overhead\n" +
            "      in RAM and CPU, and may prevent Bazel from making certain aggressive RAM optimizations in some cases.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalEnableCriticalPathProfiling = new Flag.Boolean("experimentalEnableCriticalPathProfiling");

    @Option(
        name = "experimental_enable_docker_sandbox",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "Enable Docker-based sandboxing. This option has no effect if Docker is not installed.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalEnableDockerSandbox = new Flag.Boolean("experimentalEnableDockerSandbox");

    @Option(
        name = "experimental_enable_execution_graph_log",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Enabling this flag makes Blaze write a file of all actions executed during a build. Note that this dump may\n" +
            "      use a different granularity of actions than other APIs, and may also contain additional information as\n" +
            "      necessary to reconstruct the full dependency graph in combination with other sources of data.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalEnableExecutionGraphLog = new Flag.Boolean("experimentalEnableExecutionGraphLog");

    @Option(
        name = "experimental_enable_first_class_macros",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "If set to true, enables the `macro()` construct for defining symbolic macros.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalEnableFirstClassMacros = new Flag.Boolean("experimentalEnableFirstClassMacros");

    @Option(
        name = "experimental_enable_jspecify",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Enable experimental jspecify integration.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalEnableJspecify = new Flag.Boolean("experimentalEnableJspecify");

    @Option(
        name = "experimental_enable_scl_dialect",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "If set to true, .scl files may be used in load() statements.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalEnableSclDialect = new Flag.Boolean("experimentalEnableSclDialect");

    @Option(
        name = "experimental_enable_skyfocus",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      If true, enable the use of --experimental_working_set to reduce Bazel's memory footprint for incremental\n" +
            "      builds. This feature is known as Skyfocus.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalEnableSkyfocus = new Flag.Boolean("experimentalEnableSkyfocus");

    @Option(
        name = "experimental_enable_starlark_doc_extract",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Deprecated no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalEnableStarlarkDocExtract = new Flag.Boolean("experimentalEnableStarlarkDocExtract");

    @Option(
        name = "experimental_enable_starlark_set",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If true, enable the set data type and set() constructor in Starlark.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalEnableStarlarkSet = new Flag.Boolean("experimentalEnableStarlarkSet");

    @Option(
        name = "experimental_exclude_defines_from_exec_config",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, don't propagate '--define's to the exec transition at default; only propagate defines specified by\n" +
            "      `--experimental_propagate_custom_flag`.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalExcludeDefinesFromExecConfig = new Flag.Boolean("experimentalExcludeDefinesFromExecConfig");

    @Option(
        name = "experimental_exclude_starlark_flags_from_exec_config",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, don't propagate starlark flags to the exec transition at default; only propagate starlark flags\n" +
            "      specified in `--experimental_propagate_custom_flag`.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalExcludeStarlarkFlagsFromExecConfig = new Flag.Boolean("experimentalExcludeStarlarkFlagsFromExecConfig");

    @Option(
        name = "experimental_exec_config",
        defaultValue = "@_builtins//:common/builtin_exec_platforms.bzl%bazel_exec_transition",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set to '//some:label:my.bzl%my_transition', uses my_transition for 'cfg = \"exec\"' semantics instead of\n" +
            "      Bazel's internal exec transition logic.  Else uses Bazel's internal logic.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalExecConfig = new Flag.Str("experimentalExecConfig");

    @Option(
        name = "experimental_exec_configuration_distinguisher",
        defaultValue = "off",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Please only use this flag as part of a suggested migration or testing strategy due to potential for action\n" +
            "      conflicts. Controls how the execution transition changes the platform_suffix flag. In legacy mode, sets it to\n" +
            "      a hash of the execution platform. In fullhash mode, sets it to a hash of the entire configuration. In off\n" +
            "      mode, does not touch it.\n" +
            "    ",
        valueHelp = "legacy, off, full_hash or diff_to_affected",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalExecConfigurationDistinguisher = new Flag.OneOf("experimentalExecConfigurationDistinguisher");

    @Option(
        name = "experimental_execution_graph_enable_edges_from_filewrite_actions",
        defaultValue = "true",
        help = "Handle edges from filewrite actions to their inputs correctly.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalExecutionGraphEnableEdgesFromFilewriteActions =
        new Flag.Boolean("experimentalExecutionGraphEnableEdgesFromFilewriteActions");

    @Option(
        name = "experimental_execution_graph_log_dep_type",
        defaultValue = "none",
        help = "\n" +
            "      Selects what kind of dependency information is reported in the action dump. If 'all', every inter-action edge\n" +
            "      will be reported.\n" +
            "    ",
        valueHelp = "none, runfiles or all",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalExecutionGraphLogDepType = new Flag.OneOf("experimentalExecutionGraphLogDepType");

    @Option(
        name = "experimental_execution_graph_log_middleman",
        defaultValue = "false",
        help = "Subscribe to ActionMiddlemanEvent in ExecutionGraphModule.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalExecutionGraphLogMiddleman = new Flag.Boolean("experimentalExecutionGraphLogMiddleman");

    @Option(
        name = "experimental_execution_graph_log_path",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Local path at which the execution path will be written. If this is set, the log will only be written locally,\n" +
            "      and not to BEP. If this is set when experimental_enable_execution_graph_log is disabled, there will be an\n" +
            "      error. If this is unset while BEP uploads are disabled and experimental_enable_execution_graph_log is\n" +
            "      enabled, the log will be written to a local default.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalExecutionGraphLogPath = new Flag.Str("experimentalExecutionGraphLogPath");

    @Option(
        name = "experimental_execution_graph_log_queue_size",
        defaultValue = "-1",
        help = "\n" +
            "      The size of the action dump queue, where actions are kept before writing. Larger sizes will increase peak\n" +
            "      memory usage, but should decrease queue blocking. -1 means unbounded\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer experimentalExecutionGraphLogQueueSize = new Flag.Integer("experimentalExecutionGraphLogQueueSize");

    @Option(
        name = "experimental_explicit_aspects",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      aquery, cquery: whether to include aspect-generated actions in the output. query: no-op (aspects are always\n" +
            "      followed).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean experimentalExplicitAspects = new Flag.Boolean("experimentalExplicitAspects");

    @Option(
        name = "experimental_extended_sanity_checks",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Enables internal validation checks to make sure that configured target implementations only access things\n" +
            "      they should. Causes a performance hit.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalExtendedSanityChecks = new Flag.Boolean("experimentalExtendedSanityChecks");

    @Option(
        name = "experimental_extra_action_filter",
        defaultValue = "",
        help = "Deprecated in favor of aspects. Filters set of targets to schedule extra_actions for.",
        valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalExtraActionFilter = new Flag.Unknown("experimentalExtraActionFilter");

    @Option(
        name = "experimental_extra_action_top_level_only",
        defaultValue = "false",
        help = "Deprecated in favor of aspects. Only schedules extra_actions for top level targets.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalExtraActionTopLevelOnly = new Flag.Boolean("experimentalExtraActionTopLevelOnly");

    @Option(
        name = "experimental_fetch_all_coverage_outputs",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, then Bazel fetches the entire coverage data directory for each test during a coverage run.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalFetchAllCoverageOutputs = new Flag.Boolean("experimentalFetchAllCoverageOutputs");

    @Option(
        name = "experimental_filter_library_jar_with_program_jar",
        defaultValue = "false",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Filter the ProGuard ProgramJar to remove any classes also present in the LibraryJar.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalFilterLibraryJarWithProgramJar = new Flag.Boolean("experimentalFilterLibraryJarWithProgramJar");

    @Option(
        name = "experimental_filter_r_jars_from_android_test",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If enabled, R Jars will be filtered from the test apk built by android_test.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalFilterRJarsFromAndroidTest = new Flag.Boolean("experimentalFilterRJarsFromAndroidTest");

    @Option(
        name = "experimental_fix_deps_tool",
        defaultValue = "add_dep",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "Specifies which tool should be used to resolve missing dependencies.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalFixDepsTool = new Flag.Str("experimentalFixDepsTool");

    @Option(
        name = "experimental_force_gc_after_build",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "If true calls System.gc() after a build to try and get a post-gc peak heap measurement.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalForceGcAfterBuild = new Flag.Boolean("experimentalForceGcAfterBuild");

    @Option(
        name = "experimental_fsvc_threads",
        defaultValue = "200",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "The number of threads that are used by the FileSystemValueChecker.",
        valueHelp = "\n" +
            "      an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation\n" +
            "      ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalFsvcThreads = new Flag.Unknown("experimentalFsvcThreads");

    @Option(
        name = "experimental_generate_llvm_lcov",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If true, coverage for clang will generate an LCOV report.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalGenerateLlvmLcov = new Flag.Boolean("experimentalGenerateLlvmLcov");

    @Option(
        name = "experimental_genquery_use_graphless_query",
        defaultValue = "auto",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated. No-op.",
        valueHelp = "a tri-state (auto, yes, no)",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.TriState experimentalGenqueryUseGraphlessQuery = new Flag.TriState("experimentalGenqueryUseGraphlessQuery");

    @Option(
        name = "experimental_get_android_java_resources_from_optimized_jar",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Get Java resources from _proguard.jar instead of _deploy.jar in android_binary when bundling the final APK.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalGetAndroidJavaResourcesFromOptimizedJar = new Flag.Boolean("experimentalGetAndroidJavaResourcesFromOptimizedJar");

    @Option(
        name = "experimental_google_legacy_api",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set to true, exposes a number of experimental pieces of Starlark build API pertaining to Google legacy\n" +
            "      code.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalGoogleLegacyApi = new Flag.Boolean("experimentalGoogleLegacyApi");

    @Option(
        name = "experimental_graphless_query",
        defaultValue = "auto",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      If true, uses a Query implementation that does not make a copy of the graph. The new implementation only\n" +
            "      supports --order_output=no, as well as only a subset of output formatters.\n" +
            "    ",
        valueHelp = "a tri-state (auto, yes, no)",
        commands = {"query"}
    )
    public static final Flag.TriState experimentalGraphlessQuery = new Flag.TriState("experimentalGraphlessQuery");

    @Option(
        name = "experimental_guard_against_concurrent_changes",
        defaultValue = "false",
        help = "\n" +
            "      Turn this off to disable checking the ctime of input files of an action before uploading it to a remote\n" +
            "      cache. There may be cases where the Linux kernel delays writing of files, which could cause false positives.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalGuardAgainstConcurrentChanges = new Flag.Boolean("experimentalGuardAgainstConcurrentChanges");

    @Option(
        name = "experimental_import_deps_checking",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "No-op, kept only for backwards compatibility",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalImportDepsChecking = new Flag.Str("experimentalImportDepsChecking");

    @Option(
        name = "experimental_include_default_values",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.TERMINAL_OUTPUT},
        help = "Whether Starlark options set to their default values are included in the output.",
        valueHelp = "a boolean",
        commands = {"canonicalize-flags"}
    )
    public static final Flag.Boolean experimentalIncludeDefaultValues = new Flag.Boolean("experimentalIncludeDefaultValues");

    @Option(
        name = "experimental_include_scanning_parallelism",
        defaultValue = "80",
        effectTags = {
            OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.EXECUTION,
            OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
        },
        help = "\n" +
            "      Configures the size of the thread pool used for include scanning. Takes an integer, or a keyword (\"auto\",\n" +
            "      \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\". 0\n" +
            "      means to disable parallelism and to just rely on the build graph parallelism for concurrency.  \"auto\" means\n" +
            "      to use a reasonable value derived from the machine's hardware profile (e.g. the number of processors).\n" +
            "    ",
        valueHelp = "\n" +
            "      an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation\n" +
            "      ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalIncludeScanningParallelism = new Flag.Unknown("experimentalIncludeScanningParallelism");

    @Option(
        name = "experimental_include_xcode_execution_requirements",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.EXECUTION,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set, add a \"requires-xcode:{version}\" execution requirement to every Xcode action.  If the Xcode version\n" +
            "      has a hyphenated label,  also add a \"requires-xcode-label:{version_label}\" execution requirement.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalIncludeXcodeExecutionRequirements = new Flag.Boolean("experimentalIncludeXcodeExecutionRequirements");

    @Option(
        name = "experimental_incremental_dexing_after_proguard",
        defaultValue = "50",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Whether to use incremental dexing tools when building proguarded Android binaries.  Values > 0 turn the\n" +
            "      feature on, values > 1 run that many dexbuilder shards.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer experimentalIncrementalDexingAfterProguard = new Flag.Integer("experimentalIncrementalDexingAfterProguard");

    @Option(
        name = "experimental_incremental_dexing_after_proguard_by_default",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Whether to use incremental dexing for proguarded Android binaries by default.  Use incremental_dexing\n" +
            "      attribute to override default for a particular android_binary.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalIncrementalDexingAfterProguardByDefault = new Flag.Boolean("experimentalIncrementalDexingAfterProguardByDefault");

    @Option(
        name = "experimental_inmemory_dotd_files",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled, C++ .d files will be passed through in memory directly from the remote build nodes instead of\n" +
            "      being written to disk.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalInmemoryDotdFiles = new Flag.Boolean("experimentalInmemoryDotdFiles");

    @Option(
        name = "experimental_inmemory_dotincludes_files",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.EXECUTION,
            OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
        },
        help = "\n" +
            "      If enabled, searching for '#include' lines in generated header files will not touch local disk. This makes\n" +
            "      include scanning of C++ files less disk-intensive.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalInmemoryDotincludesFiles = new Flag.Boolean("experimentalInmemoryDotincludesFiles");

    @Option(
        name = "experimental_inmemory_jdeps_files",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled, the dependency (.jdeps) files generated from Java compilations will be passed through in memory\n" +
            "      directly from the remote build nodes instead of being written to disk.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalInmemoryJdepsFiles = new Flag.Boolean("experimentalInmemoryJdepsFiles");

    @Option(
        name = "experimental_inmemory_sandbox_stashes",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If set to true, the contents of stashed sandboxes for reuse_sandbox_directories will be tracked in memory.\n" +
            "      This reduces the amount of I/O needed during reuse. Depending on the build this flag may improve wall time.\n" +
            "      Depending on the build as well this flag may use a significant amount of additional memory.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalInmemorySandboxStashes = new Flag.Boolean("experimentalInmemorySandboxStashes");

    @Option(
        name = "experimental_inprocess_symlink_creation",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Whether to make direct filesystem calls to create symlink trees instead of delegating to a helper process.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalInprocessSymlinkCreation = new Flag.Boolean("experimentalInprocessSymlinkCreation");

    @Option(
        name = "experimental_install_base_gc_max_age",
        defaultValue = "30d",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      How long an install base must go unused before it's eligible for garbage collection. If nonzero, the server\n" +
            "      will attempt to garbage collect other install bases when idle.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration experimentalInstallBaseGcMaxAge = new Flag.Duration("experimentalInstallBaseGcMaxAge");

    @Option(
        name = "experimental_isolated_extension_usages",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If true, enables the <code>isolate</code> parameter in the <a\n" +
            "      href=\"https://bazel.build/rules/lib/globals/module#use_extension\"><code>use_extension</code></a> function.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalIsolatedExtensionUsages = new Flag.Boolean("experimentalIsolatedExtensionUsages");

    @Option(
        name = "experimental_j2objc_header_map",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Whether to generate J2ObjC header map in parallel of J2ObjC transpilation.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalJ2objcHeaderMap = new Flag.Boolean("experimentalJ2objcHeaderMap");

    @Option(
        name = "experimental_j2objc_shorter_header_path",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Whether to generate with shorter header path (uses \"_ios\" instead of \"_j2objc\").",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalJ2objcShorterHeaderPath = new Flag.Boolean("experimentalJ2objcShorterHeaderPath");

    @Option(
        name = "experimental_java_classpath",
        oldName = "java_classpath",
        defaultValue = "javabuilder",
        help = "Enables reduced classpaths for Java compilations.",
        valueHelp = "off, javabuilder or bazel",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalJavaClasspath = new Flag.OneOf("experimentalJavaClasspath");

    @Option(
        name = "experimental_java_header_input_pruning",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "No-op, kept only for backwards compatibility",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalJavaHeaderInputPruning = new Flag.Boolean("experimentalJavaHeaderInputPruning");

    @Option(
        name = "experimental_java_library_export",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If enabled, experimental_java_library_export_do_not_use module is available.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalJavaLibraryExport = new Flag.Boolean("experimentalJavaLibraryExport");

    @Option(
        name = "experimental_java_proto_add_allowed_public_imports",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "This flag is a noop and scheduled for removal.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalJavaProtoAddAllowedPublicImports = new Flag.Boolean("experimentalJavaProtoAddAllowedPublicImports");

    @Option(
        name = "experimental_java_test_auto_create_deploy_jar",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "DO NOT USE",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalJavaTestAutoCreateDeployJar = new Flag.Boolean("experimentalJavaTestAutoCreateDeployJar");

    @Option(
        name = "experimental_limit_android_lint_to_android_constrained_java",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "No-op, kept only for backwards compatibility",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalLimitAndroidLintToAndroidConstrainedJava = new Flag.Boolean("experimentalLimitAndroidLintToAndroidConstrainedJava");

    @Option(
        name = "experimental_link_static_libraries_once",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE, OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled, cc_shared_library will link all libraries statically linked into it, that should only be linked\n" +
            "      once.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalLinkStaticLibrariesOnce = new Flag.Boolean("experimentalLinkStaticLibrariesOnce");

    @Option(
        name = "experimental_local_java_optimization_configuration",
        help = "Do not use.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label experimentalLocalJavaOptimizationConfiguration = new Flag.Label("experimentalLocalJavaOptimizationConfiguration");

    @Option(
        name = "experimental_local_java_optimizations",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Do not use.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalLocalJavaOptimizations = new Flag.Boolean("experimentalLocalJavaOptimizations");

    @Option(
        name = "experimental_local_lockfree_output",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      When true, the local spawn runner doesn't lock the output tree during dynamic execution. Instead, spawns are\n" +
            "      allowed to execute until they are explicitly interrupted by a faster remote action.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalLocalLockfreeOutput = new Flag.Boolean("experimentalLocalLockfreeOutput");

    @Option(
        name = "experimental_local_retries_on_crash",
        defaultValue = "0",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Number of times to retry a local action when we detect that it crashed. This exists to workaround a bug in\n" +
            "      OSXFUSE which is tickled by the use of the dynamic scheduler and --experimental_local_lockfree_output due to\n" +
            "      constant process churn. The bug can be triggered by a cancelled process that ran *before* the process we are\n" +
            "      trying to run, introducing corruption in its file reads.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer experimentalLocalRetriesOnCrash = new Flag.Integer("experimentalLocalRetriesOnCrash");

    @Option(
        name = "experimental_materialize_param_files_directly",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "If materializing param files, do so with direct writes to disk.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalMaterializeParamFilesDirectly = new Flag.Boolean("experimentalMaterializeParamFilesDirectly");

    @Option(
        name = "experimental_max_directories_to_eagerly_visit_in_globbing",
        defaultValue = "-1",
        help = "\n" +
            "      If non-negative, the first time a glob is evaluated in a package, the subdirectories of the package will be\n" +
            "      traversed in order to warm filesystem caches and compensate for lack of parallelism in globbing. At most this\n" +
            "      many directories will be visited.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Integer experimentalMaxDirectoriesToEagerlyVisitInGlobbing = new Flag.Integer("experimentalMaxDirectoriesToEagerlyVisitInGlobbing");

    @Option(
        name = "experimental_merged_skyframe_analysis_execution",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If this flag is set, the analysis and execution phases of Skyframe are merged.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalMergedSkyframeAnalysisExecution = new Flag.Boolean("experimentalMergedSkyframeAnalysisExecution");

    @Option(
        name = "experimental_objc_fastbuild_options",
        defaultValue = "-O0,-DDEBUG=1",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "Uses these strings as objc fastbuild compiler options.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalObjcFastbuildOptions = new Flag.Unknown("experimentalObjcFastbuildOptions");

    @Option(
        name = "experimental_objc_provider_from_linked",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "No-op. Kept here for backwards compatibility. This field will be removed in a future release.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalObjcProviderFromLinked = new Flag.Boolean("experimentalObjcProviderFromLinked");

    @Option(
        name = "experimental_omit_resources_info_provider_from_android_binary",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Omit AndroidResourcesInfo provider from android_binary rules. Propagating resources out to other binaries is\n" +
            "      usually unintentional.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalOmitResourcesInfoProviderFromAndroidBinary = new Flag.Boolean("experimentalOmitResourcesInfoProviderFromAndroidBinary");

    @Option(
        name = "experimental_omitfp",
        defaultValue = "false",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, use libunwind for stack unwinding, and compile with -fomit-frame-pointer and\n" +
            "      -fasynchronous-unwind-tables.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalOmitfp = new Flag.Boolean("experimentalOmitfp");

    @Option(
        name = "experimental_one_version_enforcement",
        defaultValue = "OFF",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      When enabled, enforce that a java_binary rule can't contain more than one version of the same class file on\n" +
            "      the classpath. This enforcement can break the build, or can just result in warnings.\n" +
            "    ",
        valueHelp = "off, warning or error",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalOneVersionEnforcement = new Flag.OneOf("experimentalOneVersionEnforcement");

    @Option(
        name = "experimental_one_version_enforcement_use_transitive_jars_for_binary_under_test",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.ACTION_COMMAND_LINES},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled, one version enforcement for android_test uses the binary_under_test's transitive classpath,\n" +
            "      otherwise it uses the deploy jar\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalOneVersionEnforcementUseTransitiveJarsForBinaryUnderTest =
        new Flag.Boolean("experimentalOneVersionEnforcementUseTransitiveJarsForBinaryUnderTest");

    @Option(
        name = "experimental_oom_sensitive_skyfunctions_semaphore_size",
        defaultValue = "HOST_CPUS",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Sets the size of the semaphore used to prevent SkyFunctions with large peak memory requirement from OOM-ing\n" +
            "      blaze. A value of 0 indicates that no semaphore should be used. Example value: \"HOST_CPUS*0.5\".\n" +
            "    ",
        valueHelp = "an integer, or \"HOST_CPUS\", optionally followed by [-|*]<float>.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalOomSensitiveSkyfunctionsSemaphoreSize = new Flag.Unknown("experimentalOomSensitiveSkyfunctionsSemaphoreSize");

    @Option(
        name = "experimental_output_directory_naming_scheme",
        defaultValue = "diff_against_dynamic_baseline",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Please only use this flag as part of a suggested migration or testing strategy. In legacy mode, transitions\n" +
            "      (generally only Starlark) set and use `affected by Starlark transition` to determine the ST hash. In\n" +
            "      diff_against_baseline mode, `affected by Starlark transition` is ignored and instead ST hash is determined,\n" +
            "      for all configuration, by diffing against the top-level configuration.\n" +
            "    ",
        valueHelp = "legacy, diff_against_baseline or diff_against_dynamic_baseline",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalOutputDirectoryNamingScheme = new Flag.OneOf("experimentalOutputDirectoryNamingScheme");

    @Option(
        name = "experimental_output_paths",
        defaultValue = "off",
        effectTags = {
            OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION,
        },
        help = "\n" +
            "      Which model to use for where in the output tree rules write their outputs, particularly for multi-platform /\n" +
            "      multi-configuration builds. This is highly experimental. See https://github.com/bazelbuild/bazel/issues/6526\n" +
            "      for details. Starlark actions canopt into path mapping by adding the key 'supports-path-mapping' to the\n" +
            "      'execution_requirements' dict.\n" +
            "    ",
        valueHelp = "off, content or strip",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalOutputPaths = new Flag.OneOf("experimentalOutputPaths");

    @Option(
        name = "experimental_override_name_platform_in_output_dir",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Each entry should be of the form label=value where label refers to a platform and values is the desired\n" +
            "      shortname to use in the output path. Only used when --experimental_platform_in_output_dir is true. Has\n" +
            "      highest naming priority.\n" +
            "    ",
        valueHelp = "a 'label=value' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalOverrideNamePlatformInOutputDir = new Flag.Unknown("experimentalOverrideNamePlatformInOutputDir");

    @Option(
        name = "experimental_parallel_aquery_output",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalParallelAqueryOutput = new Flag.Boolean("experimentalParallelAqueryOutput");

    @Option(
        name = "experimental_parse_headers_skipped_if_corresponding_srcs_found",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalParseHeadersSkippedIfCorrespondingSrcsFound =
        new Flag.Boolean("experimentalParseHeadersSkippedIfCorrespondingSrcsFound");

    @Option(
        name = "experimental_persistent_aar_extractor",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Enable persistent aar extractor by using workers.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalPersistentAarExtractor = new Flag.Boolean("experimentalPersistentAarExtractor");

    @Option(
        name = "experimental_platform_cc_test",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled, a Starlark version of cc_test can be used which will use platform-based toolchain() resolution to\n" +
            "      choose a test runner.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalPlatformCcTest = new Flag.Boolean("experimentalPlatformCcTest");

    @Option(
        name = "experimental_platform_in_output_dir",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, a shortname for the target platform is used in the output directory name instead of the CPU. The\n" +
            "      exact scheme is experimental and subject to change: First, in the rare case the --platforms option does not\n" +
            "      have exactly one value, a hash of the platforms option is used. Next, if any shortname for the current\n" +
            "      platform was registered by --experimental_override_name_platform_in_output_dir, then that shortname is used.\n" +
            "      Then, if --experimental_use_platforms_in_output_dir_legacy_heuristic is set, use a shortname based off the\n" +
            "      current platform Label. Finally, a hash of the platform option is used as a last resort.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalPlatformInOutputDir = new Flag.Boolean("experimentalPlatformInOutputDir");

    @Option(
        name = "experimental_platforms_api",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If set to true, enables a number of platform-related Starlark APIs useful for debugging.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalPlatformsApi = new Flag.Boolean("experimentalPlatformsApi");

    @Option(
        name = "experimental_post_profile_started_event",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalPostProfileStartedEvent = new Flag.Boolean("experimentalPostProfileStartedEvent");

    @Option(
        name = "experimental_prefer_mutual_xcode",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, use the most recent Xcode that is available both locally and remotely. If false, or if there are no\n" +
            "      mutual available versions, use the local Xcode version selected via xcode-select.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalPreferMutualXcode = new Flag.Boolean("experimentalPreferMutualXcode");

    @Option(
        name = "experimental_process_wrapper_graceful_sigterm",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      When true, make the process-wrapper propagate SIGTERMs (used by the dynamic scheduler to stop process trees)\n" +
            "      to the subprocesses themselves, giving them the grace period in --local_termination_grace_seconds before\n" +
            "      forcibly sending a SIGKILL.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalProcessWrapperGracefulSigterm = new Flag.Boolean("experimentalProcessWrapperGracefulSigterm");

    @Option(
        name = "experimental_profile_action_counts",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalProfileActionCounts = new Flag.Boolean("experimentalProfileActionCounts");

    @Option(
        name = "experimental_profile_additional_tasks",
        allowMultiple = true,
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Specifies additional profile tasks to be included in the profile.",
        valueHelp = "\n" +
            "      phase, action, discover_inputs, action_check, action_lock, action_update, action_complete,\n" +
            "      action_rewinding, bzlmod, info, create_package, remote_execution, local_execution, scanner,\n" +
            "      local_parse, upload_time, remote_process_time, remote_queue, remote_setup, fetch,\n" +
            "      local_process_time, vfs_stat, vfs_dir, vfs_readlink, vfs_md5, vfs_xattr, vfs_delete, vfs_open,\n" +
            "      vfs_read, vfs_write, vfs_glob, vfs_vmfs_stat, vfs_vmfs_dir, vfs_vmfs_read, wait, thread_name,\n" +
            "      thread_sort_index, skyframe_eval, skyfunction, critical_path, critical_path_component,\n" +
            "      handle_gc_notification, local_action_counts, starlark_parser, starlark_user_fn,\n" +
            "      starlark_builtin_fn, starlark_user_compiled_fn, starlark_repository_fn, action_fs_staging,\n" +
            "      remote_cache_check, remote_download, remote_network, filesystem_traversal, worker_execution,\n" +
            "      worker_setup, worker_borrow, worker_working, worker_copying_outputs, credential_helper,\n" +
            "      conflict_check, dynamic_lock, repository_fetch, repository_vendor or unknown\n" +
            "    ",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf experimentalProfileAdditionalTasks = new Flag.OneOf("experimentalProfileAdditionalTasks");

    @Option(
        name = "experimental_profile_include_primary_output",
        oldName = "experimental_include_primary_output",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      Includes the extra \"out\" attribute in action events that contains the exec path to the action's primary\n" +
            "      output.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalProfileIncludePrimaryOutput = new Flag.Boolean("experimentalProfileIncludePrimaryOutput");

    @Option(
        name = "experimental_profile_include_target_configuration",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Includes target configuration hash in action events' JSON profile data.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalProfileIncludeTargetConfiguration = new Flag.Boolean("experimentalProfileIncludeTargetConfiguration");

    @Option(
        name = "experimental_profile_include_target_label",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Includes target label in action events' JSON profile data.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalProfileIncludeTargetLabel = new Flag.Boolean("experimentalProfileIncludeTargetLabel");

    @Option(
        name = "experimental_propagate_custom_flag",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Which custom flags (starlark flags or defines) to propagate to the exec transition, by key. e.g. if\n" +
            "      '--define=a=b' should be propagated, set `--experimental_propagate_custom_flag=a`\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalPropagateCustomFlag = new Flag.Str("experimentalPropagateCustomFlag");

    @Option(
        name = "experimental_proto_descriptor_sets_include_source_info",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Run extra actions for alternative Java api versions in a proto_library.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalProtoDescriptorSetsIncludeSourceInfo = new Flag.Boolean("experimentalProtoDescriptorSetsIncludeSourceInfo");

    @Option(
        name = "experimental_proto_extra_actions",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Deprecated. No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalProtoExtraActions = new Flag.Boolean("experimentalProtoExtraActions");

    @Option(
        name = "experimental_publish_package_metrics_in_bep",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Whether to publish package metrics in the BEP.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalPublishPackageMetricsInBep = new Flag.Boolean("experimentalPublishPackageMetricsInBep");

    @Option(
        name = "experimental_py_binaries_include_label",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "py_binary targets include their label even when stamping is disabled.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalPyBinariesIncludeLabel = new Flag.Boolean("experimentalPyBinariesIncludeLabel");

    @Option(
        name = "experimental_python_import_all_repositories",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, the roots of repositories in the runfiles tree are added to PYTHONPATH, so that imports like `import\n" +
            "      mytoplevelpackage.package.module` are valid. Regardless of whether this flag is true, the runfiles root\n" +
            "      itself is also added to the PYTHONPATH, so `import myreponame.mytoplevelpackage.package.module` is valid. The\n" +
            "      latter form is less likely to experience import name collisions.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalPythonImportAllRepositories = new Flag.Boolean("experimentalPythonImportAllRepositories");

    @Option(
        name = "experimental_record_metrics_for_all_mnemonics",
        defaultValue = "false",
        help = "\n" +
            "      Controls the output of BEP ActionSummary and BuildGraphMetrics, limiting the number of mnemonics in\n" +
            "      ActionData and number of entries reported in BuildGraphMetrics.AspectCount/RuleClassCount. By default the\n" +
            "      number of types is limited to the top 20, by number of executed actions for ActionData, and instances for\n" +
            "      RuleClass and Asepcts. Setting this option will write statistics for all mnemonics, rule classes and aspects.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRecordMetricsForAllMnemonics = new Flag.Boolean("experimentalRecordMetricsForAllMnemonics");

    @Option(
        name = "experimental_record_skyframe_metrics",
        defaultValue = "false",
        help = "\n" +
            "      Controls the output of BEP BuildGraphMetrics, including expensiveto compute skyframe metrics about Skykeys,\n" +
            "      RuleClasses and Aspects.With this flag set to false BuildGraphMetrics.rule_count and aspectfields will not be\n" +
            "      populated in the BEP.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRecordSkyframeMetrics = new Flag.Boolean("experimentalRecordSkyframeMetrics");

    @Option(
        name = "experimental_remotable_source_manifests",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Whether to make source manifest actions remotable",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalRemotableSourceManifests = new Flag.Boolean("experimentalRemotableSourceManifests");

    @Option(
        name = "experimental_remote_analysis_cache",
        defaultValue = "",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "The URL for the remote analysis caching backend.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalRemoteAnalysisCache = new Flag.Str("experimentalRemoteAnalysisCache");

    @Option(
        name = "experimental_remote_analysis_cache_concurrency",
        defaultValue = "4",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "Target concurrency for remote analysis caching RPCs.",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer experimentalRemoteAnalysisCacheConcurrency = new Flag.Integer("experimentalRemoteAnalysisCacheConcurrency");

    @Option(
        name = "experimental_remote_analysis_cache_deadline",
        defaultValue = "120s",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "Deadline to use for remote analysis cache operations.",
        valueHelp = "An immutable length of time.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Duration experimentalRemoteAnalysisCacheDeadline = new Flag.Duration("experimentalRemoteAnalysisCacheDeadline");

    @Option(
        name = "experimental_remote_analysis_cache_mode",
        defaultValue = "off",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "The transport direction for the remote analysis cache.",
        valueHelp = "upload, download or off",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalRemoteAnalysisCacheMode = new Flag.OneOf("experimentalRemoteAnalysisCacheMode");

    @Option(
        name = "experimental_remote_cache_compression_threshold",
        defaultValue = "100",
        help = "\n" +
            "      The minimum blob size required to compress/decompress with zstd. Ineffectual unless\n" +
            "      --remote_cache_compression is set.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer experimentalRemoteCacheCompressionThreshold = new Flag.Integer("experimentalRemoteCacheCompressionThreshold");

    @Option(
        name = "experimental_remote_cache_eviction_retries",
        defaultValue = "5",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The maximum number of attempts to retry if the build encountered a transient remote cache error that would\n" +
            "      otherwise fail the build. Applies for example when artifacts are evicted from the remote cache, or in certain\n" +
            "      cache failure conditions. A non-zero value will implicitly set\n" +
            "      --incompatible_remote_use_new_exit_code_for_lost_inputs to true. A new invocation id will be generated for\n" +
            "      each attempt. If you generate invocation id and provide it to Bazel with --invocation_id, you should not use\n" +
            "      this flag. Instead, set flag --incompatible_remote_use_new_exit_code_for_lost_inputs and check for the exit\n" +
            "      code 39.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer experimentalRemoteCacheEvictionRetries = new Flag.Integer("experimentalRemoteCacheEvictionRetries");

    @Option(
        name = "experimental_remote_cache_lease_extension",
        defaultValue = "false",
        help = "\n" +
            "      If set to true, Bazel will extend the lease for outputs of remote actions during the build by sending\n" +
            "      `FindMissingBlobs` calls periodically to remote cache. The frequency is based on the value of\n" +
            "      `--experimental_remote_cache_ttl`.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRemoteCacheLeaseExtension = new Flag.Boolean("experimentalRemoteCacheLeaseExtension");

    @Option(
        name = "experimental_remote_cache_ttl",
        defaultValue = "3h",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The guaranteed minimal TTL of blobs in the remote cache after their digests are recently referenced e.g. by\n" +
            "      an ActionResult or FindMissingBlobs. Bazel does several optimizations based on the blobs' TTL e.g. doesn't\n" +
            "      repeatedly call GetActionResult in an incremental build. The value should be set slightly less than the real\n" +
            "      TTL since there is a gap between when the server returns the digests and when Bazel receives them.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration experimentalRemoteCacheTtl = new Flag.Duration("experimentalRemoteCacheTtl");

    @Option(
        name = "experimental_remote_capture_corrupted_outputs",
        help = "A path to a directory where the corrupted outputs will be captured to.",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path experimentalRemoteCaptureCorruptedOutputs = new Flag.Path("experimentalRemoteCaptureCorruptedOutputs");

    @Option(
        name = "experimental_remote_discard_merkle_trees",
        defaultValue = "true",
        help = "\n" +
            "      If set to true, discard in-memory copies of the input root's Merkle tree and associated input mappings during\n" +
            "      calls to GetActionResult() and Execute(). This reduces memory usage significantly, but does require Bazel to\n" +
            "      recompute them upon remote cache misses and retries.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRemoteDiscardMerkleTrees = new Flag.Boolean("experimentalRemoteDiscardMerkleTrees");

    @Option(
        name = "experimental_remote_downloader",
        help = "\n" +
            "      A Remote Asset API endpoint URI, to be used as a remote download proxy. The supported schemas are grpc, grpcs\n" +
            "      (grpc with TLS enabled) and unix (local UNIX sockets). If no schema is provided Bazel will default to grpcs.\n" +
            "      See: https://github.com/bazelbuild/remote-apis/blob/master/build/bazel/remote/asset/v1/remote_asset.proto\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str experimentalRemoteDownloader = new Flag.Str("experimentalRemoteDownloader");

    @Option(
        name = "experimental_remote_downloader_local_fallback",
        defaultValue = "false",
        help = "Whether to fall back to the local downloader if remote downloader fails.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRemoteDownloaderLocalFallback = new Flag.Boolean("experimentalRemoteDownloaderLocalFallback");

    @Option(
        name = "experimental_remote_downloader_propagate_credentials",
        defaultValue = "false",
        help = "\n" +
            "      Whether to propagate credentials from netrc and credential helper to the remote downloader server. The server\n" +
            "      implementation needs to support the new `http_header_url:<url-index>:<header-key>` qualifier where the\n" +
            "      `<url-index>` is a 0-based position of the URL inside the FetchBlobRequest's `uris` field. The URL-specific\n" +
            "      headers should take precedence over the global headers.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRemoteDownloaderPropagateCredentials = new Flag.Boolean("experimentalRemoteDownloaderPropagateCredentials");

    @Option(
        name = "experimental_remote_execution_keepalive",
        defaultValue = "false",
        help = "Whether to use keepalive for remote execution calls.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRemoteExecutionKeepalive = new Flag.Boolean("experimentalRemoteExecutionKeepalive");

    @Option(
        name = "experimental_remote_failure_rate_threshold",
        defaultValue = "10",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Sets the allowed number of failure rate in percentage for a specific time window after which it stops calling\n" +
            "      to the remote cache/executor. By default the value is 10. Setting this to 0 means no limitation.\n" +
            "    ",
        valueHelp = "an integer in 0-100 range",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalRemoteFailureRateThreshold = new Flag.Unknown("experimentalRemoteFailureRateThreshold");

    @Option(
        name = "experimental_remote_failure_window_interval",
        defaultValue = "60s",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The interval in which the failure rate of the remote requests are computed. On zero or negative value the\n" +
            "      failure duration is computed the whole duration of the execution.Following units can be used: Days (d), hours\n" +
            "      (h), minutes (m), seconds (s), and milliseconds (ms). If the unit is omitted, the value is interpreted as\n" +
            "      seconds.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration experimentalRemoteFailureWindowInterval = new Flag.Duration("experimentalRemoteFailureWindowInterval");

    @Option(
        name = "experimental_remote_include_extraction_size_threshold",
        defaultValue = "1000000",
        effectTags = {
            OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.EXECUTION,
            OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
        },
        help = "Run remotable C++ include extraction remotely if the file size in bytes exceeds this.",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer experimentalRemoteIncludeExtractionSizeThreshold = new Flag.Integer("experimentalRemoteIncludeExtractionSizeThreshold");

    @Option(
        name = "experimental_remote_mark_tool_inputs",
        defaultValue = "false",
        help = "\n" +
            "      If set to true, Bazel will mark inputs as tool inputs for the remote executor. This can be used to implement\n" +
            "      remote persistent workers.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRemoteMarkToolInputs = new Flag.Boolean("experimentalRemoteMarkToolInputs");

    @Option(
        name = "experimental_remote_merkle_tree_cache",
        defaultValue = "false",
        help = "\n" +
            "      If set to true, Merkle tree calculations will be memoized to improve the remote cache hit checking speed. The\n" +
            "      memory foot print of the cache is controlled by --experimental_remote_merkle_tree_cache_size.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRemoteMerkleTreeCache = new Flag.Boolean("experimentalRemoteMerkleTreeCache");

    @Option(
        name = "experimental_remote_merkle_tree_cache_size",
        defaultValue = "1000",
        help = "\n" +
            "      The number of Merkle trees to memoize to improve the remote cache hit checking speed. Even though the cache\n" +
            "      is automatically pruned according to Java's handling of soft references, out-of-memory errors can occur if\n" +
            "      set too high. If set to 0  the cache size is unlimited. Optimal value varies depending on project's size.\n" +
            "      Default to 1000.\n" +
            "    ",
        valueHelp = "a long integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalRemoteMerkleTreeCacheSize = new Flag.Unknown("experimentalRemoteMerkleTreeCacheSize");

    @Option(
        name = "experimental_remote_output_service",
        help = "\n" +
            "      HOST or HOST:PORT of a remote output service endpoint. The supported schemas are grpc, grpcs (grpc with TLS\n" +
            "      enabled) and unix (local UNIX sockets). If no schema is provided Bazel will default to grpcs. Specify grpc://\n" +
            "      or unix: schema to disable TLS.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str experimentalRemoteOutputService = new Flag.Str("experimentalRemoteOutputService");

    @Option(
        name = "experimental_remote_output_service_output_path_prefix",
        defaultValue = "",
        help = "\n" +
            "      The path under which the contents of output directories managed by the --experimental_remote_output_service\n" +
            "      are placed. The actual output directory used by a build will be a descendant of this path and determined by\n" +
            "      the output service.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str experimentalRemoteOutputServiceOutputPathPrefix = new Flag.Str("experimentalRemoteOutputServiceOutputPathPrefix");

    @Option(
        name = "experimental_remote_require_cached",
        defaultValue = "false",
        help = "\n" +
            "      If set to true, enforce that all actions that can run remotely are cached, or else fail the build. This is\n" +
            "      useful to troubleshoot non-determinism issues as it allows checking whether actions that should be cached are\n" +
            "      actually cached without spuriously injecting new results into the cache.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRemoteRequireCached = new Flag.Boolean("experimentalRemoteRequireCached");

    @Option(
        name = "experimental_remote_scrubbing_config",
        help = "\n" +
            "      Enables remote cache key scrubbing with the supplied configuration file, which must be a protocol buffer in\n" +
            "      text format (see src/main/protobuf/remote_scrubbing.proto).\n" +
            "      \n" +
            "      This feature is intended to facilitate sharing a remote/disk cache between actions executing on different\n" +
            "      platforms but targeting the same platform. It should be used with extreme care, as improper settings may\n" +
            "      cause accidental sharing of cache entries and result in incorrect builds.\n" +
            "      \n" +
            "      Scrubbing does not affect how an action is executed, only how its remote/disk cache key is computed for the\n" +
            "      purpose of retrieving or storing an action result. Scrubbed actions are incompatible with remote execution,\n" +
            "      and will always be executed locally instead.\n" +
            "      \n" +
            "      Modifying the scrubbing configuration does not invalidate outputs present in the local filesystem or internal\n" +
            "      caches; a clean build is required to reexecute affected actions.\n" +
            "      \n" +
            "      In order to successfully use this feature, you likely want to set a custom --host_platform together with\n" +
            "      --experimental_platform_in_output_dir (to normalize output prefixes) and --incompatible_strict_action_env (to\n" +
            "      normalize environment variables).\n" +
            "    ",
        valueHelp = "Converts to a Scrubber",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalRemoteScrubbingConfig = new Flag.Unknown("experimentalRemoteScrubbingConfig");

    @Option(
        name = "experimental_remove_r_classes_from_instrumentation_test_jar",
        defaultValue = "true",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If enabled and the test instruments an application, all the R classes from the test's deploy jar will be\n" +
            "      removed.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalRemoveRClassesFromInstrumentationTestJar = new Flag.Boolean("experimentalRemoveRClassesFromInstrumentationTestJar");

    @Option(
        name = "experimental_repo_remote_exec",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If set to true, repository_rule gains some remote execution capabilities.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRepoRemoteExec = new Flag.Boolean("experimentalRepoRemoteExec");

    @Option(
        name = "experimental_repository_cache_hardlinks",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      If set, the repository cache will hardlink the file in case of a cache hit, rather than copying. This is\n" +
            "      intended to save disk space.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRepositoryCacheHardlinks = new Flag.Boolean("experimentalRepositoryCacheHardlinks");

    @Option(
        name = "experimental_repository_downloader_retries",
        defaultValue = "5",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "The maximum number of attempts to retry a download error. If set to 0, retries are disabled.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer experimentalRepositoryDownloaderRetries = new Flag.Integer("experimentalRepositoryDownloaderRetries");

    @Option(
        name = "experimental_repository_resolved_file",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If non-empty, write a Starlark value with the resolved information of all Starlark repository rules that were\n" +
            "      executed.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalRepositoryResolvedFile = new Flag.Str("experimentalRepositoryResolvedFile");

    @Option(
        name = "experimental_require_availability_info",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalRequireAvailabilityInfo = new Flag.Boolean("experimentalRequireAvailabilityInfo");

    @Option(
        name = "experimental_resolved_file_instead_of_workspace",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "If non-empty read the specified resolved file instead of the WORKSPACE file",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str experimentalResolvedFileInsteadOfWorkspace = new Flag.Str("experimentalResolvedFileInsteadOfWorkspace");

    @Option(
        name = "experimental_retain_test_configuration_across_testonly",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      When enabled, --trim_test_configuration will not trim the test configuration for rules marked testonly=1.\n" +
            "      This is meant to reduce action conflict issues when non-test rules depend on cc_test rules. No effect if\n" +
            "      --trim_test_configuration is false.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalRetainTestConfigurationAcrossTestonly = new Flag.Boolean("experimentalRetainTestConfigurationAcrossTestonly");

    @Option(
        name = "experimental_reuse_include_scanning_threads",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.EXECUTION,
            OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS,
        },
        help = "If enabled core threads of include scanner pool will not die during execution.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalReuseIncludeScanningThreads = new Flag.Boolean("experimentalReuseIncludeScanningThreads");

    @Option(
        name = "experimental_rule_extension_api",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Enable experimental rule extension API and subrule APIs",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRuleExtensionApi = new Flag.Boolean("experimentalRuleExtensionApi");

    @Option(
        name = "experimental_run_android_lint_on_java_rules",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Whether to validate java_* sources.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalRunAndroidLintOnJavaRules = new Flag.Boolean("experimentalRunAndroidLintOnJavaRules");

    @Option(
        name = "experimental_run_bep_event_include_residue",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Whether to include the command-line residue in run build events which could contain the residue. By default,\n" +
            "      the residue is not included in run command build events that could contain the residue.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalRunBepEventIncludeResidue = new Flag.Boolean("experimentalRunBepEventIncludeResidue");

    @Option(
        name = "experimental_sandbox_async_tree_delete_idle_threads",
        defaultValue = "4",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If 0, delete sandbox trees as soon as an action completes (causing completion of the action to be delayed).\n" +
            "      If greater than zero, execute the deletion of such threes on an asynchronous thread pool that has size 1 when\n" +
            "      the build is running and grows to the size specified by this flag when the server is idle.\n" +
            "    ",
        valueHelp = "\n" +
            "      an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation\n" +
            "      ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalSandboxAsyncTreeDeleteIdleThreads = new Flag.Unknown("experimentalSandboxAsyncTreeDeleteIdleThreads");

    @Option(
        name = "experimental_sandbox_enforce_resources_regexp",
        defaultValue = "",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If true, actions whose mnemonic matches the input regex will have their resources request enforced as limits,\n" +
            "      overriding the value of --experimental_sandbox_limits, if the resource type supports it. For example a test\n" +
            "      that declares cpu:3 and resources:memory:10, will run with at most 3 cpus and 10 megabytes of memory.\n" +
            "    ",
        valueHelp = "a valid Java regular expression",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalSandboxEnforceResourcesRegexp = new Flag.Unknown("experimentalSandboxEnforceResourcesRegexp");

    @Option(
        name = "experimental_sandbox_limits",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If > 0, each Linux sandbox will be limited to the given amount for the specified resource. Requires\n" +
            "      --incompatible_use_new_cgroup_implementation and overrides --experimental_sandbox_memory_limit_mb. Requires\n" +
            "      cgroups v1 or v2 and permissions for the users to the cgroups dir.\n" +
            "    ",
        valueHelp = "\n" +
            "      a named double, 'name=value', where value is an integer, or a keyword (\"auto\", \"HOST_CPUS\",\n" +
            "      \"HOST_RAM\"), optionally followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalSandboxLimits = new Flag.Unknown("experimentalSandboxLimits");

    @Option(
        name = "experimental_sandbox_memory_limit_mb",
        defaultValue = "0",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If > 0, each Linux sandbox will be limited to the given amount of memory (in MB). Requires cgroups v1 or v2\n" +
            "      and permissions for the users to the cgroups dir.\n" +
            "    ",
        valueHelp = "an integer number of MBs, or \"HOST_RAM\", optionally followed by [-|*]<float>.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalSandboxMemoryLimitMb = new Flag.Unknown("experimentalSandboxMemoryLimitMb");

    @Option(
        name = "experimental_sandboxfs_map_symlink_targets",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSandboxfsMapSymlinkTargets = new Flag.Boolean("experimentalSandboxfsMapSymlinkTargets");

    @Option(
        name = "experimental_save_feature_state",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Save the state of enabled and requested feautres as an output of compilation.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSaveFeatureState = new Flag.Boolean("experimentalSaveFeatureState");

    @Option(
        name = "experimental_scale_timeouts",
        defaultValue = "1.0",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Scale all timeouts in Starlark repository rules by this factor. In this way, external repositories can be\n" +
            "      made working on machines that are slower than the rule author expected, without changing the source code\n" +
            "    ",
        valueHelp = "a double",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Double experimentalScaleTimeouts = new Flag.Double("experimentalScaleTimeouts");

    @Option(
        name = "experimental_show_artifacts",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalShowArtifacts = new Flag.Boolean("experimentalShowArtifacts");

    @Option(
        name = "experimental_shrink_worker_pool",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      If enabled, could shrink worker pool if worker memory pressure is high. This flag works only when flag\n" +
            "      experimental_total_worker_memory_limit_mb is enabled.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalShrinkWorkerPool = new Flag.Boolean("experimentalShrinkWorkerPool");

    @Option(
        name = "experimental_sibling_repository_layout",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
            OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set to true, non-main repositories are planted as symlinks to the main repository in the execution root.\n" +
            "      That is, all repositories are direct children of the $output_base/execution_root directory. This has the\n" +
            "      side effect of freeing up $output_base/execution_root/__main__/external for the real top-level\n" +
            "      'external' directory.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalSiblingRepositoryLayout = new Flag.Boolean("experimentalSiblingRepositoryLayout");

    @Option(
        name = "experimental_single_package_toolchain_binding",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled, the register_toolchain function may not include target patterns which may refer to more than one\n" +
            "      package.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalSinglePackageToolchainBinding = new Flag.Boolean("experimentalSinglePackageToolchainBinding");

    @Option(
        name = "experimental_skip_ttvs_for_genquery",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, genquery loads its scope's transitive closure directly instead of by using 'TransitiveTargetValue'\n" +
            "      Skyframe work.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSkipTtvsForGenquery = new Flag.Boolean("experimentalSkipTtvsForGenquery");

    @Option(
        name = "experimental_skyfocus_dump_keys",
        defaultValue = "none",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "For debugging Skyfocus. Dump the focused SkyKeys (roots, leafs, focused deps, focused rdeps).",
        valueHelp = "none, count or verbose",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalSkyfocusDumpKeys = new Flag.OneOf("experimentalSkyfocusDumpKeys");

    @Option(
        name = "experimental_skyfocus_dump_post_gc_stats",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      For debugging Skyfocus. If enabled, trigger manual GC before/after focusing to report heap sizes reductions.\n" +
            "      This will increase the Skyfocus latency.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSkyfocusDumpPostGcStats = new Flag.Boolean("experimentalSkyfocusDumpPostGcStats");

    @Option(
        name = "experimental_skyfocus_handling_strategy",
        defaultValue = "strict",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "Strategies for Skyfocus to handle changes outside of the working set.",
        valueHelp = "strict or warn",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalSkyfocusHandlingStrategy = new Flag.OneOf("experimentalSkyfocusHandlingStrategy");

    @Option(
        name = "experimental_skyframe_cpu_heavy_skykeys_thread_pool_size",
        defaultValue = "HOST_CPUS",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set to a positive value (e.g. \"HOST_CPUS*1.5\"), Skyframe will run the loading/analysis phase with 2\n" +
            "      separate thread pools: 1 with <value> threads (ideally close to HOST_CPUS) reserved for CPU-heavy SkyKeys,\n" +
            "      and 1 \"standard\" thread pool (whose size is controlled by --loading_phase_threads) for the rest.\n" +
            "    ",
        valueHelp = "an integer, or \"HOST_CPUS\", optionally followed by [-|*]<float>.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalSkyframeCpuHeavySkykeysThreadPoolSize = new Flag.Unknown("experimentalSkyframeCpuHeavySkykeysThreadPoolSize");

    @Option(
        name = "experimental_skyframe_memory_dump",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Dump the memory use of individual nodes in the Skyframe graph after the build. This option takes a number of\n" +
            "      flags separated by commas: 'json' (no-op, that's the only format), 'notransient' (don't traverse transient\n" +
            "      fields), 'noconfig' (ignore objects related to configurations), 'noprecomputed' (ignore precomputed values)\n" +
            "      and 'noworkspacestatus' (ignore objects related to the workspace status machinery)\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalSkyframeMemoryDump = new Flag.Str("experimentalSkyframeMemoryDump");

    @Option(
        name = "experimental_skyframe_native_filesets",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSkyframeNativeFilesets = new Flag.Boolean("experimentalSkyframeNativeFilesets");

    @Option(
        name = "experimental_skyframe_prepare_analysis",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated. No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSkyframePrepareAnalysis = new Flag.Boolean("experimentalSkyframePrepareAnalysis");

    @Option(
        name = "experimental_skyframe_target_pattern_evaluator",
        defaultValue = "true",
        help = "\n" +
            "      Use the Skyframe-based target pattern evaluator; implies --experimental_interleave_loading_and_analysis.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSkyframeTargetPatternEvaluator = new Flag.Boolean("experimentalSkyframeTargetPatternEvaluator");

    @Option(
        name = "experimental_skylark_debug",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, Blaze will open the Starlark debug server at the start of the build invocation, and wait for a\n" +
            "      debugger to attach before running the build.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSkylarkDebug = new Flag.Boolean("experimentalSkylarkDebug");

    @Option(
        name = "experimental_skylark_debug_reset_analysis",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If true, resets analysis before executing the build. Has no effect without --experimental_skylark_debug\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSkylarkDebugResetAnalysis = new Flag.Boolean("experimentalSkylarkDebugResetAnalysis");

    @Option(
        name = "experimental_skylark_debug_server_port",
        defaultValue = "7300",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "The port on which the Starlark debug server will listen for connections.",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer experimentalSkylarkDebugServerPort = new Flag.Integer("experimentalSkylarkDebugServerPort");

    @Option(
        name = "experimental_skylark_debug_verbose_logging",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Show verbose logs for the debugger.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSkylarkDebugVerboseLogging = new Flag.Boolean("experimentalSkylarkDebugVerboseLogging");

    @Option(
        name = "experimental_skymeld_analysis_overlap_percentage",
        defaultValue = "100",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      The value represents the % of the analysis phase which will be overlapped with the execution phase. A value\n" +
            "      of x means Skyframe will queue up execution tasks and wait until there's x% of the top level target left to\n" +
            "      be analyzed before allowing them to launch. When the value is 0%, we'd wait for all analysis to finish before\n" +
            "      executing (no overlap). When it's 100%, the phases are free to overlap as much as they can.\n" +
            "    ",
        valueHelp = "an integer in 0-100 range",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalSkymeldAnalysisOverlapPercentage = new Flag.Unknown("experimentalSkymeldAnalysisOverlapPercentage");

    @Option(
        name = "experimental_spawn_scheduler",
        expandsTo = {"--internal_spawn_scheduler", "--spawn_strategy=dynamic"},
        help = "\n" +
            "      Enable dynamic execution by running actions locally and remotely in parallel. Bazel spawns each action\n" +
            "      locally and remotely and picks the one that completes first. If an action supports workers, the local action\n" +
            "      will be run in the persistent worker mode. To enable dynamic execution for an individual action mnemonic, use\n" +
            "      the `--internal_spawn_scheduler` and `--strategy=<mnemonic>=dynamic` flags instead.\n" +
            "    ",
        valueHelp = "",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalSpawnScheduler = new Flag.Unknown("experimentalSpawnScheduler");

    @Option(
        name = "experimental_split_coverage_postprocessing",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If true, then Bazel will run coverage postprocessing for test in a new spawn.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSplitCoveragePostprocessing = new Flag.Boolean("experimentalSplitCoveragePostprocessing");

    @Option(
        name = "experimental_split_xml_generation",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If this flag is set, and a test action does not generate a test.xml file, then Bazel uses a separate action\n" +
            "      to generate a dummy test.xml file containing the test log. Otherwise, Bazel generates a test.xml as part of\n" +
            "      the test action.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalSplitXmlGeneration = new Flag.Boolean("experimentalSplitXmlGeneration");

    @Option(
        name = "experimental_starlark_cc_import",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If enabled, the Starlark version of cc_import can be used.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalStarlarkCcImport = new Flag.Boolean("experimentalStarlarkCcImport");

    @Option(
        name = "experimental_stats_summary",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Enable a modernized summary of the build stats.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalStatsSummary = new Flag.Boolean("experimentalStatsSummary");

    @Option(
        name = "experimental_stream_log_file_uploads",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Stream log file uploads directly to the remote storage rather than writing them to disk.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalStreamLogFileUploads = new Flag.Boolean("experimentalStreamLogFileUploads");

    @Option(
        name = "experimental_strict_fileset_output",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If this option is enabled, filesets will treat all output artifacts as regular files. They will not traverse\n" +
            "      directories or be sensitive to symlinks.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalStrictFilesetOutput = new Flag.Boolean("experimentalStrictFilesetOutput");

    @Option(
        name = "experimental_strict_java_deps",
        oldName = "strict_java_deps",
        defaultValue = "default",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      If true, checks that a Java target explicitly declares all directly used targets as dependencies.\n" +
            "    ",
        valueHelp = "off, warn, error, strict or default",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf experimentalStrictJavaDeps = new Flag.OneOf("experimentalStrictJavaDeps");

    @Option(
        name = "experimental_throttle_action_cache_check",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Whether to throttle the check whether an action is cached.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalThrottleActionCacheCheck = new Flag.Boolean("experimentalThrottleActionCacheCheck");

    @Option(
        name = "experimental_throttle_remote_action_building",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Whether to throttle the building of remote action to avoid OOM. Defaults to true.\n" +
            "      \n" +
            "      This is a temporary flag to allow users switch off the behaviour. Once Bazel is smart enough about the\n" +
            "      RAM/CPU usages, this flag will be removed.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalThrottleRemoteActionBuilding = new Flag.Boolean("experimentalThrottleRemoteActionBuilding");

    @Option(
        name = "experimental_tool_command_line",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL, OptionMetadataTag.HIDDEN},
        help = "\n" +
            "      An extra command line to report with this invocation's command line. Useful for tools that invoke Bazel and\n" +
            "      want the original information that the tool received to be logged with the rest of the Bazel invocation.\n" +
            "    ",
        valueHelp = "A command line, either as a simple string, or as a base64-encoded binary form of a CommandLine proto",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalToolCommandLine = new Flag.Unknown("experimentalToolCommandLine");

    @Option(
        name = "experimental_total_worker_memory_limit_mb",
        defaultValue = "0",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      If this limit is greater than zero idle workers might be killed if the total memory usage of all  workers\n" +
            "      exceed the limit.\n" +
            "    ",
        valueHelp = "an integer number of MBs, or \"HOST_RAM\", optionally followed by [-|*]<float>.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalTotalWorkerMemoryLimitMb = new Flag.Unknown("experimentalTotalWorkerMemoryLimitMb");

    @Option(
        name = "experimental_turbine_annotation_processing",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If enabled, turbine is used for all annotation processing",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalTurbineAnnotationProcessing = new Flag.Boolean("experimentalTurbineAnnotationProcessing");

    @Option(
        name = "experimental_ui_debug_all_events",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "Report all events known to the Bazel UI.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalUiDebugAllEvents = new Flag.Boolean("experimentalUiDebugAllEvents");

    @Option(
        name = "experimental_ui_max_stdouterr_bytes",
        defaultValue = "1048576",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The maximum size of the stdout / stderr files that will be printed to the console. -1 implies no limit.\n" +
            "    ",
        valueHelp = "an integer in (-1)-1073741819 range",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown experimentalUiMaxStdouterrBytes = new Flag.Unknown("experimentalUiMaxStdouterrBytes");

    @Option(
        name = "experimental_unsupported_and_brittle_include_scanning",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION, OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Whether to narrow inputs to C/C++ compilation by parsing #include lines from input files. This can improve\n" +
            "      performance and incrementality by decreasing the size of compilation input trees. However, it can also break\n" +
            "      builds because the include scanner does not fully implement C preprocessor semantics. In particular, it does\n" +
            "      not understand dynamic #include directives and ignores preprocessor conditional logic. Use at your own risk.\n" +
            "      Any issues relating to this flag that are filed will be closed.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUnsupportedAndBrittleIncludeScanning = new Flag.Boolean("experimentalUnsupportedAndBrittleIncludeScanning");

    @Option(
        name = "experimental_use_cpp_compile_action_args_params_file",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If enabled, write CppCompileAction exposed action.args to parameters file.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseCppCompileActionArgsParamsFile = new Flag.Boolean("experimentalUseCppCompileActionArgsParamsFile");

    @Option(
        name = "experimental_use_dex_splitter_for_incremental_dexing",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Do not use.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseDexSplitterForIncrementalDexing = new Flag.Boolean("experimentalUseDexSplitterForIncrementalDexing");

    @Option(
        name = "experimental_use_event_based_build_completion_status",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalUseEventBasedBuildCompletionStatus = new Flag.Boolean("experimentalUseEventBasedBuildCompletionStatus");

    @Option(
        name = "experimental_use_hermetic_linux_sandbox",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If set to true, do not mount root, only mount whats provided with sandbox_add_mount_pair. Input files will be\n" +
            "      hardlinked to the sandbox instead of symlinked to from the sandbox. If action input files are located on a\n" +
            "      filesystem different from the sandbox, then the input files will be copied instead.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseHermeticLinuxSandbox = new Flag.Boolean("experimentalUseHermeticLinuxSandbox");

    @Option(
        name = "experimental_use_llvm_covmap",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If specified, Bazel will generate llvm-cov coverage map information rather than gcov when\n" +
            "      collect_code_coverage is enabled.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseLlvmCovmap = new Flag.Boolean("experimentalUseLlvmCovmap");

    @Option(
        name = "experimental_use_new_worker_pool",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Uses a new worker pool implementation (no change in behavior, reimplementation of  the worker pool in order\n" +
            "      to deprecate the use of a third party tool).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseNewWorkerPool = new Flag.Boolean("experimentalUseNewWorkerPool");

    @Option(
        name = "experimental_use_platforms_in_output_dir_legacy_heuristic",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Please only use this flag as part of a suggested migration or testing strategy. Note that the heuristic has\n" +
            "      known deficiencies and it is suggested to migrate to relying on just\n" +
            "      --experimental_override_name_platform_in_output_dir.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUsePlatformsInOutputDirLegacyHeuristic = new Flag.Boolean("experimentalUsePlatformsInOutputDirLegacyHeuristic");

    @Option(
        name = "experimental_use_rtxt_from_merged_resources",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "Use R.txt from the merging action, instead of from the validation action.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseRtxtFromMergedResources = new Flag.Boolean("experimentalUseRtxtFromMergedResources");

    @Option(
        name = "experimental_use_scheduling_middlemen",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated. No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseSchedulingMiddlemen = new Flag.Boolean("experimentalUseSchedulingMiddlemen");

    @Option(
        name = "experimental_use_semaphore_for_jobs",
        defaultValue = "true",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "If set to true, additionally use semaphore to limit number of concurrent jobs.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseSemaphoreForJobs = new Flag.Boolean("experimentalUseSemaphoreForJobs");

    @Option(
        name = "experimental_use_validation_aspect",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Whether to run validation actions using aspect (for parallelism with tests).",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalUseValidationAspect = new Flag.Boolean("experimentalUseValidationAspect");

    @Option(
        name = "experimental_use_windows_sandbox",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Use Windows sandbox to run actions. If \"yes\", the binary provided by --experimental_windows_sandbox_path must\n" +
            "      be valid and correspond to a supported version of sandboxfs. If \"auto\", the binary may be missing or not\n" +
            "      compatible.\n" +
            "    ",
        valueHelp = "a tri-state (auto, yes, no)",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.TriState experimentalUseWindowsSandbox = new Flag.TriState("experimentalUseWindowsSandbox");

    @Option(
        name = "experimental_windows_sandbox_path",
        defaultValue = "BazelSandbox.exe",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Path to the Windows sandbox binary to use when --experimental_use_windows_sandbox is true. If a bare name,\n" +
            "      use the first binary of that name found in the PATH.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalWindowsSandboxPath = new Flag.Str("experimentalWindowsSandboxPath");

    @Option(
        name = "experimental_windows_watchfs",
        defaultValue = "false",
        help = "\n" +
            "      If true, experimental Windows support for --watchfs is enabled. Otherwise --watchfsis a non-op on Windows.\n" +
            "      Make sure to also enable --watchfs.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean experimentalWindowsWatchfs = new Flag.Boolean("experimentalWindowsWatchfs");

    @Option(
        name = "experimental_worker_allowlist",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "If non-empty, only allow using persistent workers with the given worker key mnemonic.",
        valueHelp = "comma-separated set of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalWorkerAllowlist = new Flag.Unknown("experimentalWorkerAllowlist");

    @Option(
        name = "experimental_worker_as_resource",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op, will be removed soon.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalWorkerAsResource = new Flag.Boolean("experimentalWorkerAsResource");

    @Option(
        name = "experimental_worker_cancellation",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "If enabled, Bazel may send cancellation requests to workers that support them.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalWorkerCancellation = new Flag.Boolean("experimentalWorkerCancellation");

    @Option(
        name = "experimental_worker_for_repo_fetching",
        defaultValue = "auto",
        help = "\n" +
            "      The threading mode to use for repo fetching. If set to 'off', no worker thread is used, and the repo fetching\n" +
            "      is subject to restarts. Otherwise, uses a virtual worker thread.\n" +
            "    ",
        valueHelp = "off, platform, virtual or auto",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf experimentalWorkerForRepoFetching = new Flag.OneOf("experimentalWorkerForRepoFetching");

    @Option(
        name = "experimental_worker_memory_limit_mb",
        defaultValue = "0",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      If this limit is greater than zero, workers might be killed if the memory usage of the worker exceeds the\n" +
            "      limit. If not used together with dynamic execution and `--experimental_dynamic_ignore_local_signals=9`, this\n" +
            "      may crash your build.\n" +
            "    ",
        valueHelp = "an integer number of MBs, or \"HOST_RAM\", optionally followed by [-|*]<float>.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalWorkerMemoryLimitMb = new Flag.Unknown("experimentalWorkerMemoryLimitMb");

    @Option(
        name = "experimental_worker_metrics_poll_interval",
        defaultValue = "5s",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      The interval between collecting worker metrics and possibly attempting evictions. Cannot effectively be less\n" +
            "      than 1s for performance reasons.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Duration experimentalWorkerMetricsPollInterval = new Flag.Duration("experimentalWorkerMetricsPollInterval");

    @Option(
        name = "experimental_worker_multiplex_sandboxing",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If enabled, multiplex workers with a 'supports-multiplex-sandboxing' execution requirement will run in a\n" +
            "      sandboxed environment, using a separate sandbox directory per work request. Multiplex workers with the\n" +
            "      execution requirement are always sandboxed when running under the dynamic execution strategy, irrespective of\n" +
            "      this flag.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalWorkerMultiplexSandboxing = new Flag.Boolean("experimentalWorkerMultiplexSandboxing");

    @Option(
        name = "experimental_worker_sandbox_hardening",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If enabled, workers are run in a hardened sandbox, if the implementation allows it. If hardening is enabled\n" +
            "      then tmp directories are distinct for different workers.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalWorkerSandboxHardening = new Flag.Boolean("experimentalWorkerSandboxHardening");

    @Option(
        name = "experimental_worker_sandbox_inmemory_tracking",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      A worker key mnemonic for which the contents of the sandbox directory are tracked in memory. This may improve\n" +
            "      build performance at the cost of additional memory usage. Only affects sandboxed workers. May be specified\n" +
            "      multiple times for different mnemonics.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str experimentalWorkerSandboxInmemoryTracking = new Flag.Str("experimentalWorkerSandboxInmemoryTracking");

    @Option(
        name = "experimental_worker_strict_flagfiles",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If enabled, actions arguments for workers that do not follow the worker specification will cause an error.\n" +
            "      Worker arguments must have exactly one @flagfile argument as the last of its list of arguments.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalWorkerStrictFlagfiles = new Flag.Boolean("experimentalWorkerStrictFlagfiles");

    @Option(
        name = "experimental_worker_use_cgroups_on_linux",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      On linux, run all workers in its own cgroup (without any limits set) and use the cgroup's own resource\n" +
            "      accounting for memory measurements. This is overridden by --experimental_worker_sandbox_hardening for\n" +
            "      sandboxed workers.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalWorkerUseCgroupsOnLinux = new Flag.Boolean("experimentalWorkerUseCgroupsOnLinux");

    @Option(
        name = "experimental_working_set",
        defaultValue = "",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      The working set for Skyfocus. Specify as comma-separated workspace root-relative paths. This is a stateful\n" +
            "      flag. Defining a working set persists it for subsequent invocations, until it is redefined with a new set.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown experimentalWorkingSet = new Flag.Unknown("experimentalWorkingSet");

    @Option(
        name = "experimental_workspace_rules_log_file",
        help = "Log certain Workspace Rules events into this file as delimited WorkspaceEvent protos.",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path experimentalWorkspaceRulesLogFile = new Flag.Path("experimentalWorkspaceRulesLogFile");

    @Option(
        name = "experimental_writable_outputs",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "If true, the file permissions of action outputs are set to 0755 instead of 0555",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean experimentalWritableOutputs = new Flag.Boolean("experimentalWritableOutputs");

    @Option(
        name = "explain",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Causes the build system to explain each executed step of the build. The explanation is written to the\n" +
            "      specified log file.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Path explain = new Flag.Path("explain");

    @Option(
        name = "explicit_java_test_deps",
        defaultValue = "false",
        help = "\n" +
            "      Explicitly specify a dependency to JUnit or Hamcrest in a java_test instead of  accidentally obtaining from\n" +
            "      the TestRunner's deps. Only works for bazel right now.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean explicitJavaTestDeps = new Flag.Boolean("explicitJavaTestDeps");

    @Option(
        name = "expunge",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      If true, clean removes the entire working tree for this %{product} instance, which includes all\n" +
            "      %{product}-created temporary and build output files, and stops the %{product} server if it is running.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"clean"}
    )
    public static final Flag.Boolean expunge = new Flag.Boolean("expunge");

    @Option(
        name = "expunge_async",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        expandsTo = {"--expunge", "--async"},
        help = "\n" +
            "      If specified, clean asynchronously removes the entire working tree for this %{product} instance, which\n" +
            "      includes all %{product}-created temporary and build output files, and stops the %{product} server if it is\n" +
            "      running. When this command completes, it will be safe to execute new commands in the same client, even though\n" +
            "      the deletion may continue in the background.\n" +
            "    ",
        valueHelp = "",
        commands = {"clean"}
    )
    public static final Flag.Unknown expungeAsync = new Flag.Unknown("expungeAsync");

    @Option(
        name = "extension_filter",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Only display the usages of these module extensions and the repos generated by them if their respective flags\n" +
            "      are set. If set, the result graph will only include paths that contain modules using the specified\n" +
            "      extensions. An empty list disables the filter, effectively specifying all possible extensions.\n" +
            "    ",
        valueHelp = "a comma-separated list of <extension>s",
        commands = {"mod"}
    )
    public static final Flag.Unknown extensionFilter = new Flag.Unknown("extensionFilter");

    @Option(
        name = "extension_info",
        defaultValue = "hidden",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Specify how much detail about extension usages to include in the query result. \"Usages\" will only show the\n" +
            "      extensions names, \"repos\" will also include repos imported with use_repo, and \"all\" will also show the other\n" +
            "      repositories generated by extensions.\n" +
            "      \n" +
            "    ",
        valueHelp = "hidden, usages, repos or all",
        commands = {"mod"}
    )
    public static final Flag.OneOf extensionInfo = new Flag.OneOf("extensionInfo");

    @Option(
        name = "extension_usages",
        defaultValue = "",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Specify modules whose extension usages will be displayed in the show_extension query.",
        valueHelp = "a comma-separated list of <module>s",
        commands = {"mod"}
    )
    public static final Flag.Unknown extensionUsages = new Flag.Unknown("extensionUsages");

    @Option(
        name = "extra_execution_platforms",
        defaultValue = "",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The platforms that are available as execution platforms to run actions. Platforms can be specified by exact\n" +
            "      target, or as a target pattern. These platforms will be considered before those declared in the WORKSPACE\n" +
            "      file by register_execution_platforms(). This option may only be set once; later instances will override\n" +
            "      earlier flag settings.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown extraExecutionPlatforms = new Flag.Unknown("extraExecutionPlatforms");

    @Option(
        name = "extra_toolchains",
        allowMultiple = true,
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "\n" +
            "      The toolchain rules to be considered during toolchain resolution. Toolchains can be specified by exact\n" +
            "      target, or as a target pattern. These toolchains will be considered before those declared in the WORKSPACE\n" +
            "      file by register_toolchains().\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown extraToolchains = new Flag.Unknown("extraToolchains");

    @Option(
        name = "extract_data_time",
        defaultValue = "0",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "The time in ms spent on extracting the new bazel version.",
        valueHelp = "a long integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown extractDataTime = new Flag.Unknown("extractDataTime");

    @Option(
        name = "failure_detail_out",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      If set, specifies a location to write a failure_detail protobuf message if the server experiences a failure\n" +
            "      and cannot report it via gRPC, as normal. Otherwise, the location will be\n" +
            "      ${OUTPUT_BASE}/failure_detail.rawproto.\n" +
            "    ",
        valueHelp = "a path",
        commands = {"startup"}
    )
    public static final Flag.Path failureDetailOut = new Flag.Path("failureDetailOut");

    @Option(
        name = "fat_apk_cpu",
        defaultValue = "armeabi-v7a",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op",
        valueHelp = "comma-separated set of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown fatApkCpu = new Flag.Unknown("fatApkCpu");

    @Option(
        name = "fat_apk_hwasan",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op flag. Will be removed in a future release.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean fatApkHwasan = new Flag.Boolean("fatApkHwasan");

    @Option(
        name = "fatal_event_bus_exceptions",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Whether or not to exit if an exception is thrown by an internal EventBus handler. No-op if\n" +
            "      --fatal_async_exceptions_exclusions is available; that flag's behavior is preferentially used.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean fatalEventBusExceptions = new Flag.Boolean("fatalEventBusExceptions");

    @Option(
        name = "fdo_instrument",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Generate binaries with FDO instrumentation. With Clang/LLVM compiler, it also accepts the directory name\n" +
            "      under which the raw profile file(s) will be dumped at runtime.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str fdoInstrument = new Flag.Str("fdoInstrument");

    @Option(
        name = "fdo_optimize",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Use FDO profile information to optimize compilation. Specify the name of a zip file containing a .gcda file\n" +
            "      tree, an afdo file containing an auto profile, or an LLVM profile file. This flag also accepts files\n" +
            "      specified as labels (e.g. `//foo/bar:file.afdo` - you may need to add an `exports_files` directive to the\n" +
            "      corresponding package) and labels pointing to `fdo_profile` targets. This flag will be superseded by the\n" +
            "      `fdo_profile` rule.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str fdoOptimize = new Flag.Str("fdoOptimize");

    @Option(
        name = "fdo_prefetch_hints",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Use cache prefetch hints.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label fdoPrefetchHints = new Flag.Label("fdoPrefetchHints");

    @Option(
        name = "fdo_profile",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "The fdo_profile representing the profile to be used for optimization.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label fdoProfile = new Flag.Label("fdoProfile");

    @Option(
        name = "features",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      The given features will be enabled or disabled by default for targets built in the target configuration.\n" +
            "      Specifying -<feature> will disable the feature. Negative features always override positive ones. See also\n" +
            "      --host_features\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str features = new Flag.Str("features");

    @Option(
        name = "fetch",
        defaultValue = "true",
        help = "\n" +
            "      Allows the command to fetch external dependencies. If set to false, the command will utilize any cached\n" +
            "      version of the dependency, and if none exists, the command will result in failure.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Boolean fetch = new Flag.Boolean("fetch");

    @Option(
        name = "fission",
        defaultValue = "no",
        effectTags = {
            OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.ACTION_COMMAND_LINES,
            OptionEffectTag.AFFECTS_OUTPUTS,
        },
        help = "\n" +
            "      Specifies which compilation modes use fission for C++ compilations and links.  May be any combination of\n" +
            "      {'fastbuild', 'dbg', 'opt'} or the special values 'yes'  to enable all modes and 'no' to disable all modes.\n" +
            "    ",
        valueHelp = "a set of compilation modes",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown fission = new Flag.Unknown("fission");

    @Option(
        name = "flag_alias",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      Sets a shorthand name for a Starlark flag. It takes a single key-value pair in the form \"<key>=<value>\" as an\n" +
            "      argument.\n" +
            "    ",
        valueHelp = "a 'name=value' flag alias",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown flagAlias = new Flag.Unknown("flagAlias");

    @Option(
        name = "flaky_test_attempts",
        defaultValue = "default",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Each test will be retried up to the specified number of times in case of any test failure. Tests that\n" +
            "      required more than one attempt to pass are marked as 'FLAKY' in the test summary. Normally the value\n" +
            "      specified is just an integer or the string 'default'. If an integer, then all tests will be run up to N\n" +
            "      times. If 'default', then only a single test attempt will be made for regular tests and three for tests\n" +
            "      marked explicitly as flaky by their rule (flaky=1 attribute). Alternate syntax:\n" +
            "      regex_filter@flaky_test_attempts. Where flaky_test_attempts is as above and regex_filter stands for a list of\n" +
            "      include and exclude regular expression patterns (Also see --runs_per_test). Example:\n" +
            "      --flaky_test_attempts=//foo/.*,-//foo/bar/.*@3 deflakes all tests in //foo/ except those under foo/bar three\n" +
            "      times. This option can be passed multiple times. The most recently passed argument that matches takes\n" +
            "      precedence. If nothing matches, behavior is as if 'default' above.\n" +
            "    ",
        valueHelp = "\n" +
            "      a positive integer, the string \"default\", or test_regex@attempts. This flag may be passed more than\n" +
            "      once\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown flakyTestAttempts = new Flag.Unknown("flakyTestAttempts");

    @Option(
        name = "for_command",
        defaultValue = "build",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.TERMINAL_OUTPUT},
        help = "The command for which the options should be canonicalized.",
        valueHelp = "a string",
        commands = {"canonicalize-flags"}
    )
    public static final Flag.Str forCommand = new Flag.Str("forCommand");

    @Option(
        name = "force",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      Ignore existing repository if any and force fetch the repository again. Only works when --enable_bzlmod is on.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"fetch"}
    )
    public static final Flag.Boolean force = new Flag.Boolean("force");

    @Option(
        name = "force_ignore_dash_static",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        help = "noop",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean forceIgnoreDashStatic = new Flag.Boolean("forceIgnoreDashStatic");

    @Option(
        name = "force_pic",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If enabled, all C++ compilations produce position-independent code (\"-fPIC\"), links prefer PIC pre-built\n" +
            "      libraries over non-PIC libraries, and links produce position-independent executables (\"-pie\").\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean forcePic = new Flag.Boolean("forcePic");

    @Option(
        name = "force_python",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "No-op, will be removed soon.",
        valueHelp = "PY2 or PY3",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf forcePython = new Flag.OneOf("forcePython");

    @Option(
        name = "from",
        defaultValue = "<root>",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      The module(s) starting from which the dependency graph query will be displayed. Check each query’s\n" +
            "      description for the exact semantics. Defaults to <root>.\n" +
            "      \n" +
            "    ",
        valueHelp = "a comma-separated list of <module>s",
        commands = {"mod"}
    )
    public static final Flag.Unknown from = new Flag.Unknown("from");

    @Option(
        name = "gc_thrashing_limits",
        defaultValue = "1s:2,20s:3,1m:5",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Limits which, if reached, cause GcThrashingDetector to crash Bazel with an OOM. Each limit is specified as\n" +
            "      <period>:<count> where period is a duration and count is a positive integer. If more than\n" +
            "      --gc_thrashing_threshold percent of tenured space (old gen heap) remains occupied after <count> consecutive\n" +
            "      full GCs within <period>, an OOM is triggered. Multiple limits can be specified separated by commas.\n" +
            "    ",
        valueHelp = "comma separated pairs of <period>:<count>",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown gcThrashingLimits = new Flag.Unknown("gcThrashingLimits");

    @Option(
        name = "gc_thrashing_threshold",
        oldName = "experimental_oom_more_eagerly_threshold",
        defaultValue = "100",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      The percent of tenured space occupied (0-100) above which GcThrashingDetector considers memory pressure\n" +
            "      events against its limits (--gc_thrashing_limits). If set to 100, GcThrashingDetector is disabled.\n" +
            "    ",
        valueHelp = "an integer in 0-100 range",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown gcThrashingThreshold = new Flag.Unknown("gcThrashingThreshold");

    @Option(
        name = "generate_json_trace_profile",
        oldName = "experimental_generate_json_trace_profile",
        defaultValue = "auto",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      If enabled, Bazel profiles the build and writes a JSON-format profile into a file in the output base. View\n" +
            "      profile by loading into chrome://tracing. By default Bazel writes the profile for all build-like commands and\n" +
            "      query.\n" +
            "    ",
        valueHelp = "a tri-state (auto, yes, no)",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.TriState generateJsonTraceProfile = new Flag.TriState("generateJsonTraceProfile");

    @Option(
        name = "genrule_strategy",
        defaultValue = "",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Specify how to execute genrules. This flag will be phased out. Instead, use --spawn_strategy=<value> to\n" +
            "      control all actions or --strategy=Genrule=<value> to control genrules only.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown genruleStrategy = new Flag.Unknown("genruleStrategy");

    @Option(
        name = "gnu_format",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION},
        help = "If set, write the version to stdout using the conventions described in the GNU standards.",
        valueHelp = "a boolean",
        commands = {"version"}
    )
    public static final Flag.Boolean gnuFormat = new Flag.Boolean("gnuFormat");

    @Option(
        name = "google_auth_scopes",
        oldName = "auth_scope",
        defaultValue = "https://www.googleapis.com/auth/cloud-platform",
        help = "A comma-separated list of Google Cloud authentication scopes.",
        valueHelp = "comma-separated list of options",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown googleAuthScopes = new Flag.Unknown("googleAuthScopes");

    @Option(
        name = "google_credentials",
        oldName = "auth_credentials",
        help = "\n" +
            "      Specifies the file to get authentication credentials from. See https://cloud.google.com/docs/authentication\n" +
            "      for details.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str googleCredentials = new Flag.Str("googleCredentials");

    @Option(
        name = "google_default_credentials",
        oldName = "auth_enabled",
        defaultValue = "false",
        help = "\n" +
            "      Whether to use 'Google Application Default Credentials' for authentication. See\n" +
            "      https://cloud.google.com/docs/authentication for details. Disabled by default.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean googleDefaultCredentials = new Flag.Boolean("googleDefaultCredentials");

    @Option(
        name = "graph:conditional_edges_limit",
        defaultValue = "4",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      The maximum number of condition labels to show. -1 means no truncation and 0 means no annotation. This option\n" +
            "      is only applicable to --output=graph.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {"query"}
    )
    public static final Flag.Integer graph_conditionalEdgesLimit = new Flag.Integer("graph_conditionalEdgesLimit");

    @Option(
        name = "graph:factored",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      If true, then the graph will be emitted 'factored', i.e. topologically-equivalent nodes will be merged\n" +
            "      together and their labels concatenated. This option is only applicable to --output=graph.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean graph_factored = new Flag.Boolean("graph_factored");

    @Option(
        name = "graph:node_limit",
        defaultValue = "512",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      The maximum length of the label string for a graph node in the output.  Longer labels will be truncated; -1\n" +
            "      means no truncation.  This option is only applicable to --output=graph.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Integer graph_nodeLimit = new Flag.Integer("graph_nodeLimit");

    @Option(
        name = "grpc_keepalive_time",
        help = "\n" +
            "      Configures keep-alive pings for outgoing gRPC connections. If this is set, then Bazel sends pings after this\n" +
            "      much time of no read operations on the connection, but only if there is at least one pending gRPC call. Times\n" +
            "      are treated as second granularity; it is an error to set a value less than one second. By default, keep-alive\n" +
            "      pings are disabled. You should coordinate with the service owner before enabling this setting. For example to\n" +
            "      set a value of 30 seconds to this flag, it should be done as this --grpc_keepalive_time=30s\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration grpcKeepaliveTime = new Flag.Duration("grpcKeepaliveTime");

    @Option(
        name = "grpc_keepalive_timeout",
        defaultValue = "20s",
        help = "\n" +
            "      Configures a keep-alive timeout for outgoing gRPC connections. If keep-alive pings are enabled with\n" +
            "      --grpc_keepalive_time, then Bazel times out a connection if it does not receive a ping reply after this much\n" +
            "      time. Times are treated as second granularity; it is an error to set a value less than one second. If\n" +
            "      keep-alive pings are disabled, then this setting is ignored.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration grpcKeepaliveTimeout = new Flag.Duration("grpcKeepaliveTimeout");

    @Option(
        name = "grte_top",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      A label to a checked-in libc library. The default value is selected by the crosstool toolchain, and you\n" +
            "      almost never need to override it.\n" +
            "    ",
        valueHelp = "a label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown grteTop = new Flag.Unknown("grteTop");

    @Option(
        name = "heap_dump_on_oom",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      Whether to manually output a heap dump if an OOM is thrown (including manual OOMs due to reaching\n" +
            "      --gc_thrashing_limits). The dump will be written to <output_base>/<invocation_id>.heapdump.hprof. This option\n" +
            "      effectively replaces -XX:+HeapDumpOnOutOfMemoryError, which has no effect for manual OOMs.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean heapDumpOnOom = new Flag.Boolean("heapDumpOnOom");

    @Option(
        name = "help_verbosity",
        defaultValue = "medium",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Select the verbosity of the help command.",
        valueHelp = "long, medium or short",
        commands = {"help"}
    )
    public static final Flag.OneOf helpVerbosity = new Flag.OneOf("helpVerbosity");

    @Option(
        name = "heuristically_drop_nodes",
        oldName = "experimental_heuristically_drop_nodes",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      If true, Blaze will remove FileState and DirectoryListingState nodes after related File and DirectoryListing\n" +
            "      node is done to save memory. We expect that it is less likely that these nodes will be needed again. If so,\n" +
            "      the program will re-evaluate them.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean heuristicallyDropNodes = new Flag.Boolean("heuristicallyDropNodes");

    @Option(
        name = "hide_aspect_results",
        defaultValue = "",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Comma-separated list of aspect names to not display in results (see --show_result). Useful for keeping\n" +
            "      aspects added by wrappers which are typically not interesting to end users out of console output.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown hideAspectResults = new Flag.Unknown("hideAspectResults");

    @Option(
        name = "high_priority_workers",
        allowMultiple = true,
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op, will be removed soon.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str highPriorityWorkers = new Flag.Str("highPriorityWorkers");

    @Option(
        name = "home_rc",
        defaultValue = "true",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "Whether or not to look for the home bazelrc file at ${'$'}HOME/.bazelrc",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean homeRc = new Flag.Boolean("homeRc");

    @Option(
        name = "host_action_env",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "\n" +
            "      Specifies the set of environment variables available to actions with execution configurations. Variables can\n" +
            "      be either specified by name, in which case the value will be taken from the invocation environment, or by the\n" +
            "      name=value pair which sets the value independent of the invocation environment. This option can be used\n" +
            "      multiple times; for options given for the same variable, the latest wins, options for different variables\n" +
            "      accumulate.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment with an optional value part",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown hostActionEnv = new Flag.Unknown("hostActionEnv");

    @Option(
        name = "host_compilation_mode",
        defaultValue = "opt",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.ACTION_COMMAND_LINES},
        help = "\n" +
            "      Specify the mode the tools used during the build will be built in. Values: 'fastbuild', 'dbg', 'opt'.\n" +
            "    ",
        valueHelp = "fastbuild, dbg or opt",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf hostCompilationMode = new Flag.OneOf("hostCompilationMode");

    @Option(
        name = "host_compiler",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        help = "No-op flag. Will be removed in a future release.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostCompiler = new Flag.Str("hostCompiler");

    @Option(
        name = "host_conlyopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Additional option to pass to the C compiler when compiling C (but not C++) source files in the exec\n" +
            "      configurations.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostConlyopt = new Flag.Str("hostConlyopt");

    @Option(
        name = "host_copt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional options to pass to the C compiler for tools built in the exec configurations.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostCopt = new Flag.Str("hostCopt");

    @Option(
        name = "host_cpu",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "The host CPU.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostCpu = new Flag.Str("hostCpu");

    @Option(
        name = "host_crosstool_top",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op flag. Will be removed in a future release.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label hostCrosstoolTop = new Flag.Label("hostCrosstoolTop");

    @Option(
        name = "host_cxxopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional options to pass to C++ compiler for tools built in the exec configurations.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostCxxopt = new Flag.Str("hostCxxopt");

    @Option(
        name = "host_features",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      The given features will be enabled or disabled by default for targets built in the exec configuration.\n" +
            "      Specifying -<feature> will disable the feature. Negative features always override positive ones.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostFeatures = new Flag.Str("hostFeatures");

    @Option(
        name = "host_force_python",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Overrides the Python version for the exec configuration. Can be \"PY2\" or \"PY3\".",
        valueHelp = "PY2 or PY3",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf hostForcePython = new Flag.OneOf("hostForcePython");

    @Option(
        name = "host_grte_top",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If specified, this setting overrides the libc top-level directory (--grte_top) for the exec configuration.\n" +
            "    ",
        valueHelp = "a label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown hostGrteTop = new Flag.Unknown("hostGrteTop");

    @Option(
        name = "host_java_launcher",
        help = "The Java launcher used by tools that are executed during a build.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label hostJavaLauncher = new Flag.Label("hostJavaLauncher");

    @Option(
        name = "host_java_toolchain",
        help = "No-op. Kept here for backwards compatibility.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label hostJavaToolchain = new Flag.Label("hostJavaToolchain");

    @Option(
        name = "host_javabase",
        help = "No-op.  Kept here for backwards compatibility.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label hostJavabase = new Flag.Label("hostJavabase");

    @Option(
        name = "host_javacopt",
        allowMultiple = true,
        help = "Additional options to pass to javac when building tools that are executed during a build.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostJavacopt = new Flag.Str("hostJavacopt");

    @Option(
        name = "host_jvm_args",
        allowMultiple = true,
        help = "Flags to pass to the JVM executing Blaze.",
        valueHelp = "a string",
        commands = {"startup"}
    )
    public static final Flag.Str hostJvmArgs = new Flag.Str("hostJvmArgs");

    @Option(
        name = "host_jvm_debug",
        expandsTo = {"--host_jvm_args=-Xdebug", "--host_jvm_args=-Xrunjdwp:transport=dt_socket,server=y,address=5005"},
        help = "\n" +
            "      Convenience option to add some additional JVM startup flags, which cause the JVM to wait during startup until\n" +
            "      you connect from a JDWP-compliant debugger (like Eclipse) to port 5005.\n" +
            "    ",
        valueHelp = "",
        commands = {"startup"}
    )
    public static final Flag.Unknown hostJvmDebug = new Flag.Unknown("hostJvmDebug");

    @Option(
        name = "host_jvmopt",
        allowMultiple = true,
        help = "\n" +
            "      Additional options to pass to the Java VM when building tools that are executed during  the build. These\n" +
            "      options will get added to the VM startup options of each  java_binary target.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostJvmopt = new Flag.Str("hostJvmopt");

    @Option(
        name = "host_linkopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional option to pass to linker when linking tools in the exec configurations.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str hostLinkopt = new Flag.Str("hostLinkopt");

    @Option(
        name = "host_macos_minimum_os",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "Minimum compatible macOS version for host targets. If unspecified, uses 'macos_sdk_version'.",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown hostMacosMinimumOs = new Flag.Unknown("hostMacosMinimumOs");

    @Option(
        name = "host_per_file_copt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Additional options to selectively pass to the C/C++ compiler when compiling certain files in the exec\n" +
            "      configurations. This option can be passed multiple times. Syntax:\n" +
            "      regex_filter@option_1,option_2,...,option_n. Where regex_filter stands for a list of include and exclude\n" +
            "      regular expression patterns (Also see --instrumentation_filter). option_1 to option_n stand for arbitrary\n" +
            "      command line options. If an option contains a comma it has to be quoted with a backslash. Options can contain\n" +
            "      @. Only the first @ is used to split the string. Example:\n" +
            "      --host_per_file_copt=//foo/.*\\.cc,-//foo/bar\\.cc@-O0 adds the -O0 command line option to the gcc command line\n" +
            "      of all cc files in //foo/ except bar.cc.\n" +
            "    ",
        valueHelp = "\n" +
            "      a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by\n" +
            "      an @ and a comma separated list of options\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown hostPerFileCopt = new Flag.Unknown("hostPerFileCopt");

    @Option(
        name = "host_platform",
        oldName = "experimental_host_platform",
        defaultValue = "@bazel_tools//tools:host_platform",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "The label of a platform rule that describes the host system.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label hostPlatform = new Flag.Label("hostPlatform");

    @Option(
        name = "http_connector_attempts",
        defaultValue = "8",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "The maximum number of attempts for http downloads.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer httpConnectorAttempts = new Flag.Integer("httpConnectorAttempts");

    @Option(
        name = "http_connector_retry_max_timeout",
        defaultValue = "0s",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      The maximum timeout for http download retries. With a value of 0, no timeout maximum is defined.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration httpConnectorRetryMaxTimeout = new Flag.Duration("httpConnectorRetryMaxTimeout");

    @Option(
        name = "http_max_parallel_downloads",
        defaultValue = "8",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "The maximum number parallel http downloads.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer httpMaxParallelDownloads = new Flag.Integer("httpMaxParallelDownloads");

    @Option(
        name = "http_timeout_scaling",
        defaultValue = "1.0",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "Scale all timeouts related to http downloads by the given factor",
        valueHelp = "a double",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Double httpTimeoutScaling = new Flag.Double("httpTimeoutScaling");

    @Option(
        name = "idle_server_tasks",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "Run System.gc() when the server is idle",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean idleServerTasks = new Flag.Boolean("idleServerTasks");

    @Option(
        name = "iff_heap_size_greater_than",
        defaultValue = "0",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      Iff non-zero, then shutdown will only shut down the server if the total memory (in MB) consumed by the JVM\n" +
            "      exceeds this value.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {"shutdown"}
    )
    public static final Flag.Integer iffHeapSizeGreaterThan = new Flag.Integer("iffHeapSizeGreaterThan");

    @Option(
        name = "ignore_all_rc_files",
        defaultValue = "false",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      Disables all rc files, regardless of the values of other rc-modifying flags, even if these flags come later\n" +
            "      in the list of startup options.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean ignoreAllRcFiles = new Flag.Boolean("ignoreAllRcFiles");

    @Option(
        name = "ignore_dev_dependency",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If true, Bazel ignores `bazel_dep` and `use_extension` declared as `dev_dependency` in the MODULE.bazel of\n" +
            "      the root module. Note that, those dev dependencies are always ignored in the MODULE.bazel if it's not the\n" +
            "      root module regardless of the value of this flag.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean ignoreDevDependency = new Flag.Boolean("ignoreDevDependency");

    @Option(
        name = "ignore_unsupported_sandboxing",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Do not print a warning when sandboxed execution is not supported on this system.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean ignoreUnsupportedSandboxing = new Flag.Boolean("ignoreUnsupportedSandboxing");

    @Option(
        name = "implicit_deps",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "\n" +
            "      If enabled, implicit dependencies will be included in the dependency graph over which the query operates. An\n" +
            "      implicit dependency is one that is not explicitly specified in the BUILD file but added by bazel. For cquery,\n" +
            "      this option controls filtering resolved toolchains.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean implicitDeps = new Flag.Boolean("implicitDeps");

    @Option(
        name = "include_artifacts",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Includes names of the action inputs and outputs in the output (potentially large).",
        valueHelp = "a boolean",
        commands = {"aquery"}
    )
    public static final Flag.Boolean includeArtifacts = new Flag.Boolean("includeArtifacts");

    @Option(
        name = "include_aspects",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      aquery, cquery: whether to include aspect-generated actions in the output. query: no-op (aspects are always\n" +
            "      followed).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean includeAspects = new Flag.Boolean("includeAspects");

    @Option(
        name = "include_builtin",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Include built-in modules in the dependency graph. Disabled by default because it is quite noisy.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"mod"}
    )
    public static final Flag.Boolean includeBuiltin = new Flag.Boolean("includeBuiltin");

    @Option(
        name = "include_commandline",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Includes the content of the action command lines in the output (potentially large).",
        valueHelp = "a boolean",
        commands = {"aquery"}
    )
    public static final Flag.Boolean includeCommandline = new Flag.Boolean("includeCommandline");

    @Option(
        name = "include_config_fragments_provider",
        defaultValue = "off",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "\n" +
            "      INTERNAL BLAZE DEVELOPER FEATURE: If \"direct\", all configured targets expose RequiredConfigFragmentsProvider\n" +
            "      with the configuration fragments they directly require. If \"transitive\", they do the same but also include\n" +
            "      the fragments their transitive dependencies require. If \"off\", the provider is omitted. If not \"off\", this\n" +
            "      also populates config_setting's ConfigMatchingProvider.requiredFragmentOptions with the fragment options the\n" +
            "      config_setting requires.Be careful using this feature: it adds memory to every configured target in the build.\n" +
            "    ",
        valueHelp = "off, direct or transitive",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf includeConfigFragmentsProvider = new Flag.OneOf("includeConfigFragmentsProvider");

    @Option(
        name = "include_file_write_contents",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Include the file contents for the FileWrite, SourceSymlinkManifest, and RepoMappingManifest actions\n" +
            "      (potentially large).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery"}
    )
    public static final Flag.Boolean includeFileWriteContents = new Flag.Boolean("includeFileWriteContents");

    @Option(
        name = "include_param_files",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Include the content of the param files used in the command (potentially large). Note: Enabling this flag will\n" +
            "      automatically enable the --include_commandline flag.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery"}
    )
    public static final Flag.Boolean includeParamFiles = new Flag.Boolean("includeParamFiles");

    @Option(
        name = "include_pruned_inputs",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Includes action inputs that were pruned during action execution. Only affects actions that discover inputs\n" +
            "      and have been executed in a previous invocation. Only takes effect if --include_artifacts is also set.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery"}
    )
    public static final Flag.Boolean includePrunedInputs = new Flag.Boolean("includePrunedInputs");

    @Option(
        name = "include_unused",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      The queries will also take into account and display the unused modules, which are not present in the module\n" +
            "      resolution graph after selection (due to the Minimal-Version Selection or override rules). This can have\n" +
            "      different effects for each of the query types i.e. include new paths in the all_paths command, or extra\n" +
            "      dependants in the explain command.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"mod"}
    )
    public static final Flag.Boolean includeUnused = new Flag.Boolean("includeUnused");

    @Option(
        name = "incompatible_allow_python_version_transitions",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op, will be removed soon.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleAllowPythonVersionTransitions = new Flag.Boolean("incompatibleAllowPythonVersionTransitions");

    @Option(
        name = "incompatible_allow_tags_propagation",
        oldName = "experimental_allow_tags_propagation",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      If set to true, tags will be propagated from a target to the actions' execution requirements; otherwise tags\n" +
            "      are not propagated. See https://github.com/bazelbuild/bazel/issues/8830 for details.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleAllowTagsPropagation = new Flag.Boolean("incompatibleAllowTagsPropagation");

    @Option(
        name = "incompatible_always_check_depset_elements",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      Check the validity of elements added to depsets, in all constructors. Elements must be immutable, but\n" +
            "      historically the depset(direct=...) constructor forgot to check. Use tuples instead of lists in depset\n" +
            "      elements. See https://github.com/bazelbuild/bazel/issues/10313 for details.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleAlwaysCheckDepsetElements = new Flag.Boolean("incompatibleAlwaysCheckDepsetElements");

    @Option(
        name = "incompatible_always_include_files_in_data",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, native rules add <code>DefaultInfo.files</code> of data dependencies to their runfiles, which\n" +
            "      matches the recommended behavior for Starlark rules\n" +
            "      (https://bazel.build/extending/rules#runfiles_features_to_avoid).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleAlwaysIncludeFilesInData = new Flag.Boolean("incompatibleAlwaysIncludeFilesInData");

    @Option(
        name = "incompatible_auto_configure_host_platform",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "This option is deprecated and has no effect.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleAutoConfigureHostPlatform = new Flag.Boolean("incompatibleAutoConfigureHostPlatform");

    @Option(
        name = "incompatible_auto_exec_groups",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When enabled, an exec groups is automatically created for each toolchain used by a rule. For this to work\n" +
            "      rule needs to specify `toolchain` parameter on its actions. For more information, see\n" +
            "      https://github.com/bazelbuild/bazel/issues/17134.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleAutoExecGroups = new Flag.Boolean("incompatibleAutoExecGroups");

    @Option(
        name = "incompatible_autoload_externally",
        defaultValue = "\n" +
            "      +@rules_python,+java_common,+JavaInfo,+JavaPluginInfo,ProguardSpecProvider,java_binary,\n" +
            "      java_import,java_library,java_plugin,java_test,java_runtime,java_toolchain,java_package_configuration,\n" +
            "      @com_google_protobuf,@rules_shell,+@rules_android\n" +
            "    ",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      A comma-separated list of rules (or other symbols) that were previously part of Bazel and which are now to be\n" +
            "      retrieved from their respective external repositories. This flag is intended to be used to facilitate\n" +
            "      migration of rules out of Bazel. See also https://github.com/bazelbuild/bazel/issues/23043.\n" +
            "      A symbol that is autoloaded within a file behaves as if its built-into-Bazel definition were replaced by its\n" +
            "      canonical new definition in an external repository. For a BUILD file, this essentially means implicitly\n" +
            "      adding a load() statement. For a .bzl file, it's either a load() statement or a change to a field of the\n" +
            "      `native` object, depending on whether the autoloaded symbol is a rule.\n" +
            "      Bazel maintains a hardcoded list of all symbols that may be autoloaded; only those symbols may appear in this\n" +
            "      flag. For each symbol, Bazel knows the new definition location in an external repository, as well as a set of\n" +
            "      special-cased repositories that must not autoload it to avoid creating cycles.\n" +
            "      A list item of \"+foo\" in this flag causes symbol foo to be autoloaded, except in foo's exempt repositories,\n" +
            "      within which the Bazel-defined version of foo is still available.\n" +
            "      A list item of \"foo\" triggers autoloading as above, but the Bazel-defined version of foo is not made\n" +
            "      available to the excluded repositories. This ensures that foo's external repository does not depend on the\n" +
            "      old Bazel implementation of foo\n" +
            "      A list item of \"-foo\" does not trigger any autoloading, but makes the Bazel-defined version of foo\n" +
            "      inaccessible throughout the workspace. This is used to validate that the workspace is ready for foo's\n" +
            "      definition to be deleted from Bazel.\n" +
            "      If a symbol is not named in this flag then it continues to work as normal -- no autoloading is done, nor is\n" +
            "      the Bazel-defined version suppressed. For configuration see\n" +
            "      https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/packages/AutoloadSymbols.java\n" +
            "      As a shortcut also whole repository may be used, for example +@rules_python will autoload all Python rules.\n" +
            "    ",
        valueHelp = "comma-separated set of options",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown incompatibleAutoloadExternally = new Flag.Unknown("incompatibleAutoloadExternally");

    @Option(
        name = "incompatible_avoid_hardcoded_objc_compilation_flags",
        defaultValue = "false",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION,
            OptionEffectTag.EXECUTION, OptionEffectTag.ACTION_COMMAND_LINES,
        },
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      Prevents Bazel from adding compiler options to Objective-C compilation actions. Options set in the crosstool\n" +
            "      are still applied.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleAvoidHardcodedObjcCompilationFlags = new Flag.Boolean("incompatibleAvoidHardcodedObjcCompilationFlags");

    @Option(
        name = "incompatible_bazel_test_exec_run_under",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled, \"bazel test --run_under=//:runner\" builds \"//:runner\" in the exec configuration. If disabled, it\n" +
            "      builds \"//:runner\" in the target configuration. Bazel executes tests on exec machines, so the former is more\n" +
            "      correct. This doesn't affect \"bazel run\", which always builds \"`--run_under=//foo\" in the target\n" +
            "      configuration.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleBazelTestExecRunUnder = new Flag.Boolean("incompatibleBazelTestExecRunUnder");

    @Option(
        name = "incompatible_check_sharding_support",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, Bazel will fail a sharded test if the test runner does not indicate that it supports sharding by\n" +
            "      touching the file at the path in TEST_SHARD_STATUS_FILE. If false, a test runner that does not support\n" +
            "      sharding will lead to all tests running in each shard.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleCheckShardingSupport = new Flag.Boolean("incompatibleCheckShardingSupport");

    @Option(
        name = "incompatible_check_testonly_for_output_files",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled, check testonly for prerequisite targets that are output files by looking up the testonly of the\n" +
            "      generating rule. This matches visibility checking.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleCheckTestonlyForOutputFiles = new Flag.Boolean("incompatibleCheckTestonlyForOutputFiles");

    @Option(
        name = "incompatible_check_visibility_for_toolchains",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If enabled, visibility checking also applies to toolchain implementations.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleCheckVisibilityForToolchains = new Flag.Boolean("incompatibleCheckVisibilityForToolchains");

    @Option(
        name = "incompatible_config_setting_private_default_visibility",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If incompatible_enforce_config_setting_visibility=false, this is a noop. Else, if this flag is false, any\n" +
            "      config_setting without an explicit visibility attribute is //visibility:public. If this flag is true,\n" +
            "      config_setting follows the same visibility logic as all other rules. See\n" +
            "      https://github.com/bazelbuild/bazel/issues/12933.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleConfigSettingPrivateDefaultVisibility = new Flag.Boolean("incompatibleConfigSettingPrivateDefaultVisibility");

    @Option(
        name = "incompatible_default_to_explicit_init_py",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      This flag changes the default behavior so that __init__.py files are no longer automatically created in the\n" +
            "      runfiles of Python targets. Precisely, when a py_binary or py_test target has legacy_create_init set to\n" +
            "      \"auto\" (the default), it is treated as false if and only if this flag is set. See\n" +
            "      https://github.com/bazelbuild/bazel/issues/10076.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDefaultToExplicitInitPy = new Flag.Boolean("incompatibleDefaultToExplicitInitPy");

    @Option(
        name = "incompatible_depset_for_java_output_source_jars",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDepsetForJavaOutputSourceJars = new Flag.Boolean("incompatibleDepsetForJavaOutputSourceJars");

    @Option(
        name = "incompatible_depset_for_libraries_to_link_getter",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When true, Bazel no longer returns a list from linking_context.libraries_to_link but returns a depset instead.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDepsetForLibrariesToLinkGetter = new Flag.Boolean("incompatibleDepsetForLibrariesToLinkGetter");

    @Option(
        name = "incompatible_disable_legacy_cc_provider",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op flag. Will be removed in a future release.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisableLegacyCcProvider = new Flag.Boolean("incompatibleDisableLegacyCcProvider");

    @Option(
        name = "incompatible_disable_legacy_flags_cc_toolchain_api",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE, OptionMetadataTag.DEPRECATED},
        help = "Flag for disabling the legacy cc_toolchain Starlark API for accessing legacy CROSSTOOL fields.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisableLegacyFlagsCcToolchainApi = new Flag.Boolean("incompatibleDisableLegacyFlagsCcToolchainApi");

    @Option(
        name = "incompatible_disable_native_android_rules",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled, direct usage of the native Android rules is disabled. Please use the Starlark Android rules from\n" +
            "      https://github.com/bazelbuild/rules_android\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisableNativeAndroidRules = new Flag.Boolean("incompatibleDisableNativeAndroidRules");

    @Option(
        name = "incompatible_disable_native_apple_binary_rule",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op. Kept here for backwards compatibility.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisableNativeAppleBinaryRule = new Flag.Boolean("incompatibleDisableNativeAppleBinaryRule");

    @Option(
        name = "incompatible_disable_native_repo_rules",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      If false, native repo rules can be used in WORKSPACE; otherwise, Starlark repo rules must be used instead.\n" +
            "      Native repo rules include local_repository, new_local_repository, local_config_platform, and\n" +
            "      android_sdk_repository.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisableNativeRepoRules = new Flag.Boolean("incompatibleDisableNativeRepoRules");

    @Option(
        name = "incompatible_disable_nocopts",
        defaultValue = "true",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When enabled, it removes nocopts attribute from C++ rules. See\n" +
            "      https://github.com/bazelbuild/bazel/issues/8706 for details.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisableNocopts = new Flag.Boolean("incompatibleDisableNocopts");

    @Option(
        name = "incompatible_disable_non_executable_java_binary",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If true, java_binary is always executable. create_executable attribute is removed.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisableNonExecutableJavaBinary = new Flag.Boolean("incompatibleDisableNonExecutableJavaBinary");

    @Option(
        name = "incompatible_disable_objc_library_transition",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      Disable objc_library's custom transition and inherit from the top level target instead (No-op in Bazel)\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisableObjcLibraryTransition = new Flag.Boolean("incompatibleDisableObjcLibraryTransition");

    @Option(
        name = "incompatible_disable_starlark_host_transitions",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, rule attributes cannot set 'cfg = \"host\"'. Rules should set 'cfg = \"exec\"' instead.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisableStarlarkHostTransitions = new Flag.Boolean("incompatibleDisableStarlarkHostTransitions");

    @Option(
        name = "incompatible_disable_target_default_provider_fields",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, disable the ability to access providers on 'target' objects via field syntax. Use\n" +
            "      provider-key syntax instead. For example, instead of using `ctx.attr.dep.my_info` to access `my_info` from\n" +
            "      inside a rule implementation function, use `ctx.attr.dep[MyInfo]`. See\n" +
            "      https://github.com/bazelbuild/bazel/issues/9014 for details.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisableTargetDefaultProviderFields = new Flag.Boolean("incompatibleDisableTargetDefaultProviderFields");

    @Option(
        name = "incompatible_disable_target_provider_fields",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, disable the ability to utilize the default provider via field syntax. Use provider-key syntax\n" +
            "      instead. For example, instead of using `ctx.attr.dep.files` to access `files`, utilize\n" +
            "      `ctx.attr.dep[DefaultInfo].files See https://github.com/bazelbuild/bazel/issues/9014 for details.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisableTargetProviderFields = new Flag.Boolean("incompatibleDisableTargetProviderFields");

    @Option(
        name = "incompatible_disallow_ctx_resolve_tools",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, calling the deprecated ctx.resolve_tools API always fails. Uses of this API should be\n" +
            "      replaced by an executable or tools argument to ctx.actions.run or ctx.actions.run_shell.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisallowCtxResolveTools = new Flag.Boolean("incompatibleDisallowCtxResolveTools");

    @Option(
        name = "incompatible_disallow_empty_glob",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If set to true, the default value of the `allow_empty` argument of glob() is False.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisallowEmptyGlob = new Flag.Boolean("incompatibleDisallowEmptyGlob");

    @Option(
        name = "incompatible_disallow_java_import_empty_jars",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "When enabled, empty java_import.jars is not supported.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisallowJavaImportEmptyJars = new Flag.Boolean("incompatibleDisallowJavaImportEmptyJars");

    @Option(
        name = "incompatible_disallow_java_import_exports",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "When enabled, java_import.exports is not supported.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisallowJavaImportExports = new Flag.Boolean("incompatibleDisallowJavaImportExports");

    @Option(
        name = "incompatible_disallow_legacy_py_provider",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op, will be removed soon.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisallowLegacyPyProvider = new Flag.Boolean("incompatibleDisallowLegacyPyProvider");

    @Option(
        name = "incompatible_disallow_resource_jars",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op, kept only for backwards compatibility",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisallowResourceJars = new Flag.Boolean("incompatibleDisallowResourceJars");

    @Option(
        name = "incompatible_disallow_sdk_frameworks_attributes",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, disallow sdk_frameworks and weak_sdk_frameworks attributes in objc_library andobjc_import.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDisallowSdkFrameworksAttributes = new Flag.Boolean("incompatibleDisallowSdkFrameworksAttributes");

    @Option(
        name = "incompatible_disallow_struct_provider_syntax",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, rule implementation functions may not return a struct. They must instead return a list of\n" +
            "      provider instances.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDisallowStructProviderSyntax = new Flag.Boolean("incompatibleDisallowStructProviderSyntax");

    @Option(
        name = "incompatible_do_not_split_linking_cmdline",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When true, Bazel no longer modifies command line flags used for linking, and also doesn't selectively decide\n" +
            "      which flags go to the param file and which don't.  See https://github.com/bazelbuild/bazel/issues/7670 for\n" +
            "      details.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleDoNotSplitLinkingCmdline = new Flag.Boolean("incompatibleDoNotSplitLinkingCmdline");

    @Option(
        name = "incompatible_dont_collect_native_libraries_in_data",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "This flag is a noop and scheduled for removal.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDontCollectNativeLibrariesInData = new Flag.Boolean("incompatibleDontCollectNativeLibrariesInData");

    @Option(
        name = "incompatible_dont_enable_host_nonhost_crosstool_features",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, Bazel will not enable 'host' and 'nonhost' features in the c++ toolchain (see\n" +
            "      https://github.com/bazelbuild/bazel/issues/7407 for more information).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDontEnableHostNonhostCrosstoolFeatures = new Flag.Boolean("incompatibleDontEnableHostNonhostCrosstoolFeatures");

    @Option(
        name = "incompatible_dont_use_javasourceinfoprovider",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleDontUseJavasourceinfoprovider = new Flag.Boolean("incompatibleDontUseJavasourceinfoprovider");

    @Option(
        name = "incompatible_enable_android_toolchain_resolution",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleEnableAndroidToolchainResolution = new Flag.Boolean("incompatibleEnableAndroidToolchainResolution");

    @Option(
        name = "incompatible_enable_apple_toolchain_resolution",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "Use toolchain resolution to select the Apple SDK for apple rules (Starlark and native)",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleEnableAppleToolchainResolution = new Flag.Boolean("incompatibleEnableAppleToolchainResolution");

    @Option(
        name = "incompatible_enable_cc_test_feature",
        defaultValue = "false",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When enabled, it switches Crosstool to use feature 'is_cc_test' rather than the link-time build variable of\n" +
            "      the same name.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleEnableCcTestFeature = new Flag.Boolean("incompatibleEnableCcTestFeature");

    @Option(
        name = "incompatible_enable_cc_toolchain_resolution",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op flag. Will be removed in a future release.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleEnableCcToolchainResolution = new Flag.Boolean("incompatibleEnableCcToolchainResolution");

    @Option(
        name = "incompatible_enable_deprecated_label_apis",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If enabled, certain deprecated APIs (native.repository_name, Label.workspace_name, Label.relative) can be\n" +
            "      used.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleEnableDeprecatedLabelApis = new Flag.Boolean("incompatibleEnableDeprecatedLabelApis");

    @Option(
        name = "incompatible_enable_profile_by_default",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleEnableProfileByDefault = new Flag.Boolean("incompatibleEnableProfileByDefault");

    @Option(
        name = "incompatible_enable_proto_toolchain_resolution",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If true, proto lang rules define toolchains from protobuf repository.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleEnableProtoToolchainResolution = new Flag.Boolean("incompatibleEnableProtoToolchainResolution");

    @Option(
        name = "incompatible_enforce_config_setting_visibility",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, enforce config_setting visibility restrictions. If false, every config_setting is visible to every\n" +
            "      target. See https://github.com/bazelbuild/bazel/issues/12932.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleEnforceConfigSettingVisibility = new Flag.Boolean("incompatibleEnforceConfigSettingVisibility");

    @Option(
        name = "incompatible_enforce_starlark_utf8",
        defaultValue = "warning",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled (or set to 'error'), fail if Starlark files are not UTF-8 encoded. If set to 'warning', emit a\n" +
            "      warning instead. If set to 'off', Bazel assumes that Starlark files are UTF-8 encoded but does not verify\n" +
            "      this assumption. Note that Starlark files which are not UTF-8 encoded can cause Bazel to behave\n" +
            "      inconsistently.\n" +
            "    ",
        valueHelp = "off, warning or error",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf incompatibleEnforceStarlarkUtf8 = new Flag.OneOf("incompatibleEnforceStarlarkUtf8");

    @Option(
        name = "incompatible_exclusive_test_sandboxed",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, exclusive tests will run with sandboxed strategy. Add 'local' tag to force an exclusive test run\n" +
            "      locally\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleExclusiveTestSandboxed = new Flag.Boolean("incompatibleExclusiveTestSandboxed");

    @Option(
        name = "incompatible_existing_rules_immutable_view",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleExistingRulesImmutableView = new Flag.Boolean("incompatibleExistingRulesImmutableView");

    @Option(
        name = "incompatible_fail_on_unknown_attributes",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If enabled, targets that have unknown attributes set to None fail.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleFailOnUnknownAttributes = new Flag.Boolean("incompatibleFailOnUnknownAttributes");

    @Option(
        name = "incompatible_fix_package_group_reporoot_syntax",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      In package_group's `packages` attribute, changes the meaning of the value \"//...\" to refer to all packages in\n" +
            "      the current repository instead of all packages in any repository. You can use the special value \"public\" in\n" +
            "      place of \"//...\" to obtain the old behavior. This flag requires that\n" +
            "      --incompatible_package_group_has_public_syntax also be enabled.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleFixPackageGroupReporootSyntax = new Flag.Boolean("incompatibleFixPackageGroupReporootSyntax");

    @Option(
        name = "incompatible_j2objc_library_migration",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled, direct usage of the native j2objc_library rules is disabled. Please use the Starlark rule instead.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleJ2objcLibraryMigration = new Flag.Boolean("incompatibleJ2objcLibraryMigration");

    @Option(
        name = "incompatible_java_common_parameters",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, the output_jar, and host_javabase parameters in pack_sources and host_javabase in compile\n" +
            "      will all be removed.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleJavaCommonParameters = new Flag.Boolean("incompatibleJavaCommonParameters");

    @Option(
        name = "incompatible_java_info_merge_runtime_module_flags",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, the JavaInfo constructor will merge add_exports and add_opens of runtime_deps in addition to\n" +
            "      deps and exports.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleJavaInfoMergeRuntimeModuleFlags = new Flag.Boolean("incompatibleJavaInfoMergeRuntimeModuleFlags");

    @Option(
        name = "incompatible_legacy_local_fallback",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, enables the legacy implicit fallback from sandboxed to local strategy. This flag will\n" +
            "      eventually default to false and then become a no-op. Use --strategy, --spawn_strategy, or\n" +
            "      --dynamic_local_strategy to configure fallbacks instead.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleLegacyLocalFallback = new Flag.Boolean("incompatibleLegacyLocalFallback");

    @Option(
        name = "incompatible_lexicographical_output",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If this option is set, sorts --order_output=auto output in lexicographical order.",
        valueHelp = "a boolean",
        commands = {"query"}
    )
    public static final Flag.Boolean incompatibleLexicographicalOutput = new Flag.Boolean("incompatibleLexicographicalOutput");

    @Option(
        name = "incompatible_load_java_rules_from_bzl",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "Deprecated no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleLoadJavaRulesFromBzl = new Flag.Boolean("incompatibleLoadJavaRulesFromBzl");

    @Option(
        name = "incompatible_load_proto_rules_from_bzl",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "Deprecated no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleLoadProtoRulesFromBzl = new Flag.Boolean("incompatibleLoadProtoRulesFromBzl");

    @Option(
        name = "incompatible_load_python_rules_from_bzl",
        defaultValue = "false",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "Deprecated no-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleLoadPythonRulesFromBzl = new Flag.Boolean("incompatibleLoadPythonRulesFromBzl");

    @Option(
        name = "incompatible_locations_prefers_executable",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      Whether a target that provides an executable expands to the executable rather than the files in\n" +
            "      <code>DefaultInfo.files</code> under $(locations ...) expansion if the number of files is not 1.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleLocationsPrefersExecutable = new Flag.Boolean("incompatibleLocationsPrefersExecutable");

    @Option(
        name = "incompatible_macos_set_install_name",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      Whether to explicitly set `-install_name` when creating dynamic libraries. See\n" +
            "      https://github.com/bazelbuild/bazel/issues/12370\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleMacosSetInstallName = new Flag.Boolean("incompatibleMacosSetInstallName");

    @Option(
        name = "incompatible_make_thinlto_command_lines_standalone",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "This flag is a noop and scheduled for removal.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleMakeThinltoCommandLinesStandalone = new Flag.Boolean("incompatibleMakeThinltoCommandLinesStandalone");

    @Option(
        name = "incompatible_merge_fixed_and_default_shell_env",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled, actions registered with ctx.actions.run and ctx.actions.run_shell with both 'env' and\n" +
            "      'use_default_shell_env = True' specified will use an environment obtained from the default shell environment\n" +
            "      by overriding with the values passed in to 'env'. If disabled, the value of 'env' is completely ignored in\n" +
            "      this case.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleMergeFixedAndDefaultShellEnv = new Flag.Boolean("incompatibleMergeFixedAndDefaultShellEnv");

    @Option(
        name = "incompatible_merge_genfiles_directory",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If true, the genfiles directory is folded into the bin directory.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleMergeGenfilesDirectory = new Flag.Boolean("incompatibleMergeGenfilesDirectory");

    @Option(
        name = "incompatible_modify_execution_info_additive",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When enabled, passing multiple --modify_execution_info flags is additive. When disabled, only the last flag\n" +
            "      is taken into account.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleModifyExecutionInfoAdditive = new Flag.Boolean("incompatibleModifyExecutionInfoAdditive");

    @Option(
        name = "incompatible_multi_release_deploy_jars",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "When enabled, java_binary creates Multi-Release deploy jars.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleMultiReleaseDeployJars = new Flag.Boolean("incompatibleMultiReleaseDeployJars");

    @Option(
        name = "incompatible_new_actions_api",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleNewActionsApi = new Flag.Boolean("incompatibleNewActionsApi");

    @Option(
        name = "incompatible_no_attr_license",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If set to true, disables the function `attr.license`.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleNoAttrLicense = new Flag.Boolean("incompatibleNoAttrLicense");

    @Option(
        name = "incompatible_no_implicit_file_export",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set, (used) source files are are package private unless exported explicitly. See\n" +
            "      https://github.com/bazelbuild/proposals/blob/master/designs/2019-10-24-file-visibility.md\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleNoImplicitFileExport = new Flag.Boolean("incompatibleNoImplicitFileExport");

    @Option(
        name = "incompatible_no_implicit_watch_label",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, then methods on <code>repository_ctx</code> that are passed a Label will no longer automatically\n" +
            "      watch the file under that label for changes even if <code>watch = \"no\"</code>, and\n" +
            "      <code>repository_ctx.path</code> no longer causes the returned path to be watched. Use\n" +
            "      <code>repository_ctx.watch</code> instead.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleNoImplicitWatchLabel = new Flag.Boolean("incompatibleNoImplicitWatchLabel");

    @Option(
        name = "incompatible_no_package_distribs",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If set to true, disables the `package(distribs=...)`.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleNoPackageDistribs = new Flag.Boolean("incompatibleNoPackageDistribs");

    @Option(
        name = "incompatible_no_rule_outputs_param",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If set to true, disables the `outputs` parameter of the `rule()` Starlark function.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleNoRuleOutputsParam = new Flag.Boolean("incompatibleNoRuleOutputsParam");

    @Option(
        name = "incompatible_objc_alwayslink_by_default",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If true, make the default value true for alwayslink attributes in objc_library and objc_import.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleObjcAlwayslinkByDefault = new Flag.Boolean("incompatibleObjcAlwayslinkByDefault");

    @Option(
        name = "incompatible_override_toolchain_transition",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated, this is no longer in use and should be removed.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleOverrideToolchainTransition = new Flag.Boolean("incompatibleOverrideToolchainTransition");

    @Option(
        name = "incompatible_package_group_has_public_syntax",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      In package_group's `packages` attribute, allows writing \"public\" or \"private\" to refer to all packages or no\n" +
            "      packages respectively.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatiblePackageGroupHasPublicSyntax = new Flag.Boolean("incompatiblePackageGroupHasPublicSyntax");

    @Option(
        name = "incompatible_package_group_includes_double_slash",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled, when outputting package_group's `packages` attribute, the leading `//` will not be omitted.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean incompatiblePackageGroupIncludesDoubleSlash = new Flag.Boolean("incompatiblePackageGroupIncludesDoubleSlash");

    @Option(
        name = "incompatible_py2_outputs_are_suffixed",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, targets built in the Python 2 configuration will appear under an output root that includes the\n" +
            "      suffix '-py2', while targets built for Python 3 will appear in a root with no Python-related suffix. This\n" +
            "      means that the `bazel-bin` convenience symlink will point to Python 3 targets rather than Python 2. If you\n" +
            "      enable this option it is also recommended to enable `--incompatible_py3_is_default`.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatiblePy2OutputsAreSuffixed = new Flag.Boolean("incompatiblePy2OutputsAreSuffixed");

    @Option(
        name = "incompatible_py3_is_default",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, `py_binary` and `py_test` targets that do not set their `python_version` (or\n" +
            "      `default_python_version`) attribute will default to PY3 rather than to PY2. If you set this flag it is also\n" +
            "      recommended to set `--incompatible_py2_outputs_are_suffixed`.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatiblePy3IsDefault = new Flag.Boolean("incompatiblePy3IsDefault");

    @Option(
        name = "incompatible_python_disable_py2",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, using Python 2 settings will cause an error. This includes python_version=PY2, srcs_version=PY2, and\n" +
            "      srcs_version=PY2ONLY. See https://github.com/bazelbuild/bazel/issues/15684 for more information.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatiblePythonDisablePy2 = new Flag.Boolean("incompatiblePythonDisablePy2");

    @Option(
        name = "incompatible_python_disallow_native_rules",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When true, an error occurs when using the builtin py_* rules; instead the rule_python rules should be used.\n" +
            "      See https://github.com/bazelbuild/bazel/issues/17773 for more information and migration instructions.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatiblePythonDisallowNativeRules = new Flag.Boolean("incompatiblePythonDisallowNativeRules");

    @Option(
        name = "incompatible_remote_use_new_exit_code_for_lost_inputs",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, Bazel will use new exit code 39 instead of 34 if remote cacheerrors, including cache\n" +
            "      evictions, cause the build to fail.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleRemoteUseNewExitCodeForLostInputs = new Flag.Boolean("incompatibleRemoteUseNewExitCodeForLostInputs");

    @Option(
        name = "incompatible_remove_binary_profile",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleRemoveBinaryProfile = new Flag.Boolean("incompatibleRemoveBinaryProfile");

    @Option(
        name = "incompatible_remove_legacy_whole_archive",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, Bazel will not link library dependencies as whole archive by default (see\n" +
            "      https://github.com/bazelbuild/bazel/issues/7362 for migration instructions).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleRemoveLegacyWholeArchive = new Flag.Boolean("incompatibleRemoveLegacyWholeArchive");

    @Option(
        name = "incompatible_remove_old_python_version_api",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op, will be removed soon.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleRemoveOldPythonVersionApi = new Flag.Boolean("incompatibleRemoveOldPythonVersionApi");

    @Option(
        name = "incompatible_require_ctx_in_configure_features",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "This flag is a noop and scheduled for removal.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleRequireCtxInConfigureFeatures = new Flag.Boolean("incompatibleRequireCtxInConfigureFeatures");

    @Option(
        name = "incompatible_require_javaplugininfo_in_javacommon",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "When enabled java_common.compile only accepts JavaPluginInfo for plugins.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleRequireJavaplugininfoInJavacommon = new Flag.Boolean("incompatibleRequireJavaplugininfoInJavacommon");

    @Option(
        name = "incompatible_require_linker_input_cc_api",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, rule create_linking_context will require linker_inputs instead of libraries_to_link. The old\n" +
            "      getters of linking_context will also be disabled and just linker_inputs will be available.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleRequireLinkerInputCcApi = new Flag.Boolean("incompatibleRequireLinkerInputCcApi");

    @Option(
        name = "incompatible_run_shell_command_string",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If set to true, the command parameter of actions.run_shell will only accept string",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleRunShellCommandString = new Flag.Boolean("incompatibleRunShellCommandString");

    @Option(
        name = "incompatible_sandbox_hermetic_tmp",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If set to true, each Linux sandbox will have its own dedicated empty directory mounted as /tmp rather than\n" +
            "      sharing /tmp with the host filesystem. Use --sandbox_add_mount_pair=/tmp to keep seeing the host's /tmp in\n" +
            "      all sandboxes.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleSandboxHermeticTmp = new Flag.Boolean("incompatibleSandboxHermeticTmp");

    @Option(
        name = "incompatible_simplify_unconditional_selects_in_rule_attrs",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, simplify configurable rule attributes which contain only unconditional selects; for example, if\n" +
            "      [\"a\"] + select(\"//conditions:default\", [\"b\"]) is assigned to a rule attribute, it is stored as [\"a\", \"b\"].\n" +
            "      This option does not affect attributes of symbolic macros or attribute default values.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleSimplifyUnconditionalSelectsInRuleAttrs = new Flag.Boolean("incompatibleSimplifyUnconditionalSelectsInRuleAttrs");

    @Option(
        name = "incompatible_skip_genfiles_symlink",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, the genfiles symlink will not be created. For more information, see\n" +
            "      https://github.com/bazelbuild/bazel/issues/8651\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleSkipGenfilesSymlink = new Flag.Boolean("incompatibleSkipGenfilesSymlink");

    @Option(
        name = "incompatible_stop_exporting_build_file_path",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, deprecated ctx.build_file_path will not be available. ctx.label.package + '/BUILD' can be\n" +
            "      used instead.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleStopExportingBuildFilePath = new Flag.Boolean("incompatibleStopExportingBuildFilePath");

    @Option(
        name = "incompatible_stop_exporting_language_modules",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If enabled, certain language-specific modules (such as `cc_common`) are unavailable in user .bzl files and\n" +
            "      may only be called from their respective rules repositories.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleStopExportingLanguageModules = new Flag.Boolean("incompatibleStopExportingLanguageModules");

    @Option(
        name = "incompatible_strict_action_env",
        oldName = "experimental_strict_action_env",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, Bazel uses an environment with a static value for PATH and does not inherit LD_LIBRARY_PATH. Use\n" +
            "      --action_env=ENV_VARIABLE if you want to inherit specific environment variables from the client, but note\n" +
            "      that doing so can prevent cross-user caching if a shared cache is used.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleStrictActionEnv = new Flag.Boolean("incompatibleStrictActionEnv");

    @Option(
        name = "incompatible_strip_executable_safely",
        defaultValue = "false",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If true, strip action for executables will use flag -x, which does not break dynamic symbol resolution.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleStripExecutableSafely = new Flag.Boolean("incompatibleStripExecutableSafely");

    @Option(
        name = "incompatible_top_level_aspects_require_providers",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, the top level aspect will honor its required providers and only run on top level targets\n" +
            "      whose rules' advertised providers satisfy the required providers of the aspect.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleTopLevelAspectsRequireProviders = new Flag.Boolean("incompatibleTopLevelAspectsRequireProviders");

    @Option(
        name = "incompatible_unambiguous_label_stringification",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When true, Bazel will stringify the label @//foo:bar to @//foo:bar, instead of //foo:bar. This only affects\n" +
            "      the behavior of str(), the % operator, and so on; the behavior of repr() is unchanged. See\n" +
            "      https://github.com/bazelbuild/bazel/issues/15916 for more information.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleUnambiguousLabelStringification = new Flag.Boolean("incompatibleUnambiguousLabelStringification");

    @Option(
        name = "incompatible_use_cc_configure_from_rules_cc",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      When true, Bazel will no longer allow using cc_configure from @bazel_tools. Please see\n" +
            "      https://github.com/bazelbuild/bazel/issues/10134 for details and migration instructions.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleUseCcConfigureFromRulesCc = new Flag.Boolean("incompatibleUseCcConfigureFromRulesCc");

    @Option(
        name = "incompatible_use_cpp_compile_header_mnemonic",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "If enabled, give distinguishing mnemonic to header processing actions",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleUseCppCompileHeaderMnemonic = new Flag.Boolean("incompatibleUseCppCompileHeaderMnemonic");

    @Option(
        name = "incompatible_use_new_cgroup_implementation",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If true, use the new implementation for cgroups. The old implementation only supports the memory controller\n" +
            "      and ignores the value of --experimental_sandbox_limits.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleUseNewCgroupImplementation = new Flag.Boolean("incompatibleUseNewCgroupImplementation");

    @Option(
        name = "incompatible_use_plus_in_repo_names",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "No-op.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleUsePlusInRepoNames = new Flag.Boolean("incompatibleUsePlusInRepoNames");

    @Option(
        name = "incompatible_use_python_toolchains",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      If set to true, executable native Python rules will use the Python runtime specified by the Python toolchain,\n" +
            "      rather than the runtime given by legacy flags like --python_top.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleUsePythonToolchains = new Flag.Boolean("incompatibleUsePythonToolchains");

    @Option(
        name = "incompatible_use_specific_tool_files",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      Use cc toolchain's compiler_files, as_files, and ar_files as inputs to appropriate actions. See\n" +
            "      https://github.com/bazelbuild/bazel/issues/8531\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleUseSpecificToolFiles = new Flag.Boolean("incompatibleUseSpecificToolFiles");

    @Option(
        name = "incompatible_use_toolchain_resolution_for_java_rules",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op. Kept here for backwards compatibility.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleUseToolchainResolutionForJavaRules = new Flag.Boolean("incompatibleUseToolchainResolutionForJavaRules");

    @Option(
        name = "incompatible_validate_top_level_header_inclusions",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "This flag is a noop and scheduled for removal.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incompatibleValidateTopLevelHeaderInclusions = new Flag.Boolean("incompatibleValidateTopLevelHeaderInclusions");

    @Option(
        name = "incompatible_visibility_private_attributes_at_definition",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean incompatibleVisibilityPrivateAttributesAtDefinition = new Flag.Boolean("incompatibleVisibilityPrivateAttributesAtDefinition");

    @Option(
        name = "incremental",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Whether to do an incremental install. If true, try to avoid unnecessary additional work by reading the state\n" +
            "      of the device the code is to be installed on and using that information to avoid unnecessary work. If false\n" +
            "      (the default), always do a full install.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"mobile-install"}
    )
    public static final Flag.Boolean incremental = new Flag.Boolean("incremental");

    @Option(
        name = "incremental_dexing",
        defaultValue = "true",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS,
            OptionEffectTag.LOSES_INCREMENTAL_STATE,
        },
        help = "Does most of the work for dexing separately for each Jar file.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean incrementalDexing = new Flag.Boolean("incrementalDexing");

    @Option(
        name = "incremental_install_verbosity",
        defaultValue = "",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "The verbosity for incremental install. Set to 1 for debug logging.",
        valueHelp = "a string",
        commands = {"mobile-install"}
    )
    public static final Flag.Str incrementalInstallVerbosity = new Flag.Str("incrementalInstallVerbosity");

    @Option(
        name = "infer_universe_scope",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If set and --universe_scope is unset, then a value of --universe_scope will be inferred as the list of unique\n" +
            "      target patterns in the query expression. Note that the --universe_scope value inferred for a query expression\n" +
            "      that uses universe-scoped functions (e.g.`allrdeps`) may not be what you want, so you should use this option\n" +
            "      only if you know what you are doing. See https://bazel.build/reference/query#sky-query for details and\n" +
            "      examples. If --universe_scope is set, then this option's value is ignored. Note: this option applies only to\n" +
            "      `query` (i.e. not `cquery`).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean inferUniverseScope = new Flag.Boolean("inferUniverseScope");

    @Option(
        name = "inject_repository",
        allowMultiple = true,
        help = "\n" +
            "      Adds a new repository with a local path in the form of <repository name>=<path>. This only takes effect with\n" +
            "      --enable_bzlmod and is equivalent to adding a corresponding `local_repository` to the root module's\n" +
            "      MODULE.bazel file via `use_repo_rule`. If the given path is an absolute path, it will be used as it is. If\n" +
            "      the given path is a relative path, it is relative to the current working directory. If the given path starts\n" +
            "      with '%workspace%', it is relative to the workspace root, which is the output of `bazel info workspace`. If\n" +
            "      the given path is empty, then remove any previous injections.\n" +
            "    ",
        valueHelp = "an equals-separated mapping of repository name to path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown injectRepository = new Flag.Unknown("injectRepository");

    @Option(
        name = "install_base",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "This launcher option is intended for use only by tests.",
        valueHelp = "a path",
        commands = {"startup"}
    )
    public static final Flag.Path installBase = new Flag.Path("installBase");

    @Option(
        name = "install_md5",
        defaultValue = "",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BAZEL_MONITORING},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "This launcher option is intended for use only by tests.",
        valueHelp = "a string",
        commands = {"startup"}
    )
    public static final Flag.Str installMd5 = new Flag.Str("installMd5");

    @Option(
        name = "instrument_test_targets",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      When coverage is enabled, specifies whether to consider instrumenting test rules. When set, test rules\n" +
            "      included by --instrumentation_filter are instrumented. Otherwise, test rules are always excluded from\n" +
            "      coverage instrumentation.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean instrumentTestTargets = new Flag.Boolean("instrumentTestTargets");

    @Option(
        name = "instrumentation_filter",
        defaultValue = "-/javatests[/:],-/test/java[/:]",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      When coverage is enabled, only rules with names included by the specified regex-based filter will be\n" +
            "      instrumented. Rules prefixed with '-' are excluded instead. Note that only non-test rules are instrumented\n" +
            "      unless --instrument_test_targets is enabled.\n" +
            "    ",
        valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown instrumentationFilter = new Flag.Unknown("instrumentationFilter");

    @Option(
        name = "interface_shared_objects",
        defaultValue = "true",
        effectTags = {
            OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.AFFECTS_OUTPUTS,
        },
        help = "\n" +
            "      Use interface shared objects if supported by the toolchain. All ELF toolchains currently support this setting.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean interfaceSharedObjects = new Flag.Boolean("interfaceSharedObjects");

    @Option(
        name = "internal_persistent_android_dex_desugar",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "Tracking flag for when dexing and desugaring workers are enabled.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean internalPersistentAndroidDexDesugar = new Flag.Boolean("internalPersistentAndroidDexDesugar");

    @Option(
        name = "internal_persistent_busybox_tools",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "Tracking flag for when busybox workers are enabled.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean internalPersistentBusyboxTools = new Flag.Boolean("internalPersistentBusyboxTools");

    @Option(
        name = "internal_persistent_multiplex_android_dex_desugar",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "Tracking flag for when multiplexed dexing and desugaring workers are enabled.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean internalPersistentMultiplexAndroidDexDesugar = new Flag.Boolean("internalPersistentMultiplexAndroidDexDesugar");

    @Option(
        name = "internal_persistent_multiplex_busybox_tools",
        defaultValue = "false",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "Tracking flag for when multiplexed busybox workers are enabled.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean internalPersistentMultiplexBusyboxTools = new Flag.Boolean("internalPersistentMultiplexBusyboxTools");

    @Option(
        name = "internal_spawn_scheduler",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "Placeholder option so that we can tell in Blaze whether the spawn scheduler was enabled.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean internalSpawnScheduler = new Flag.Boolean("internalSpawnScheduler");

    @Option(
        name = "internal_starlark_flag_test_canary",
        defaultValue = "false",
        help = "",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean internalStarlarkFlagTestCanary = new Flag.Boolean("internalStarlarkFlagTestCanary");

    @Option(
        name = "invocation_id",
        defaultValue = "",
        effectTags = {OptionEffectTag.BAZEL_MONITORING, OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      Unique identifier, in UUID format, for the command being run. If explicitly specified uniqueness must be\n" +
            "      ensured by the caller. The UUID is printed to stderr, the BEP and remote execution protocol.\n" +
            "    ",
        valueHelp = "a UUID",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown invocationId = new Flag.Unknown("invocationId");

    @Option(
        name = "invocation_policy",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      A base64-encoded-binary-serialized or text-formated invocation_policy.InvocationPolicy proto. Unlike other\n" +
            "      options, it is an error to specify --invocation_policy multiple times.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"startup", "canonicalize-flags"}
    )
    public static final Flag.Str invocationPolicy = new Flag.Str("invocationPolicy");

    @Option(
        name = "io_nice_level",
        defaultValue = "-1",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Only on Linux; set a level from 0-7 for best-effort IO scheduling using the sys_ioprio_set system call. 0 is\n" +
            "      highest priority, 7 is lowest. The anticipatory scheduler may only honor up to priority 4. If set to a\n" +
            "      negative value, then Bazel does not perform a system call.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {"startup"}
    )
    public static final Flag.Integer ioNiceLevel = new Flag.Integer("ioNiceLevel");

    @Option(
        name = "ios_memleaks",
        defaultValue = "false",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "Enable checking for memory leaks in ios_test targets.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean iosMemleaks = new Flag.Boolean("iosMemleaks");

    @Option(
        name = "ios_minimum_os",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Minimum compatible iOS version for target simulators and devices. If unspecified, uses 'ios_sdk_version'.\n" +
            "    ",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown iosMinimumOs = new Flag.Unknown("iosMinimumOs");

    @Option(
        name = "ios_multi_cpus",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Comma-separated list of architectures to build an ios_application with. The result is a universal binary\n" +
            "      containing all specified architectures.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown iosMultiCpus = new Flag.Unknown("iosMultiCpus");

    @Option(
        name = "ios_sdk_version",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Specifies the version of the iOS SDK to use to build iOS applications. If unspecified, uses the default iOS\n" +
            "      SDK version from 'xcode_version'.\n" +
            "    ",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown iosSdkVersion = new Flag.Unknown("iosSdkVersion");

    @Option(
        name = "ios_signing_cert_name",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "\n" +
            "      Certificate name to use for iOS signing. If not set will fall back to provisioning profile. May be the\n" +
            "      certificate's keychain identity preference or (substring) of the certificate's common name, as per codesign's\n" +
            "      man page (SIGNING IDENTITIES).\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str iosSigningCertName = new Flag.Str("iosSigningCertName");

    @Option(
        name = "ios_simulator_device",
        effectTags = {OptionEffectTag.TEST_RUNNER},
        help = "\n" +
            "      The device to simulate when running an iOS application in the simulator, e.g. 'iPhone 6'. You can get a list\n" +
            "      of devices by running 'xcrun simctl list devicetypes' on the machine the simulator will be run on.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str iosSimulatorDevice = new Flag.Str("iosSimulatorDevice");

    @Option(
        name = "ios_simulator_version",
        effectTags = {OptionEffectTag.TEST_RUNNER},
        help = "\n" +
            "      The version of iOS to run on the simulator when running or testing. This is ignored for ios_test rules if a\n" +
            "      target device is specified in the rule.\n" +
            "    ",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown iosSimulatorVersion = new Flag.Unknown("iosSimulatorVersion");

    @Option(
        name = "isatty",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "\n" +
            "      A system-generated parameter which is used to notify the server whether this client is running in a terminal.\n" +
            "      If this is set to false, then '--color=auto' will be treated as '--color=no'. If this is set to true, then\n" +
            "      '--color=auto' will be treated as '--color=yes'.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean isatty = new Flag.Boolean("isatty");

    @Option(
        name = "j2objc_dead_code_removal",
        defaultValue = "false",
        help = "Whether to perform J2ObjC dead code removal to strip unused code from the final app bundle.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean j2objcDeadCodeRemoval = new Flag.Boolean("j2objcDeadCodeRemoval");

    @Option(
        name = "j2objc_dead_code_report",
        help = "\n" +
            "      Allows J2ObjC to strip dead code reported by ProGuard. Takes a label that can generate a dead code report as\n" +
            "      argument.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label j2objcDeadCodeReport = new Flag.Label("j2objcDeadCodeReport");

    @Option(
        name = "j2objc_translation_flags",
        allowMultiple = true,
        help = "Additional options to pass to the J2ObjC tool.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown j2objcTranslationFlags = new Flag.Unknown("j2objcTranslationFlags");

    @Option(
        name = "java_debug",
        expandsTo = {
            "--test_arg=--wrapper_script_flag=--debug", "--test_output=streamed", "--test_strategy=exclusive",
            "--test_timeout=9999", "--nocache_test_results",
        },
        help = "\n" +
            "      Causes the Java virtual machine of a java test to wait for a connection from a JDWP-compliant debugger (such\n" +
            "      as jdb) before starting the test. Implies -test_output=streamed.\n" +
            "    ",
        valueHelp = "",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown javaDebug = new Flag.Unknown("javaDebug");

    @Option(
        name = "java_deps",
        defaultValue = "true",
        help = "Generate dependency information (for now, compile-time classpath) per Java target.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean javaDeps = new Flag.Boolean("javaDeps");

    @Option(
        name = "java_header_compilation",
        oldName = "experimental_java_header_compilation",
        defaultValue = "true",
        help = "Compile ijars directly from source.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean javaHeaderCompilation = new Flag.Boolean("javaHeaderCompilation");

    @Option(
        name = "java_language_version",
        defaultValue = "",
        help = "The Java language version",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str javaLanguageVersion = new Flag.Str("javaLanguageVersion");

    @Option(
        name = "java_launcher",
        help = "\n" +
            "      The Java launcher to use when building Java binaries.  If this flag is set to the empty string, the JDK\n" +
            "      launcher is used. The \"launcher\" attribute overrides this flag.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label javaLauncher = new Flag.Label("javaLauncher");

    @Option(
        name = "java_optimization_mode",
        defaultValue = "",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Do not use.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str javaOptimizationMode = new Flag.Str("javaOptimizationMode");

    @Option(
        name = "java_runtime_version",
        defaultValue = "local_jdk",
        help = "The Java runtime version",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str javaRuntimeVersion = new Flag.Str("javaRuntimeVersion");

    @Option(
        name = "java_toolchain",
        help = "No-op. Kept here for backwards compatibility.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label javaToolchain = new Flag.Label("javaToolchain");

    @Option(
        name = "javabase",
        help = "No-op. Kept here for backwards compatibility.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label javabase = new Flag.Label("javabase");

    @Option(
        name = "javacopt",
        allowMultiple = true,
        help = "Additional options to pass to javac.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str javacopt = new Flag.Str("javacopt");

    @Option(
        name = "jobs",
        abbrev = 'j',
        defaultValue = "auto",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The number of concurrent jobs to run. Takes an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"),\n" +
            "      optionally followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\". Values must be between 1 and\n" +
            "      5000. Values above 2500 may cause memory issues. \"auto\" calculates a reasonable default based on host\n" +
            "      resources.\n" +
            "    ",
        valueHelp = "\n" +
            "      an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation\n" +
            "      ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown jobs = new Flag.Unknown("jobs");

    @Option(
        name = "jplPropagateCcLinkParamsStore",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "No-op, kept only for backwards compatibility",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean jplPropagateCcLinkParamsStore = new Flag.Boolean("jplPropagateCcLinkParamsStore");

    @Option(
        name = "jvm_heap_histogram_internal_object_pattern",
        defaultValue = "jdk\\.internal\\.vm\\.Filler.+",
        help = "\n" +
            "      Regex for overriding the matching logic for JDK21+ JVM heap memory collection. We are relying on volatile\n" +
            "      internal G1 GC implemenation details to get a clean memory metric, this option allows us to adapt to changes\n" +
            "      in that internal implementation without having to wait for a binary release.  Passed to JDK Matcher.find()\n" +
            "    ",
        valueHelp = "a valid Java regular expression",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown jvmHeapHistogramInternalObjectPattern = new Flag.Unknown("jvmHeapHistogramInternalObjectPattern");

    @Option(
        name = "jvmopt",
        allowMultiple = true,
        help = "\n" +
            "      Additional options to pass to the Java VM. These options will get added to the VM startup options of each\n" +
            "      java_binary target.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str jvmopt = new Flag.Str("jvmopt");

    @Option(
        name = "keep_backend_build_event_connections_alive",
        defaultValue = "true",
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "If enabled, keep connections to build event backend connections alive across builds.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean keepBackendBuildEventConnectionsAlive = new Flag.Boolean("keepBackendBuildEventConnectionsAlive");

    @Option(
        name = "keep_going",
        abbrev = 'k',
        defaultValue = "false",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      Continue as much as possible after an error.  While the target that failed and those that depend on it cannot\n" +
            "      be analyzed, other prerequisites of these targets can be.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Boolean keepGoing = new Flag.Boolean("keepGoing");

    @Option(
        name = "keep_state_after_build",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      If false, Blaze will discard the inmemory state from this build when the build finishes. Subsequent builds\n" +
            "      will not have any incrementality with respect to this one.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean keepStateAfterBuild = new Flag.Boolean("keepStateAfterBuild");

    @Option(
        name = "legacy_bazel_java_test",
        defaultValue = "false",
        help = "No-op, kept only for backwards compatibility",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean legacyBazelJavaTest = new Flag.Boolean("legacyBazelJavaTest");

    @Option(
        name = "legacy_external_runfiles",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If true, build runfiles symlink forests for external repositories under .runfiles/wsname/external/repo (in\n" +
            "      addition to .runfiles/repo).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean legacyExternalRunfiles = new Flag.Boolean("legacyExternalRunfiles");

    @Option(
        name = "legacy_globbing_threads",
        defaultValue = "100",
        help = "\n" +
            "      Number of threads to use for glob evaluation. Takes an integer, or a keyword (\"auto\", \"HOST_CPUS\",\n" +
            "      \"HOST_RAM\"), optionally followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\". \"auto\" means to\n" +
            "      use a reasonable value derived from the machine's hardware profile (e.g. the number of processors).\n" +
            "    ",
        valueHelp = "\n" +
            "      an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation\n" +
            "      ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Unknown legacyGlobbingThreads = new Flag.Unknown("legacyGlobbingThreads");

    @Option(
        name = "legacy_important_outputs",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Use this to suppress generation of the legacy important_outputs field in the TargetComplete event.\n" +
            "      important_outputs are required for Bazel to ResultStore/BTX integration.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean legacyImportantOutputs = new Flag.Boolean("legacyImportantOutputs");

    @Option(
        name = "legacy_main_dex_list_generator",
        help = "\n" +
            "      Specifies a binary to use to generate the list of classes that must be in the main dex when compiling legacy\n" +
            "      multidex.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label legacyMainDexListGenerator = new Flag.Label("legacyMainDexListGenerator");

    @Option(
        name = "legacy_whole_archive",
        defaultValue = "true",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.DEPRECATED},
        help = "\n" +
            "      Deprecated, superseded by --incompatible_remove_legacy_whole_archive (see\n" +
            "      https://github.com/bazelbuild/bazel/issues/7362 for details). When on, use --whole-archive for cc_binary\n" +
            "      rules that have linkshared=True and either linkstatic=True or '-static' in linkopts. This is for backwards\n" +
            "      compatibility only. A better alternative is to use alwayslink=1 where required.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean legacyWholeArchive = new Flag.Boolean("legacyWholeArchive");

    @Option(
        name = "line_terminator_null",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Whether each format is terminated with \\0 instead of newline.",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean lineTerminatorNull = new Flag.Boolean("lineTerminatorNull");

    @Option(
        name = "linkopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional option to pass to gcc when linking.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str linkopt = new Flag.Str("linkopt");

    @Option(
        name = "loading_phase_threads",
        defaultValue = "auto",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      Number of parallel threads to use for the loading/analysis phase.Takes an integer, or a keyword (\"auto\",\n" +
            "      \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\".\n" +
            "      \"auto\" sets a reasonable default based on host resources. Must be at least 1.\n" +
            "    ",
        valueHelp = "\n" +
            "      an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation\n" +
            "      ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Unknown loadingPhaseThreads = new Flag.Unknown("loadingPhaseThreads");

    @Option(
        name = "local_cpu_resources",
        defaultValue = "HOST_CPUS",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Explicitly set the total number of local CPU cores available to Bazel to spend on build actions executed\n" +
            "      locally. Takes an integer, or \"HOST_CPUS\", optionally followed by [-|*]<float> (eg. HOST_CPUS*.5 to use half\n" +
            "      the available CPU cores). By default, (\"HOST_CPUS\"), Bazel will query system configuration to estimate the\n" +
            "      number of CPU cores available.\n" +
            "    ",
        valueHelp = "an integer, or \"HOST_CPUS\", optionally followed by [-|*]<float>.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown localCpuResources = new Flag.Unknown("localCpuResources");

    @Option(
        name = "local_extra_resources",
        allowMultiple = true,
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Set the number of extra resources available to Bazel. Takes in a string-float pair. Can be used multiple\n" +
            "      times to specify multiple types of extra resources. Bazel will limit concurrently running actions based on\n" +
            "      the available extra resources and the extra resources required. Tests can declare the amount of extra\n" +
            "      resources they need by using a tag of the \"resources:<resoucename>:<amount>\" format. Available CPU, RAM and\n" +
            "      resources cannot be set with this flag.\n" +
            "    ",
        valueHelp = "a named float, 'name=value'",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown localExtraResources = new Flag.Unknown("localExtraResources");

    @Option(
        name = "local_ram_resources",
        defaultValue = "HOST_RAM*.67",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Explicitly set the total amount of local host RAM (in MB) available to Bazel to spend on build actions\n" +
            "      executed locally. Takes an integer, or \"HOST_RAM\", optionally followed by [-|*]<float> (eg. HOST_RAM*.5 to\n" +
            "      use half the available RAM). By default, (\"HOST_RAM*.67\"), Bazel will query system configuration to estimate\n" +
            "      the amount of RAM available and will use 67% of it.\n" +
            "    ",
        valueHelp = "an integer number of MBs, or \"HOST_RAM\", optionally followed by [-|*]<float>.",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown localRamResources = new Flag.Unknown("localRamResources");

    @Option(
        name = "local_resources",
        allowMultiple = true,
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Set the number of resources available to Bazel. Takes in an assignment to a float or HOST_RAM/HOST_CPUS,\n" +
            "      optionally followed by [-|*]<float> (eg. memory=HOST_RAM*.5 to use half the available RAM). Can be used\n" +
            "      multiple times to specify multiple types of resources. Bazel will limit concurrently running actions based on\n" +
            "      the available resources and the resources required. Tests can declare the amount of resources they need by\n" +
            "      using a tag of the \"resources:<resource name>:<amount>\" format. Overrides resources specified by\n" +
            "      --local_{cpu|ram|extra}_resources.\n" +
            "    ",
        valueHelp = "\n" +
            "      a named double, 'name=value', where value is an integer, or a keyword (\"auto\", \"HOST_CPUS\",\n" +
            "      \"HOST_RAM\"), optionally followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown localResources = new Flag.Unknown("localResources");

    @Option(
        name = "local_startup_timeout_secs",
        defaultValue = "120",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "The maximum amount of time the client waits to connect to the server",
        valueHelp = "an integer",
        commands = {"startup"}
    )
    public static final Flag.Integer localStartupTimeoutSecs = new Flag.Integer("localStartupTimeoutSecs");

    @Option(
        name = "local_termination_grace_seconds",
        oldName = "local_sigkill_grace_seconds",
        defaultValue = "15",
        help = "\n" +
            "      Time to wait between terminating a local process due to timeout and forcefully shutting it down.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer localTerminationGraceSeconds = new Flag.Integer("localTerminationGraceSeconds");

    @Option(
        name = "local_test_jobs",
        defaultValue = "auto",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      The max number of local test jobs to run concurrently. Takes an integer, or a keyword (\"auto\", \"HOST_CPUS\",\n" +
            "      \"HOST_RAM\"), optionally followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\". 0 means local\n" +
            "      resources will limit the number of local test jobs to run concurrently instead. Setting this greater than the\n" +
            "      value for --jobs is ineffectual.\n" +
            "    ",
        valueHelp = "\n" +
            "      an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation\n" +
            "      ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown localTestJobs = new Flag.Unknown("localTestJobs");

    @Option(
        name = "lock_install_base",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "\n" +
            "      Whether the server should hold a lock on the install base while running, to prevent another server from\n" +
            "      attempting to garbage collect it.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean lockInstallBase = new Flag.Boolean("lockInstallBase");

    @Option(
        name = "lockfile_mode",
        defaultValue = "update",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Specifies how and whether or not to use the lockfile. Valid values are `update` to use the lockfile and\n" +
            "      update it if there are changes, `refresh` to additionally refresh mutable information (yanked versions and\n" +
            "      previously missing modules) from remote registries from time to time, `error` to use the lockfile but throw\n" +
            "      an error if it's not up-to-date, or `off` to neither read from or write to the lockfile.\n" +
            "    ",
        valueHelp = "off, update, refresh or error",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf lockfileMode = new Flag.OneOf("lockfileMode");

    @Option(
        name = "log_top_n_packages",
        defaultValue = "10",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Configures number of packages included in top-package INFO logging, <= 0 disables.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer logTopNPackages = new Flag.Integer("logTopNPackages");

    @Option(
        name = "logging",
        defaultValue = "3",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "The logging level.",
        valueHelp = "0 <= an integer <= 6",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown logging = new Flag.Unknown("logging");

    @Option(
        name = "long",
        abbrev = 'l',
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        expandsTo = {"--help_verbosity=long"},
        help = "Show full description of each option, instead of just its name.",
        valueHelp = "",
        commands = {"help"}
    )
    public static final Flag.Unknown long_ = new Flag.Unknown("long");

    @Option(
        name = "ltobackendopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional option to pass to the LTO backend step (under --features=thin_lto).",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str ltobackendopt = new Flag.Str("ltobackendopt");

    @Option(
        name = "ltoindexopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional option to pass to the LTO indexing step (under --features=thin_lto).",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str ltoindexopt = new Flag.Str("ltoindexopt");

    @Option(
        name = "macos_cpus",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Comma-separated list of architectures for which to build Apple macOS binaries.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown macosCpus = new Flag.Unknown("macosCpus");

    @Option(
        name = "macos_minimum_os",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "Minimum compatible macOS version for targets. If unspecified, uses 'macos_sdk_version'.",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown macosMinimumOs = new Flag.Unknown("macosMinimumOs");

    @Option(
        name = "macos_qos_class",
        defaultValue = "default",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Sets the QoS service class of the %{product} server when running on macOS. This flag has no effect on all\n" +
            "      other platforms but is supported to ensure rc files can be shared among them without changes. Possible values\n" +
            "      are: user-interactive, user-initiated, default, utility, and background.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"startup"}
    )
    public static final Flag.Str macosQosClass = new Flag.Str("macosQosClass");

    @Option(
        name = "macos_sdk_version",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Specifies the version of the macOS SDK to use to build macOS applications. If unspecified, uses the default\n" +
            "      macOS SDK version from 'xcode_version'.\n" +
            "    ",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown macosSdkVersion = new Flag.Unknown("macosSdkVersion");

    @Option(
        name = "make_variables_source",
        defaultValue = "configuration",
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.HIDDEN, OptionMetadataTag.DEPRECATED},
        help = "",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str makeVariablesSource = new Flag.Str("makeVariablesSource");

    @Option(
        name = "materialize_param_files",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Writes intermediate parameter files to output tree even when using remote action execution. Useful when\n" +
            "      debugging actions. This is implied by --subcommands and --verbose_failures.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean materializeParamFiles = new Flag.Boolean("materializeParamFiles");

    @Option(
        name = "max_computation_steps",
        defaultValue = "0",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "\n" +
            "      The maximum number of Starlark computation steps that may be executed by a BUILD file (zero means no limit).\n" +
            "    ",
        valueHelp = "a long integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown maxComputationSteps = new Flag.Unknown("maxComputationSteps");

    @Option(
        name = "max_config_changes_to_show",
        defaultValue = "3",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      When discarding the analysis cache due to a change in the build options, displays up to the given number of\n" +
            "      changed option names. If the number given is -1, all changed options will be displayed.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer maxConfigChangesToShow = new Flag.Integer("maxConfigChangesToShow");

    @Option(
        name = "max_idle_secs",
        defaultValue = "10800",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      The number of seconds the build server will wait idling before shutting down. Zero means that the server will\n" +
            "      never shutdown. This is only read on server-startup, changing this option will not cause the server to\n" +
            "      restart.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {"startup"}
    )
    public static final Flag.Integer maxIdleSecs = new Flag.Integer("maxIdleSecs");

    @Option(
        name = "max_test_output_bytes",
        defaultValue = "-1",
        effectTags = {OptionEffectTag.TEST_RUNNER, OptionEffectTag.TERMINAL_OUTPUT, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Specifies maximum per-test-log size that can be emitted when --test_output is 'errors' or 'all'. Useful for\n" +
            "      avoiding overwhelming the output with excessively noisy test output. The test header is included in the log\n" +
            "      size. Negative values imply no limit. Output is all or nothing.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer maxTestOutputBytes = new Flag.Integer("maxTestOutputBytes");

    @Option(
        name = "memory",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Dump the memory use of the given Skyframe node.",
        valueHelp = "memory mode",
        commands = {"dump"}
    )
    public static final Flag.Unknown memory = new Flag.Unknown("memory");

    @Option(
        name = "memory_profile",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      If set, write memory usage data to the specified file at phase ends and stable heap to master log at end of\n" +
            "      build.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path memoryProfile = new Flag.Path("memoryProfile");

    @Option(
        name = "memory_profile_stable_heap_parameters",
        defaultValue = "1,0",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      Tune memory profile's computation of stable heap at end of build. Should be and even number of  integers\n" +
            "      separated by commas. In each pair the first integer is the number of GCs to perform. The second integer in\n" +
            "      each pair is the number of seconds to wait between GCs. Ex: 2,4,4,0 would 2 GCs with a 4sec pause, followed\n" +
            "      by 4 GCs with zero second pause\n" +
            "    ",
        valueHelp = "integers, separated by a comma expected in pairs",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown memoryProfileStableHeapParameters = new Flag.Unknown("memoryProfileStableHeapParameters");

    @Option(
        name = "memprof_profile",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Use memprof profile.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label memprofProfile = new Flag.Label("memprofProfile");

    @Option(
        name = "merge_android_manifest_permissions",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If enabled, the manifest merger will merge uses-permission and uses-permission-sdk-23 attributes.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean mergeAndroidManifestPermissions = new Flag.Boolean("mergeAndroidManifestPermissions");

    @Option(
        name = "min_param_file_size",
        defaultValue = "32768",
        effectTags = {
            OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION,
            OptionEffectTag.ACTION_COMMAND_LINES,
        },
        help = "Minimum command line length before creating a parameter file.",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer minParamFileSize = new Flag.Integer("minParamFileSize");

    @Option(
        name = "minimum_os_version",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "The minimum OS version which your compilation targets.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str minimumOsVersion = new Flag.Str("minimumOsVersion");

    @Option(
        name = "mobile_install_aspect",
        defaultValue = "@android_test_support//tools/android/mobile_install:mobile-install.bzl",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.CHANGES_INPUTS},
        help = "The aspect to use for mobile-install.",
        valueHelp = "a string",
        commands = {"mobile-install"}
    )
    public static final Flag.Str mobileInstallAspect = new Flag.Str("mobileInstallAspect");

    @Option(
        name = "mobile_install_run_deployer",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION},
        help = "Whether to run the mobile-install deployer after building all artifacts.",
        valueHelp = "a boolean",
        commands = {"mobile-install"}
    )
    public static final Flag.Boolean mobileInstallRunDeployer = new Flag.Boolean("mobileInstallRunDeployer");

    @Option(
        name = "mobile_install_supported_rules",
        defaultValue = "",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "The supported rules for mobile-install.",
        valueHelp = "comma-separated list of options",
        commands = {"mobile-install"}
    )
    public static final Flag.Unknown mobileInstallSupportedRules = new Flag.Unknown("mobileInstallSupportedRules");

    @Option(
        name = "mode",
        defaultValue = "skylark",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EXECUTION},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "Deprecated no-effect flag. Only skylark mode is still supported.",
        valueHelp = "classic, classic_internal_test_do_not_use or skylark",
        commands = {"mobile-install"}
    )
    public static final Flag.OneOf mode = new Flag.OneOf("mode");

    @Option(
        name = "modify_execution_info",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Add or remove keys from an action's execution info based on action mnemonic.  Applies only to actions which\n" +
            "      support execution info. Many common actions support execution info, e.g. Genrule, CppCompile, Javac,\n" +
            "      StarlarkAction, TestRunner. When specifying multiple values, order matters because many regexes may apply to\n" +
            "      the same mnemonic.\n" +
            "      \n" +
            "      Syntax: \"regex=[+-]key,regex=[+-]key,...\".\n" +
            "      \n" +
            "      Examples:\n" +
            "        '.*=+x,.*=-y,.*=+z' adds 'x' and 'z' to, and removes 'y' from, the execution info for all actions.\n" +
            "        'Genrule=+requires-x' adds 'requires-x' to the execution info for all Genrule actions.\n" +
            "        '(?!Genrule).*=-requires-x' removes 'requires-x' from the execution info for all non-Genrule actions.\n" +
            "      \n" +
            "    ",
        valueHelp = "regex=[+-]key,regex=[+-]key,...",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown modifyExecutionInfo = new Flag.Unknown("modifyExecutionInfo");

    @Option(
        name = "nested_set_depth_limit",
        defaultValue = "3500",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      The maximum depth of the graph internal to a depset (also known as NestedSet), above which the depset()\n" +
            "      constructor will fail.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer nestedSetDepthLimit = new Flag.Integer("nestedSetDepthLimit");

    @Option(
        name = "nodep_deps",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "\n" +
            "      If enabled, deps from \"nodep\" attributes will be included in the dependency graph over which the query\n" +
            "      operates. A common example of a \"nodep\" attribute is \"visibility\". Run and parse the output of `info\n" +
            "      build-language` to learn about all the \"nodep\" attributes in the build language.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean nodepDeps = new Flag.Boolean("nodepDeps");

    @Option(
        name = "non_incremental_per_target_dexopts",
        defaultValue = "--positions",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      dx flags that that prevent incremental dexing for binary targets that list any of the flags listed here in\n" +
            "      their 'dexopts' attribute, which are ignored with incremental dexing (superseding\n" +
            "      --dexopts_supported_in_incremental_dexing).  Defaults to --positions for safety but can in general be used to\n" +
            "      make sure the listed dx flags are honored, with additional build latency.  Please notify us if you find\n" +
            "      yourself needing this flag.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown nonIncrementalPerTargetDexopts = new Flag.Unknown("nonIncrementalPerTargetDexopts");

    @Option(
        name = "noorder_results",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        expandsTo = {"--order_output=no"},
        help = "\n" +
            "      Output the results in dependency-ordered (default) or unordered fashion. The unordered output is faster but\n" +
            "      only supported when --output is not minrank, maxrank, or graph.\n" +
            "    ",
        valueHelp = "",
        commands = {"query"}
    )
    public static final Flag.Unknown noorderResults = new Flag.Unknown("noorderResults");

    @Option(
        name = "null",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        expandsTo = {"--line_terminator_null=true"},
        help = "Whether each format is terminated with \\0 instead of newline.",
        valueHelp = "",
        commands = {"query"}
    )
    public static final Flag.Unknown null_ = new Flag.Unknown("null");

    @Option(
        name = "objc_debug_with_GLIBCXX",
        defaultValue = "false",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "\n" +
            "      If set, and compilation mode is set to 'dbg', define GLIBCXX_DEBUG,  GLIBCXX_DEBUG_PEDANTIC and\n" +
            "      GLIBCPP_CONCEPT_CHECKS.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean objcDebugWith_GLIBCXX = new Flag.Boolean("objcDebugWith_GLIBCXX");

    @Option(
        name = "objc_enable_binary_stripping",
        defaultValue = "false",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "\n" +
            "      Whether to perform symbol and dead-code strippings on linked binaries. Binary strippings will be performed if\n" +
            "      both this flag and --compilation_mode=opt are specified.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean objcEnableBinaryStripping = new Flag.Boolean("objcEnableBinaryStripping");

    @Option(
        name = "objc_generate_linkmap",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Specifies whether to generate a linkmap file.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean objcGenerateLinkmap = new Flag.Boolean("objcGenerateLinkmap");

    @Option(
        name = "objc_use_dotd_pruning",
        defaultValue = "true",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      If set, .d files emitted by clang will be used to prune the set of inputs passed into objc compiles.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean objcUseDotdPruning = new Flag.Boolean("objcUseDotdPruning");

    @Option(
        name = "objccopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "Additional options to pass to gcc when compiling Objective-C/C++ source files.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str objccopt = new Flag.Str("objccopt");

    @Option(
        name = "one_version_enforcement_on_java_tests",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      When enabled, and with experimental_one_version_enforcement set to a non-NONE value, enforce one version on\n" +
            "      java_test targets. This flag can be disabled to improve incremental test performance at the expense of\n" +
            "      missing potential one version violations.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean oneVersionEnforcementOnJavaTests = new Flag.Boolean("oneVersionEnforcementOnJavaTests");

    @Option(
        name = "only",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      If this option is given, only sync the repositories specified with this option. Still consider all (or all\n" +
            "      configure-like, of --configure is given) outdated.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"sync"}
    )
    public static final Flag.Str only = new Flag.Str("only");

    @Option(
        name = "oom_message",
        defaultValue = "",
        effectTags = {OptionEffectTag.BAZEL_MONITORING, OptionEffectTag.TERMINAL_OUTPUT},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "Custom message to be emitted on an out of memory failure.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str oomMessage = new Flag.Str("oomMessage");

    @Option(
        name = "optimizing_dexer",
        help = "Specifies a binary to use to do dexing without sharding.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label optimizingDexer = new Flag.Label("optimizingDexer");

    @Option(
        name = "option_sources",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "",
        valueHelp = "a list of option-source pairs",
        commands = {"startup"}
    )
    public static final Flag.Unknown optionSources = new Flag.Unknown("optionSources");

    @Option(
        name = "order_output",
        defaultValue = "auto",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Output the results unordered (no), dependency-ordered (deps), or fully ordered (full). The default is 'auto',\n" +
            "      meaning that results are output either dependency-ordered or fully ordered, depending on the output formatter\n" +
            "      (dependency-ordered for proto, minrank, maxrank, and graph, fully ordered for all others). When output is\n" +
            "      fully ordered, nodes are printed in a fully deterministic (total) order. First, all nodes are sorted\n" +
            "      alphabetically. Then, each node in the list is used as the start of a post-order depth-first search in which\n" +
            "      outgoing edges to unvisited nodes are traversed in alphabetical order of the successor nodes. Finally, nodes\n" +
            "      are printed in the reverse of the order in which they were visited.\n" +
            "    ",
        valueHelp = "no, deps, auto or full",
        commands = {"query"}
    )
    public static final Flag.OneOf orderOutput = new Flag.OneOf("orderOutput");

    @Option(
        name = "order_results",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        expandsTo = {"--order_output=auto"},
        help = "\n" +
            "      Output the results in dependency-ordered (default) or unordered fashion. The unordered output is faster but\n" +
            "      only supported when --output is not minrank, maxrank, or graph.\n" +
            "    ",
        valueHelp = "",
        commands = {"query"}
    )
    public static final Flag.Unknown orderResults = new Flag.Unknown("orderResults");

    @Option(
        name = "output",
        defaultValue = "text",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      The format in which the aquery results should be printed. Allowed values for aquery are: text, textproto,\n" +
            "      proto, streamed_proto, jsonproto.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"aquery", "config", "cquery", "mod", "query"}
    )
    public static final Flag.Str output = new Flag.Str("output");

    @Option(
        name = "output_base",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      If set, specifies the output location to which all build output will be written. Otherwise, the location will\n" +
            "      be ${OUTPUT_ROOT}/_blaze_${USER}/${MD5_OF_WORKSPACE_ROOT}. Note: If you specify a different\n" +
            "      option from one to the next Bazel invocation for this value, you'll likely start up a new, additional Bazel\n" +
            "      server. Bazel starts exactly one server per specified output base. Typically there is one output base per\n" +
            "      workspace - however, with this option you may have multiple output bases per workspace and thereby run\n" +
            "      multiple builds for the same client on the same machine concurrently. See 'bazel help shutdown' on how to\n" +
            "      shutdown a Bazel server.\n" +
            "    ",
        valueHelp = "a path",
        commands = {"startup"}
    )
    public static final Flag.Path outputBase = new Flag.Path("outputBase");

    @Option(
        name = "output_filter",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Only shows warnings and action outputs for rules with a name matching the provided regular expression.\n" +
            "    ",
        valueHelp = "a valid Java regular expression",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown outputFilter = new Flag.Unknown("outputFilter");

    @Option(
        name = "output_groups",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      A list of comma-separated output group names, each of which optionally prefixed by a + or a -. A group\n" +
            "      prefixed by + is added to the default set of output groups, while a group prefixed by - is removed from the\n" +
            "      default set. If at least one group is not prefixed, the default set of output groups is omitted. For example,\n" +
            "      --output_groups=+foo,+bar builds the union of the default set, foo, and bar, while --output_groups=foo,bar\n" +
            "      overrides the default set such that only foo and bar are built.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown outputGroups = new Flag.Unknown("outputGroups");

    @Option(
        name = "output_library_merged_assets",
        defaultValue = "true",
        help = "If disabled, does not produce merged asset.zip outputs for library targets",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean outputLibraryMergedAssets = new Flag.Boolean("outputLibraryMergedAssets");

    @Option(
        name = "output_tree_tracking",
        oldName = "experimental_output_tree_tracking",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      If set, tell the output service (if any) to track when files in the output tree have been modified externally\n" +
            "      (not by the build system). This should improve incremental build speed when an appropriate output service is\n" +
            "      enabled.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean outputTreeTracking = new Flag.Boolean("outputTreeTracking");

    @Option(
        name = "output_user_root",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      The user-specific directory beneath which all build outputs are written; by default, this is a function of\n" +
            "      $USER, but by specifying a constant, build outputs can be shared between collaborating users.\n" +
            "    ",
        valueHelp = "a path",
        commands = {"startup"}
    )
    public static final Flag.Path outputUserRoot = new Flag.Path("outputUserRoot");

    @Option(
        name = "override_module",
        allowMultiple = true,
        help = "\n" +
            "      Override a module with a local path in the form of <module name>=<path>. If the given path is an absolute\n" +
            "      path, it will be used as it is. If the given path is a relative path, it is relative to the current working\n" +
            "      directory. If the given path starts with '%workspace%, it is relative to the workspace root, which is the\n" +
            "      output of `bazel info workspace`. If the given path is empty, then remove any previous overrides.\n" +
            "    ",
        valueHelp = "an equals-separated mapping of module name to path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown overrideModule = new Flag.Unknown("overrideModule");

    @Option(
        name = "override_repository",
        allowMultiple = true,
        help = "\n" +
            "      Override a repository with a local path in the form of <repository name>=<path>. If the given path is an\n" +
            "      absolute path, it will be used as it is. If the given path is a relative path, it is relative to the current\n" +
            "      working directory. If the given path starts with '%workspace%, it is relative to the workspace root, which is\n" +
            "      the output of `bazel info workspace`. If the given path is empty, then remove any previous overrides.\n" +
            "    ",
        valueHelp = "an equals-separated mapping of repository name to path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown overrideRepository = new Flag.Unknown("overrideRepository");

    @Option(
        name = "package_path",
        defaultValue = "%workspace%",
        help = "\n" +
            "      A colon-separated list of where to look for packages. Elements beginning with '%workspace%' are relative to\n" +
            "      the enclosing workspace. If omitted or empty, the default is the output of 'bazel info default-package-path'.\n" +
            "    ",
        valueHelp = "colon-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Unknown packagePath = new Flag.Unknown("packagePath");

    @Option(
        name = "packages",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Dump package cache content.",
        valueHelp = "a boolean",
        commands = {"dump"}
    )
    public static final Flag.Boolean packages = new Flag.Boolean("packages");

    @Option(
        name = "per_file_copt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Additional options to selectively pass to gcc when compiling certain files. This option can be passed\n" +
            "      multiple times. Syntax: regex_filter@option_1,option_2,...,option_n. Where regex_filter stands for a list of\n" +
            "      include and exclude regular expression patterns (Also see --instrumentation_filter). option_1 to option_n\n" +
            "      stand for arbitrary command line options. If an option contains a comma it has to be quoted with a backslash.\n" +
            "      Options can contain @. Only the first @ is used to split the string. Example:\n" +
            "      --per_file_copt=//foo/.*\\.cc,-//foo/bar\\.cc@-O0 adds the -O0 command line option to the gcc command line of\n" +
            "      all cc files in //foo/ except bar.cc.\n" +
            "    ",
        valueHelp = "\n" +
            "      a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by\n" +
            "      an @ and a comma separated list of options\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown perFileCopt = new Flag.Unknown("perFileCopt");

    @Option(
        name = "per_file_ltobackendopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Additional options to selectively pass to LTO backend (under --features=thin_lto) when compiling certain\n" +
            "      backend objects. This option can be passed multiple times. Syntax:\n" +
            "      regex_filter@option_1,option_2,...,option_n. Where regex_filter stands for a list of include and exclude\n" +
            "      regular expression patterns. option_1 to option_n stand for arbitrary command line options. If an option\n" +
            "      contains a comma it has to be quoted with a backslash. Options can contain @. Only the first @ is used to\n" +
            "      split the string. Example: --per_file_ltobackendopt=//foo/.*\\.o,-//foo/bar\\.o@-O0 adds the -O0 command line\n" +
            "      option to the LTO backend command line of all o files in //foo/ except bar.o.\n" +
            "    ",
        valueHelp = "\n" +
            "      a comma-separated list of regex expressions with prefix '-' specifying excluded paths followed by\n" +
            "      an @ and a comma separated list of options\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown perFileLtobackendopt = new Flag.Unknown("perFileLtobackendopt");

    @Option(
        name = "persistent_android_dex_desugar",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        expandsTo = {
            "--internal_persistent_android_dex_desugar", "--strategy=Desugar=worker",
            "--strategy=DexBuilder=worker",
        },
        help = "Enable persistent Android dex and desugar actions by using workers.",
        valueHelp = "",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown persistentAndroidDexDesugar = new Flag.Unknown("persistentAndroidDexDesugar");

    @Option(
        name = "persistent_android_resource_processor",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        expandsTo = {
            "--internal_persistent_busybox_tools", "--strategy=AaptPackage=worker",
            "--strategy=AndroidResourceParser=worker", "--strategy=AndroidResourceValidator=worker",
            "--strategy=AndroidResourceCompiler=worker", "--strategy=RClassGenerator=worker",
            "--strategy=AndroidResourceLink=worker", "--strategy=AndroidAapt2=worker",
            "--strategy=AndroidAssetMerger=worker", "--strategy=AndroidResourceMerger=worker",
            "--strategy=AndroidCompiledResourceMerger=worker", "--strategy=ManifestMerger=worker",
            "--strategy=AndroidManifestMerger=worker", "--strategy=Aapt2Optimize=worker",
            "--strategy=AARGenerator=worker", "--strategy=ProcessDatabinding=worker",
            "--strategy=GenerateDataBindingBaseClasses=worker",
        },
        help = "Enable persistent Android resource processor by using workers.",
        valueHelp = "",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown persistentAndroidResourceProcessor = new Flag.Unknown("persistentAndroidResourceProcessor");

    @Option(
        name = "persistent_multiplex_android_dex_desugar",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        expandsTo = {"--persistent_android_dex_desugar", "--internal_persistent_multiplex_android_dex_desugar"},
        help = "Enable persistent multiplexed Android dex and desugar actions by using workers.",
        valueHelp = "",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown persistentMultiplexAndroidDexDesugar = new Flag.Unknown("persistentMultiplexAndroidDexDesugar");

    @Option(
        name = "persistent_multiplex_android_resource_processor",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        expandsTo = {
            "--persistent_android_resource_processor",
            "--modify_execution_info=AaptPackage=+supports-multiplex-workers",
            "--modify_execution_info=AndroidResourceParser=+supports-multiplex-workers",
            "--modify_execution_info=AndroidResourceValidator=+supports-multiplex-workers",
            "--modify_execution_info=AndroidResourceCompiler=+supports-multiplex-workers",
            "--modify_execution_info=RClassGenerator=+supports-multiplex-workers",
            "--modify_execution_info=AndroidResourceLink=+supports-multiplex-workers",
            "--modify_execution_info=AndroidAapt2=+supports-multiplex-workers",
            "--modify_execution_info=AndroidAssetMerger=+supports-multiplex-workers",
            "--modify_execution_info=AndroidResourceMerger=+supports-multiplex-workers",
            "--modify_execution_info=AndroidCompiledResourceMerger=+supports-multiplex-workers",
            "--modify_execution_info=ManifestMerger=+supports-multiplex-workers",
            "--modify_execution_info=AndroidManifestMerger=+supports-multiplex-workers",
            "--modify_execution_info=Aapt2Optimize=+supports-multiplex-workers",
            "--modify_execution_info=AARGenerator=+supports-multiplex-workers",
        },
        help = "Enable persistent multiplexed Android resource processor by using workers.",
        valueHelp = "",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown persistentMultiplexAndroidResourceProcessor = new Flag.Unknown("persistentMultiplexAndroidResourceProcessor");

    @Option(
        name = "persistent_multiplex_android_tools",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        expandsTo = {
            "--internal_persistent_multiplex_busybox_tools",
            "--persistent_multiplex_android_resource_processor", "--persistent_multiplex_android_dex_desugar",
        },
        help = "Enable persistent and multiplexed Android tools (dexing, desugaring, resource processing).",
        valueHelp = "",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown persistentMultiplexAndroidTools = new Flag.Unknown("persistentMultiplexAndroidTools");

    @Option(
        name = "platform_mappings",
        defaultValue = "",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        metadataTags = {OptionMetadataTag.IMMUTABLE},
        help = "\n" +
            "      The location of a mapping file that describes which platform to use if none is set or which flags to set when\n" +
            "      a platform already exists. Must be relative to the main workspace root. Defaults to 'platform_mappings' (a\n" +
            "      file directly under the workspace root).\n" +
            "    ",
        valueHelp = "a main workspace-relative path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown platformMappings = new Flag.Unknown("platformMappings");

    @Option(
        name = "platform_suffix",
        effectTags = {
            OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "Specifies a suffix to be added to the configuration directory.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str platformSuffix = new Flag.Str("platformSuffix");

    @Option(
        name = "platforms",
        oldName = "experimental_platforms",
        defaultValue = "",
        effectTags = {
            OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.CHANGES_INPUTS,
            OptionEffectTag.LOADING_AND_ANALYSIS,
        },
        help = "The labels of the platform rules describing the target platforms for the current command.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label platforms = new Flag.Label("platforms");

    @Option(
        name = "plugin",
        allowMultiple = true,
        help = "Plugins to use in the build. Currently works with java_plugin.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label plugin = new Flag.Label("plugin");

    @Option(
        name = "portable_paths",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "If true, includes paths to replace in ExecRequest to make the resulting paths portable.",
        valueHelp = "a boolean",
        commands = {"run"}
    )
    public static final Flag.Boolean portablePaths = new Flag.Boolean("portablePaths");

    @Option(
        name = "preemptible",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "If true, the command can be preempted if another command is started.",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean preemptible = new Flag.Boolean("preemptible");

    @Option(
        name = "print_action_mnemonics",
        allowMultiple = true,
        help = "Lists which mnemonics to filter print_action data by, no filtering takes place when left empty.",
        valueHelp = "a string",
        commands = {"print_action"}
    )
    public static final Flag.Str printActionMnemonics = new Flag.Str("printActionMnemonics");

    @Option(
        name = "print_relative_test_log_paths",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If true, when printing the path to a test log, use relative path that makes use of the 'testlogs' convenience\n" +
            "      symlink. N.B. - A subsequent 'build'/'test'/etc invocation with a different configuration can cause the\n" +
            "      target of this symlink to change, making the path printed previously no longer useful.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"coverage", "cquery", "fetch", "test", "vendor"}
    )
    public static final Flag.Boolean printRelativeTestLogPaths = new Flag.Boolean("printRelativeTestLogPaths");

    @Option(
        name = "process_headers_in_dependencies",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      When building a target //a:a, process headers in all targets that //a:a depends on (if header processing is\n" +
            "      enabled for the toolchain).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean processHeadersInDependencies = new Flag.Boolean("processHeadersInDependencies");

    @Option(
        name = "product_name",
        defaultValue = "bazel",
        effectTags = {
            OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.AFFECTS_OUTPUTS,
            OptionEffectTag.BAZEL_MONITORING,
        },
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "\n" +
            "      The name of the build system. It is used as part of the name of the generated directories (e.g.\n" +
            "      productName-bin for binaries) as well as for printing error messages and logging\n" +
            "    ",
        valueHelp = "a string",
        commands = {"startup"}
    )
    public static final Flag.Str productName = new Flag.Str("productName");

    @Option(
        name = "profile",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      If set, profile Bazel and write data to the specified file. Use bazel analyze-profile to analyze the profile.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path profile = new Flag.Path("profile");

    @Option(
        name = "profiles_to_retain",
        defaultValue = "5",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      Number of profiles to retain in the output base. If there are more than this number of profiles in the output\n" +
            "      base, the oldest are deleted until the total is under the limit.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer profilesToRetain = new Flag.Integer("profilesToRetain");

    @Option(
        name = "progress_in_terminal_title",
        defaultValue = "false",
        help = "\n" +
            "      Show the command progress in the terminal title. Useful to see what bazel is doing when having multiple\n" +
            "      terminal tabs.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean progressInTerminalTitle = new Flag.Boolean("progressInTerminalTitle");

    @Option(
        name = "progress_report_interval",
        defaultValue = "0",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      The number of seconds to wait between reports on still running jobs. The default value 0 means the first\n" +
            "      report will be printed after 10 seconds, then 30 seconds and after that progress is reported once every\n" +
            "      minute. When --curses is enabled, progress is reported every second.\n" +
            "    ",
        valueHelp = "an integer in 0-3600 range",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown progressReportInterval = new Flag.Unknown("progressReportInterval");

    @Option(
        name = "proguard_top",
        help = "Specifies which version of ProGuard to use for code removal when building a Java binary.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label proguardTop = new Flag.Label("proguardTop");

    @Option(
        name = "propeller_optimize",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Use Propeller profile information to optimize the build target.A propeller profile must consist of at least\n" +
            "      one of two files, a cc profile and a ld profile.  This flag accepts a build label which must refer to the\n" +
            "      propeller profile input files. For example, the BUILD file that defines the label, in\n" +
            "      a/b/BUILD:propeller_optimize(    name = \"propeller_profile\",    cc_profile = \"propeller_cc_profile.txt\",\n" +
            "      ld_profile = \"propeller_ld_profile.txt\",)An exports_files directive may have to be added to the corresponding\n" +
            "      package to make these files visible to Bazel. The option must be used as:\n" +
            "      --propeller_optimize=//a/b:propeller_profile\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label propellerOptimize = new Flag.Label("propellerOptimize");

    @Option(
        name = "propeller_optimize_absolute_cc_profile",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Absolute path name of cc_profile file for Propeller Optimized builds.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str propellerOptimizeAbsoluteCcProfile = new Flag.Str("propellerOptimizeAbsoluteCcProfile");

    @Option(
        name = "propeller_optimize_absolute_ld_profile",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Absolute path name of ld_profile file for Propeller Optimized builds.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str propellerOptimizeAbsoluteLdProfile = new Flag.Str("propellerOptimizeAbsoluteLdProfile");

    @Option(
        name = "proto:default_values",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      If true, attributes whose value is not explicitly specified in the BUILD file are included; otherwise they\n" +
            "      are omitted. This option is applicable to --output=proto\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_defaultValues = new Flag.Boolean("proto_defaultValues");

    @Option(
        name = "proto:definition_stack",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Populate the definition_stack proto field, which records for each rule instance the Starlark call stack at\n" +
            "      the moment the rule's class was defined.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_definitionStack = new Flag.Boolean("proto_definitionStack");

    @Option(
        name = "proto:flatten_selects",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "\n" +
            "      If enabled, configurable attributes created by select() are flattened. For list types the flattened\n" +
            "      representation is a list containing each value of the select map exactly once. Scalar types are flattened to\n" +
            "      null.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_flattenSelects = new Flag.Boolean("proto_flattenSelects");

    @Option(
        name = "proto:include_attribute_source_aspects",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Populate the source_aspect_name proto field of each Attribute with the source aspect that the attribute came\n" +
            "      from (empty string if it did not).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_includeAttributeSourceAspects = new Flag.Boolean("proto_includeAttributeSourceAspects");

    @Option(
        name = "proto:include_configurations",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      if enabled, proto output will include information about configurations. When disabled,cquery proto output\n" +
            "      format resembles query output format.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"cquery"}
    )
    public static final Flag.Boolean proto_includeConfigurations = new Flag.Boolean("proto_includeConfigurations");

    @Option(
        name = "proto:include_synthetic_attribute_hash",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Whether or not to calculate and populate the ${'$'}internal_attr_hash attribute.",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_includeSyntheticAttributeHash = new Flag.Boolean("proto_includeSyntheticAttributeHash");

    @Option(
        name = "proto:instantiation_stack",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Populate the instantiation call stack of each rule. Note that this requires the stack to be present\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_instantiationStack = new Flag.Boolean("proto_instantiationStack");

    @Option(
        name = "proto:locations",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Whether to output location information in proto output at all.",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_locations = new Flag.Boolean("proto_locations");

    @Option(
        name = "proto:output_rule_attrs",
        defaultValue = "all",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Comma separated list of attributes to include in output. Defaults to all attributes. Set to empty string to\n" +
            "      not output any attribute. This option is applicable to --output=proto.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Unknown proto_outputRuleAttrs = new Flag.Unknown("proto_outputRuleAttrs");

    @Option(
        name = "proto:rule_classes",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Populate the rule_class_key field of each rule; and for the first rule with a given rule_class_key, also\n" +
            "      populate its rule_class_info proto field. The rule_class_key field uniquely identifies a rule class, and the\n" +
            "      rule_class_info field is a Stardoc-format rule class API definition.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_ruleClasses = new Flag.Boolean("proto_ruleClasses");

    @Option(
        name = "proto:rule_inputs_and_outputs",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "Whether or not to populate the rule_input and rule_output fields.",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean proto_ruleInputsAndOutputs = new Flag.Boolean("proto_ruleInputsAndOutputs");

    @Option(
        name = "proto_compiler",
        defaultValue = "@bazel_tools//tools/proto:protoc",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "The label of the proto-compiler.",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label protoCompiler = new Flag.Label("protoCompiler");

    @Option(
        name = "proto_profile",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Whether to pass profile_path to the proto compiler.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean protoProfile = new Flag.Boolean("protoProfile");

    @Option(
        name = "proto_profile_path",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      The profile to pass to the proto compiler as profile_path. If unset, but  --proto_profile is true (the\n" +
            "      default), infers the path from --fdo_optimize.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label protoProfilePath = new Flag.Label("protoProfilePath");

    @Option(
        name = "proto_toolchain_for_cc",
        defaultValue = "@bazel_tools//tools/proto:cc_toolchain",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Label of proto_lang_toolchain() which describes how to compile C++ protos",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label protoToolchainForCc = new Flag.Label("protoToolchainForCc");

    @Option(
        name = "proto_toolchain_for_j2objc",
        defaultValue = "@bazel_tools//tools/j2objc:j2objc_proto_toolchain",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Label of proto_lang_toolchain() which describes how to compile j2objc protos",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label protoToolchainForJ2objc = new Flag.Label("protoToolchainForJ2objc");

    @Option(
        name = "proto_toolchain_for_java",
        defaultValue = "@bazel_tools//tools/proto:java_toolchain",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Label of proto_lang_toolchain() which describes how to compile Java protos",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label protoToolchainForJava = new Flag.Label("protoToolchainForJava");

    @Option(
        name = "proto_toolchain_for_javalite",
        defaultValue = "@bazel_tools//tools/proto:javalite_toolchain",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Label of proto_lang_toolchain() which describes how to compile JavaLite protos",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label protoToolchainForJavalite = new Flag.Label("protoToolchainForJavalite");

    @Option(
        name = "protocopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional options to pass to the protobuf compiler.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str protocopt = new Flag.Str("protocopt");

    @Option(
        name = "python_native_rules_allowlist",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      An allowlist (package_group target) to use when enforcing --incompatible_python_disallow_native_rules.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label pythonNativeRulesAllowlist = new Flag.Label("pythonNativeRulesAllowlist");

    @Option(
        name = "python_path",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      The absolute path of the Python interpreter invoked to run Python targets on the target platform. Deprecated;\n" +
            "      disabled by --incompatible_use_python_toolchains.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str pythonPath = new Flag.Str("pythonPath");

    @Option(
        name = "python_top",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      The label of a py_runtime representing the Python interpreter invoked to run Python targets on the target\n" +
            "      platform. Deprecated; disabled by --incompatible_use_python_toolchains.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label pythonTop = new Flag.Label("pythonTop");

    @Option(
        name = "python_version",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      The Python major version mode, either `PY2` or `PY3`. Note that this is overridden by `py_binary` and\n" +
            "      `py_test` targets (even if they don't explicitly specify a version) so there is usually not much reason to\n" +
            "      supply this flag.\n" +
            "    ",
        valueHelp = "PY2 or PY3",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf pythonVersion = new Flag.OneOf("pythonVersion");

    @Option(
        name = "query_file",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      If set, query will read the query from the file named here, rather than on the command line. It is an error\n" +
            "      to specify a file here as well as a command-line query.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Str queryFile = new Flag.Str("queryFile");

    @Option(
        name = "quiet",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      If true, no informational messages are emitted on the console, only errors. Changing this option will not\n" +
            "      cause the server to restart.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean quiet = new Flag.Boolean("quiet");

    @Option(
        name = "rc_source",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str rcSource = new Flag.Str("rcSource");

    @Option(
        name = "record_full_profiler_data",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      By default, Bazel profiler will record only aggregated data for fast but numerous events (such as statting\n" +
            "      the file). If this option is enabled, profiler will record each event - resulting in more precise profiling\n" +
            "      data but LARGE performance hit. Option only has effect if --profile used as well.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean recordFullProfilerData = new Flag.Boolean("recordFullProfilerData");

    @Option(
        name = "record_metrics_for_all_packages",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Configures PackageMetrics to record all metrics for all packages. Disables Top-n INFO logging.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean recordMetricsForAllPackages = new Flag.Boolean("recordMetricsForAllPackages");

    @Option(
        name = "redirect_local_instrumentation_output_writes",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      If true and supported, instrumentation output is redirected to be written locally on a different machine than\n" +
            "      where bazel is running on.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean redirectLocalInstrumentationOutputWrites = new Flag.Boolean("redirectLocalInstrumentationOutputWrites");

    @Option(
        name = "registry",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      Specifies the registries to use to locate Bazel module dependencies. The order is important: modules will be\n" +
            "      looked up in earlier registries first, and only fall back to later registries when they're missing from the\n" +
            "      earlier ones.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str registry = new Flag.Str("registry");

    @Option(
        name = "relative_locations",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      If true, the location of BUILD files in xml and proto outputs will be relative. By default, the location\n" +
            "      output is an absolute path and will not be consistent across machines. You can set this option to true to\n" +
            "      have a consistent result across machines.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean relativeLocations = new Flag.Boolean("relativeLocations");

    @Option(
        name = "remote_accept_cached",
        defaultValue = "true",
        help = "Whether to accept remotely cached action results.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean remoteAcceptCached = new Flag.Boolean("remoteAcceptCached");

    @Option(
        name = "remote_build_event_upload",
        oldName = "experimental_remote_build_event_upload",
        defaultValue = "minimal",
        help = "\n" +
            "      If set to 'all', all local outputs referenced by BEP are uploaded to remote cache.\n" +
            "      If set to 'minimal', local outputs referenced by BEP are not uploaded to the remote cache, except for files\n" +
            "      that are important to the consumers of BEP (e.g. test logs and timing profile). bytestream:// scheme is\n" +
            "      always used for the uri of files even if they are missing from remote cache.\n" +
            "      Default to 'minimal'.\n" +
            "    ",
        valueHelp = "all or minimal",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf remoteBuildEventUpload = new Flag.OneOf("remoteBuildEventUpload");

    @Option(
        name = "remote_bytestream_uri_prefix",
        help = "\n" +
            "      The hostname and instance name to be used in bytestream:// URIs that are written into build event streams.\n" +
            "      This option can be set when builds are performed using a proxy, which causes the values of --remote_executor\n" +
            "      and --remote_instance_name to no longer correspond to the canonical name of the remote execution service.\n" +
            "      When not set, it will default to \"${hostname}/${instance_name}\".\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str remoteBytestreamUriPrefix = new Flag.Str("remoteBytestreamUriPrefix");

    @Option(
        name = "remote_cache",
        oldName = "remote_http_cache",
        help = "\n" +
            "      A URI of a caching endpoint. The supported schemas are http, https, grpc, grpcs (grpc with TLS enabled) and\n" +
            "      unix (local UNIX sockets). If no schema is provided Bazel will default to grpcs. Specify grpc://, http:// or\n" +
            "      unix: schema to disable TLS. See https://bazel.build/remote/caching\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str remoteCache = new Flag.Str("remoteCache");

    @Option(
        name = "remote_cache_async",
        oldName = "experimental_remote_cache_async",
        defaultValue = "true",
        help = "\n" +
            "      If true, uploading of action results to a disk or remote cache will happen in the background instead of\n" +
            "      blocking the completion of an action. Some actions are incompatible with background uploads, and may still\n" +
            "      block even when this flag is set.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean remoteCacheAsync = new Flag.Boolean("remoteCacheAsync");

    @Option(
        name = "remote_cache_compression",
        oldName = "experimental_remote_cache_compression",
        defaultValue = "false",
        help = "\n" +
            "      If enabled, compress/decompress cache blobs with zstd when their size is at least\n" +
            "      --experimental_remote_cache_compression_threshold.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean remoteCacheCompression = new Flag.Boolean("remoteCacheCompression");

    @Option(
        name = "remote_cache_header",
        allowMultiple = true,
        help = "\n" +
            "      Specify a header that will be included in cache requests: --remote_cache_header=Name=Value. Multiple headers\n" +
            "      can be passed by specifying the flag multiple times. Multiple values for the same name will be converted to a\n" +
            "      comma-separated list.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteCacheHeader = new Flag.Unknown("remoteCacheHeader");

    @Option(
        name = "remote_default_exec_properties",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Set the default exec properties to be used as the remote execution platform if an execution platform does not\n" +
            "      already set exec_properties.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteDefaultExecProperties = new Flag.Unknown("remoteDefaultExecProperties");

    @Option(
        name = "remote_default_platform_properties",
        oldName = "host_platform_remote_properties_override",
        defaultValue = "",
        help = "\n" +
            "      Set the default platform properties to be set for the remote execution API, if the execution platform does\n" +
            "      not already set remote_execution_properties. This value will also be used if the host platform is selected as\n" +
            "      the execution platform for remote execution.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str remoteDefaultPlatformProperties = new Flag.Str("remoteDefaultPlatformProperties");

    @Option(
        name = "remote_download_all",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        expandsTo = {"--remote_download_outputs=all"},
        help = "\n" +
            "      Downloads all remote outputs to the local machine. This flag is an alias for --remote_download_outputs=all.\n" +
            "    ",
        valueHelp = "",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteDownloadAll = new Flag.Unknown("remoteDownloadAll");

    @Option(
        name = "remote_download_minimal",
        oldName = "experimental_remote_download_minimal",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        expandsTo = {"--remote_download_outputs=minimal"},
        help = "\n" +
            "      Does not download any remote build outputs to the local machine. This flag is an alias for\n" +
            "      --remote_download_outputs=minimal.\n" +
            "    ",
        valueHelp = "",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteDownloadMinimal = new Flag.Unknown("remoteDownloadMinimal");

    @Option(
        name = "remote_download_outputs",
        oldName = "experimental_remote_download_outputs",
        defaultValue = "toplevel",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If set to 'minimal' doesn't download any remote build outputs to the local machine, except the ones required\n" +
            "      by local actions. If set to 'toplevel' behaves like'minimal' except that it also downloads outputs of top\n" +
            "      level targets to the local machine. Both options can significantly reduce build times if network bandwidth is\n" +
            "      a bottleneck.\n" +
            "    ",
        valueHelp = "all, minimal or toplevel",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf remoteDownloadOutputs = new Flag.OneOf("remoteDownloadOutputs");

    @Option(
        name = "remote_download_regex",
        oldName = "experimental_remote_download_regex",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Force remote build outputs whose path matches this pattern to be downloaded, irrespective of\n" +
            "      --remote_download_outputs. Multiple patterns may be specified by repeating this flag.\n" +
            "    ",
        valueHelp = "a valid Java regular expression",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteDownloadRegex = new Flag.Unknown("remoteDownloadRegex");

    @Option(
        name = "remote_download_symlink_template",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Instead of downloading remote build outputs to the local machine, create symbolic links. The target of the\n" +
            "      symbolic links can be specified in the form of a template string. This template string may contain {hash} and\n" +
            "      {size_bytes} that expand to the hash of the object and the size in bytes, respectively. These symbolic links\n" +
            "      may, for example, point to a FUSE file system that loads objects from the CAS on demand.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str remoteDownloadSymlinkTemplate = new Flag.Str("remoteDownloadSymlinkTemplate");

    @Option(
        name = "remote_download_toplevel",
        oldName = "experimental_remote_download_toplevel",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        expandsTo = {"--remote_download_outputs=toplevel"},
        help = "\n" +
            "      Only downloads remote outputs of top level targets to the local machine. This flag is an alias for\n" +
            "      --remote_download_outputs=toplevel.\n" +
            "    ",
        valueHelp = "",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteDownloadToplevel = new Flag.Unknown("remoteDownloadToplevel");

    @Option(
        name = "remote_downloader_header",
        allowMultiple = true,
        help = "\n" +
            "      Specify a header that will be included in remote downloader requests: --remote_downloader_header=Name=Value.\n" +
            "      Multiple headers can be passed by specifying the flag multiple times. Multiple values for the same name will\n" +
            "      be converted to a comma-separated list.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteDownloaderHeader = new Flag.Unknown("remoteDownloaderHeader");

    @Option(
        name = "remote_exec_header",
        allowMultiple = true,
        help = "\n" +
            "      Specify a header that will be included in execution requests: --remote_exec_header=Name=Value. Multiple\n" +
            "      headers can be passed by specifying the flag multiple times. Multiple values for the same name will be\n" +
            "      converted to a comma-separated list.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteExecHeader = new Flag.Unknown("remoteExecHeader");

    @Option(
        name = "remote_execution_priority",
        defaultValue = "0",
        help = "\n" +
            "      The relative priority of actions to be executed remotely. The semantics of the particular priority values are\n" +
            "      server-dependent.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer remoteExecutionPriority = new Flag.Integer("remoteExecutionPriority");

    @Option(
        name = "remote_executor",
        help = "\n" +
            "      HOST or HOST:PORT of a remote execution endpoint. The supported schemas are grpc, grpcs (grpc with TLS\n" +
            "      enabled) and unix (local UNIX sockets). If no schema is provided Bazel will default to grpcs. Specify grpc://\n" +
            "      or unix: schema to disable TLS.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str remoteExecutor = new Flag.Str("remoteExecutor");

    @Option(
        name = "remote_grpc_log",
        oldName = "experimental_remote_grpc_log",
        help = "\n" +
            "      If specified, a path to a file to log gRPC call related details. This log consists of a sequence of\n" +
            "      serialized com.google.devtools.build.lib.remote.logging.RemoteExecutionLog.LogEntry protobufs with each\n" +
            "      message prefixed by a varint denoting the size of the following serialized protobuf message, as performed by\n" +
            "      the method LogEntry.writeDelimitedTo(OutputStream).\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path remoteGrpcLog = new Flag.Path("remoteGrpcLog");

    @Option(
        name = "remote_header",
        allowMultiple = true,
        help = "\n" +
            "      Specify a header that will be included in requests: --remote_header=Name=Value. Multiple headers can be\n" +
            "      passed by specifying the flag multiple times. Multiple values for the same name will be converted to a\n" +
            "      comma-separated list.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown remoteHeader = new Flag.Unknown("remoteHeader");

    @Option(
        name = "remote_instance_name",
        defaultValue = "",
        help = "Value to pass as instance_name in the remote execution API.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str remoteInstanceName = new Flag.Str("remoteInstanceName");

    @Option(
        name = "remote_local_fallback",
        defaultValue = "false",
        help = "Whether to fall back to standalone local execution strategy if remote execution fails.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean remoteLocalFallback = new Flag.Boolean("remoteLocalFallback");

    @Option(
        name = "remote_local_fallback_strategy",
        defaultValue = "local",
        help = "Deprecated. See https://github.com/bazelbuild/bazel/issues/7480 for details.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str remoteLocalFallbackStrategy = new Flag.Str("remoteLocalFallbackStrategy");

    @Option(
        name = "remote_max_connections",
        defaultValue = "100",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Limit the max number of concurrent connections to remote cache/executor. By default the value is 100. Setting\n" +
            "      this to 0 means no limitation.\n" +
            "      For HTTP remote cache, one TCP connection could handle one request at one time, so Bazel could make up to\n" +
            "      --remote_max_connections concurrent requests.\n" +
            "      For gRPC remote cache/executor, one gRPC channel could usually handle 100+ concurrent requests, so Bazel\n" +
            "      could make around `--remote_max_connections * 100` concurrent requests.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer remoteMaxConnections = new Flag.Integer("remoteMaxConnections");

    @Option(
        name = "remote_print_execution_messages",
        defaultValue = "failure",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Choose when to print remote execution messages. Valid values are `failure`, to print only on failures,\n" +
            "      `success` to print only on successes and `all` to print always.\n" +
            "    ",
        valueHelp = "failure, success or all",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.OneOf remotePrintExecutionMessages = new Flag.OneOf("remotePrintExecutionMessages");

    @Option(
        name = "remote_proxy",
        oldName = "remote_cache_proxy",
        help = "\n" +
            "      Connect to the remote cache through a proxy. Currently this flag can only be used to configure a Unix domain\n" +
            "      socket (unix:/path/to/socket).\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str remoteProxy = new Flag.Str("remoteProxy");

    @Option(
        name = "remote_result_cache_priority",
        defaultValue = "0",
        help = "\n" +
            "      The relative priority of remote actions to be stored in remote cache. The semantics of the particular\n" +
            "      priority values are server-dependent.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer remoteResultCachePriority = new Flag.Integer("remoteResultCachePriority");

    @Option(
        name = "remote_retries",
        oldName = "experimental_remote_retry_max_attempts",
        defaultValue = "5",
        help = "The maximum number of attempts to retry a transient error. If set to 0, retries are disabled.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer remoteRetries = new Flag.Integer("remoteRetries");

    @Option(
        name = "remote_retry_max_delay",
        defaultValue = "5s",
        help = "\n" +
            "      The maximum backoff delay between remote retry attempts. Following units can be used: Days (d), hours (h),\n" +
            "      minutes (m), seconds (s), and milliseconds (ms). If the unit is omitted, the value is interpreted as seconds.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration remoteRetryMaxDelay = new Flag.Duration("remoteRetryMaxDelay");

    @Option(
        name = "remote_timeout",
        defaultValue = "60s",
        help = "\n" +
            "      The maximum amount of time to wait for remote execution and cache calls. For the REST cache, this is both the\n" +
            "      connect and the read timeout. Following units can be used: Days (d), hours (h), minutes (m), seconds (s), and\n" +
            "      milliseconds (ms). If the unit is omitted, the value is interpreted as seconds.\n" +
            "    ",
        valueHelp = "An immutable length of time.",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Duration remoteTimeout = new Flag.Duration("remoteTimeout");

    @Option(
        name = "remote_upload_local_results",
        defaultValue = "true",
        help = "\n" +
            "      Whether to upload locally executed action results to the remote cache if the remote cache supports it and the\n" +
            "      user is authorized to do so.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean remoteUploadLocalResults = new Flag.Boolean("remoteUploadLocalResults");

    @Option(
        name = "remote_verify_downloads",
        defaultValue = "true",
        help = "\n" +
            "      If set to true, Bazel will compute the hash sum of all remote downloads and  discard the remotely cached\n" +
            "      values if they don't match the expected value.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean remoteVerifyDownloads = new Flag.Boolean("remoteVerifyDownloads");

    @Option(
        name = "repo",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      Only fetches the specified repository, which can be either {@apparent_repo_name} or {@@canonical_repo_name}.\n" +
            "      Only works when --enable_bzlmod is on.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"fetch", "vendor"}
    )
    public static final Flag.Str repo = new Flag.Str("repo");

    @Option(
        name = "repo_env",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "\n" +
            "      Specifies additional environment variables to be available only for repository rules. Note that repository\n" +
            "      rules see the full environment anyway, but in this way configuration information can be passed to\n" +
            "      repositories through options without invalidating the action graph.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment with an optional value part",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown repoEnv = new Flag.Unknown("repoEnv");

    @Option(
        name = "repositories_without_autoloads",
        defaultValue = "",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.BUILD_FILE_SEMANTICS},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      A list of additional repositories (beyond the hardcoded ones Bazel knows about) where autoloads are not to be\n" +
            "      added. This should typically contain repositories that are transitively depended on by a repository that may\n" +
            "      be loaded automatically (and which can therefore potentially create a cycle).\n" +
            "    ",
        valueHelp = "comma-separated set of options",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown repositoriesWithoutAutoloads = new Flag.Unknown("repositoriesWithoutAutoloads");

    @Option(
        name = "repository_cache",
        oldName = "experimental_repository_cache",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      Specifies the cache location of the downloaded values obtained during the fetching of external repositories.\n" +
            "      An empty string as argument requests the cache to be disabled, otherwise the default of\n" +
            "      '<output_user_root>/cache/repos/v1' is used\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path repositoryCache = new Flag.Path("repositoryCache");

    @Option(
        name = "repository_disable_download",
        oldName = "experimental_repository_disable_download",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      If set, downloading using ctx.download{,_and_extract} is not allowed during repository fetching. Note that\n" +
            "      network access is not completely disabled; ctx.execute could still run an arbitrary executable that accesses\n" +
            "      the Internet.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean repositoryDisableDownload = new Flag.Boolean("repositoryDisableDownload");

    @Option(
        name = "restart_reason",
        defaultValue = "no_restart",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "The reason for the server restart.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str restartReason = new Flag.Str("restartReason");

    @Option(
        name = "reuse_sandbox_directories",
        oldName = "experimental_reuse_sandbox_directories",
        defaultValue = "true",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If set to true, directories used by sandboxed non-worker execution may be reused to avoid unnecessary setup\n" +
            "      costs.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean reuseSandboxDirectories = new Flag.Boolean("reuseSandboxDirectories");

    @Option(
        name = "rewind_lost_inputs",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "Whether to use action rewinding to recover from lost inputs.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean rewindLostInputs = new Flag.Boolean("rewindLostInputs");

    @Option(
        name = "rule_classes",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Dump rule classes.",
        valueHelp = "a boolean",
        commands = {"dump"}
    )
    public static final Flag.Boolean ruleClasses = new Flag.Boolean("ruleClasses");

    @Option(
        name = "rules",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Dump rules, including counts and memory usage (if memory is tracked).",
        valueHelp = "a boolean",
        commands = {"dump"}
    )
    public static final Flag.Boolean rules = new Flag.Boolean("rules");

    @Option(
        name = "run",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If false, skip running the command line constructed for the built target. Note that this flag is ignored for\n" +
            "      all --script_path builds.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"run"}
    )
    public static final Flag.Boolean run = new Flag.Boolean("run");

    @Option(
        name = "run_env",
        allowMultiple = true,
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specifies the set of environment variables available to actions with target configuration. Variables can be\n" +
            "      either specified by name, in which case the value will be taken from the invocation environment, or by the\n" +
            "      name=value pair which sets the value independent of the invocation environment. This option can be used\n" +
            "      multiple times; for options given for the same variable, the latest wins, options for different variables\n" +
            "      accumulate.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment with an optional value part",
        commands = {"run"}
    )
    public static final Flag.Unknown runEnv = new Flag.Unknown("runEnv");

    @Option(
        name = "run_in_client",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      If true, the mobile-install deployer command will be sent to the bazel client for execution. Useful for\n" +
            "      configurations where the bazel client is on a different machine than the bazel server.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"mobile-install"}
    )
    public static final Flag.Boolean runInClient = new Flag.Boolean("runInClient");

    @Option(
        name = "run_under",
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES},
        help = "\n" +
            "      Prefix to insert before the executables for the 'test' and 'run' commands. If the value is 'foo -bar', and\n" +
            "      the execution command line is 'test_binary -baz', then the final command line is 'foo -bar test_binary\n" +
            "      -baz'.This can also be a label to an executable target. Some examples are: 'valgrind', 'strace', 'strace -c',\n" +
            "      'valgrind --quiet --num-callers=20', '//package:target',  '//package:target --options'.\n" +
            "    ",
        valueHelp = "a prefix in front of command",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown runUnder = new Flag.Unknown("runUnder");

    @Option(
        name = "run_validations",
        oldName = "experimental_run_validations",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Whether to run validation actions as part of the build. See\n" +
            "      https://bazel.build/extending/rules#validation_actions\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean runValidations = new Flag.Boolean("runValidations");

    @Option(
        name = "runs_per_test",
        defaultValue = "1",
        allowMultiple = true,
        help = "\n" +
            "      Specifies number of times to run each test. If any of those attempts fail for any reason, the whole test is\n" +
            "      considered failed. Normally the value specified is just an integer. Example: --runs_per_test=3 will run all\n" +
            "      tests 3 times. Alternate syntax: regex_filter@runs_per_test. Where runs_per_test stands for an integer value\n" +
            "      and regex_filter stands for a list of include and exclude regular expression patterns (Also see\n" +
            "      --instrumentation_filter). Example: --runs_per_test=//foo/.*,-//foo/bar/.*@3 runs all tests in //foo/ except\n" +
            "      those under foo/bar three times. This option can be passed multiple times. The most recently passed argument\n" +
            "      that matches takes precedence. If nothing matches, the test is only run once.\n" +
            "    ",
        valueHelp = "a positive integer or test_regex@runs. This flag may be passed more than once",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown runsPerTest = new Flag.Unknown("runsPerTest");

    @Option(
        name = "runs_per_test_detects_flakes",
        defaultValue = "false",
        help = "\n" +
            "      If true, any shard in which at least one run/attempt passes and at least one run/attempt fails gets a FLAKY\n" +
            "      status.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean runsPerTestDetectsFlakes = new Flag.Boolean("runsPerTestDetectsFlakes");

    @Option(
        name = "sandbox_add_mount_pair",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        help = "Add additional path pair to mount in sandbox.",
        valueHelp = "a single path or a 'source:target' pair",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown sandboxAddMountPair = new Flag.Unknown("sandboxAddMountPair");

    @Option(
        name = "sandbox_base",
        oldName = "experimental_sandbox_base",
        defaultValue = "",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Lets the sandbox create its sandbox directories underneath this path. Specify a path on tmpfs (like /run/shm)\n" +
            "      to possibly improve performance a lot when your build / tests have many input files. Note: You need enough\n" +
            "      RAM and free space on the tmpfs to hold output and intermediate files generated by running actions.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str sandboxBase = new Flag.Str("sandboxBase");

    @Option(
        name = "sandbox_block_path",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        help = "For sandboxed actions, disallow access to this path.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str sandboxBlockPath = new Flag.Str("sandboxBlockPath");

    @Option(
        name = "sandbox_debug",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Enables debugging features for the sandboxing feature. This includes two things: first, the sandbox root\n" +
            "      contents are left untouched after a build; and second, prints extra debugging information on execution. This\n" +
            "      can help developers of Bazel or Starlark rules with debugging failures due to missing input files, etc.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean sandboxDebug = new Flag.Boolean("sandboxDebug");

    @Option(
        name = "sandbox_default_allow_network",
        oldName = "experimental_sandbox_default_allow_network",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Allow network access by default for actions; this may not work with all sandboxing implementations.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean sandboxDefaultAllowNetwork = new Flag.Boolean("sandboxDefaultAllowNetwork");

    @Option(
        name = "sandbox_explicit_pseudoterminal",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Explicitly enable the creation of pseudoterminals for sandboxed actions. Some linux distributions require\n" +
            "      setting the group id of the process to 'tty' inside the sandbox in order for pseudoterminals to function. If\n" +
            "      this is causing issues, this flag can be disabled to enable other groups to be used.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean sandboxExplicitPseudoterminal = new Flag.Boolean("sandboxExplicitPseudoterminal");

    @Option(
        name = "sandbox_fake_hostname",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "Change the current hostname to 'localhost' for sandboxed actions.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean sandboxFakeHostname = new Flag.Boolean("sandboxFakeHostname");

    @Option(
        name = "sandbox_fake_username",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "Change the current username to 'nobody' for sandboxed actions.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean sandboxFakeUsername = new Flag.Boolean("sandboxFakeUsername");

    @Option(
        name = "sandbox_tmpfs_path",
        allowMultiple = true,
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      For sandboxed actions, mount an empty, writable directory at this absolute path (if supported by the\n" +
            "      sandboxing implementation, ignored otherwise).\n" +
            "    ",
        valueHelp = "an absolute path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown sandboxTmpfsPath = new Flag.Unknown("sandboxTmpfsPath");

    @Option(
        name = "sandbox_writable_path",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      For sandboxed actions, make an existing directory writable in the sandbox (if supported by the sandboxing\n" +
            "      implementation, ignored otherwise).\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str sandboxWritablePath = new Flag.Str("sandboxWritablePath");

    @Option(
        name = "save_temps",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If set, temporary outputs from gcc will be saved.  These include .s files (assembler code), .i files\n" +
            "      (preprocessed C) and .ii files (preprocessed C++).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean saveTemps = new Flag.Boolean("saveTemps");

    @Option(
        name = "scl_config",
        metadataTags = {OptionMetadataTag.EXPERIMENTAL},
        help = "\n" +
            "      Name of the scl config defined in PROJECT.scl. Note that this feature is still under development b/324119879.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str sclConfig = new Flag.Str("sclConfig");

    @Option(
        name = "script_path",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If set, write a shell script to the given file which invokes the target. If this option is set, the target is\n" +
            "      not run from %{product}. Use '%{product} run --script_path=foo //foo && ./foo' to invoke target '//foo' This\n" +
            "      differs from '%{product} run //foo' in that the %{product} lock is released and the executable is connected\n" +
            "      to the terminal's stdin.\n" +
            "    ",
        valueHelp = "a path",
        commands = {"run"}
    )
    public static final Flag.Path scriptPath = new Flag.Path("scriptPath");

    @Option(
        name = "separate_aspect_deps",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "No-op",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean separateAspectDeps = new Flag.Boolean("separateAspectDeps");

    @Option(
        name = "serialized_frontier_profile",
        defaultValue = "",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Dump a profile of serialized frontier bytes. Specifies the output path.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str serializedFrontierProfile = new Flag.Str("serializedFrontierProfile");

    @Option(
        name = "server_javabase",
        defaultValue = "",
        help = "Path to the JVM used to execute Bazel itself.",
        valueHelp = "a string",
        commands = {"startup"}
    )
    public static final Flag.Str serverJavabase = new Flag.Str("serverJavabase");

    @Option(
        name = "server_jvm_out",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      The location to write the server's JVM's output. If unset then defaults to a location in output_base.\n" +
            "    ",
        valueHelp = "a path",
        commands = {"startup"}
    )
    public static final Flag.Path serverJvmOut = new Flag.Path("serverJvmOut");

    @Option(
        name = "share_native_deps",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If true, native libraries that contain identical functionality will be shared among different targets\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean shareNativeDeps = new Flag.Boolean("shareNativeDeps");

    @Option(
        name = "shell_executable",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Absolute path to the shell executable for Bazel to use. If this is unset, but the BAZEL_SH environment\n" +
            "      variable is set on the first Bazel invocation (that starts up a Bazel server), Bazel uses that. If neither is\n" +
            "      set, Bazel uses a hard-coded default path depending on the operating system it runs on (Windows:\n" +
            "      c:/msys64/usr/bin/bash.exe, FreeBSD: /usr/local/bin/bash, all others: /bin/bash). Note that using a shell\n" +
            "      that is not compatible with bash may lead to build failures or runtime failures of the generated binaries.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Path shellExecutable = new Flag.Path("shellExecutable");

    @Option(
        name = "short",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        expandsTo = {"--help_verbosity=short"},
        help = "Show only the names of the options, not their types or meanings.",
        valueHelp = "",
        commands = {"help"}
    )
    public static final Flag.Unknown short_ = new Flag.Unknown("short");

    @Option(
        name = "show_config_fragments",
        defaultValue = "off",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Shows the configuration fragments required by a rule and its transitive dependencies. This can be useful for\n" +
            "      evaluating how much a configured target graph can be trimmed.\n" +
            "    ",
        valueHelp = "off, direct or transitive",
        commands = {"cquery"}
    )
    public static final Flag.OneOf showConfigFragments = new Flag.OneOf("showConfigFragments");

    @Option(
        name = "show_loading_progress",
        defaultValue = "true",
        help = "If enabled, causes Bazel to print \"Loading package:\" messages.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "mod", "print_action", "query", "run", "sync", "test", "vendor",
        }
    )
    public static final Flag.Boolean showLoadingProgress = new Flag.Boolean("showLoadingProgress");

    @Option(
        name = "show_make_env",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.TERMINAL_OUTPUT},
        help = "Include the \"Make\" environment in the output.",
        valueHelp = "a boolean",
        commands = {"info"}
    )
    public static final Flag.Boolean showMakeEnv = new Flag.Boolean("showMakeEnv");

    @Option(
        name = "show_progress",
        defaultValue = "true",
        help = "Display progress messages during a build.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean showProgress = new Flag.Boolean("showProgress");

    @Option(
        name = "show_progress_rate_limit",
        defaultValue = "0.2",
        help = "Minimum number of seconds between progress messages in the output.",
        valueHelp = "a double",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Double showProgressRateLimit = new Flag.Double("showProgressRateLimit");

    @Option(
        name = "show_result",
        defaultValue = "1",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Show the results of the build.  For each target, state whether or not it was brought up-to-date, and if so, a\n" +
            "      list of output files that were built.  The printed files are convenient strings for copy+pasting to the\n" +
            "      shell, to execute them.\n" +
            "      This option requires an integer argument, which is the threshold number of targets above which result\n" +
            "      information is not printed. Thus zero causes suppression of the message and MAX_INT causes printing of the\n" +
            "      result to occur always. The default is one.\n" +
            "      If nothing was built for a target its results may be omitted to keep the output under the threshold.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer showResult = new Flag.Integer("showResult");

    @Option(
        name = "show_timestamps",
        defaultValue = "false",
        help = "Include timestamps in messages",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean showTimestamps = new Flag.Boolean("showTimestamps");

    @Option(
        name = "shutdown_on_low_sys_mem",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EAGERNESS_TO_EXIT, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      If max_idle_secs is set and the build server has been idle for a while, shut down the server when the system\n" +
            "      is low on free RAM. Linux only.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean shutdownOnLowSysMem = new Flag.Boolean("shutdownOnLowSysMem");

    @Option(
        name = "skip_incompatible_explicit_targets",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Skip incompatible targets that are explicitly listed on the command line. By default, building such targets\n" +
            "      results in an error but they are silently skipped when this option is enabled. See:\n" +
            "      https://bazel.build/extending/platforms#skipping-incompatible-targets\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean skipIncompatibleExplicitTargets = new Flag.Boolean("skipIncompatibleExplicitTargets");

    @Option(
        name = "skyframe",
        defaultValue = "off",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Dump the Skyframe graph.",
        valueHelp = "off, summary, count, value, deps, rdeps, function_graph, working_set or working_set_frontier_deps",
        commands = {"dump"}
    )
    public static final Flag.OneOf skyframe = new Flag.OneOf("skyframe");

    @Option(
        name = "skyframe_high_water_mark_full_gc_drops_per_invocation",
        defaultValue = "10",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel detects its retained heap\n" +
            "      percentage usage exceeds the threshold set by --skyframe_high_water_mark_threshold, when a full GC event\n" +
            "      occurs, it will drop unnecessary temporary Skyframe state, up to this many times per invocation. Defaults to\n" +
            "      10. Zero means that full GC events will never trigger drops. If the limit is reached, Skyframe state will no\n" +
            "      longer be dropped when a full GC event occurs and that retained heap percentage threshold is exceeded.\n" +
            "    ",
        valueHelp = "an integer, >= 0",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown skyframeHighWaterMarkFullGcDropsPerInvocation = new Flag.Unknown("skyframeHighWaterMarkFullGcDropsPerInvocation");

    @Option(
        name = "skyframe_high_water_mark_minor_gc_drops_per_invocation",
        defaultValue = "10",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel detects its retained heap\n" +
            "      percentage usage exceeds the threshold set by --skyframe_high_water_mark_threshold, when a minor GC event\n" +
            "      occurs, it will drop unnecessary temporary Skyframe state, up to this many times per invocation. Defaults to\n" +
            "      10. Zero means that minor GC events will never trigger drops. If the limit is reached, Skyframe state will no\n" +
            "      longer be dropped when a minor GC event occurs and that retained heap percentage threshold is exceeded.\n" +
            "    ",
        valueHelp = "an integer, >= 0",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown skyframeHighWaterMarkMinorGcDropsPerInvocation = new Flag.Unknown("skyframeHighWaterMarkMinorGcDropsPerInvocation");

    @Option(
        name = "skyframe_high_water_mark_threshold",
        defaultValue = "85",
        effectTags = {OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Flag for advanced configuration of Bazel's internal Skyframe engine. If Bazel detects its retained heap\n" +
            "      percentage usage is at least this threshold, it will drop unnecessary temporary Skyframe state. Tweaking this\n" +
            "      may let you mitigate wall time impact of GC thrashing, when the GC thrashing is (i) caused by the memory\n" +
            "      usage of this temporary state and (ii) more costly than reconstituting the state when it is needed.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer skyframeHighWaterMarkThreshold = new Flag.Integer("skyframeHighWaterMarkThreshold");

    @Option(
        name = "skyframe_state",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Without performing extra analysis, dump the current Action Graph from Skyframe. Note: Specifying a target\n" +
            "      with --skyframe_state is currently not supported. This flag is only available with --output=proto or\n" +
            "      --output=textproto.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery"}
    )
    public static final Flag.Boolean skyframeState = new Flag.Boolean("skyframeState");

    @Option(
        name = "skykey_filter",
        defaultValue = ".*",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Regex filter of SkyKey names to output. Only used with --skyframe=deps, rdeps, function_graph.",
        valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
        commands = {"dump"}
    )
    public static final Flag.Unknown skykeyFilter = new Flag.Unknown("skykeyFilter");

    @Option(
        name = "skylark_memory",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "\n" +
            "      Dumps a pprof-compatible memory profile to the specified path. To learn more please see\n" +
            "      https://github.com/google/pprof.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"dump"}
    )
    public static final Flag.Str skylarkMemory = new Flag.Str("skylarkMemory");

    @Option(
        name = "slim_profile",
        oldName = "experimental_slim_json_profile",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Slims down the size of the JSON profile by merging events if the profile gets  too large.",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean slimProfile = new Flag.Boolean("slimProfile");

    @Option(
        name = "spawn_strategy",
        defaultValue = "",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Specify how spawn actions are executed by default. Accepts a comma-separated list of strategies from highest\n" +
            "      to lowest priority. For each action Bazel picks the strategy with the highest priority that can execute the\n" +
            "      action. The default value is \"remote,worker,sandboxed,local\". See\n" +
            "      https://blog.bazel.build/2019/06/19/list-strategy.html for details.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown spawnStrategy = new Flag.Unknown("spawnStrategy");

    @Option(
        name = "split_apks",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Whether to use split apks to install and update the application on the device. Works only with devices with\n" +
            "      Marshmallow or later\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"mobile-install"}
    )
    public static final Flag.Boolean splitApks = new Flag.Boolean("splitApks");

    @Option(
        name = "split_bytecode_optimization_pass",
        defaultValue = "false",
        help = "Do not use.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean splitBytecodeOptimizationPass = new Flag.Boolean("splitBytecodeOptimizationPass");

    @Option(
        name = "stamp",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Stamp binaries with the date, username, hostname, workspace information, etc.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean stamp = new Flag.Boolean("stamp");

    @Option(
        name = "starlark:expr",
        defaultValue = "",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      A Starlark expression to format each configured target in cquery's --output=starlark mode. The configured\n" +
            "      target is bound to 'target'. If neither --starlark:expr nor --starlark:file is specified, this option will\n" +
            "      default to 'str(target.label)'. It is an error to specify both --starlark:expr and --starlark:file.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"cquery"}
    )
    public static final Flag.Str starlark_expr = new Flag.Str("starlark_expr");

    @Option(
        name = "starlark:file",
        defaultValue = "",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      The name of a file that defines a Starlark function called 'format', of one argument, that is applied to each\n" +
            "      configured target to format it as a string. It is an error to specify both --starlark:expr and\n" +
            "      --starlark:file. See help for --output=starlark for additional detail.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"cquery"}
    )
    public static final Flag.Str starlark_file = new Flag.Str("starlark_file");

    @Option(
        name = "starlark_cpu_profile",
        defaultValue = "",
        effectTags = {OptionEffectTag.BAZEL_MONITORING},
        help = "Writes into the specified file a pprof profile of CPU usage by all Starlark threads.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str starlarkCpuProfile = new Flag.Str("starlarkCpuProfile");

    @Option(
        name = "start",
        defaultValue = "NO",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      How the app should be started after installing it. Set to WARM to preserve and restore application state on\n" +
            "      incremental installs.\n" +
            "    ",
        valueHelp = "no, cold, warm or debug",
        commands = {"mobile-install"}
    )
    public static final Flag.OneOf start = new Flag.OneOf("start");

    @Option(
        name = "start_app",
        effectTags = {OptionEffectTag.EXECUTION},
        expandsTo = {"--start=COLD"},
        help = "Whether to start the app after installing it.",
        valueHelp = "",
        commands = {"mobile-install"}
    )
    public static final Flag.Unknown startApp = new Flag.Unknown("startApp");

    @Option(
        name = "start_end_lib",
        defaultValue = "true",
        effectTags = {
            OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.CHANGES_INPUTS,
            OptionEffectTag.AFFECTS_OUTPUTS,
        },
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "Use the --start-lib/--end-lib ld options if supported by the toolchain.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean startEndLib = new Flag.Boolean("startEndLib");

    @Option(
        name = "startup_time",
        defaultValue = "0",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "The time in ms the launcher spends before sending the request to the bazel server.",
        valueHelp = "a long integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown startupTime = new Flag.Unknown("startupTime");

    @Option(
        name = "strategy",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Specify how to distribute compilation of other spawn actions. Accepts a comma-separated list of strategies\n" +
            "      from highest to lowest priority. For each action Bazel picks the strategy with the highest priority that can\n" +
            "      execute the action. The default value is \"remote,worker,sandboxed,local\". This flag overrides the values set\n" +
            "      by --spawn_strategy (and --genrule_strategy if used with mnemonic Genrule). See\n" +
            "      https://blog.bazel.build/2019/06/19/list-strategy.html for details.\n" +
            "    ",
        valueHelp = "a '[name=]value1[,..,valueN]' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown strategy = new Flag.Unknown("strategy");

    @Option(
        name = "strategy_regexp",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Override which spawn strategy should be used to execute spawn actions that have descriptions matching a\n" +
            "      certain regex_filter. See --per_file_copt for details onregex_filter matching. The last regex_filter that\n" +
            "      matches the description is used. This option overrides other flags for specifying strategy. Example:\n" +
            "      --strategy_regexp=//foo.*\\.cc,-//foo/bar=local means to run actions using local strategy if their\n" +
            "      descriptions match //foo.*.cc but not //foo/bar. Example: --strategy_regexp='Compiling.*/bar=local\n" +
            "      --strategy_regexp=Compiling=sandboxed will run 'Compiling //foo/bar/baz' with the 'local' strategy, but\n" +
            "      reversing the order would run it with 'sandboxed'.\n" +
            "    ",
        valueHelp = "a '<RegexFilter>=value[,value]' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown strategyRegexp = new Flag.Unknown("strategyRegexp");

    @Option(
        name = "strict_deps_java_protos",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "No-op, kept only for backwards compatibility",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean strictDepsJavaProtos = new Flag.Boolean("strictDepsJavaProtos");

    @Option(
        name = "strict_filesets",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "If this option is enabled, filesets crossing package boundaries are reported as errors.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean strictFilesets = new Flag.Boolean("strictFilesets");

    @Option(
        name = "strict_proto_deps",
        defaultValue = "error",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      Unless OFF, checks that a proto_library target explicitly declares all directly used targets as dependencies.\n" +
            "    ",
        valueHelp = "off, warn, error, strict or default",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf strictProtoDeps = new Flag.OneOf("strictProtoDeps");

    @Option(
        name = "strict_public_imports",
        defaultValue = "off",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT},
        metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
        help = "\n" +
            "      Unless OFF, checks that a proto_library target explicitly declares all targets used in 'import public' as\n" +
            "      exported.\n" +
            "    ",
        valueHelp = "off, warn, error, strict or default",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf strictPublicImports = new Flag.OneOf("strictPublicImports");

    @Option(
        name = "strict_system_includes",
        defaultValue = "false",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      If true, headers found through system include paths (-isystem) are also required to be declared.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean strictSystemIncludes = new Flag.Boolean("strictSystemIncludes");

    @Option(
        name = "strict_test_suite",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS, OptionEffectTag.EAGERNESS_TO_EXIT},
        help = "\n" +
            "      If true, the tests() expression gives an error if it encounters a test_suite containing non-test targets.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"query"}
    )
    public static final Flag.Boolean strictTestSuite = new Flag.Boolean("strictTestSuite");

    @Option(
        name = "strip",
        defaultValue = "sometimes",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Specifies whether to strip binaries and shared libraries  (using \"-Wl,--strip-debug\").  The default value of\n" +
            "      'sometimes' means strip iff --compilation_mode=fastbuild.\n" +
            "    ",
        valueHelp = "always, sometimes or never",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf strip = new Flag.OneOf("strip");

    @Option(
        name = "stripopt",
        allowMultiple = true,
        effectTags = {OptionEffectTag.ACTION_COMMAND_LINES, OptionEffectTag.AFFECTS_OUTPUTS},
        help = "Additional options to pass to strip when generating a '<name>.stripped' binary.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str stripopt = new Flag.Str("stripopt");

    @Option(
        name = "subcommands",
        abbrev = 's',
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Display the subcommands executed during a build. Related flags: --execution_log_json_file,\n" +
            "      --execution_log_binary_file (for logging subcommands to a file in a tool-friendly format).\n" +
            "    ",
        valueHelp = "true, pretty_print or false",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf subcommands = new Flag.OneOf("subcommands");

    @Option(
        name = "symlink_prefix",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      The prefix that is prepended to any of the convenience symlinks that are created after a build. If omitted,\n" +
            "      the default value is the name of the build tool followed by a hyphen. If '/' is passed, then no symlinks are\n" +
            "      created and no warning is emitted. Warning: the special functionality for '/' will be deprecated soon; use\n" +
            "      --experimental_convenience_symlinks=ignore instead.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str symlinkPrefix = new Flag.Str("symlinkPrefix");

    @Option(
        name = "system_rc",
        defaultValue = "true",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "Whether or not to look for the system-wide bazelrc.",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean systemRc = new Flag.Boolean("systemRc");

    @Option(
        name = "target_environment",
        allowMultiple = true,
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      Declares this build's target environment. Must be a label reference to an \"environment\" rule. If specified,\n" +
            "      all top-level targets must be compatible with this environment.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label targetEnvironment = new Flag.Label("targetEnvironment");

    @Option(
        name = "target_pattern_file",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "\n" +
            "      If set, build will read patterns from the file named here, rather than on the command line. It is an error to\n" +
            "      specify a file here as well as command-line patterns.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str targetPatternFile = new Flag.Str("targetPatternFile");

    @Option(
        name = "target_platform_fallback",
        defaultValue = "",
        effectTags = {OptionEffectTag.NO_OP},
        help = "This option is deprecated and has no effect.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str targetPlatformFallback = new Flag.Str("targetPlatformFallback");

    @Option(
        name = "terminal_columns",
        defaultValue = "80",
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "A system-generated parameter which specifies the terminal width in columns.",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer terminalColumns = new Flag.Integer("terminalColumns");

    @Option(
        name = "test_arg",
        allowMultiple = true,
        help = "\n" +
            "      Specifies additional options and arguments that should be passed to the test executable. Can be used multiple\n" +
            "      times to specify several arguments. If multiple tests are executed, each of them will receive identical\n" +
            "      arguments. Used only by the 'bazel test' command.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str testArg = new Flag.Str("testArg");

    @Option(
        name = "test_env",
        allowMultiple = true,
        effectTags = {OptionEffectTag.TEST_RUNNER},
        help = "\n" +
            "      Specifies additional environment variables to be injected into the test runner environment. Variables can be\n" +
            "      either specified by name, in which case its value will be read from the Bazel client environment, or by the\n" +
            "      name=value pair. This option can be used multiple times to specify several variables. Used only by the 'bazel\n" +
            "      test' command.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment with an optional value part",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown testEnv = new Flag.Unknown("testEnv");

    @Option(
        name = "test_filter",
        help = "\n" +
            "      Specifies a filter to forward to the test framework.  Used to limit the tests run. Note that this does not\n" +
            "      affect which targets are built.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str testFilter = new Flag.Str("testFilter");

    @Option(
        name = "test_keep_going",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      When disabled, any non-passing test will cause the entire build to stop. By default all tests are run, even\n" +
            "      if some do not pass.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean testKeepGoing = new Flag.Boolean("testKeepGoing");

    @Option(
        name = "test_lang_filters",
        defaultValue = "",
        help = "\n" +
            "      Specifies a comma-separated list of test languages. Each language can be optionally preceded with '-' to\n" +
            "      specify excluded languages. Only those test targets will be found that are written in the specified\n" +
            "      languages. The name used for each language should be the same as the language prefix in the *_test rule, e.g.\n" +
            "      one of 'cc', 'java', 'py', etc. This option affects --build_tests_only behavior and the test command.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown testLangFilters = new Flag.Unknown("testLangFilters");

    @Option(
        name = "test_output",
        defaultValue = "summary",
        effectTags = {OptionEffectTag.TEST_RUNNER, OptionEffectTag.TERMINAL_OUTPUT, OptionEffectTag.EXECUTION},
        help = "\n" +
            "      Specifies desired output mode. Valid values are 'summary' to output only test status summary, 'errors' to\n" +
            "      also print test logs for failed tests, 'all' to print logs for all tests and 'streamed' to output logs for\n" +
            "      all tests in real time (this will force tests to be executed locally one at a time regardless of\n" +
            "      --test_strategy value).\n" +
            "    ",
        valueHelp = "summary, errors, all or streamed",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf testOutput = new Flag.OneOf("testOutput");

    @Option(
        name = "test_result_expiration",
        defaultValue = "-1",
        help = "This option is deprecated and has no effect.",
        valueHelp = "an integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Integer testResultExpiration = new Flag.Integer("testResultExpiration");

    @Option(
        name = "test_runner_fail_fast",
        defaultValue = "false",
        help = "\n" +
            "      Forwards fail fast option to the test runner. The test runner should stop execution upon first failure.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean testRunnerFailFast = new Flag.Boolean("testRunnerFailFast");

    @Option(
        name = "test_sharding_strategy",
        defaultValue = "explicit",
        help = "\n" +
            "      Specify strategy for test sharding: 'explicit' to only use sharding if the 'shard_count' BUILD attribute is\n" +
            "      present. 'disabled' to never use test sharding. 'forced=k' to enforce 'k' shards for testing regardless of\n" +
            "      the 'shard_count' BUILD attribute.\n" +
            "    ",
        valueHelp = "explicit, disabled or forced=k where k is the number of shards to enforce",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown testShardingStrategy = new Flag.Unknown("testShardingStrategy");

    @Option(
        name = "test_size_filters",
        defaultValue = "",
        help = "\n" +
            "      Specifies a comma-separated list of test sizes. Each size can be optionally preceded with '-' to specify\n" +
            "      excluded sizes. Only those test targets will be found that contain at least one included size and do not\n" +
            "      contain any excluded sizes. This option affects --build_tests_only behavior and the test command.\n" +
            "    ",
        valueHelp = "comma-separated list of values: small, medium, large, or enormous",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown testSizeFilters = new Flag.Unknown("testSizeFilters");

    @Option(
        name = "test_strategy",
        defaultValue = "",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "Specifies which strategy to use when running tests.",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str testStrategy = new Flag.Str("testStrategy");

    @Option(
        name = "test_summary",
        defaultValue = "short",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Specifies the desired format of the test summary. Valid values are 'short' to print information only about\n" +
            "      tests executed, 'terse', to print information only about unsuccessful tests that were run, 'detailed' to\n" +
            "      print detailed information about failed test cases, 'testcase' to print summary in test case resolution, do\n" +
            "      not print detailed information about failed test cases and 'none' to omit the summary.\n" +
            "    ",
        valueHelp = "short, terse, detailed, none or testcase",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.OneOf testSummary = new Flag.OneOf("testSummary");

    @Option(
        name = "test_tag_filters",
        defaultValue = "",
        help = "\n" +
            "      Specifies a comma-separated list of test tags. Each tag can be optionally preceded with '-' to specify\n" +
            "      excluded tags. Only those test targets will be found that contain at least one included tag and do not\n" +
            "      contain any excluded tags. This option affects --build_tests_only behavior and the test command.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown testTagFilters = new Flag.Unknown("testTagFilters");

    @Option(
        name = "test_timeout",
        defaultValue = "-1",
        help = "\n" +
            "      Override the default test timeout values for test timeouts (in secs). If a single positive integer value is\n" +
            "      specified it will override all categories.  If 4 comma-separated integers are specified, they will override\n" +
            "      the timeouts for short, moderate, long and eternal (in that order). In either form, a value of -1 tells blaze\n" +
            "      to use its default timeouts for that category.\n" +
            "    ",
        valueHelp = "a single integer or comma-separated list of 4 integers",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown testTimeout = new Flag.Unknown("testTimeout");

    @Option(
        name = "test_timeout_filters",
        defaultValue = "",
        help = "\n" +
            "      Specifies a comma-separated list of test timeouts. Each timeout can be optionally preceded with '-' to\n" +
            "      specify excluded timeouts. Only those test targets will be found that contain at least one included timeout\n" +
            "      and do not contain any excluded timeouts. This option affects --build_tests_only behavior and the test\n" +
            "      command.\n" +
            "    ",
        valueHelp = "comma-separated list of values: short, moderate, long, or eternal",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown testTimeoutFilters = new Flag.Unknown("testTimeoutFilters");

    @Option(
        name = "test_tmpdir",
        help = "Specifies the base temporary directory for 'bazel test' to use.",
        valueHelp = "a path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Path testTmpdir = new Flag.Path("testTmpdir");

    @Option(
        name = "test_verbose_timeout_warnings",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      If true, print additional warnings when the actual test execution time does not match the timeout defined by\n" +
            "      the test (whether implied or explicit).\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"coverage", "cquery", "fetch", "test", "vendor"}
    )
    public static final Flag.Boolean testVerboseTimeoutWarnings = new Flag.Boolean("testVerboseTimeoutWarnings");

    @Option(
        name = "tls_authority_override",
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "\n" +
            "      TESTING ONLY! Can be used with a self-signed certificate to consider the specified value a valid TLS\n" +
            "      authority.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str tlsAuthorityOverride = new Flag.Str("tlsAuthorityOverride");

    @Option(
        name = "tls_certificate",
        help = "Specify a path to a TLS certificate that is trusted to sign server certificates.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str tlsCertificate = new Flag.Str("tlsCertificate");

    @Option(
        name = "tls_client_certificate",
        help = "\n" +
            "      Specify the TLS client certificate to use; you also need to provide a client key to enable client\n" +
            "      authentication.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str tlsClientCertificate = new Flag.Str("tlsClientCertificate");

    @Option(
        name = "tls_client_key",
        help = "\n" +
            "      Specify the TLS client key to use; you also need to provide a client certificate to enable client\n" +
            "      authentication.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str tlsClientKey = new Flag.Str("tlsClientKey");

    @Option(
        name = "tool_deps",
        oldName = "host_deps",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BUILD_FILE_SEMANTICS},
        help = "\n" +
            "      Query: If disabled, dependencies on 'exec configuration' will not be included in the dependency graph over\n" +
            "      which the query operates. An 'exec configuration' dependency edge, such as the one from any 'proto_library'\n" +
            "      rule to the Protocol Compiler, usually points to a tool executed during the build rather than a part of the\n" +
            "      same 'target' program.\n" +
            "      Cquery: If disabled, filters out all configured targets which cross an execution transition from the\n" +
            "      top-level target that discovered this configured target. That means if the top-level target is in the target\n" +
            "      configuration, only configured targets also in the target configuration will be returned. If the top-level\n" +
            "      target is in the exec configuration, only exec configured targets will be returned. This option will NOT\n" +
            "      exclude resolved toolchains.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Boolean toolDeps = new Flag.Boolean("toolDeps");

    @Option(
        name = "tool_java_language_version",
        defaultValue = "",
        help = "The Java language version used to execute the tools that are needed during a build",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str toolJavaLanguageVersion = new Flag.Str("toolJavaLanguageVersion");

    @Option(
        name = "tool_java_runtime_version",
        defaultValue = "remotejdk_11",
        help = "The Java runtime version used to execute tools during the build",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str toolJavaRuntimeVersion = new Flag.Str("toolJavaRuntimeVersion");

    @Option(
        name = "tool_tag",
        defaultValue = "",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.BAZEL_MONITORING},
        help = "A tool name to attribute this Bazel invocation to.",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str toolTag = new Flag.Str("toolTag");

    @Option(
        name = "toolchain_resolution_debug",
        defaultValue = "-.*",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Print debug information during toolchain resolution. The flag takes a regex, which is checked against\n" +
            "      toolchain types and specific targets to see which to debug. Multiple regexes may be  separated by commas, and\n" +
            "      then each regex is checked separately. Note: The output of this flag is very complex and will likely only be\n" +
            "      useful to experts in toolchain resolution.\n" +
            "    ",
        valueHelp = "a comma-separated list of regex expressions with prefix '-' specifying excluded paths",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown toolchainResolutionDebug = new Flag.Unknown("toolchainResolutionDebug");

    @Option(
        name = "track_incremental_state",
        oldName = "keep_incrementality_data",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      If false, Blaze will not persist data that allows for invalidation and re-evaluation on incremental builds in\n" +
            "      order to save memory on this build. Subsequent builds will not have any incrementality with respect to this\n" +
            "      one. Usually you will want to specify --batch when setting this to false.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean trackIncrementalState = new Flag.Boolean("trackIncrementalState");

    @Option(
        name = "transitions",
        defaultValue = "none",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "The format in which cquery will print transition information.",
        valueHelp = "full, lite or none",
        commands = {"cquery"}
    )
    public static final Flag.OneOf transitions = new Flag.OneOf("transitions");

    @Option(
        name = "trim_test_configuration",
        defaultValue = "true",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      When enabled, test-related options will be cleared below the top level of the build. When this flag is\n" +
            "      active, tests cannot be built as dependencies of non-test rules, but changes to test-related options will not\n" +
            "      cause non-test rules to be re-analyzed.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean trimTestConfiguration = new Flag.Boolean("trimTestConfiguration");

    @Option(
        name = "tvos_cpus",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Comma-separated list of architectures for which to build Apple tvOS binaries.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown tvosCpus = new Flag.Unknown("tvosCpus");

    @Option(
        name = "tvos_minimum_os",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Minimum compatible tvOS version for target simulators and devices. If unspecified, uses 'tvos_sdk_version'.\n" +
            "    ",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown tvosMinimumOs = new Flag.Unknown("tvosMinimumOs");

    @Option(
        name = "tvos_sdk_version",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Specifies the version of the tvOS SDK to use to build tvOS applications. If unspecified, uses the default\n" +
            "      tvOS SDK version from 'xcode_version'.\n" +
            "    ",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown tvosSdkVersion = new Flag.Unknown("tvosSdkVersion");

    @Option(
        name = "ui_actions_shown",
        oldName = "experimental_ui_actions_shown",
        defaultValue = "8",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Number of concurrent actions shown in the detailed progress bar; each action is shown on a separate line. The\n" +
            "      progress bar always shows at least one one, all numbers less than 1 are mapped to 1.\n" +
            "    ",
        valueHelp = "an integer",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Integer uiActionsShown = new Flag.Integer("uiActionsShown");

    @Option(
        name = "ui_event_filters",
        allowMultiple = true,
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      Specifies which events to show in the UI. It is possible to add or remove events to the default ones using\n" +
            "      leading +/-, or override the default set completely with direct assignment. The set of supported event kinds\n" +
            "      include INFO, DEBUG, ERROR and more.\n" +
            "    ",
        valueHelp = "Convert list of comma separated event kind to list of filters",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Unknown uiEventFilters = new Flag.Unknown("uiEventFilters");

    @Option(
        name = "unconditional_warning",
        allowMultiple = true,
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      A warning that will unconditionally get printed with build warnings and errors. This is useful to deprecate\n" +
            "      bazelrc files or --config definitions. If the intent is to effectively deprecate some flag or combination of\n" +
            "      flags, this is NOT sufficient. The flag or flags should use the deprecationWarning field in the option\n" +
            "      definition, or the bad combination should be checked for programmatically.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Str unconditionalWarning = new Flag.Str("unconditionalWarning");

    @Option(
        name = "universe_scope",
        defaultValue = "",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      A comma-separated set of target patterns (additive and subtractive). The query may be performed in the\n" +
            "      universe defined by the transitive closure of the specified targets. This option is used for the query and\n" +
            "      cquery commands.\n" +
            "      For cquery, the input to this option is the targets all answers are built under and so this option may affect\n" +
            "      configurations and transitions. If this option is not specified, the top-level targets are assumed to be the\n" +
            "      targets parsed from the query expression. Note: For cquery, not specifying this option may cause the build to\n" +
            "      break if targets parsed from the query expression are not buildable with top-level options.\n" +
            "    ",
        valueHelp = "comma-separated list of options",
        commands = {"aquery", "cquery", "query"}
    )
    public static final Flag.Unknown universeScope = new Flag.Unknown("universeScope");

    @Option(
        name = "unix_digest_hash_attribute_name",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      The name of an extended attribute that can be placed on files to store a precomputed copy of the file's hash,\n" +
            "      corresponding with --digest_function. This option can be used to reduce disk I/O and CPU load caused by hash\n" +
            "      computation. This extended attribute is checked on all source files and output files, meaning that it causes\n" +
            "      a significant number of invocations of the getxattr() system call.\n" +
            "    ",
        valueHelp = "a string",
        commands = {"startup"}
    )
    public static final Flag.Str unixDigestHashAttributeName = new Flag.Str("unixDigestHashAttributeName");

    @Option(
        name = "unlimit_coredumps",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      Raises the soft coredump limit to the hard limit to make coredumps of the server (including the JVM) and the\n" +
            "      client possible under common conditions. Stick this flag in your bazelrc once and forget about it so that you\n" +
            "      get coredumps when you actually encounter a condition that triggers them.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean unlimitCoredumps = new Flag.Boolean("unlimitCoredumps");

    @Option(
        name = "use_action_cache",
        defaultValue = "true",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "Whether to use the action cache",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean useActionCache = new Flag.Boolean("useActionCache");

    @Option(
        name = "use_ijars",
        defaultValue = "true",
        help = "\n" +
            "      If enabled, this option causes Java compilation to use interface jars. This will result in faster incremental\n" +
            "      compilation, but error messages can be different.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean useIjars = new Flag.Boolean("useIjars");

    @Option(
        name = "use_target_platform_for_tests",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If true, then Bazel will use the target platform for running tests rather than the test exec group.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean useTargetPlatformForTests = new Flag.Boolean("useTargetPlatformForTests");

    @Option(
        name = "use_top_level_targets_for_symlinks",
        defaultValue = "true",
        effectTags = {OptionEffectTag.NO_OP},
        help = "Deprecated. No-op.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean useTopLevelTargetsForSymlinks = new Flag.Boolean("useTopLevelTargetsForSymlinks");

    @Option(
        name = "vendor_dir",
        effectTags = {OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      Specifies the directory that should hold the external repositories in vendor mode, whether for the purpose of\n" +
            "      fetching them into it or using them while building. The path can be specified as either an absolute path or a\n" +
            "      path relative to the workspace directory.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config", "coverage",
            "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod", "print_action",
            "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Path vendorDir = new Flag.Path("vendorDir");

    @Option(
        name = "verbose",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      The queries will also display the reason why modules were resolved to their current version (if changed).\n" +
            "      Defaults to true only for the explain query.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"mod"}
    )
    public static final Flag.Boolean verbose = new Flag.Boolean("verbose");

    @Option(
        name = "verbose_explanations",
        defaultValue = "false",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Increases the verbosity of the explanations issued if --explain is enabled. Has no effect if --explain is not\n" +
            "      enabled.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean verboseExplanations = new Flag.Boolean("verboseExplanations");

    @Option(
        name = "verbose_failures",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "If a command fails, print out the full command line.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean verboseFailures = new Flag.Boolean("verboseFailures");

    @Option(
        name = "verbose_test_summary",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "If true, print additional information (timing, number of failed runs, etc) in the test summary.",
        valueHelp = "a boolean",
        commands = {"coverage", "cquery", "fetch", "test", "vendor"}
    )
    public static final Flag.Boolean verboseTestSummary = new Flag.Boolean("verboseTestSummary");

    @Option(
        name = "version_window_for_dirty_node_gc",
        defaultValue = "0",
        help = "\n" +
            "      Nodes that have been dirty for more than this many versions will be deleted from the graph upon the next\n" +
            "      update. Values must be non-negative long integers, or -1 indicating the maximum possible window.\n" +
            "    ",
        valueHelp = "a long integer",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown versionWindowForDirtyNodeGc = new Flag.Unknown("versionWindowForDirtyNodeGc");

    @Option(
        name = "visionos_cpus",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Comma-separated list of architectures for which to build Apple visionOS binaries.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown visionosCpus = new Flag.Unknown("visionosCpus");

    @Option(
        name = "watchfs",
        defaultValue = "false",
        metadataTags = {OptionMetadataTag.DEPRECATED},
        help = "\n" +
            "      If true, %{product} tries to use the operating system's file watch service for local changes instead of\n" +
            "      scanning every file for a change.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "startup", "analyze-profile", "aquery", "build", "canonicalize-flags", "clean", "config",
            "coverage", "cquery", "dump", "fetch", "help", "info", "license", "mobile-install", "mod",
            "print_action", "query", "run", "shutdown", "sync", "test", "vendor", "version",
        }
    )
    public static final Flag.Boolean watchfs = new Flag.Boolean("watchfs");

    @Option(
        name = "watchos_cpus",
        allowMultiple = true,
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "Comma-separated list of architectures for which to build Apple watchOS binaries.",
        valueHelp = "comma-separated list of options",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown watchosCpus = new Flag.Unknown("watchosCpus");

    @Option(
        name = "watchos_minimum_os",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Minimum compatible watchOS version for target simulators and devices. If unspecified, uses\n" +
            "      'watchos_sdk_version'.\n" +
            "    ",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown watchosMinimumOs = new Flag.Unknown("watchosMinimumOs");

    @Option(
        name = "watchos_sdk_version",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      Specifies the version of the watchOS SDK to use to build watchOS applications. If unspecified, uses the\n" +
            "      default watchOS SDK version from 'xcode_version'.\n" +
            "    ",
        valueHelp = "a dotted version (for example '2.3' or '3.3alpha2.4')",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown watchosSdkVersion = new Flag.Unknown("watchosSdkVersion");

    @Option(
        name = "windows_enable_symlinks",
        defaultValue = "false",
        effectTags = {OptionEffectTag.BAZEL_INTERNAL_CONFIGURATION},
        help = "\n" +
            "      If true, real symbolic links will be created on Windows instead of file copying. Requires Windows developer\n" +
            "      mode to be enabled and Windows 10 version 1703 or greater.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean windowsEnableSymlinks = new Flag.Boolean("windowsEnableSymlinks");

    @Option(
        name = "worker_extra_flag",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      Extra command-flags that will be passed to worker processes in addition to --persistent_worker, keyed by\n" +
            "      mnemonic (e.g. --worker_extra_flag=Javac=--debug.\n" +
            "    ",
        valueHelp = "a 'name=value' assignment",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown workerExtraFlag = new Flag.Unknown("workerExtraFlag");

    @Option(
        name = "worker_max_instances",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      How many instances of each kind of persistent worker may be launched if you use the 'worker' strategy. May be\n" +
            "      specified as [name=value] to give a different value per mnemonic. The limit is based on worker keys, which\n" +
            "      are differentiated based on mnemonic, but also on startup flags and environment, so there can in some cases\n" +
            "      be more workers per mnemonic than this flag specifies. Takes an integer, or a keyword (\"auto\", \"HOST_CPUS\",\n" +
            "      \"HOST_RAM\"), optionally followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\". 'auto' calculates\n" +
            "      a reasonable default based on machine capacity. \"=value\" sets a default for unspecified mnemonics.\n" +
            "    ",
        valueHelp = "\n" +
            "      [name=]value, where value is an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally\n" +
            "      followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown workerMaxInstances = new Flag.Unknown("workerMaxInstances");

    @Option(
        name = "worker_max_multiplex_instances",
        oldName = "experimental_worker_max_multiplex_instances",
        allowMultiple = true,
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "\n" +
            "      How many WorkRequests a multiplex worker process may receive in parallel if you use the 'worker' strategy\n" +
            "      with --worker_multiplex. May be specified as [name=value] to give a different value per mnemonic. The limit\n" +
            "      is based on worker keys, which are differentiated based on mnemonic, but also on startup flags and\n" +
            "      environment, so there can in some cases be more workers per mnemonic than this flag specifies. Takes an\n" +
            "      integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally followed by an operation ([-|*]<float>)\n" +
            "      eg. \"auto\", \"HOST_CPUS*.5\". 'auto' calculates a reasonable default based on machine capacity. \"=value\" sets a\n" +
            "      default for unspecified mnemonics.\n" +
            "    ",
        valueHelp = "\n" +
            "      [name=]value, where value is an integer, or a keyword (\"auto\", \"HOST_CPUS\", \"HOST_RAM\"), optionally\n" +
            "      followed by an operation ([-|*]<float>) eg. \"auto\", \"HOST_CPUS*.5\"\n" +
            "    ",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Unknown workerMaxMultiplexInstances = new Flag.Unknown("workerMaxMultiplexInstances");

    @Option(
        name = "worker_multiplex",
        oldName = "experimental_worker_multiplex",
        defaultValue = "true",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "If enabled, workers will use multiplexing if they support it. ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean workerMultiplex = new Flag.Boolean("workerMultiplex");

    @Option(
        name = "worker_quit_after_build",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
        help = "If enabled, all workers quit after a build is done.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean workerQuitAfterBuild = new Flag.Boolean("workerQuitAfterBuild");

    @Option(
        name = "worker_sandboxing",
        defaultValue = "false",
        effectTags = {OptionEffectTag.EXECUTION},
        help = "\n" +
            "      If enabled, singleplex workers will run in a sandboxed environment. Singleplex workers are always sandboxed\n" +
            "      when running under the dynamic execution strategy, irrespective of this flag.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean workerSandboxing = new Flag.Boolean("workerSandboxing");

    @Option(
        name = "worker_verbose",
        defaultValue = "false",
        help = "If enabled, prints verbose messages when workers are started, shutdown, ...",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean workerVerbose = new Flag.Boolean("workerVerbose");

    @Option(
        name = "workspace_directory",
        defaultValue = "",
        effectTags = {OptionEffectTag.CHANGES_INPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        metadataTags = {OptionMetadataTag.HIDDEN},
        help = "\n" +
            "      The root of the workspace, that is, the directory that Bazel uses as the root of the build. This flag is only\n" +
            "      to be set by the bazel client.\n" +
            "    ",
        valueHelp = "a path",
        commands = {"startup"}
    )
    public static final Flag.Path workspaceDirectory = new Flag.Path("workspaceDirectory");

    @Option(
        name = "workspace_rc",
        defaultValue = "true",
        effectTags = {OptionEffectTag.CHANGES_INPUTS},
        help = "Whether or not to look for the workspace bazelrc file at ${'$'}workspace/.bazelrc",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean workspaceRc = new Flag.Boolean("workspaceRc");

    @Option(
        name = "workspace_status_command",
        defaultValue = "",
        help = "\n" +
            "      A command invoked at the beginning of the build to provide status information about the workspace in the form\n" +
            "      of key/value pairs.  See the User's Manual for the full specification. Also see\n" +
            "      tools/buildstamp/get_workspace_status for an example.\n" +
            "    ",
        valueHelp = "a path",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Path workspaceStatusCommand = new Flag.Path("workspaceStatusCommand");

    @Option(
        name = "write_command_log",
        defaultValue = "true",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS, OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "Whether or not to write the command.log file",
        valueHelp = "a boolean",
        commands = {"startup"}
    )
    public static final Flag.Boolean writeCommandLog = new Flag.Boolean("writeCommandLog");

    @Option(
        name = "xbinary_fdo",
        effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
        help = "\n" +
            "      Use XbinaryFDO profile information to optimize compilation. Specify the name of default cross binary profile.\n" +
            "      When the option is used together with --fdo_instrument/--fdo_optimize/--fdo_profile, those options will\n" +
            "      always prevail as if xbinary_fdo is never specified.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label xbinaryFdo = new Flag.Label("xbinaryFdo");

    @Option(
        name = "xcode_version",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE},
        help = "\n" +
            "      If specified, uses Xcode of the given version for relevant build actions. If unspecified, uses the executor\n" +
            "      default version of Xcode.\n" +
            "    ",
        valueHelp = "a string",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Str xcodeVersion = new Flag.Str("xcodeVersion");

    @Option(
        name = "xcode_version_config",
        defaultValue = "@bazel_tools//tools/cpp:host_xcodes",
        effectTags = {OptionEffectTag.LOSES_INCREMENTAL_STATE, OptionEffectTag.LOADING_AND_ANALYSIS},
        help = "\n" +
            "      The label of the xcode_config rule to be used for selecting the Xcode version in the build configuration.\n" +
            "    ",
        valueHelp = "a build target label",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Label xcodeVersionConfig = new Flag.Label("xcodeVersionConfig");

    @Option(
        name = "xml:default_values",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      If true, rule attributes whose value is not explicitly specified in the BUILD file are printed; otherwise\n" +
            "      they are omitted.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"query"}
    )
    public static final Flag.Boolean xml_defaultValues = new Flag.Boolean("xml_defaultValues");

    @Option(
        name = "xml:line_numbers",
        defaultValue = "true",
        effectTags = {OptionEffectTag.TERMINAL_OUTPUT},
        help = "\n" +
            "      If true, XML output contains line numbers. Disabling this option may make diffs easier to read.  This option\n" +
            "      is only applicable to --output=xml.\n" +
            "    ",
        valueHelp = "a boolean",
        commands = {"query"}
    )
    public static final Flag.Boolean xml_lineNumbers = new Flag.Boolean("xml_lineNumbers");

    @Option(
        name = "zip_undeclared_test_outputs",
        defaultValue = "false",
        effectTags = {OptionEffectTag.TEST_RUNNER},
        help = "If true, undeclared test outputs will be archived in a zip file.",
        valueHelp = "a boolean",
        commands = {
            "aquery", "build", "canonicalize-flags", "clean", "config", "coverage", "cquery", "fetch", "info",
            "mobile-install", "print_action", "run", "test", "vendor",
        }
    )
    public static final Flag.Boolean zipUndeclaredTestOutputs = new Flag.Boolean("zipUndeclaredTestOutputs");
}
