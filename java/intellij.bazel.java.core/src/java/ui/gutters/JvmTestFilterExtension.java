package org.jetbrains.bazel.java.ui.gutters;

import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.project.Project;
import org.jetbrains.bsp.protocol.BuildTargetInfo;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface JvmTestFilterExtension {
    boolean isApplicable(Project project, BuildTargetInfo target);

    String getTestFilter(String className, @Nullable PsiMethod method);

    static ExtensionPoint<JvmTestFilterExtension> getEp() {
        return Application.get().getExtensionPoint(JvmTestFilterExtension.class);
    }

    static JvmTestFilterExtension getInstance(Project project, BuildTargetInfo target) {
        return Objects.requireNonNull(getEp().findFirstSafe(it -> it.isApplicable(project, target)));
    }
}

@ExtensionImpl(order = "last")
class DefaultJvmTestFilterExtension implements JvmTestFilterExtension {
    public DefaultJvmTestFilterExtension() {
    }

    @Override
    public boolean isApplicable(Project project, BuildTargetInfo target) {
        return true;
    }

    @Override
    public String getTestFilter(String className, @Nullable PsiMethod method) {
        return BazelJavaRunConfigurationProducer.getTestFilter(className, method != null ? method.getName() : null);
    }
}
