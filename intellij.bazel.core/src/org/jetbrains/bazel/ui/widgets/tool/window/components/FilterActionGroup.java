package org.jetbrains.bazel.ui.widgets.tool.window.components;

import consulo.application.dumb.DumbAware;
import consulo.bazel.core.localize.BazelPluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.action.Toggleable;
import org.jetbrains.bazel.commons.RuleType;
import org.jetbrains.bsp.protocol.BuildTarget;

import java.util.function.Predicate;

public class FilterActionGroup extends DefaultActionGroup implements
    Toggleable,
    DumbAware,
    // consulo: update(e) comes from AnActionWithSyncUpdate (always runs in the background, upstream ActionUpdateThread.BGT)
    AnActionWithSyncUpdate {
    // consulo: upstream top-level enum of FilterActionGroup.kt
    public enum TargetFilter {
        OFF(it -> true),
        CAN_RUN(it -> it.getKind().ruleType() == RuleType.BINARY),
        CAN_TEST(it -> it.getKind().ruleType() == RuleType.TEST);

        public final Predicate<BuildTarget> predicate;

        TargetFilter(Predicate<BuildTarget> predicate) {
            this.predicate = predicate;
        }
    }

    private final BazelTargetsPanelModel myModel;

    public FilterActionGroup(BazelTargetsPanelModel model) {
        super(
            BazelPluginLocalize.widgetFilterActionGroup(),
            LocalizeValue.empty(),
            PlatformIconGroup.generalFilter()
        );
        myModel = model;
        setPopup(true);
        addFilterChangeAction(
            TargetFilter.OFF,
            BazelPluginLocalize.widgetFilterTurnOff()
        );
        addFilterChangeAction(
            TargetFilter.CAN_RUN,
            BazelPluginLocalize.widgetFilterCanRun()
        );
        addFilterChangeAction(
            TargetFilter.CAN_TEST,
            BazelPluginLocalize.widgetFilterCanTest()
        );
    }

    private void addFilterChangeAction(TargetFilter filterType, LocalizeValue text) {
        add(new FilterChangeAction(myModel, filterType, text));
    }

    @Override
    public void update(AnActionEvent e) {
        // consulo: DefaultActionGroup has no update(e) to call (upstream super.update(e))
        Toggleable.setSelected(e.getPresentation(), myModel.getTargetFilter() != TargetFilter.OFF);
    }
}

final class FilterChangeAction extends ToggleAction implements DumbAware {
    private final BazelTargetsPanelModel myModel;
    private final FilterActionGroup.TargetFilter myFilterType;

    FilterChangeAction(
        BazelTargetsPanelModel model,
        FilterActionGroup.TargetFilter filterType,
        LocalizeValue text
    ) {
        super(text);
        myModel = model;
        myFilterType = filterType;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myModel.getTargetFilter() == myFilterType;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        if (state) {
            myModel.setTargetFilter(myFilterType);
        }
    }

    // consulo: ToggleAction always updates in the background (upstream ActionUpdateThread.BGT)
}
