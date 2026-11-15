package org.jetbrains.bazel.workspace.importer;

import com.intellij.java.language.projectRoots.JavaSdkType;
import com.intellij.java.language.projectRoots.JavaSdkVersion;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.java.language.bundle.JavaSdkTypeUtil;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// TODO: check if I can remove this completely, probably yes
// consulo: Consulo has no JavaSdk / ProjectJdkTable, JDKs are Java SDKs (JavaSdkType) of the SdkTable;
// edtWriteAction { } runs as a write action on the UI thread
public final class SdkUtils {
    private SdkUtils() {
    }

    public static CompletableFuture<Void> addJdkIfNeeded(String projectName, Path javaHome) {
        String jdkName = JvmTargetEntitiesBuilder.projectNameToJdkName(projectName, javaHome);
        // Normalize the JDK path, because some code in the platform compares paths using `startsWith`, e.g.
        // https://github.com/JetBrains/intellij-community/blob/b41a4084da5521effedd334e28896fd9d07410da/java/codeserver/core/src/com/intellij/java/codeserver/core/JpmsModuleAccessInfo.kt#L216
        String path = javaHome.normalize().toString();
        // consulo: upstream ExternalSystemJdkProvider.createJdk(jdkName, path), the SDK is created like Consulo adds a JDK
        JavaSdkType javaSdkType = javaSdkInstance();
        Sdk jdk = SdkTable.getInstance().createSdk(Path.of(path), jdkName, javaSdkType);
        javaSdkType.setupSdkPaths(jdk);
        return addSdkIfNeeded(jdk);
    }

    public static CompletableFuture<Void> addSdkIfNeeded(Sdk sdk) {
        SdkTable sdkTable = SdkTable.getInstance();
        Sdk existingSdk = findJdk(sdkTable, sdk);
        if (existingSdk == null || !Objects.equals(existingSdk.getHomePath(), sdk.getHomePath())) {
            return edtWriteAction(() -> {
                if (existingSdk != null) {
                    sdkTable.removeSdk(existingSdk);
                }
                sdkTable.addSdk(sdk);
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    // consulo: ProjectJdkTable.findJdk(name, type)
    private static @Nullable Sdk findJdk(SdkTable sdkTable, Sdk sdk) {
        Sdk existing = sdkTable.findSdk(sdk.getName());
        return existing != null && existing.getSdkType().equals(sdk.getSdkType()) ? existing : null;
    }

    public static CompletableFuture<Void> cleanUpInvalidJdks(Project project) {
        SdkTable sdkTable = SdkTable.getInstance();
        String jdkPrefix = JvmTargetEntitiesBuilder.projectNameToBaseJdkName(org.jetbrains.bazel.config.project.getBazelProjectName(project));
        List<Sdk> invalidJdks = getAllAvailableJdks().stream()
            .filter(it -> it.getName().startsWith(jdkPrefix) && !isValidJdk(it))
            .toList();
        CompletableFuture<Void> result = edtWriteAction(() -> {
            for (Sdk it : invalidJdks) {
                sdkTable.removeSdk(it);
            }
        });

        // We don't need the project SDK anyway. If the user sets it manually, it can cause red code, so remove it upon sync.
        // consulo: Consulo has no project SDK (ProjectRootManager.projectSdk), the SDK is set per module extension
        return result;
    }

    private static boolean isValidJdk(Sdk sdk) {
        String homePath = sdk.getHomePath();
        if (homePath == null) {
            return false;
        }
        return javaSdkInstance().isValidSdkHome(homePath) &&
            sdk.getRootProvider().getUrls(BinariesOrderRootType.ID).length != 0;
    }

    // consulo: Consulo has no project SDK, so this is the most recent JDK
    public static @Nullable Sdk getProjectJdkOrMostRecentJdk(Project project) {
        return getMostRecentJdk();
    }

    private static @Nullable Sdk getMostRecentJdk() {
        // consulo: JavaSdk.comparator compares the JDK versions
        return getAllAvailableJdks().stream()
            .max(Comparator.comparing((Sdk it) -> JavaSdkTypeUtil.getVersion(it), Comparator.nullsFirst(Comparator.<JavaSdkVersion>naturalOrder())))
            .orElse(null);
    }

    private static List<Sdk> getAllAvailableJdks() {
        return JavaSdkTypeUtil.getAllJavaSdks();
    }

    private static JavaSdkType javaSdkInstance() {
        return JavaSdkType.getDefaultJavaSdkType();
    }

    // consulo: upstream edtWriteAction { }
    @SuppressWarnings("deprecation")
    private static CompletableFuture<Void> edtWriteAction(Runnable action) {
        return Application.get().getLastUIAccess().giveAsync(() -> WriteAction.run(action::run)).<Void>thenApply(ignored -> null);
    }
}
