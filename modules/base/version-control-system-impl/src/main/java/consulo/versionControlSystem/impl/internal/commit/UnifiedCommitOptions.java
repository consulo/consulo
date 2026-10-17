/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.impl.internal.commit;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Separator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.style.ComponentColors;
import consulo.ui.ex.action.Presentation;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.checkin.CommitCheck;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitOptions {
    private static final Logger LOG = Logger.getInstance(UnifiedCommitOptions.class);

    private final VerticalLayout myLayout = VerticalLayout.create();
    private final List<RefreshableOnComponent> myOptions = new ArrayList<>();

    public Component getComponent() {
        return myLayout;
    }

    public boolean isEmpty() {
        return myOptions.isEmpty();
    }

    @RequiredUIAccess
    public void fill(UnifiedCommitWorkflow workflow, LocalizeValue actionName, boolean includeAfterCommit, Disposable parentDisposable) {
        myLayout.removeAll();
        myOptions.clear();

        CheckinProjectPanel projectPanel = workflow.getProjectPanel();
        if (projectPanel != null) {
            AbstractVcs[] vcses = ProjectLevelVcsManager.getInstance(workflow.getProject()).getAllActiveVcss();
            Arrays.sort(vcses, Comparator.comparing(vcs -> vcs.getDisplayName().get(), String.CASE_INSENSITIVE_ORDER));
            for (AbstractVcs vcs : vcses) {
                CheckinEnvironment environment = vcs.getCheckinEnvironment();
                if (environment == null) {
                    continue;
                }

                RefreshableOnComponent option;
                try {
                    option = environment.createAdditionalOptionsPanel(projectPanel, workflow.getAdditionalData());
                }
                catch (RuntimeException e) {
                    LOG.warn("Commit options of " + vcs.getId() + " cannot be created", e);
                    continue;
                }

                VerticalLayout section = VerticalLayout.create();
                if (add(section, option)) {
                    myLayout.add(LabeledLayout.create(vcs.getDisplayName(), section));
                }
            }
        }

        String borderTitleName = actionName.map(Presentation.NO_MNEMONIC).get();

        VerticalLayout checks = VerticalLayout.create();
        VerticalLayout advancedChecks = VerticalLayout.create();
        boolean hasChecks = false;
        boolean hasAdvancedChecks = false;
        for (CheckinHandler handler : workflow.getHandlers()) {
            if (isPostCommit(handler)) {
                hasAdvancedChecks |= add(advancedChecks, handler.getBeforeCheckinConfigurationPanel());
            }
            else {
                hasChecks |= add(checks, handler.getBeforeCheckinConfigurationPanel());
            }
        }
        if (hasChecks) {
            myLayout.add(LabeledLayout.create(VcsLocalize.commitOptionsChecks(borderTitleName), checks));
        }
        if (hasAdvancedChecks) {
            if (!workflow.isModalChecks()) {
                advancedChecks.add(Separator.horizontal());
                advancedChecks.add(createPostponeSlowChecksOption(workflow));
            }
            myLayout.add(LabeledLayout.create(VcsLocalize.commitOptionsAdvancedChecks(borderTitleName), advancedChecks));
        }

        if (includeAfterCommit) {
            VerticalLayout after = VerticalLayout.create();
            boolean hasAfter = false;
            for (CheckinHandler handler : workflow.getHandlers()) {
                hasAfter |= add(after, handler.getAfterCheckinConfigurationPanel(parentDisposable));
            }
            if (hasAfter) {
                myLayout.add(LabeledLayout.create(VcsLocalize.borderStandardAfterCheckinOptionsGroup(borderTitleName), after));
            }
        }
    }

    @RequiredUIAccess
    private Component createPostponeSlowChecksOption(UnifiedCommitWorkflow workflow) {
        VcsConfiguration configuration = VcsConfiguration.getInstance(workflow.getProject());

        CheckBox checkBox = CheckBox.create(VcsLocalize.settingsCommitPostponeSlowChecks());
        checkBox.setValue(configuration.NON_MODAL_COMMIT_POSTPONE_SLOW_CHECKS);
        checkBox.addValueListener(event -> configuration.NON_MODAL_COMMIT_POSTPONE_SLOW_CHECKS = event.getValue());

        Label description = Label.create(VcsLocalize.settingsCommitPostponeSlowChecksDescription());
        description.setForegroundColor(ComponentColors.DISABLED_TEXT);

        VerticalLayout option = VerticalLayout.create();
        option.add(checkBox);
        option.add(description);
        return option;
    }

    private static boolean isPostCommit(CheckinHandler handler) {
        return handler instanceof CommitCheck check && check.getExecutionOrder() == CommitCheck.ExecutionOrder.POST_COMMIT;
    }

    @RequiredUIAccess
    public void restoreState() {
        for (RefreshableOnComponent option : myOptions) {
            option.restoreState();
        }
    }

    @RequiredUIAccess
    public void saveState() {
        for (RefreshableOnComponent option : myOptions) {
            option.saveState();
        }
    }

    @RequiredUIAccess
    private boolean add(VerticalLayout section, @Nullable RefreshableOnComponent option) {
        if (option == null) {
            return false;
        }

        try {
            if (option.getClass().getMethod("getUIComponent").getDeclaringClass() == RefreshableOnComponent.class) {
                LOG.warn("Commit option " + option.getClass().getName() + " has no unified component, skipped");
                return false;
            }
        }
        catch (NoSuchMethodException e) {
            return false;
        }

        option.restoreState();
        Component component = option.getUIComponent();
        if (component == null) {
            return false;
        }

        myOptions.add(option);
        section.add(component);
        return true;
    }
}
