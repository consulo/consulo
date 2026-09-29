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

import consulo.configurable.ConfigurationException;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposer;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.internal.ConfigurationSettingsEditorWrapper;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author anna
 * @since 2006-03-27
 */
public class ConfigurationSettingsEditorWrapperImpl extends ConfigurationSettingsEditorWrapper {
    private final ConfigurationSettingsEditor myEditor;
    private final BeforeRunStepsPanel myBeforeRunStepsPanel;

    private @Nullable Element myBaselineElement;

    @RequiredUIAccess
    public ConfigurationSettingsEditorWrapperImpl(RunnerAndConfigurationSettings settings) {
        myEditor = new ConfigurationSettingsEditor(settings);
        Disposer.register(this, myEditor);
        myEditor.addSettingsEditorListener(editor -> fireEditorStateChanged());
        myBeforeRunStepsPanel = new BeforeRunStepsPanel(settings, this::fireEditorStateChanged);
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        DockLayout layout = DockLayout.create();
        layout.top(myEditor.getUIComponent());
        layout.center(myBeforeRunStepsPanel.createComponent());
        layout.putUserData(UiDataProvider.KEY, sink -> sink.set(CONFIGURATION_EDITOR_KEY, this));
        return layout;
    }

    @RequiredUIAccess
    @Override
    public void resetEditorFrom(RunnerAndConfigurationSettings settings) {
        myEditor.resetFrom(settings);
        myBeforeRunStepsPanel.reset(settings);
        myBaselineElement = writeSnapshotElement();
    }

    @Override
    public void applyEditorTo(RunnerAndConfigurationSettings settings) throws ConfigurationException {
        myEditor.applyTo(settings);
        doApply(settings);
        myBaselineElement = writeElement(settings.getConfiguration());
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

    @RequiredUIAccess
    boolean isModified(RunnerAndConfigurationSettings settings) {
        try {
            RunnerAndConfigurationSettings snapshot = myEditor.getSnapshot();
            Element baselineElement = myBaselineElement != null ? myBaselineElement : writeElement(settings.getConfiguration());
            Element snapshotElement = writeElement(snapshot.getConfiguration());
            return baselineElement == null || snapshotElement == null || !JDOMUtil.areElementsEqual(baselineElement, snapshotElement);
        }
        catch (ConfigurationException e) {
            return true;
        }
    }

    private @Nullable Element writeSnapshotElement() {
        try {
            return writeElement(myEditor.getSnapshot().getConfiguration());
        }
        catch (ConfigurationException e) {
            return null;
        }
    }

    static @Nullable Element writeElement(RunConfiguration configuration) {
        try {
            Element element = new Element("configuration");
            configuration.writeExternal(element);
            return element;
        }
        catch (WriteExternalException e) {
            return null;
        }
    }

    @RequiredUIAccess
    @Override
    public void addBeforeLaunchStep(BeforeRunTask<?> task) {
        myBeforeRunStepsPanel.addTask(task);
    }

    @Override
    public List<BeforeRunTask> getStepsBeforeLaunch() {
        return myBeforeRunStepsPanel.getTasks(true);
    }
}
