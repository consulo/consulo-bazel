package org.jetbrains.bazel.ui.widgets.tool.window.utils;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * consulo: upstream implements {@code ExtendableTextComponent.Extension}, which is not in the Consulo API (desktop-awt-ui-impl only).
 * The class declares the same methods (with the interface defaults of {@code getActionOnClick} and {@code isSelected}),
 * {@code SearchBarPanel}'s text field shows them.
 */
public abstract class TextComponentExtension {
    private final @Nullable String myTooltip;
    private final boolean myBeforeText;

    private TextComponentExtension(@Nullable String tooltip, boolean beforeText) {
        myTooltip = tooltip;
        myBeforeText = beforeText;
    }

    public boolean isIconBeforeText() {
        return myBeforeText;
    }

    public @Nullable String getTooltip() {
        return myTooltip;
    }

    public abstract @Nullable Image getIcon(boolean hovered);

    // consulo: ExtendableTextComponent.Extension default
    public @Nullable Runnable getActionOnClick() {
        return null;
    }

    // consulo: ExtendableTextComponent.Extension default
    public boolean isSelected() {
        return false;
    }

    /**
     * Non-clickable extension, showing one of two icons based on the given predicate.
     * <p>
     * trueIcon The icon to display when the predicate evaluates to `true`
     * falseIcon The icon to display when the predicate evaluates to `false`
     * predicate The predicate function to determine the icon to display
     * tooltip The tooltip text to display when hovering over the indicator
     * beforeText `true` if the extension should appear before the text
     */
    public static class Indicator extends TextComponentExtension {
        private final Image myTrueIcon;
        private final Image myFalseIcon;
        private final BooleanSupplier myPredicate;

        // consulo: upstream default arguments (tooltip = null, beforeText = false)
        public Indicator(Image trueIcon, Image falseIcon, BooleanSupplier predicate) {
            this(trueIcon, falseIcon, predicate, null, false);
        }

        public Indicator(
            Image trueIcon,
            Image falseIcon,
            BooleanSupplier predicate,
            @Nullable String tooltip,
            boolean beforeText
        ) {
            super(tooltip, beforeText);
            myTrueIcon = trueIcon;
            myFalseIcon = falseIcon;
            myPredicate = predicate;
        }

        @Override
        public Image getIcon(boolean hovered) {
            return myPredicate.getAsBoolean() ? myTrueIcon : myFalseIcon;
        }

        @Override
        public @Nullable Runnable getActionOnClick() {
            return null;
        }

        @Override
        public boolean isSelected() {
            return false;
        }
    }

    /**
     * Clickable extension toggling a boolean variable.
     * <p>
     * icon The icon to display in the switch component
     * valueGetter A function that retrieves the current value of the boolean
     * valueSetter A function that sets the value of the boolean
     * parentComponent The parent component of the switch (will be repainted after every value change)
     * tooltip The tooltip text to display when hovering over the switch
     * beforeText `true` if the extension should appear before the text
     */
    public static class Switch extends TextComponentExtension {
        private final Image myIcon;
        private final BooleanSupplier myValueGetter;
        private final Consumer<Boolean> myValueSetter;
        private final JComponent myParentComponent;

        // consulo: upstream default argument (beforeText = false)
        public Switch(
            Image icon,
            BooleanSupplier valueGetter,
            Consumer<Boolean> valueSetter,
            JComponent parentComponent,
            @Nullable String tooltip
        ) {
            this(icon, valueGetter, valueSetter, parentComponent, tooltip, false);
        }

        public Switch(
            Image icon,
            BooleanSupplier valueGetter,
            Consumer<Boolean> valueSetter,
            JComponent parentComponent,
            @Nullable String tooltip,
            boolean beforeText
        ) {
            super(tooltip, beforeText);
            myIcon = icon;
            myValueGetter = valueGetter;
            myValueSetter = valueSetter;
            myParentComponent = parentComponent;
        }

        @Override
        public Image getIcon(boolean hovered) {
            return myIcon;
        }

        @Override
        public Runnable getActionOnClick() {
            return () -> {
                myValueSetter.accept(!myValueGetter.getAsBoolean());
                myParentComponent.repaint(); // button selection will not update otherwise
            };
        }

        @Override
        public boolean isSelected() {
            return myValueGetter.getAsBoolean();
        }
    }

    /**
     * Clickable extension clearing a text component.
     * <p>
     * isEmpty A function that checks if the text component is empty
     * clearAction A function to be executed when the clear button is clicked
     * tooltip The tooltip text to display when hovering over the clear button
     */
    public static class Clear extends TextComponentExtension {
        private final BooleanSupplier myIsEmpty;
        private final Runnable myClearAction;

        public Clear(
            BooleanSupplier isEmpty,
            Runnable clearAction,
            String tooltip
        ) {
            super(tooltip, false);
            myIsEmpty = isEmpty;
            myClearAction = clearAction;
        }

        @Override
        public @Nullable Image getIcon(boolean hovered) {
            if (myIsEmpty.getAsBoolean()) {
                return null;
            }
            else if (hovered) {
                // consulo: AllIcons.Actions.CloseHovered does not exist, Close is used for both states
                return PlatformIconGroup.actionsClose();
            }
            else {
                return PlatformIconGroup.actionsClose();
            }
        }

        @Override
        public Runnable getActionOnClick() {
            return myClearAction;
        }
    }
}
