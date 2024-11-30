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
package consulo.desktop.awt.settings;

import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.ide.impl.configurable.ConfigurablePreselectStrategy;
import consulo.ide.impl.configurable.ProjectStructureSelectorOverSettings;
import consulo.ide.setting.ProjectStructureSelector;
import consulo.ide.setting.Settings;
import consulo.platform.Platform;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.CustomLineBorder;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.WholeWestDialogWrapper;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class DesktopSettingsDialog extends WholeWestDialogWrapper implements DataProvider {

    private Project myProject;
    private final Function<Project, Configurable[]> myConfigurablesBuilder;
    private ConfigurablePreselectStrategy myPreselectStrategy;
    private OptionsEditor myEditor;

    private ApplyAction myApplyAction;
    public static final String DIMENSION_KEY = "OptionsEditor";

    /**
     * This constructor should be eliminated after the new modality approach
     * will have been checked. See a {@code Registry} key ide.mac.modalDialogsOnFullscreen
     *
     * @deprecated
     */
    public DesktopSettingsDialog(Project project,
                                 Function<Project, Configurable[]> configurablesBuilder,
                                 @Nonnull ConfigurablePreselectStrategy strategy,
                                 boolean applicationModalIfPossible
    ) {
        super(true, applicationModalIfPossible);
        myProject = project;
        myConfigurablesBuilder = configurablesBuilder;
        myPreselectStrategy = strategy;
        setTitle(Platform.current().os().isMac() ? CommonLocalize.titleSettingsMac() : CommonLocalize.titleSettings());
        init();
    }

    public DesktopSettingsDialog(Project project,
                                 Function<Project, Configurable[]> configurablesBuilder,
                                 @Nonnull ConfigurablePreselectStrategy strategy) {
        super(project, true);
        myProject = project;
        myConfigurablesBuilder = configurablesBuilder;
        myPreselectStrategy = strategy;
        setTitle(Platform.current().os().isMac() ? CommonLocalize.titleSettingsMac() : CommonLocalize.titleSettings());
        init();
    }

    @Nonnull
    @Override
    public String getSplitterKey() {
        return OptionsEditor.MAIN_SPLITTER_PROPORTION;
    }

    @Override
    public Size getDefaultSize() {
        return new Size(1028, 500);
    }

    @Nullable
    @Override
    protected Border createContentPaneBorder() {
        return JBUI.Borders.empty();
    }

    @Nullable
    @Override
    protected JComponent createSouthPanel() {
        JComponent southPanel = super.createSouthPanel();
        if (southPanel != null) {
            southPanel.setBorder(JBUI.Borders.empty(ourDefaultBorderInsets));
            BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel(southPanel);
            borderLayoutPanel.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
            return borderLayoutPanel;
        }
        return null;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Couple<JComponent> createSplitterComponents(JPanel rootPanel) {
        myEditor = new OptionsEditor(myProject, myConfigurablesBuilder, myPreselectStrategy, rootPanel);
        myEditor.getContext().addColleague(new OptionsEditorColleague() {
            @Override
            public AsyncResult<Void> onModifiedAdded(final Configurable configurable) {
                updateStatus();
                return AsyncResult.resolved();
            }

            @Override
            public AsyncResult<Void> onModifiedRemoved(final Configurable configurable) {
                updateStatus();
                return AsyncResult.resolved();
            }

            @Override
            public AsyncResult<Void> onErrorsChanged() {
                updateStatus();
                return AsyncResult.resolved();
            }
        });
        Disposer.register(myDisposable, myEditor);
        return Couple.of(myEditor.getLeftSide(), myEditor.getRightSide());
    }

    public boolean updateStatus() {
        myApplyAction.setEnabled(myEditor.canApply());

        final Map<Configurable, ConfigurationException> errors = myEditor.getContext().getErrors();
        if (errors.size() == 0) {
            clearErrorText();
        }
        else {
            String text = "Changes were not applied because of an error";

            final String errorMessage = getErrorMessage(errors);
            if (errorMessage != null) {
                text += "<br>" + errorMessage;
            }

            setErrorText(text);
        }

        return errors.size() == 0;
    }

    @Nullable
    private static String getErrorMessage(final Map<Configurable, ConfigurationException> errors) {
        final Collection<ConfigurationException> values = errors.values();
        final ConfigurationException[] exceptions = values.toArray(new ConfigurationException[values.size()]);
        if (exceptions.length > 0) {
            return exceptions[0].getMessage();
        }
        return null;
    }

    @Override
    protected String getDimensionServiceKey() {
        return DIMENSION_KEY;
    }

    @Override
    @RequiredUIAccess
    public void doOKAction() {
        myEditor.flushModifications();

        if (myEditor.canApply()) {
            myEditor.apply();
            if (!updateStatus()) {
                return;
            }
        }

        saveCurrentConfigurable();

        Application.get().saveAll();

        super.doOKAction();
    }

    private void saveCurrentConfigurable() {
        final Configurable current = myEditor.getContext().getCurrentConfigurable();
        if (current == null) {
            return;
        }

        myPreselectStrategy.save(current);
    }

    @Override
    public void doCancelAction(final AWTEvent source) {
        if (source instanceof KeyEvent || source instanceof ActionEvent) {
            if (myEditor.getContext().isHoldingFilter()) {
                myEditor.clearFilter();
                return;
            }
        }

        super.doCancelAction(source);
    }

    @Override
    public void doCancelAction() {
        saveCurrentConfigurable();
        super.doCancelAction();
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        myApplyAction = new ApplyAction();
        return new Action[]{getOKAction(), getCancelAction(), myApplyAction, getHelpAction()};
    }

    @Override
    @RequiredUIAccess
    protected void doHelpAction() {
        final String topic = myEditor.getHelpTopic();
        HelpManager.getInstance().invokeHelp(topic);
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myEditor.getPreferredFocusedComponent();
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (Settings.KEY == dataId) {
            return myEditor;
        }
        else if (ProjectStructureSelector.KEY == dataId) {
            return new ProjectStructureSelectorOverSettings(myEditor);
        }
        return null;
    }

    private class ApplyAction extends DialogWrapperAction {
        public ApplyAction() {
            super(CommonLocalize.buttonApply());
            setEnabled(false);
        }

        @Override
        @RequiredUIAccess
        protected void doAction(ActionEvent e) {
            myEditor.apply();
            myEditor.repaint();
        }
    }
}
