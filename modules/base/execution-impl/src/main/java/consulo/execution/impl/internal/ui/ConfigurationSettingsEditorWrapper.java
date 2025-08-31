/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.application.ApplicationPropertiesComponent;
import consulo.configurable.ConfigurationException;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.dataContext.TypeSafeDataProviderAdapter;
import consulo.disposer.Disposer;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.UnknownRunConfiguration;
import consulo.ui.ex.awt.HideableDecorator;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.VerticalLayout;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * @author anna
 * @since 2006-03-27
 */
public class ConfigurationSettingsEditorWrapper extends SettingsEditor<RunnerAndConfigurationSettings> implements BeforeRunStepsPanel.StepsBeforeRunListener {
    public static Key<ConfigurationSettingsEditorWrapper> CONFIGURATION_EDITOR_KEY = Key.create("ConfigurationSettingsEditor");

    private static final String EXPAND_PROPERTY_KEY = "ExpandBeforeRunStepsPanel";

    private final JPanel myBeforeLaunchContainer;
    private BeforeRunStepsPanel myBeforeRunStepsPanel;

    private final ConfigurationSettingsEditor myEditor;
    private final HideableDecorator myDecorator;

    public ConfigurationSettingsEditorWrapper(RunnerAndConfigurationSettings settings) {
        myEditor = new ConfigurationSettingsEditor(settings);
        Disposer.register(this, myEditor);
        myBeforeRunStepsPanel = new BeforeRunStepsPanel(this);

        myBeforeLaunchContainer = new JPanel(new BorderLayout());
        myDecorator = new HideableDecorator(myBeforeLaunchContainer, "", false) {
            @Override
            protected void on() {
                super.on();
                storeState();
            }

            @Override
            protected void off() {
                super.off();
                storeState();
            }

            private void storeState() {
                ApplicationPropertiesComponent.getInstance().setValue(EXPAND_PROPERTY_KEY, String.valueOf(isExpanded()));
            }
        };
        myDecorator.setOn(ApplicationPropertiesComponent.getInstance().getBoolean(EXPAND_PROPERTY_KEY, true));
        myDecorator.setContentComponent(myBeforeRunStepsPanel.getPanel());
        doReset(settings);
    }

    private void doReset(RunnerAndConfigurationSettings settings) {
        RunConfiguration runConfiguration = settings.getConfiguration();
        myBeforeRunStepsPanel.doReset(settings);
        myBeforeLaunchContainer.setVisible(!(runConfiguration instanceof UnknownRunConfiguration));
    }

    @Override
    @Nonnull
    protected JComponent createEditor() {
        JPanel wholePanel = new JPanel(new VerticalLayout(JBUI.scale(5)));

        wholePanel.add(myEditor.getComponent());
        wholePanel.add(myBeforeLaunchContainer);

        DataManager.registerDataProvider(wholePanel, new TypeSafeDataProviderAdapter(new MyDataProvider()));
        return ScrollPaneFactory.createScrollPane(wholePanel, true);
    }

    @Override
    protected void disposeEditor() {
    }

    @Override
    public void resetEditorFrom(RunnerAndConfigurationSettings settings) {
        myEditor.resetEditorFrom(settings);
        doReset(settings);
    }

    @Override
    public void applyEditorTo(RunnerAndConfigurationSettings settings) throws ConfigurationException {
        myEditor.applyEditorTo(settings);
        doApply(settings);
    }

    @Override
    public RunnerAndConfigurationSettings getSnapshot() throws ConfigurationException {
        RunnerAndConfigurationSettings result = myEditor.getSnapshot();
        doApply(result);
        return result;
    }

    private void doApply(RunnerAndConfigurationSettings settings) {
        RunConfiguration runConfiguration = settings.getConfiguration();
        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(runConfiguration.getProject());
        runManager.setBeforeRunTasks(runConfiguration, myBeforeRunStepsPanel.getTasks(true), false);
        RunnerAndConfigurationSettings runManagerSettings = runManager.getSettings(runConfiguration);
        if (runManagerSettings != null) {
            runManagerSettings.setEditBeforeRun(myBeforeRunStepsPanel.needEditBeforeRun());
        }
        else {
            settings.setEditBeforeRun(myBeforeRunStepsPanel.needEditBeforeRun());
        }
    }

    public void addBeforeLaunchStep(BeforeRunTask<?> task) {
        myBeforeRunStepsPanel.addTask(task);
    }

    public List<BeforeRunTask> getStepsBeforeLaunch() {
        return Collections.unmodifiableList(myBeforeRunStepsPanel.getTasks(true));
    }

    @Override
    public void fireStepsBeforeRunChanged() {
        fireEditorStateChanged();
    }

    @Override
    public void titleChanged(String title) {
        myDecorator.setTitle(title);
    }

    private class MyDataProvider implements TypeSafeDataProvider {
        @Override
        public void calcData(Key key, DataSink sink) {
            if (CONFIGURATION_EDITOR_KEY == key) {
                sink.put(CONFIGURATION_EDITOR_KEY, ConfigurationSettingsEditorWrapper.this);
            }
        }
    }
}
