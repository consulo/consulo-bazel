package org.jetbrains.bazel.java.analysis;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;

// consulo: upstream top-level declarations of InterfaceJars.kt
public final class InterfaceJars {
    private InterfaceJars() {
    }

    private static final String[] INTERFACE_JAR_SUFFIXES = new String[]{"-ijar.jar", "-hjar.jar", ".abi.jar"};

    /**
     * Returns true if the given file is a Bazel interface jar. Bazel interface jar must have a specific suffix and be located under `bazel-out`.
     */
    public static boolean isBazelInterfaceJar(VirtualFile receiver) {
        // consulo: JarFileSystem.getVirtualFileForJar -> ArchiveVfsUtil.getVirtualFileForArchive
        VirtualFile jar = ArchiveVfsUtil.getVirtualFileForArchive(receiver);
        if (jar == null) {
            return false;
        }
        String name = jar.getName();
        boolean hasSuffix = false;
        for (String suffix : INTERFACE_JAR_SUFFIXES) {
            if (name.endsWith(suffix)) {
                hasSuffix = true;
                break;
            }
        }
        if (!hasSuffix) {
            return false;
        }
        for (VirtualFile it = jar.getParent(); it != null; it = it.getParent()) {
            if (it.getName().equals("bazel-out")) {
                return true;
            }
        }
        return false;
    }
}
