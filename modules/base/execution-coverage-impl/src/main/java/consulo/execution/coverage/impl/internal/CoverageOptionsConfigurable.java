package consulo.execution.coverage.impl.internal;

import consulo.ui.RadioGroup;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.execution.coverage.CoverageOptions;
import consulo.execution.coverage.CoverageOptionsProvider;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;
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
    private static final int REPLACE = 0;
    private static final int ADD = 1;
    private static final int DO_NOT_APPLY = 2;
    private static final int SHOW_OPTIONS = 3;

    public static class Panel implements Supplier<Component> {
        private final Disposable myUiDisposable;

        private RadioGroup<Integer> myOptionToReplace;

        private CheckBox myActivateCoverageViewCB;

        private VerticalLayout myWholePanel;

        private List<Configurable> myChildren = new ArrayList<>();

        @RequiredUIAccess
        Panel(Project project, Disposable uiDisposable) {
            myUiDisposable = uiDisposable;

            myWholePanel = VerticalLayout.create();

            myOptionToReplace = RadioGroup.create();
            VerticalLayout primaryGroup = VerticalLayout.create();

            primaryGroup.add(myOptionToReplace.newButton(
                ExecutionCoverageLocalize.settingsCoverageShowOptionsBeforeApplyingCoverageToTheEditor(),
                SHOW_OPTIONS
            ));
            primaryGroup.add(myOptionToReplace.newButton(ExecutionCoverageLocalize.coverageDoNotApplyCollectedCoverage(), DO_NOT_APPLY));
            primaryGroup.add(myOptionToReplace.newButton(ExecutionCoverageLocalize.settingsCoverageReplaceActiveSuitesWithTheNewOne(), REPLACE));
            primaryGroup.add(myOptionToReplace.newButton(ExecutionCoverageLocalize.settingsCoverageAddToTheActiveSuites(), ADD));

            myActivateCoverageViewCB = CheckBox.create(ExecutionCoverageLocalize.settingsCoverageActivateCoverageView());
            primaryGroup.add(myActivateCoverageViewCB);

            myWholePanel.add(LabeledLayout.create(ExecutionCoverageLocalize.settingsCoverageWhenNewCoverageIsGathered(), primaryGroup));

            project.getExtensionPoint(CoverageOptions.class).forEach(coverageOptions -> {
                Configurable configurable = coverageOptions.createConfigurable();
                if (configurable != null) {
                    myChildren.add(configurable);
                }
            });

            for (Configurable child : myChildren) {
                Component uiComponent = child.createUIComponent(myUiDisposable);
                assert uiComponent != null;
                myWholePanel.add(LabeledLayout.create(child.getDisplayName(), uiComponent));
            }
        }

        
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

    
    @Override
    public String getId() {
        return "coverage";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return ExecutionCoverageLocalize.configurableCoverageoptionsconfigurableDisplayName();
    }

    
    @Override
    @RequiredUIAccess
    protected Panel createPanel(Disposable uiDisposable) {
        return new Panel(myProject, uiDisposable);
    }

    @Override
    @RequiredUIAccess
    protected void apply(Panel panel) throws ConfigurationException {
        myManager.setOptionsToReplace(getSelectedValue(panel));
        myManager.setActivateViewOnRun(panel.myActivateCoverageViewCB.getValueOrError());

        for (Configurable child : panel.myChildren) {
            child.apply();
        }
    }

    @Override
    @RequiredUIAccess
    protected void reset(Panel panel) {
        int addOrReplace = myManager.getOptionToReplace();
        panel.myOptionToReplace.setValue(switch (addOrReplace) {
            case REPLACE, ADD, DO_NOT_APPLY -> addOrReplace;
            default -> SHOW_OPTIONS;
        });

        panel.myActivateCoverageViewCB.setValue(myManager.activateViewOnRun());

        for (Configurable child : panel.myChildren) {
            child.reset();
        }
    }

    @Override
    @RequiredUIAccess
    protected boolean isModified(Panel panel) {
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
    protected void disposeUIResources(Panel panel) {
        for (Configurable child : panel.myChildren) {
            child.disposeUIResources();
        }
        super.disposeUIResources(panel);
    }

    private int getSelectedValue(Panel panel) {
        return panel.myOptionToReplace.getValueOrError();
    }
}
