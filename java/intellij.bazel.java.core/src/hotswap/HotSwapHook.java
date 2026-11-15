package org.jetbrains.bazel.hotswap;

import com.intellij.java.debugger.impl.DebuggerSession;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface HotSwapHook {
    CompletableFuture<?> onHotSwap(List<DebuggerSession> sessions);

    // consulo: upstream EP_NAME is replaced by Application.get().getExtensionList(HotSwapHook.class)
}
