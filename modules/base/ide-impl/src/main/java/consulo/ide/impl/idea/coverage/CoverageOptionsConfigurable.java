package consulo.ide.impl.idea.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.execution.coverage.CoverageOptionsProvider;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.ValueGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author anna
 * @since 2010-12-16
 */
@ExtensionImpl
public class CoverageOptionsConfigurable extends SimpleConfigurable<CoverageOptionsConfigurable.Panel>
    implements ProjectConfigurable, SearchableConfigurable, Configurable.NoScroll {
    public static class Panel implements Supplier<Component> {
        private final Disposable myUiDisposable;

        private RadioButton myShowOptionsRB;
        private RadioButton myReplaceRB;
        private RadioButton myAddRB;
        private RadioButton myDoNotApplyRB;

        private CheckBox myActivateCoverageViewCB;

        private VerticalLayout myWholePanel;

        private List<Configurable> myChildren = new ArrayList<>();

        @RequiredUIAccess
        Panel(Project project, Disposable uiDisposable) {
            myUiDisposable = uiDisposable;

            myWholePanel = VerticalLayout.create();

            ValueGroup<Boolean> group = ValueGroup.createBool();
            VerticalLayout primaryGroup = VerticalLayout.create();

            myShowOptionsRB = RadioButton.create(ExecutionCoverageLocalize.settingsCoverageShowOptionsBeforeApplyingCoverageToTheEditor());
            primaryGroup.add(myShowOptionsRB);
            group.add(myShowOptionsRB);

            myDoNotApplyRB = RadioButton.create(ExecutionCoverageLocalize.coverageDoNotApplyCollectedCoverage());
            primaryGroup.add(myDoNotApplyRB);
            group.add(myDoNotApplyRB);

            myReplaceRB = RadioButton.create(ExecutionCoverageLocalize.settingsCoverageReplaceActiveSuitesWithTheNewOne());
            primaryGroup.add(myReplaceRB);
            group.add(myReplaceRB);

            myAddRB = RadioButton.create(ExecutionCoverageLocalize.settingsCoverageAddToTheActiveSuites());
            primaryGroup.add(myAddRB);
            group.add(myAddRB);

            myActivateCoverageViewCB = CheckBox.create(ExecutionCoverageLocalize.settingsCoverageActivateCoverageView());
            primaryGroup.add(myActivateCoverageViewCB);

            myWholePanel.add(LabeledLayout.create(ExecutionCoverageLocalize.settingsCoverageWhenNewCoverageIsGathered(), primaryGroup));

            for (CoverageOptions options : CoverageOptions.EP_NAME.getExtensionList(project)) {
                Configurable configurable = options.createConfigurable();
                if (configurable != null) {
                    myChildren.add(configurable);
                }
            }

            for (Configurable child : myChildren) {
                Component uiComponent = child.createUIComponent(myUiDisposable);
                assert uiComponent != null;
                myWholePanel.add(LabeledLayout.create(LocalizeValue.of(child.getDisplayName()), uiComponent));
            }
        }

        @Nonnull
        @Override
        public Component get() {
            return myWholePanel;
        }
    }

    private final CoverageOptionsProvider myManager;
    private Project myProject;

    @Inject
    public CoverageOptionsConfigurable(CoverageOptionsProvider manager, Project project) {
        myManager = manager;
        myProject = project;
    }

    @Nonnull
    @Override
    public String getId() {
        return "coverage";
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return ExecutionCoverageLocalize.configurableCoverageoptionsconfigurableDisplayName().get();
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    protected Panel createPanel(@Nonnull Disposable uiDisposable) {
        return new Panel(myProject, uiDisposable);
    }

    @Override
    @RequiredUIAccess
    protected void apply(@Nonnull Panel panel) throws ConfigurationException {
        myManager.setOptionsToReplace(getSelectedValue(panel));
        myManager.setActivateViewOnRun(panel.myActivateCoverageViewCB.getValueOrError());

        for (Configurable child : panel.myChildren) {
            child.apply();
        }
    }

    @Override
    @RequiredUIAccess
    protected void reset(@Nonnull Panel panel) {
        int addOrReplace = myManager.getOptionToReplace();
        switch (addOrReplace) {
            case 0 -> panel.myReplaceRB.setValue(true);
            case 1 -> panel.myAddRB.setValue(true);
            case 2 -> panel.myDoNotApplyRB.setValue(true);
            default -> panel.myShowOptionsRB.setValue(true);
        }

        panel.myActivateCoverageViewCB.setValue(myManager.activateViewOnRun());

        for (Configurable child : panel.myChildren) {
            child.reset();
        }
    }

    @Override
    @RequiredUIAccess
    protected boolean isModified(@Nonnull Panel panel) {
        if (myManager.getOptionToReplace() != getSelectedValue(panel)) {
            return true;
        }

        if (myManager.activateViewOnRun() != panel.myActivateCoverageViewCB.getValueOrError()) {
            return true;
        }

        for (Configurable child : panel.myChildren) {
            if (child.isModified()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    protected void disposeUIResources(@Nonnull Panel panel) {
        for (Configurable child : panel.myChildren) {
            child.disposeUIResources();
        }
        super.disposeUIResources(panel);
    }

    private int getSelectedValue(Panel panel) {
        if (panel.myReplaceRB.getValueOrError()) {
            return 0;
        }
        else if (panel.myAddRB.getValueOrError()) {
            return 1;
        }
        else if (panel.myDoNotApplyRB.getValueOrError()) {
            return 2;
        }
        return 3;
    }
}
