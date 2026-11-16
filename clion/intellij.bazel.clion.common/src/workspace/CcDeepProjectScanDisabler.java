package org.jetbrains.bazel.clion.workspace;

import consulo.application.util.registry.Registry;
import consulo.project.Project;

// TODO consulo: CIDR extension point cidr.lang.projectModel.deepProjectScanDisabler (OCDeepProjectScanDisabler) does not
//  exist in consulo-cpp, the class is not registered
public class CcDeepProjectScanDisabler {
    public CcDeepProjectScanDisabler() {
    }

    // consulo: the key is not one of consulo.bazel.commons.BazelRegistryKeys (registered by upstream
    // intellij.bazel.clion.common.xml with defaultValue="false"), the default is passed here
    @SuppressWarnings("deprecation")
    public boolean isDeepProjectScanDisabled(Project project) {
        return org.jetbrains.bazel.config.project.isBazelProject(project) && !Registry.is("bazel.clion.deep.project.scan", false);
    }
}
