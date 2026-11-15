package org.jetbrains.bazel.debug.platform;

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.Value;
import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XNamedValue;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.frame.XValuePlace;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class StarlarkValue extends XNamedValue {
    private final String myType;
    private final String myValueAsString;
    private final @Nullable ComputeYourChildrenPack myComputeYourChildrenPack;

    private StarlarkValue(
        String name,
        String type,
        String valueAsString,
        @Nullable ComputeYourChildrenPack computeYourChildrenPack
    ) {
        super(name);
        myType = type;
        myValueAsString = valueAsString;
        myComputeYourChildrenPack = computeYourChildrenPack;
    }

    @Override
    public void computePresentation(XValueNode node, XValuePlace place) {
        boolean hasChildren = myComputeYourChildrenPack != null;
        Image icon = getValueIcon();
        String displayValue = getDisplayValue();
        node.setPresentation(icon, myType, displayValue, hasChildren);
    }

    private Image getValueIcon() {
        return switch (myType) {
            // types mentioned in Starlark documentation are given explicit branches here
            case "NoneType" -> ExecutionDebugIconGroup.nodePrimitive();
            case "bool" -> ExecutionDebugIconGroup.nodePrimitive();
            case "int" -> ExecutionDebugIconGroup.nodePrimitive();
            case "string" -> ExecutionDebugIconGroup.nodePrimitive();
            case "list" -> ExecutionDebugIconGroup.nodeArray();
            case "tuple" -> ExecutionDebugIconGroup.nodeArray();
            case "dict" -> ExecutionDebugIconGroup.nodeValue();
            case "function" -> PlatformIconGroup.nodesFunction();
            default -> ExecutionDebugIconGroup.nodeValue();
        };
    }

    private String getDisplayValue() {
        return switch (myType) {
            case "string" -> "\"" + myValueAsString + "\""; // display strings as "abc" instead of just abc
            default -> myValueAsString;
        };
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public void computeChildren(XCompositeNode node) {
        ComputeYourChildrenPack pack = myComputeYourChildrenPack;
        if (pack != null) {
            pack.valueComputer()
                .computeValueChildren(pack.threadId(), pack.valueId())
                .thenAccept(it -> addComputedChildren(node, it, pack));
        }
        else {
            node.addChildren(new XValueChildrenList(), true);
        }
    }

    private void addComputedChildren(XCompositeNode receiver, List<Value> children, ComputeYourChildrenPack computeYourChildrenPack) {
        XValueChildrenList xChildren = new XValueChildrenList();
        children
            .stream()
            .map(it -> toStarlarkValue(it, computeYourChildrenPack))
            .forEach(xChildren::add);
        receiver.addChildren(xChildren, true);
    }

    private static StarlarkValue toStarlarkValue(Value receiver, ComputeYourChildrenPack parentPack) {
        return fromProto(receiver, parentPack.threadId(), parentPack.valueComputer());
    }

    public static StarlarkValue fromProto(
        Value value,
        long threadId,
        StarlarkValueComputer valueComputer
    ) {
        ComputeYourChildrenPack pack;
        if (value.getHasChildren()) {
            pack = new ComputeYourChildrenPack(threadId, value.getId(), valueComputer);
        }
        else {
            pack = null;
        }
        return new StarlarkValue(value.getLabel(), value.getType(), value.getDescription(), pack);
    }

    // everything a value needs to find its children
    private record ComputeYourChildrenPack(
        long threadId,
        long valueId,
        StarlarkValueComputer valueComputer
    ) {
    }
}
