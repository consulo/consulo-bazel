package org.jetbrains.bazel.run;

import consulo.execution.action.RunConfigurationProducer;
import consulo.project.Project;

import java.util.Set;

/**
 * Disables {@link RunConfigurationProducer}s that are already supported by our plugin via {@link org.jetbrains.bazel.run.config.BazelRunConfiguration}.
 * Those producers are attempting, e.g., to launch a test via calling `python` or `go` directly (skipping Bazel entirely).
 * While this may work for very simple projects without external deps/codegen, Bazel should be the only available option to avoid confusion.
 * We could instead disable all producers except several "known" ones, but that would disable valid cases, e.g., running `.sh`/`.bat` files.
 */
// TODO consulo: RunConfigurationProducerSuppressor (runConfigurationProducerSuppressor) is missing in Consulo,
//  the suppressor is not registered
class BazelRunConfigurationProducerSuppressor {
    public boolean shouldSuppress(RunConfigurationProducer<?> producer, Project project) {
        if (!org.jetbrains.bazel.config.project.isBazelProject(project)) {
            return false;
        }
        for (Class<?> clazz = producer.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            if (myProducerNames.contains(clazz.getName())) {
                return true;
            }
        }
        return false;
    }

    private final Set<String> myProducerNames = Set.of(
        "com.android.tools.idea.run.configuration.AndroidWearRunConfigurationProducer",
        // "com.intellij.execution.junit.JavaRunConfigurationProducerBase", <-- used by actual Bazel customers (BAZEL-3522)
        "com.intellij.spring.boot.run.SpringBootApplicationRunConfigurationProducer",
        "com.android.tools.idea.run.AndroidConfigurationProducer",
        "com.intellij.execution.testDiscovery.TestDiscoveryConfigurationProducer",
        "com.intellij.ktor.run.KtorRunConfigurationProducer",
        "com.intellij.execution.testframework.AbstractJavaTestConfigurationProducer",
        "com.android.tools.idea.testartifacts.instrumented.kmp.KotlinMultiplatformAndroidTestConfigurationProducer",
        "com.intellij.execution.application.AbstractApplicationConfigurationProducer",
        "com.intellij.micronaut.run.MnRunConfigurationProducer",
        "com.android.tools.idea.testartifacts.instrumented.AndroidTestConfigurationProducer",
        "org.jetbrains.plugins.cucumber.java.run.CucumberJavaRunConfigurationProducer",
        "com.intellij.quarkus.run.application.QsApplicationRunConfigurationProducer",
        "org.jetbrains.kotlin.idea.gradleCodeInsightCommon.native.KotlinNativeRunConfigurationProducer",
        "org.jetbrains.plugins.gradle.execution.GradleRunConfigurationProducer",
        "org.jetbrains.kotlin.idea.junit.KotlinJUnitRunConfigurationProducer",
        "org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer",
        "com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRunConfigurationProducer",
        "com.jetbrains.python.run.PythonRunConfigurationProducer",
        "com.jetbrains.python.testing.AbstractPythonTestConfigurationProducer",
        "com.jetbrains.python.testing.tox.PyToxConfigurationProducer",
        "com.goide.execution.GoRunConfigurationProducerBase",
        "com.jetbrains.cidr.execution.CidrContextRunConfigurationProducer",
        "com.jetbrains.cidr.cpp.execution.debugger.CMakeRunConfigurationProducer"
    );
}
