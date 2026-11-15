package org.jetbrains.bazel.debug.platform;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Frame;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Scope;
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Value;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public class StarlarkStackFrame extends XStackFrame {
    private final Frame myFrame;
    private final long myThreadId;
    private final StarlarkValueComputer myValueComputer;

    private boolean myIsTopFrame = false;

    private final XDebuggerEvaluator myEvaluator;

    public StarlarkStackFrame(
        Frame frame,
        long threadId,
        StarlarkValueComputer valueComputer,
        StarlarkDebuggerEvaluator.Provider evaluatorProvider
    ) {
        myFrame = frame;
        myThreadId = threadId;
        myValueComputer = valueComputer;
        myEvaluator = evaluatorProvider.obtain(threadId);
    }

    public long getThreadId() {
        return myThreadId;
    }

    public boolean isTopFrame() {
        return myIsTopFrame;
    }

    public void setTopFrame(boolean isTopFrame) {
        myIsTopFrame = isTopFrame;
    }

    @Override
    public void computeChildren(XCompositeNode node) {
        XValueChildrenList children = new XValueChildrenList();
        addToplevelValues(children, myFrame);
        node.addChildren(children, true);
    }

    private void addToplevelValues(XValueChildrenList receiver, Frame frame) {
        for (Scope scope : frame.getScopeList()) {
            for (Value it : scope.getBindingList()) {
                receiver.add(StarlarkValue.fromProto(it, myThreadId, myValueComputer));
            }
        }
    }

    @Override
    public @Nullable XSourcePosition getSourcePosition() {
        String path = myFrame.getLocation().getPath();
        VirtualFile vFile = VirtualFileManager.getInstance().findFileByNioPath(Path.of(path));
        int line = myFrame.getLocation().getLineNumber() - 1; // convert from 1-indexed to 0-indexed
        return XDebuggerUtil.getInstance().createPosition(vFile, line);
    }

    @Override
    public XDebuggerEvaluator getEvaluator() {
        return myIsTopFrame ? myEvaluator : NOT_TOP_FRAME_ERROR;
    }

    private static final XDebuggerEvaluator NOT_TOP_FRAME_ERROR =
        new StarlarkDebuggerEvaluator.ErrorEvaluator(BazelPluginLocalize.starlarkDebugNotTopFrame());
}
