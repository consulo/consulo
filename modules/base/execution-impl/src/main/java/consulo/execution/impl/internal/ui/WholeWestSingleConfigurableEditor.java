/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.execution.impl.internal.ui;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.CommonBundle;
import consulo.application.HelpManager;
import consulo.application.dumb.IndexNotReadyException;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeFocusTraversalPolicy;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.WholeWestDialogWrapper;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class WholeWestSingleConfigurableEditor extends WholeWestDialogWrapper {
    private static final Logger LOG = Logger.getInstance(WholeWestSingleConfigurableEditor.class);
    private Project myProject;
    private Component myParentComponent;
    private Configurable myConfigurable;
    private String myDimensionKey;
    private final boolean myShowApplyButton;
    private boolean myChangesWereApplied;
    private JPanel myRootPanel;

    public WholeWestSingleConfigurableEditor(@Nullable Project project,
                                             Configurable configurable,
                                             @NonNls String dimensionKey,
                                             final boolean showApplyButton,
                                             final IdeModalityType ideModalityType,
                                             boolean doNotCallInit) {
        super(project, true, ideModalityType);
        myDimensionKey = dimensionKey;
        myShowApplyButton = showApplyButton;
        setTitle(createTitleString(configurable));

        myProject = project;
        myConfigurable = configurable;
    }

    public WholeWestSingleConfigurableEditor(@Nullable Project project,
                                             Configurable configurable,
                                             @NonNls String dimensionKey,
                                             final boolean showApplyButton,
                                             final IdeModalityType ideModalityType) {
        super(project, true, ideModalityType);
        myDimensionKey = dimensionKey;
        myShowApplyButton = showApplyButton;
        setTitle(createTitleString(configurable));

        myProject = project;
        myConfigurable = configurable;
        init();
        myConfigurable.reset();
    }

    public WholeWestSingleConfigurableEditor(Component parent,
                                             Configurable configurable,
                                             String dimensionServiceKey,
                                             final boolean showApplyButton,
                                             final IdeModalityType ideModalityType) {
        super(parent, true);
        myDimensionKey = dimensionServiceKey;
        myShowApplyButton = showApplyButton;
        setTitle(createTitleString(configurable));

        myParentComponent = parent;
        myConfigurable = configurable;
        init();
        myConfigurable.reset();
    }

    public WholeWestSingleConfigurableEditor(@Nullable Project project, Configurable configurable, @NonNls String dimensionKey, final boolean showApplyButton) {
        this(project, configurable, dimensionKey, showApplyButton, IdeModalityType.IDE);
    }

    public WholeWestSingleConfigurableEditor(Component parent, Configurable configurable, String dimensionServiceKey, final boolean showApplyButton) {
        this(parent, configurable, dimensionServiceKey, showApplyButton, IdeModalityType.IDE);
    }

    public WholeWestSingleConfigurableEditor(@Nullable Project project, Configurable configurable, @NonNls String dimensionKey, IdeModalityType ideModalityType) {
        this(project, configurable, dimensionKey, true, ideModalityType);
    }

    public WholeWestSingleConfigurableEditor(@Nullable Project project, Configurable configurable, @NonNls String dimensionKey) {
        this(project, configurable, dimensionKey, true);
    }

    public WholeWestSingleConfigurableEditor(Component parent, Configurable configurable, String dimensionServiceKey) {
        this(parent, configurable, dimensionServiceKey, true);
    }

    public WholeWestSingleConfigurableEditor(@Nullable Project project, Configurable configurable, IdeModalityType ideModalityType) {
        this(project, configurable, createDimensionKey(configurable), ideModalityType);
    }

    public WholeWestSingleConfigurableEditor(@Nullable Project project, Configurable configurable) {
        this(project, configurable, createDimensionKey(configurable));
    }

    public WholeWestSingleConfigurableEditor(Component parent, Configurable configurable) {
        this(parent, configurable, createDimensionKey(configurable));
    }

    public Configurable getConfigurable() {
        return myConfigurable;
    }

    public Project getProject() {
        return myProject;
    }

    private static LocalizeValue createTitleString(Configurable configurable) {
        LocalizeValue displayName = configurable.getDisplayName();
        LOG.assertTrue(displayName != LocalizeValue.of(), configurable.getClass().getName());
        return displayName.map((localizeManager, s) -> s.replaceAll("\n", " "));
    }

    @Override
    protected String getDimensionServiceKey() {
        if (myDimensionKey == null) {
            return super.getDimensionServiceKey();
        }
        else {
            return myDimensionKey;
        }
    }

    @Override
    @Nonnull
    protected Action[] createActions() {
        List<Action> actions = new ArrayList<Action>();
        actions.add(getOKAction());
        actions.add(getCancelAction());
        if (myShowApplyButton) {
            actions.add(new ApplyAction());
        }
        if (myConfigurable.getHelpTopic() != null) {
            actions.add(getHelpAction());
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @RequiredUIAccess
    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp(myConfigurable.getHelpTopic());
    }

    @Override
    public void doCancelAction() {
        if (myChangesWereApplied) {
            ApplicationManager.getApplication().saveAll();
        }
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        try {
            if (myConfigurable.isModified()) {
                myConfigurable.apply();
            }

            ApplicationManager.getApplication().saveAll();
        }
        catch (ConfigurationException e) {
            if (e.getMessage() != null) {
                if (myProject != null) {
                    Messages.showMessageDialog(myProject, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
                }
                else {
                    Messages.showMessageDialog(myParentComponent, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
                }
            }
            return;
        }
        super.doOKAction();
    }

    protected static String createDimensionKey(Configurable configurable) {
        LocalizeKey localizeKey = configurable.getDisplayName().getKey().orElseThrow();
        return "#" + localizeKey.getLocalizeId() + "@" + localizeKey.getKey();
    }

    protected class ApplyAction extends DialogWrapperAction {
        private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

        public ApplyAction() {
            super(CommonLocalize.buttonApply());
            final Runnable updateRequest = new Runnable() {
                @Override
                public void run() {
                    if (!WholeWestSingleConfigurableEditor.this.isShowing()) {
                        return;
                    }
                    try {
                        ApplyAction.this.setEnabled(myConfigurable != null && myConfigurable.isModified());
                    }
                    catch (IndexNotReadyException ignored) {
                    }
                    addUpdateRequest(this);
                }
            };

            // invokeLater necessary to make sure dialog is already shown so we calculate modality state correctly.
            SwingUtilities.invokeLater(() -> addUpdateRequest(updateRequest));
        }

        private void addUpdateRequest(final Runnable updateRequest) {
            myUpdateAlarm.addRequest(updateRequest, 500, Application.get().getModalityStateForComponent(getWindow()));
        }

        @Override
        @RequiredUIAccess
        protected void doAction(ActionEvent e) {
            try {
                if (myConfigurable.isModified()) {
                    myConfigurable.apply();
                    myChangesWereApplied = true;
                    setCancelButtonText(CommonBundle.getCloseButtonText());
                }
            }
            catch (ConfigurationException ex) {
                if (myProject != null) {
                    Messages.showMessageDialog(myProject, ex.getMessage(), ex.getTitle(), Messages.getErrorIcon());
                }
                else {
                    Messages.showMessageDialog(myParentComponent, ex.getMessage(), ex.getTitle(), Messages.getErrorIcon());
                }
            }
        }
    }

    @RequiredUIAccess
    @Override
    protected void initRootPanel(@Nonnull JPanel rootPanel) {
        myRootPanel = rootPanel;
        super.initRootPanel(rootPanel);
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        if (myConfigurable == null) {
            return null;
        }
        
        JComponent preferred = ConfigurableUIMigrationUtil.getPreferredFocusedComponent(myConfigurable);
        if (preferred != null) {
            return preferred;
        }
        return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myRootPanel);
    }

    @Override
    public void dispose() {
        super.dispose();
        myConfigurable.disposeUIResources();
        myConfigurable = null;
    }
}
