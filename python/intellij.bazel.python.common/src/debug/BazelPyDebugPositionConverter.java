package org.jetbrains.bazel.python.debug;

import com.jetbrains.python.debugger.PyPositionConverter;
import com.jetbrains.python.debugger.PySignature;
import com.jetbrains.python.debugger.PySourcePosition;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.label.Label;
import org.jspecify.annotations.Nullable;

public class BazelPyDebugPositionConverter implements PyPositionConverter {
    private final Project myProject;
    private final Label myTarget;

    public BazelPyDebugPositionConverter(Project project, Label target) {
        myProject = project;
        myTarget = target;
    }

    @Deprecated
    @Override
    public PySourcePosition create(String filePath, int line) {
        return new OurPySourcePosition(filePath, line);
    }

    @Override
    public PySourcePosition convertToPython(XSourcePosition position) {
        String file = position.getFile().getPath();
        int line = position.getLine() + 1;
        return new OurPySourcePosition(file, line);
    }

    // consulo: the Consulo PyPositionConverter has no convertFromPython(position, frameName) overload
    public @Nullable XSourcePosition convertFromPython(PySourcePosition position, @Nullable String frameName) {
        return convertFromPython(position);
    }

    @Override
    public @Nullable XSourcePosition convertFromPython(PySourcePosition position) {
        String file = PythonDebugUtils.findRealSourceFile(myProject, myTarget, position.getFile());
        if (file == null) {
            return null;
        }
        int line = position.getLine() - 1;
        return getXSourcePosition(file, line);
    }

    @Override
    public PySignature convertSignature(PySignature signature) {
        return signature;
    }

    // consulo: upstream private top-level function of BazelPyDebugPositionConverter.kt
    private static @Nullable XSourcePosition getXSourcePosition(String file, int line) {
        // consulo: StandardFileSystems.local() -> LocalFileSystem.getInstance()
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(file);
        return XDebuggerUtil.getInstance().createPosition(virtualFile, line);
    }
}

// consulo: upstream private class of BazelPyDebugPositionConverter.kt
class OurPySourcePosition extends PySourcePosition {
    OurPySourcePosition(String file, int line) {
        super(file, line);
    }
}
