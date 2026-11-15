package org.jetbrains.bazel.java.starters;

import org.jetbrains.bazel.commons.constants.Constants;

import static org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.JAVA_LANG_VERSION;
import static org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.JAVA_RUNTIME_VERSION;
import static org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.JUNIT_VERSION;
import static org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.RULES_JAVA_VERSION;
import static org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.RULES_JVM_EXTERNAL_VERSION;

// TODO consulo: the IntelliJ new project wizard (BuildSystemJavaNewProjectWizard, AssetsNewProjectWizardStep, GeneratorFile,
//   WizardContext, newProjectWizard.java.buildSystem) is missing in Consulo; the class is not registered, only the generated
//   file contents are kept
public class BazelJavaNewProjectWizard /* : BuildSystemJavaNewProjectWizard */ {
    //override val name: @NlsContexts.Label String
    //  get() = BazelPluginConstants.BAZEL_DISPLAY_NAME
    //
    //override fun isEnabled(context: WizardContext): Boolean = super.isEnabled(context)
    //
    //override fun createStep(parent: Step): NewProjectWizardStep = AssetsStep(parent)

    private static class AssetsStep /* (parent: Step) : AssetsNewProjectWizardStep(parent) */ {
        // we could dynamically fetch versions and whatnot,
        // but it's probably easier and safer to just update and test everything together once in a while
        //val generatorAssets: List<GeneratorAsset> =
        //  listOf(
        //    GeneratorFile(".gitignore", gitIgnore()),
        //    GeneratorFile(".bazelversion", BAZEL_VERSION),
        //    GeneratorFile(".bazelrc", bazelRc()),
        //    GeneratorFile(Constants.MODULE_BAZEL_FILE_NAME, moduleBazel(context)),
        //    GeneratorFile("src/main/org/example/${Constants.defaultBuildFileName()}", buildBazelMain()),
        //    GeneratorFile("src/main/org/example/Main.java", mainJava()),
        //    GeneratorFile("src/test/org/example/${Constants.defaultBuildFileName()}", buildBazelTest()),
        //    GeneratorFile("src/test/org/example/MainTest.java", testJava()),
        //  )
        //
        //override fun setupAssets(project: Project) {
        //  if (context.isCreatingNewProject) {
        //    addAssets(generatorAssets)
        //  }
        //}

        // consulo: upstream moduleBazel(context: WizardContext) uses context.projectName
        private static String moduleBazel(String projectName) {
            return """
                module(
                    name = "%s",
                    version = "0.1.0",
                )

                bazel_dep(name = "rules_java", version = "%s")
                bazel_dep(name = "rules_jvm_external", version = "%s")

                # Set up Maven repositories for dependencies
                maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")
                maven.install(
                    name = "maven",
                    artifacts = [
                        "junit:junit:%s",
                    ],
                    repositories = [
                        "https://repo1.maven.org/maven2",
                    ],
                )
                use_repo(maven, "maven")""".formatted(projectName, RULES_JAVA_VERSION, RULES_JVM_EXTERNAL_VERSION, JUNIT_VERSION);
        }

        private String gitIgnore() {
            return """
                %s/
                .idea/
                bazel-bin
                bazel-genfiles
                bazel-out
                bazel-testlogs""".formatted(Constants.DOT_BAZELBSP_DIR_NAME);
        }

        private String bazelRc() {
            return """
                build --java_language_version=%1$s
                build --java_runtime_version=%2$s
                build --tool_java_language_version=%1$s
                build --tool_java_runtime_version=%2$s""".formatted(JAVA_LANG_VERSION, JAVA_RUNTIME_VERSION);
        }


        private String buildBazelMain() {
            return """
                load("@rules_java//java:defs.bzl", "java_binary")

                package(default_visibility = ["//visibility:public"])

                java_binary(
                    name = "Main",
                    main_class = "org.example.Main",
                    srcs = glob(["**/*.java"]),
                )""";
        }

        private String mainJava() {
            return """
                package org.example;

                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello World!");
                    }

                    public static int constant4() {
                        return 4;
                    }
                }""";
        }

        private String buildBazelTest() {
            return """
                # JUnit 4 test target
                java_test(
                    name = "tests",
                    srcs = glob(["**/*.java"]),
                    test_class = "org.example.MainTest",  # Optional: specify a test suite class
                    deps = [
                        "//src/main/org/example:Main",
                        "@maven//:junit_junit",  # JUnit 4 dependency from Maven
                    ],
                )""";
        }

        private String testJava() {
            return """
                package org.example;

                import org.junit.Test;

                import static org.junit.Assert.*;

                public class MainTest {
                    @Test
                    public void testConstant4() {
                        assertEquals("constant function is 4", 4, Main.constant4());
                    }
                }""";
        }
    }
}
